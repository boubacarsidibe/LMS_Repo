import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CandidatureResponse, EtapeDossierResponse, PieceJustificativeResponse, RecrutementService } from '../../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../../shared/utils/download-file';
import { NotificationService } from '../../../../../shared/components/ui/notification.service';
import { EtapeFrise, friseCandidature } from '../../../parcours-candidature';
import { FormationDeclaree, ParcoursDeclare, libelleTypeDocument, lireParcoursDeclare } from '../../../parcours-declare';
import {
  ActionAdmission, actionSuivante as actionSuivanteDe, classeStatut, libelleStatut, peutAnnuler as peutAnnulerDossier,
  peutFaire, peutRejeter as peutRejeterDossier, reculsPossibles,
} from '../../../statuts-admission';

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

  readonly libelleStatut = libelleStatut;
  readonly classeStatut = classeStatut;

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
    return friseCandidature(this.candidature?.statut, 'admission', this.documentsAFournir.length);
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

  /** Parcours académique et situation professionnelle que le candidat a déclarés à l'étape 2 du formulaire. */
  get parcoursDeclare(): ParcoursDeclare {
    return lireParcoursDeclare(this.candidature?.informationsAcademiques, this.candidature?.informationsProfessionnelles);
  }

  /** Documents exigés que le serveur juge manquants ou à remplacer (même calcul que la relance et le suivi du candidat). */
  get documentsAFournir(): { libelle: string; etat: 'A_FOURNIR' | 'A_REMPLACER' }[] {
    return (this.candidature?.documentsAttendus ?? [])
      .filter((d): d is typeof d & { etat: 'A_FOURNIR' | 'A_REMPLACER' } => d.etat !== 'FOURNI')
      .map((d) => ({ libelle: d.libelle, etat: d.etat }));
  }

  /** Vrai seulement si le serveur a répondu sur les documents exigés et que rien n'en manque. */
  get dossierComplet(): boolean {
    return (this.candidature?.documentsAttendus?.length ?? 0) > 0 && this.documentsAFournir.length === 0;
  }

  readonly libelleTypeDocument = libelleTypeDocument;

  /** « CFPT · Attestation · 2021 · Sénégal » : ce que la formation déclarée précise, sans les champs vides. */
  detailsFormation(f: FormationDeclaree): string {
    return [f.etablissement, f.diplome, f.anneeObtention, f.pays].filter((v) => v).join(' · ');
  }

  /** Le geste suivant du dossier, décidé par le serveur (`actionsPossibles`) : voir `statuts-admission.ts`. */
  actionSuivante(c: CandidatureResponse): { action: ActionAdmission; libelle: string } | null {
    return actionSuivanteDe(c);
  }

  peutRejeter(c: CandidatureResponse): boolean {
    return peutRejeterDossier(c);
  }

  /** Annulable tant que l'accès étudiant n'est pas ouvert. */
  peutAnnuler(c: CandidatureResponse): boolean {
    return peutAnnulerDossier(c);
  }

  /**
   * Étapes vers lesquelles ce dossier peut être ramené, d'après le serveur. La liste était écrite ici avec
   * quatre anciens statuts qui n'existent plus : le menu « Reculer vers » ne proposait rien d'utilisable.
   */
  get ciblesReculerPossibles(): { valeur: string; libelle: string }[] {
    return this.candidature ? reculsPossibles(this.candidature).map((s) => ({ valeur: s, libelle: libelleStatut(s) })) : [];
  }

  /** Vrai quand le geste suivant est un résultat à choisir (réussi / échoué / absent…) plutôt qu'un simple bouton. */
  get resultatAChoisir(): 'TEST' | 'ENTRETIEN' | null {
    const suivante = this.candidature ? this.actionSuivante(this.candidature) : null;
    return suivante?.action === 'ENREGISTRER_RESULTAT_TEST' ? 'TEST'
      : suivante?.action === 'ENREGISTRER_RESULTAT_ENTRETIEN' ? 'ENTRETIEN' : null;
  }

  /** Lance le geste suivant ; un résultat à choisir passe par ses propres boutons, un règlement par la page Paiements. */
  lancerActionSuivante(c: CandidatureResponse): void {
    const suivante = this.actionSuivante(c);
    if (!suivante) return;
    switch (suivante.action) {
      case 'PRESELECTIONNER': return this.validerPreselection(c);
      case 'DEMANDER_COMPLEMENT': return this.demanderComplement(c);
      case 'CONVOQUER_TEST': return this.admettreTest(c);
      case 'ADMETTRE_ENTRETIEN': return this.admettreEntretien(c);
      case 'ADMETTRE_FINAL': return this.admettreFinal(c);
      case 'PLACER_LISTE_ATTENTE': return this.executer(this.recrutementService.placerEnListeAttente(c.id), true);
      case 'FINALISER_INSCRIPTION': return this.finaliserInscription(c);
      case 'ENREGISTRER_REGLEMENT': void this.router.navigate(['/responsable-admission/paiements']); return;
      default: return;
    }
  }

  /** Met le dossier en attente du candidat et lui dit quoi fournir (e-mail + cloche). */
  demanderComplement(c: CandidatureResponse): void {
    const liste = this.documentsAFournir.map((d) => d.libelle).join(', ');
    if (!window.confirm(`Demander un complément au candidat pour le dossier ${c.numeroDossier} ?\n\nIl recevra la liste : ${liste}.`)) return;
    this.executer(this.recrutementService.demanderComplement(c.id), true);
  }

  enregistrerResultatTest(c: CandidatureResponse, resultat: 'REUSSI' | 'ECHOUE' | 'ABSENT'): void {
    this.executer(this.recrutementService.enregistrerResultatTest(c.id, resultat), true);
  }

  enregistrerResultatEntretien(c: CandidatureResponse, resultat: 'EFFECTUE' | 'ABSENT'): void {
    this.executer(this.recrutementService.enregistrerResultatEntretien(c.id, resultat), true);
  }

  peutReculerVers(c: CandidatureResponse, cible: string): boolean {
    return reculsPossibles(c).includes(cible);
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

  /** @param rester reste sur la fiche, relue, quand l'écran doit montrer le geste suivant (résultats, complément). */
  private executer(obs: import('rxjs').Observable<CandidatureResponse>, rester = false): void {
    this.actionEnCours = true;
    obs.subscribe({
      next: () => {
        this.actionEnCours = false;
        if (rester && this.candidature) {
          this.charger(this.candidature.id);
        } else {
          this.retour();
        }
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
