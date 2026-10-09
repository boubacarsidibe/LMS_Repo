import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {
  RecrutementService,
  CandidatureResponse,
  ParcoursCandidature,
  EntretienCandidat,
  PieceJustificativeResponse,
  TestCandidat,
} from '../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { downloadBlob } from '../../../shared/utils/download-file';
import { NotificationService } from '../../../shared/components/ui/notification.service';

interface TimelineItem {
  label: string;
  desc: string;
  pending: boolean;
  active: boolean;
  erreur: boolean;
}

interface DocumentChecklistItem {
  type: string;
  label: string;
  statut: 'A_FOURNIR' | 'A_VERIFIER' | 'CONFORME' | 'A_REMPLACER';
  fichierNomOriginal: string | null;
  moyenneDeclaree: number | null;
  motifNonConformite: string | null;
}

@Component({
  selector: 'app-suivi',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FooterComponent, NavbarComponent],
  templateUrl: './suivi.component.html'
})
export class SuiviComponent implements OnInit {
  candidature: CandidatureResponse | null = null;
  loading = true;

  /** Étapes disponibles selon le statut */
  readonly STATUT_SOUMIS = 'SOUMIS';
  readonly STATUT_PRESELECTION = 'EN_COURS_DE_PRESELECTION';
  readonly STATUT_REJETE = 'REJETE';
  readonly STATUT_ADMIS1 = 'ADMIS1';
  readonly STATUT_ADMIS2 = 'ADMIS2';
  readonly STATUT_ADMIS3 = 'ADMIS3';
  readonly STATUT_LISTE_ATTENTE = 'LISTE_ATTENTE';

  /** Même liste que la relance serveur : aucun document requis n'est caché au candidat. */
  private readonly documentsRequis = [
    { type: 'PIECE_IDENTITE', label: "Pièce d'identité" },
    { type: 'CV', label: 'CV' },
    { type: 'RELEVE_NOTES', label: 'Relevé de notes' },
  ];

  /**
   * Une pièce déposée ne signifie pas « validée ». La checklist présente une action
   * claire : fournir, attendre le contrôle, ou remplacer avec le motif de l'agent.
   */
  get checklistDocuments(): DocumentChecklistItem[] {
    const pieces = this.candidature?.piecesJustificatives ?? [];
    return this.documentsRequis.map((document) => {
      const duType = pieces.filter((piece) => piece.typeDocument === document.type);
      const conforme = duType.find((piece) => piece.statutVerification === 'CONFORME');
      const aVerifier = duType.find((piece) => piece.statutVerification === 'A_VERIFIER');
      const aRemplacer = duType.find((piece) => piece.statutVerification === 'A_REMPLACER');
      const piece = conforme ?? aVerifier ?? aRemplacer;
      const statut = conforme
        ? 'CONFORME'
        : aVerifier
          ? 'A_VERIFIER'
          : aRemplacer
            ? 'A_REMPLACER'
            : 'A_FOURNIR';

      return {
        type: document.type,
        label: document.label,
        statut,
        fichierNomOriginal: piece?.fichierNomOriginal ?? null,
        moyenneDeclaree: piece?.moyenneDeclaree ?? null,
        motifNonConformite: statut === 'A_REMPLACER' ? piece?.motifNonConformite ?? null : null,
      };
    });
  }

  /**
   * Interlocuteur affiché. Aucun nom de personne n'est inventé : le dossier n'est
   * rattaché à aucun agent en base, seul le service l'est.
   */
  readonly responsableAdmissions = 'Service des admissions';

  /**
   * Parcours réel du candidat. Ces valeurs étaient auparavant codées en dur dans
   * le composant — un score de 88, un seuil de 60, un entretien le « Mardi 24 Oct.
   * » — alors que les tables tests_academiques et entretiens contenaient les
   * vraies données et n'étaient jamais lues.
   */
  parcours: ParcoursCandidature | null = null;

  /** Note globale pondérée par les coefficients, ou null si aucune épreuve saisie. */
  /**
   * Ajout d'un document depuis le suivi — l'endpoint acceptait déjà une pièce sur
   * une candidature déposée, sans qu'aucun écran n'y mène. Sert notamment à
   * rattraper un document qui aurait échoué silencieusement au dépôt initial.
   */
  typeDocumentAAjouter = '';
  ajoutPieceEnCours = false;
  ajoutPieceErreur = '';
  ajoutPieceSucces = false;

