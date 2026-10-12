import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CandidatureResponse, EtapeDossierResponse, PieceJustificativeResponse, RecrutementService } from '../../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../../shared/utils/download-file';
import { NotificationService } from '../../../../../shared/components/ui/notification.service';
import { EtapeFrise, friseCandidature } from '../../../parcours-candidature';

type TimelineItem = EtapeFrise;

/**
 * Page dédiée au détail d'une candidature — remplace le panneau latéral de la
 * liste, qui restait inaccessible sur mobile (hidden md:flex, sans équivalent).
 * Même patron que TestFormComponent/CritereFormComponent : un tiroir devient
 * une page à part entière plutôt que d'ajouter une deuxième mise en page mobile
 * au panneau existant.
 */
@Component({
  selector: 'app-candidature-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './candidature-detail.component.html',
})
export class CandidatureDetailComponent implements OnInit {
  candidature: CandidatureResponse | null = null;
  loading = true;
  errorMessage = '';
  actionEnCours = false;
  retourConfirmationOpen = false;
  retourStatutCible: string | null = null;
  motDePasseRetour = '';
  retourErreur = '';

  /** Pièce et motif affichés dans la modale de demande de remplacement. */
  pieceARefuser: PieceJustificativeResponse | null = null;
  motifNonConformite = '';

  /** Historique du dossier (procédure QMS-009). */
  journal: EtapeDossierResponse[] = [];
  journalChargement = false;

  /** ADMIS3 dont l'inscription (étape 6, QMS-009) reste à finaliser. */
  get inscriptionEnAttente(): boolean {
    return this.candidature?.statut === this.STATUT_ADMIS3
      && !this.journal.some((e) => e.etape === 'INSCRIPTION');
  }

  /** Étapes disponibles selon le statut — mêmes codes que côté candidat (SuiviComponent). */
  readonly STATUT_SOUMIS = 'SOUMIS';
  readonly STATUT_PRESELECTION = 'EN_COURS_DE_PRESELECTION';
  readonly STATUT_REJETE = 'REJETE';
  readonly STATUT_ADMIS1 = 'ADMIS1';
  readonly STATUT_ADMIS2 = 'ADMIS2';
  readonly STATUT_ADMIS3 = 'ADMIS3';
  readonly STATUT_LISTE_ATTENTE = 'LISTE_ATTENTE';

  get paiementEnAttente(): boolean {
    return this.candidature?.paiement?.statut === 'EN_ATTENTE';
  }

  get paiementEchoue(): boolean {
    return this.candidature?.paiement?.statut === 'ECHEC';
  }

  /**
   * Frise de progression : la même fonction que celle du candidat (`friseCandidature`), pour que
   * l'admission voie exactement l'étape que voit le candidat — plus de deux copies à garder alignées.
   */
  get timelineItems(): TimelineItem[] {
    return friseCandidature(this.candidature?.statut, 'admission');
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private recrutementService: RecrutementService,
    private notifications: NotificationService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.charger(id);
  }

  private charger(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.recrutementService.getCandidatureAdmin(id).subscribe({
      next: (c) => {
        this.candidature = c;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Impossible de charger ce dossier de candidature.';
      },
    });
    this.journalChargement = true;
    this.recrutementService.getJournalDossier(id).subscribe({
      next: (journal) => {
        this.journal = journal || [];
        this.journalChargement = false;
      },
      error: () => (this.journalChargement = false),
    });
  }

  /** Libellés lisibles des étapes QMS-009, pour le journal du dossier. */
  libelleEtapeQms(etape: string): string {
    switch (etape) {
      case 'RECEPTION': return 'Réception du dossier';
      case 'FILTRE_INITIAL': return 'Filtre initial';
      case 'EVALUATION': return 'Évaluation';
      case 'SELECTION': return 'Sélection';
      case 'INSCRIPTION': return 'Inscription';
      case 'PUBLICATION': return 'Publication';
      default: return etape;
    }
  }

  retour(): void {
    this.router.navigate(['/responsable-admission/candidatures']);
  }

  libelleStatut(statut: string): string {
    switch (statut) {
      case 'BROUILLON': return 'Brouillon';
      case 'SOUMIS': return 'Soumis';
      case 'EN_COURS_DE_PRESELECTION': return 'En présélection';
      case 'ADMIS1': return 'Admis au test';
      case 'ADMIS2': return 'Admis à l\'entretien';
      case 'ADMIS3': return 'Admis (final)';
      case 'REJETE': return 'Rejeté';
      case 'LISTE_ATTENTE': return 'Liste complémentaire';
      default: return statut;
    }
  }

