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
import {
  etapeActuelle as etapeActuelleDe,
  friseParcours,
  messageCandidat,
  StatutCandidature,
  TimelineItem,
} from '../../../core/recrutement/statuts-candidature';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { downloadBlob } from '../../../shared/utils/download-file';
import { NotificationService } from '../../../shared/components/ui/notification.service';

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
  imports: [CommonModule, FormsModule, RouterModule, FooterComponent],
  templateUrl: './suivi.component.html'
})
export class SuiviComponent implements OnInit {
  candidature: CandidatureResponse | null = null;
  loading = true;

  /**
   * La page a un bloc par FAMILLE de statuts : le serveur en connaît vingt (voir statuts-candidature.ts), la page n'en
   * montre que six. Ces familles remplacent les cinq anciens codes (SOUMIS, EN_COURS_DE_PRESELECTION, ADMIS1/2/3, REJETE)
   * qui n'existent plus côté serveur depuis le 22/09 : tout dossier qui n'était dans aucun des cinq blocs — c'est-à-dire
   * presque tous — affichait une page vide.
   */
  readonly BLOC_EN_COURS: readonly StatutCandidature[] = [
    'BROUILLON', 'SOUMIS', 'INCOMPLET', 'DOSSIER_VALIDE', 'PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE', 'PAIEMENT_CONFIRME',
  ];
  readonly BLOC_REFUS: readonly StatutCandidature[] = [
    'DOSSIER_REJETE', 'NON_ADMIS', 'TEST_ECHOUE', 'ABSENT_TEST', 'ABSENT_ENTRETIEN', 'DESISTEMENT',
  ];
  readonly BLOC_TEST: readonly StatutCandidature[] = ['CONVOQUE_TEST', 'TEST_REUSSI'];
  readonly BLOC_ENTRETIEN: readonly StatutCandidature[] = ['CONVOQUE_ENTRETIEN'];
  readonly BLOC_DECISION: readonly StatutCandidature[] = ['ENTRETIEN_EFFECTUE', 'LISTE_ATTENTE'];
  readonly BLOC_ADMISSION: readonly StatutCandidature[] = ['ADMIS', 'INSCRIPTION_FINALISEE'];
  /** Statuts où le dossier attend encore l'examen du jury (le bloc « Examen en cours » n'a de sens que là). */
  readonly STATUTS_EXAMEN: readonly StatutCandidature[] = ['BROUILLON', 'SOUMIS', 'INCOMPLET', 'DOSSIER_VALIDE'];
  /** Statuts où le candidat doit encore régler (ou suivre le règlement de) ses frais de dossier. */
  readonly STATUTS_PAIEMENT_REQUIS: readonly StatutCandidature[] = ['PRESELECTIONNE', 'PAIEMENT_EN_ATTENTE'];

  /** Titre, texte et badge du statut courant, au nom de la session du dossier (jamais d'un programme codé en dur). */
  get message() {
    return messageCandidat(this.candidature?.statut, this.candidature?.sessionCandidatureLibelle);
  }

  estDans(bloc: readonly string[]): boolean {
    return !!this.candidature && bloc.includes(this.candidature.statut);
  }

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
   * Ordre réel du parcours (TDR) : dépôt → présélection → paiement des frais de dossier → test académique → entretien.
   * La même frise que celle du responsable (statuts-candidature.ts) : il doit voir exactement l'étape que voit le candidat.
   */
  get timelineItems(): TimelineItem[] {
    return friseParcours(this.candidature?.statut, {
      audience: 'candidat',
      statutPrecedent: this.candidature?.statutPrecedent,
    });
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
      case 'CONFORME': return 'bg-emerald-100 text-emerald-700';
      case 'A_VERIFIER': return 'bg-amber-100 text-amber-800';
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

  /** « Étape 4 sur 5 », à partir du test — synchronisée avec la frise. */
  get etapeActuelle(): string {
    return etapeActuelleDe(this.candidature?.statut, this.candidature?.statutPrecedent);
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