  ajouterPiece(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.candidature?.id || !this.typeDocumentAAjouter) return;

    const MAX_SIZE_MO = 5;
    if (file.size > MAX_SIZE_MO * 1024 * 1024) {
      this.ajoutPieceErreur = `Le fichier dépasse la taille maximale autorisée (${MAX_SIZE_MO} Mo).`;
      this.ajoutPieceSucces = false;
      input.value = '';
      return;
    }

    this.ajoutPieceEnCours = true;
    this.ajoutPieceErreur = '';
    this.ajoutPieceSucces = false;
    this.recrutementService.televerserPiece(this.candidature.id, file, this.typeDocumentAAjouter).subscribe({
      next: () => {
        this.ajoutPieceEnCours = false;
        this.ajoutPieceSucces = true;
        this.typeDocumentAAjouter = '';
        input.value = '';
        this.rafraichirCandidature();
      },
      error: (err) => {
        this.ajoutPieceEnCours = false;
        this.ajoutPieceErreur = err.error?.message || "Échec de l'envoi du document.";
        input.value = '';
      },
    });
  }

  private rafraichirCandidature(): void {
    this.recrutementService.getMesCandidatures().subscribe({
      next: (data) => {
        this.candidature = data.length > 0 ? data[data.length - 1] : null;
      },
    });
  }

  get paiementConfirme(): boolean {
    return this.candidature?.paiement?.statut === 'SUCCES';
  }

  get paiementEnAttente(): boolean {
    return this.candidature?.paiement?.statut === 'EN_ATTENTE';
  }

  get paiementEchoue(): boolean {
    return this.candidature?.paiement?.statut === 'ECHEC';
  }

  get noteGlobale(): number | null {
    const tests = (this.parcours?.tests ?? []).filter((t) => t.noteObtenue != null);
    if (tests.length === 0) return null;
    const poidsTotal = tests.reduce((s, t) => s + (t.coefficient ?? 1), 0);
    if (poidsTotal === 0) return null;
    const somme = tests.reduce((s, t) => s + (t.noteObtenue ?? 0) * (t.coefficient ?? 1), 0);
    return Math.round((somme / poidsTotal) * 100) / 100;
  }

  get seuilScore(): number | null {
    return this.parcours?.seuilReussiteTest ?? null;
  }

  get aDesResultats(): boolean {
    return this.noteGlobale !== null;
  }

  /** Prochain entretien planifié, s'il existe. */
  get entretien(): EntretienCandidat | null {
    const entretiens = this.parcours?.entretiens ?? [];
    return entretiens.length > 0 ? entretiens[entretiens.length - 1] : null;
  }

  get lieuEntretien(): string {
    return this.entretien?.lieuOuLien || 'Lieu communiqué par courriel';
  }

  /**
   * L'entité Entretien n'a pas de champ « modalité » dédié : lieuOuLien porte soit une
   * adresse (présentiel), soit un lien de visioconférence, selon ce que l'admin y a saisi.
   * Les deux cartes Présentiel/Visio étaient jusqu'ici deux boutons radio statiques, l'un
   * toujours pré-coché, sans lien avec l'entretien réellement planifié.
   */
  get estVisio(): boolean {
    const lieu = this.entretien?.lieuOuLien || '';
    return /^https?:\/\//i.test(lieu.trim());
  }

  get mapUrl(): string {
    const lieu = this.entretien?.lieuOuLien;
    return lieu
      ? 'https://www.google.com/maps/search/' + encodeURIComponent(lieu)
      : 'https://www.google.com/maps/search/INPG+Dakar';
  }

  /** Prochaine épreuve non encore notée (convocation en attente de passage). */
  get prochainTest(): TestCandidat | null {
    const tests = (this.parcours?.tests ?? []).filter((t) => t.noteObtenue == null);
    return tests.length > 0 ? tests[tests.length - 1] : null;
  }

  constructor(private recrutementService: RecrutementService, private notifications: NotificationService) {}

  ngOnInit(): void {
    this.recrutementService.getMesCandidatures().subscribe({
      next: (data) => {
        // Prendre la candidature la plus récente (tous statuts)
        this.candidature = data.length > 0 ? data[data.length - 1] : null;
        this.loading = false;
        if (this.candidature?.id) {
          this.recrutementService.getParcoursCandidature(this.candidature.id).subscribe({
            next: (parcours) => (this.parcours = parcours),
            // Absence de parcours = aucune épreuve encore saisie : les blocs
            // correspondants restent simplement masqués.
            error: () => (this.parcours = null),
          });
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  /** Vérifie si le statut courant correspond à l'un des statuts listés */
  isStatut(...statuts: string[]): boolean {
    return statuts.includes(this.candidature?.statut ?? '');
  }

  /**
   * Ordre réel du parcours (TDR) : dépôt → évaluation/présélection → paiement des frais
   * de dossier → test académique → entretien. « Paiement confirmé » et « Dossier soumis »
   * étaient auparavant codés en dur comme toujours acquis (jamais mis à jour selon le
   * statut réel) et affichés AVANT « Dossier soumis » — un candidat qui n'avait encore
   * rien déposé ni payé voyait déjà les deux coches vertes.
   */
  get timelineItems(): TimelineItem[] {
    const statut = this.candidature?.statut;
    const paiementConfirme = this.paiementConfirme;
    const items: TimelineItem[] = [
      { label: 'Dossier', desc: 'Tous les documents requis téléchargés.', pending: true, active: false, erreur: false },
      { label: 'Présélection', desc: 'Le jury examine votre candidature.', pending: true, active: false, erreur: false },
      { label: 'Paiement', desc: 'Frais de dossier réglés.', pending: true, active: false, erreur: false },
      { label: 'Test Académique', desc: '', pending: true, active: false, erreur: false },
      { label: 'Entretien', desc: 'Rencontre avec le jury.', pending: true, active: false, erreur: false },
    ];

    if (!statut || statut === 'BROUILLON') {
      items[0].active = true;
      return items;
    }
    items[0].pending = false; // dossier déposé

    if (statut === this.STATUT_SOUMIS || statut === this.STATUT_PRESELECTION) {
      items[1].pending = false;
      items[1].active = true;
      return items;
    }
    items[1].pending = false; // évaluation/présélection franchie

    if (statut === this.STATUT_REJETE && !paiementConfirme) {
      // Rejeté dès le filtrage ou la présélection, avant tout paiement.
      items[1].erreur = true;
      return items;
    }

    if (!paiementConfirme) {
      items[2].pending = false;
      items[2].active = true;
      return items;
    }
    items[2].pending = false; // paiement confirmé

    if (statut === this.STATUT_REJETE) {
      // Rejeté après paiement : la note insuffisante au test reste la cause la plus
      // fréquente, faute d'un champ dédié à l'étape exacte du rejet.
      // `pending` doit retomber à false comme pour l'échec de présélection plus haut :
      // une étape en échec est franchie, pas en attente. Sans cela le gabarit affichait
      // à la fois le numéro d'étape et la croix, et l'étape était grisée au lieu de rouge.
      items[3].pending = false;
      items[3].erreur = true;
      return items;
    }

    if (statut === this.STATUT_ADMIS1) {
      items[3].pending = false;
      items[3].active = true;
      return items;
    }
    items[3].pending = false; // test académique franchi

    if (statut === this.STATUT_ADMIS2) {
      items[4].pending = false;
      items[4].active = true;
      return items;
    }

    // ADMIS3 (admis final) ou LISTE_ATTENTE (entretien passé, décision en attente d'une place)
    if (statut === this.STATUT_ADMIS3 || statut === this.STATUT_LISTE_ATTENTE) {
      items[4].pending = false;
      return items;
    }

    return items;
  }

  getDocIcon(type: string): string {
    switch (type) {
      case 'CV': return 'article';
      case 'PIECE_IDENTITE': return 'badge';
      case 'LETTRE_MOTIVATION': return 'edit_document';
      case 'RELEVE_NOTES': return 'description';
      case 'DIPLOME': return 'school';
      default: return 'description';
    }
  }

  getDocLabel(type: string): string {
    switch (type) {
      case 'CV': return 'CV Professionnel';
      case 'PIECE_IDENTITE': return 'Pièce d\'identité';
      case 'LETTRE_MOTIVATION': return 'Lettre de motivation';
      case 'RELEVE_NOTES': return 'Relevé de notes';
      case 'DIPLOME': return 'Diplôme';
      default: return type;
    }
  }

  libelleEtatPiece(statut: DocumentChecklistItem['statut']): string {
    switch (statut) {
      case 'CONFORME': return 'Validé';
      case 'A_VERIFIER': return 'À vérifier';
      case 'A_REMPLACER': return 'À remplacer';
      default: return 'À fournir';
    }
  }

  classeEtatPiece(statut: DocumentChecklistItem['statut']): string {
    switch (statut) {
      case 'CONFORME': return 'bg-success/15 text-success-strong';
      case 'A_VERIFIER': return 'bg-warning/15 text-warning-strong';
      case 'A_REMPLACER': return 'bg-error-container text-on-error-container';
      default: return 'bg-surface-container-high text-on-surface-variant';
    }
  }

  iconeEtatPiece(statut: DocumentChecklistItem['statut']): string {
    switch (statut) {
      case 'CONFORME': return 'check_circle';
      case 'A_VERIFIER': return 'hourglass_top';
      case 'A_REMPLACER': return 'error';
      default: return 'upload_file';
    }
  }

  getStatutBadgeClass(): string {
    const s = this.candidature?.statut;
    if (s === this.STATUT_REJETE) return 'bg-error-container text-on-error-container';
    if (s === this.STATUT_LISTE_ATTENTE) return 'bg-primary-container text-primary';
    if (s === this.STATUT_ADMIS1 || s === this.STATUT_ADMIS2 || s === this.STATUT_ADMIS3) return 'bg-success/15 text-success-strong';
    return 'bg-secondary-container text-on-secondary-container';
  }

  getStatutBadgeLabel(): string {
    const s = this.candidature?.statut;
    switch (s) {
      case this.STATUT_SOUMIS: return 'Candidature en cours';
      case this.STATUT_PRESELECTION: return 'En cours de présélection';
      case this.STATUT_REJETE: return 'Test non concluant';
      case this.STATUT_ADMIS1: return 'Test réussi';
      case this.STATUT_ADMIS2: return 'Entretien réussi';
      case this.STATUT_LISTE_ATTENTE: return 'Liste complémentaire';
      case this.STATUT_ADMIS3: return 'ADMIS';
      default: return 'Candidature en cours';
    }
  }

  getHeaderTitle(): string {
    const s = this.candidature?.statut;
    switch (s) {
      case this.STATUT_REJETE: return 'Résultats du Test Académique';
      case this.STATUT_ADMIS1: return 'Résultats du Test Académique';
      case this.STATUT_ADMIS2: return 'Tu y es presque !';
      case this.STATUT_LISTE_ATTENTE: return 'Vous êtes sur liste complémentaire';
      case this.STATUT_ADMIS3: return 'Félicitations ! Bienvenue à l\'INPG';
      default: return 'Votre dossier est en cours d\'évaluation.';
    }
  }

  getHeaderDescription(): string {
    const s = this.candidature?.statut;
    switch (s) {
      case this.STATUT_SOUMIS:
      case this.STATUT_PRESELECTION:
        return 'Félicitations pour avoir complété la première étape. Notre jury d\'admission examine actuellement vos documents avec la plus grande attention.';
      case this.STATUT_REJETE:
        return 'Malgré tout l\'intérêt que nous portons à votre candidature pour le Bachelor in Strategic Management, nous regrettons de vous informer que vos résultats au test ne nous permettent pas de poursuivre le processus d\'admission pour cette session.';
      case this.STATUT_ADMIS1:
        return 'Félicitations ! Vous avez brillamment réussi le test académique. La prochaine étape est l\'entretien.';
      case this.STATUT_ADMIS2:
        return 'Ton parcours de sélection avance avec succès. Ton dossier a retenu toute notre attention.';
      case this.STATUT_LISTE_ATTENTE:
        return 'Votre dossier a été jugé valable par le jury, mais aucune place n\'est disponible pour l\'instant. Nous reviendrons vers vous si une place se libère.';
      case this.STATUT_ADMIS3:
        return 'Votre candidature a été officiellement acceptée. Vous faites désormais partie de la promotion 2024-2025.';
      default: return '';
    }
  }

  /** Étape actuelle dans le parcours (pour admis2/admis3) — synchronisée avec timelineItems (5 étapes). */
  get etapeActuelle(): string {
    const s = this.candidature?.statut;
    if (s === this.STATUT_ADMIS2) return 'Étape 4 sur 5';
    if (s === this.STATUT_LISTE_ATTENTE || s === this.STATUT_ADMIS3) return 'Étape 5 sur 5';
    return '';
  }

  actionEntretienEnCours = false;

  /**
   * Confirme la présence à l'entretien. EntretienService#confirmerPresence existait déjà
   * côté backend (avec vérification de propriété), mais aucune route n'y menait — le
   * bouton se contentait jusqu'ici d'un message "bientôt disponible".
   */
  confirmerPresence(): void {
    const ent = this.entretien;
    if (!ent) return;
    this.actionEntretienEnCours = true;
    this.recrutementService.confirmerPresenceEntretien(ent.id).subscribe({
      next: () => {
        this.actionEntretienEnCours = false;
        this.rafraichirParcours();
      },
      error: (err) => {
        this.actionEntretienEnCours = false;
        this.notifications.erreur(err.error?.message || 'Échec de la confirmation de présence.');
      },
    });
  }

  /** Demande de report de créneau — même constat que confirmerPresence. */
  demanderReport(): void {
    const ent = this.entretien;
    if (!ent) return;
    const motif = window.prompt('Motif de la demande de report :');
    if (motif === null) return;
    this.actionEntretienEnCours = true;
    this.recrutementService.demanderReportEntretien(ent.id, motif).subscribe({
      next: () => {
        this.actionEntretienEnCours = false;
        this.rafraichirParcours();
        this.notifications.succes('Votre demande de report a été transmise au service admission.');
      },
      error: (err) => {
        this.actionEntretienEnCours = false;
        this.notifications.erreur(err.error?.message || 'Échec de la demande de report.');
      },
    });
  }

  /** Convocation PDF — ConvocationPdfService générait déjà le document, sans route pour l'atteindre. */
  telechargerConvocation(): void {
    const ent = this.entretien;
    if (!ent) return;
    this.recrutementService.telechargerConvocationEntretien(ent.id).subscribe({
      next: (blob) => downloadBlob(blob, 'convocation-entretien.pdf'),
      error: () => this.notifications.erreur('Échec du téléchargement de la convocation.'),
    });
  }

  /** Lettre d'admission PDF — LettreAdmissionPdfService générait déjà le document, sans route pour l'atteindre. */
  telechargerLettreAdmission(): void {
    if (!this.candidature?.id) return;
    this.recrutementService.telechargerLettreAdmission(this.candidature.id).subscribe({
      next: (blob) => downloadBlob(blob, 'lettre-admission.pdf'),
      error: () => this.notifications.erreur('Échec du téléchargement de la lettre.'),
    });
  }

  /** Web Share API avec repli presse-papiers : aucun document public à partager, seulement l'annonce. */
  partagerAdmission(): void {
    const texte = `Je suis admis(e) à l'INPG pour la formation ${this.candidature?.sessionCandidatureLibelle || ''} !`.trim();
    if (navigator.share) {
      navigator.share({ text: texte }).catch(() => {});
      return;
    }
    navigator.clipboard?.writeText(texte).then(
      () => this.notifications.succes('Message copié dans le presse-papiers.'),
      () => this.notifications.erreur('Impossible de partager.'),
    );
  }

  /** Fichier .ics généré côté client : aucune intégration calendrier tierce nécessaire. */
  ajouterAgenda(): void {
    const ent = this.entretien;
    if (!ent?.dateHeure) return;
    const debut = new Date(ent.dateHeure);
    const fin = new Date(debut.getTime() + 30 * 60 * 1000);
    const fmt = (d: Date) => d.toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z';
    const ics = [
      'BEGIN:VCALENDAR',
      'VERSION:2.0',
      'BEGIN:VEVENT',
      `DTSTART:${fmt(debut)}`,
      `DTEND:${fmt(fin)}`,
      'SUMMARY:Entretien d\'admission INPG',
      `LOCATION:${(ent.lieuOuLien || '').replace(/\r?\n/g, ' ')}`,
      'END:VEVENT',
      'END:VCALENDAR',
    ].join('\r\n');
    downloadBlob(new Blob([ics], { type: 'text/calendar' }), 'entretien-inpg.ics');
  }

  private rafraichirParcours(): void {
    if (!this.candidature?.id) return;
    this.recrutementService.getParcoursCandidature(this.candidature.id).subscribe({
      next: (parcours) => (this.parcours = parcours),
      error: () => {},
    });
  }
}