  classeStatut(statut: string): string {
    switch (statut) {
      case 'REJETE': return 'bg-error-container/20 text-error';
      case 'ADMIS3': return 'bg-success/15 text-success-strong';
      case 'LISTE_ATTENTE': return 'bg-primary-container text-primary';
      case 'ADMIS1':
      case 'ADMIS2': return 'bg-primary-fixed text-on-primary-fixed-variant';
      case 'SOUMIS':
      case 'EN_COURS_DE_PRESELECTION': return 'bg-secondary-container/30 text-on-secondary-container';
      default: return 'bg-surface-container text-on-surface-variant';
    }
  }

  /** Dossier sans aucune pièce jointe : la présélection est bloquée côté backend (CandidatureService#changerStatut). */
  dossierVide(c: CandidatureResponse): boolean {
    return !c.piecesJustificatives || c.piecesJustificatives.length === 0;
  }

  /** Libellé + capacité d'action selon l'étape actuelle du workflow (cf. CandidatureService#estTransitionValide côté backend). */
  actionSuivante(c: CandidatureResponse): { libelle: string; action: () => void } | null {
    switch (c.statut) {
      case 'SOUMIS':
        return { libelle: 'Valider présélection', action: () => this.validerPreselection(c) };
      case 'EN_COURS_DE_PRESELECTION':
        return { libelle: 'Admettre au test', action: () => this.admettreTest(c) };
      case 'ADMIS1':
        return { libelle: 'Admettre à l\'entretien', action: () => this.admettreEntretien(c) };
      case 'ADMIS2':
        return { libelle: 'Admettre (final)', action: () => this.admettreFinal(c) };
      case 'ADMIS3':
        // Étape 6 de QMS-009 : la sélection (admettre-final) n'ouvre plus l'accès étudiant
        // elle-même — voir finaliserInscription().
        return this.inscriptionEnAttente
          ? { libelle: 'Finaliser l\'inscription', action: () => this.finaliserInscription(c) }
          : null;
      default:
        return null;
    }
  }

  peutRejeter(c: CandidatureResponse): boolean {
    return c.statut !== 'ADMIS3' && c.statut !== 'REJETE' && c.statut !== 'BROUILLON';
  }

  /** ADMIS3 exclu : l'admission finale a déjà ouvert l'accès étudiant, non annulable ici. */
  peutAnnuler(c: CandidatureResponse): boolean {
    return c.statutPrecedent != null && c.statut !== 'ADMIS3';
  }

  /** Cibles valides pour "faire reculer vers" — mêmes règles que CandidatureService#rangParcours côté backend. */
  readonly ciblesReculer = [
    { valeur: 'SOUMIS', libelle: 'Soumis' },
    { valeur: 'EN_COURS_DE_PRESELECTION', libelle: 'En présélection' },
    { valeur: 'ADMIS1', libelle: 'Admis au test' },
    { valeur: 'ADMIS2', libelle: 'Admis à l\'entretien' },
  ];
  private readonly rangParcours: Record<string, number> = {
    SOUMIS: 1, EN_COURS_DE_PRESELECTION: 2, ADMIS1: 3, ADMIS2: 4,
  };

  peutReculerVers(c: CandidatureResponse, cible: string): boolean {
    if (c.statut === 'ADMIS3') return false;
    if (c.statut === 'REJETE') return true;
    return this.rangParcours[cible] < this.rangParcours[c.statut];
  }

  annuler(c: CandidatureResponse): void {
    if (c.statutPrecedent) this.ouvrirRetour(null);
  }

  reculerVers(c: CandidatureResponse, cible: string): void {
    if (cible && this.peutReculerVers(c, cible)) this.ouvrirRetour(cible);
  }

  ouvrirRetour(cible: string | null): void {
    this.retourStatutCible = cible;
    this.motDePasseRetour = '';
    this.retourErreur = '';
    this.retourConfirmationOpen = true;
  }

  fermerRetour(): void {
    if (!this.actionEnCours) this.retourConfirmationOpen = false;
  }

