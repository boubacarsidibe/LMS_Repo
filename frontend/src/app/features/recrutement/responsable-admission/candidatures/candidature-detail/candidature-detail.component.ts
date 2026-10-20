import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CandidatureResponse, EtapeDossierResponse, PieceJustificativeResponse, RecrutementService } from '../../../../../core/recrutement/services/recrutement.service';
import {
  actionSuivante as actionSuivanteDe,
  attenteDuDossier,
  CIBLES_DE_RECUL,
  classeStatut as classeStatutDe,
  friseParcours,
  libelleStatut as libelleStatutDe,
  peutAnnuler as peutAnnulerDe,
  peutReculerVers as peutReculerVersDe,
  peutRejeter as peutRejeterDe,
  TimelineItem,
} from '../../../../../core/recrutement/statuts-candidature';
import { downloadBlob } from '../../../../../shared/utils/download-file';
import { NotificationService } from '../../../../../shared/components/ui/notification.service';
import { ConfirmationService } from '../../../../../shared/components/ui/confirmation.service';

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

  /** Dossier ADMIS dont l'inscription (étape 6, QMS-009) reste à finaliser. */
  get inscriptionEnAttente(): boolean {
    return this.candidature?.statut === 'ADMIS'
      && !this.journal.some((e) => e.etape === 'INSCRIPTION');
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

  /**
   * Frise « dossier → présélection → paiement → test → entretien », la même que celle de la page de suivi du candidat
   * (statuts-candidature.ts) : le responsable doit voir exactement l'étape que voit le candidat. Elle raisonnait avec les
   * cinq anciens statuts — un dossier admis ou en attente de délibération restait figé devant l'entretien.
   */
  get timelineItems(): TimelineItem[] {
    return friseParcours(this.candidature?.statut, {
      audience: 'agent',
      statutPrecedent: this.candidature?.statutPrecedent,
    });
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private recrutementService: RecrutementService,
    private notifications: NotificationService,
    private confirmation: ConfirmationService,
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
    return libelleStatutDe(statut);
  }

  classeStatut(statut: string): string {
    return classeStatutDe(statut);
  }

  /** Dossier sans aucune pièce jointe : la présélection est bloquée côté backend (CandidatureService#changerStatut). */
  dossierVide(c: CandidatureResponse): boolean {
    return !c.piecesJustificatives || c.piecesJustificatives.length === 0;
  }

  /** La présélection est le geste qu'un dossier vide ne peut pas recevoir. */
  bloqueParDossierVide(c: CandidatureResponse): boolean {
    return actionSuivanteDe(c.statut)?.cle === 'VALIDER_PRESELECTION' && this.dossierVide(c);
  }

  /** Libellé + geste selon l'étape actuelle du workflow (cf. CandidatureService#estTransitionValide côté backend). */
  actionSuivante(c: CandidatureResponse): { libelle: string; action: () => void } | null {
    const geste = actionSuivanteDe(c.statut);
    if (!geste) return null;
    switch (geste.cle) {
      case 'VALIDER_PRESELECTION': return { libelle: geste.libelle, action: () => this.validerPreselection(c) };
      case 'ADMETTRE_TEST': return { libelle: geste.libelle, action: () => this.admettreTest(c) };
      case 'ADMETTRE_ENTRETIEN': return { libelle: geste.libelle, action: () => this.admettreEntretien(c) };
      case 'ADMETTRE_FINAL': return { libelle: geste.libelle, action: () => this.admettreFinal(c) };
      case 'FINALISER_INSCRIPTION':
        // Étape 6 de QMS-009 : la sélection (admettre-final) n'ouvre pas l'accès étudiant elle-même.
        return this.inscriptionEnAttente ? { libelle: geste.libelle, action: () => this.finaliserInscription(c) } : null;
    }
  }

  /** Ce qu'attend un dossier qui n'offre aucun geste : le responsable ne reste jamais devant un dossier muet. */
  attente(c: CandidatureResponse): string | null {
    return this.actionSuivante(c) ? null : attenteDuDossier(c.statut);
  }

  peutRejeter(c: CandidatureResponse): boolean {
    return peutRejeterDe(c.statut);
  }

  /** Une fois l'accès étudiant ouvert (INSCRIPTION_FINALISEE), la décision n'est plus annulable ici. */
  peutAnnuler(c: CandidatureResponse): boolean {
    return peutAnnulerDe(c.statut, c.statutPrecedent);
  }

  /** Cibles valides pour « faire reculer vers » — mêmes règles que CandidatureService#reculerStatut côté backend. */
  readonly ciblesReculer = CIBLES_DE_RECUL;

  peutReculerVers(c: CandidatureResponse, cible: string): boolean {
    return peutReculerVersDe(c.statut, cible);
  }

  /** Au moins une étape antérieure est atteignable : sinon la liste « Reculer vers » n'offrirait que des choix grisés. */
  peutReculer(c: CandidatureResponse): boolean {
    return this.ciblesReculer.some((cible) => this.peutReculerVers(c, cible.valeur));
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

  /** Le motif de rejet est obligatoire (TDR §A) : recueilli dans la boîte partagée, pas dans un window.prompt natif. */
  rejeter(c: CandidatureResponse): void {
    this.confirmation.demanderSaisie({
      titre: 'Rejeter ce dossier',
      message: `Dossier ${c.numeroDossier} — ${c.candidatPrenom} ${c.candidatNom}.\nLe motif est conservé au dossier et le candidat en est informé.`,
      libelleConfirmer: 'Rejeter le dossier',
      destructif: true,
      saisie: { libelle: 'Motif du rejet', obligatoire: true, placeholder: 'Ex. : pièces justificatives incomplètes' },
    }).subscribe((motif) => {
      if (motif) this.executer(this.recrutementService.rejeterDossier(c.id, motif));
    });
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