  confirmerRetour(): void {
    if (!this.candidature || !this.motDePasseRetour) {
      this.retourErreur = 'Saisissez votre mot de passe pour confirmer cette correction.';
      return;
    }
    const action = this.retourStatutCible
      ? this.recrutementService.reculerStatut(this.candidature.id, this.retourStatutCible, this.motDePasseRetour)
      : this.recrutementService.annulerDerniereAction(this.candidature.id, this.motDePasseRetour);
    this.actionEnCours = true;
    action.subscribe({
      next: () => { this.actionEnCours = false; this.retourConfirmationOpen = false; this.retour(); },
      error: (err) => { this.actionEnCours = false; this.retourErreur = err.error?.message || 'La correction n’a pas pu être appliquée.'; },
    });
  }

  private executer(obs: import('rxjs').Observable<CandidatureResponse>): void {
    this.actionEnCours = true;
    obs.subscribe({
      next: () => {
        this.actionEnCours = false;
        this.retour();
      },
      error: (err) => {
        this.actionEnCours = false;
        this.notifications.erreur(err.error?.message || "Échec de l'action sur la candidature.");
      },
    });
  }

  validerPreselection(c: CandidatureResponse): void {
    this.executer(this.recrutementService.validerPreselection(c.id));
  }

  admettreTest(c: CandidatureResponse): void {
    this.executer(this.recrutementService.admettreTestAcademique(c.id));
  }

  admettreEntretien(c: CandidatureResponse): void {
    this.executer(this.recrutementService.admettreEntretien(c.id));
  }

  admettreFinal(c: CandidatureResponse): void {
    this.executer(this.recrutementService.admettreFinal(c.id));
  }

  /** Étape 6 de QMS-009 : ouvre l'accès étudiant d'un dossier définitivement admis. */
  finaliserInscription(c: CandidatureResponse): void {
    this.actionEnCours = true;
    this.recrutementService.finaliserInscription(c.id).subscribe({
      next: () => {
        this.actionEnCours = false;
        this.charger(c.id);
      },
      error: (err) => {
        this.actionEnCours = false;
        this.notifications.erreur(err.error?.message || "Échec de la finalisation de l'inscription.");
      },
    });
  }

  rejeter(c: CandidatureResponse): void {
    const motif = window.prompt(`Motif de rejet pour le dossier ${c.numeroDossier} :`);
    if (motif === null) return; // annulé
    if (!motif.trim()) {
      this.notifications.erreur('Un motif de rejet est requis.');
      return;
    }
    this.executer(this.recrutementService.rejeterDossier(c.id, motif.trim()));
  }

  telechargerPiece(piece: PieceJustificativeResponse): void {
    if (piece.fichierStockeId == null) return;
    this.recrutementService.telechargerPiece(piece.id).subscribe({
      next: (blob) => downloadBlob(blob, piece.fichierNomOriginal || `document_${piece.id}`),
      error: () => this.notifications.erreur('Échec du téléchargement du document.'),
    });
  }

  marquerPieceConforme(piece: PieceJustificativeResponse): void {
    this.recrutementService.modifierConformitePiece(piece.id, true).subscribe({
      next: (miseAJour) => Object.assign(piece, miseAJour),
      error: () => this.notifications.erreur('Échec de la mise à jour de la conformité du document.'),
    });
  }

  ouvrirRefusPiece(piece: PieceJustificativeResponse): void {
    this.pieceARefuser = piece;
    this.motifNonConformite = piece.motifNonConformite || '';
  }

  annulerRefusPiece(): void {
    this.pieceARefuser = null;
    this.motifNonConformite = '';
  }

  confirmerRefusPiece(): void {
    const piece = this.pieceARefuser;
    const motif = this.motifNonConformite.trim();
    if (!piece) return;
    if (!motif) {
      this.notifications.erreur('Le motif de remplacement est requis.');
      return;
    }
    this.actionEnCours = true;
    this.recrutementService.modifierConformitePiece(piece.id, false, motif).subscribe({
      next: (miseAJour) => {
        Object.assign(piece, miseAJour);
        this.actionEnCours = false;
        this.annulerRefusPiece();
        this.notifications.succes('Le candidat a été informé du document à remplacer.');
      },
      error: (err) => {
        this.actionEnCours = false;
        this.notifications.erreur(err.error?.message || 'Échec de la demande de remplacement.');
      },
    });
  }

  libelleVerificationPiece(piece: PieceJustificativeResponse): string {
    switch (piece.statutVerification) {
      case 'CONFORME': return 'Conforme';
      case 'A_REMPLACER': return 'À remplacer';
      default: return 'À vérifier';
    }
  }
}
