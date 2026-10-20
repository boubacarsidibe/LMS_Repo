import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CandidatureResponse, PaiementResponse, RecrutementService, ResultatRelance } from '../../../../core/recrutement/services/recrutement.service';
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';
import { estAdmission, estEnEvaluation, libelleStatut } from '../../../../core/recrutement/statuts-candidature';

/**
 * Le paiement des frais est attendu dès la présélection et jusqu'à l'admission, liste complémentaire comprise. Un dossier
 * encore à l'instruction, refusé ou abandonné n'a rien à régler.
 */
const doitAvoirPaye = (statut: string): boolean => estEnEvaluation(statut) || estAdmission(statut) || statut === 'LISTE_ATTENTE';

@Component({
  selector: 'app-responsable-admission-paiements',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './responsable-admission-paiements.component.html',
})
export class ResponsableAdmissionPaiementsComponent implements OnInit {
  paiements: PaiementResponse[] = [];
  candidatures: CandidatureResponse[] = [];
  loading = false;
  errorMessage = '';
  recherche = '';
  filtreStatut: 'TOUS' | 'EN_ATTENTE' | 'SUCCES' | 'ECHEC' = 'TOUS';
  idEnCours: number | null = null;

  relanceEnCours = false;
  relanceMessage = '';

  constructor(private recrutementService: RecrutementService, private confirmation: ConfirmationService) {}

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.loading = true;
    this.errorMessage = '';
    forkJoin({
      paiements: this.recrutementService.getAllPaiements(),
      candidatures: this.recrutementService.getAllCandidatures(),
    }).subscribe({
      next: ({ paiements, candidatures }) => {
        this.paiements = paiements || [];
        this.candidatures = candidatures || [];
        this.loading = false;
      },
      error: () => {
        this.paiements = [];
        this.candidatures = [];
        this.loading = false;
        this.errorMessage = 'Impossible de charger l\'historique des paiements.';
      },
    });
  }

  /**
   * Dossiers validés dont les frais restent impayés — {@link paiements} seul ne suffit
   * pas : un candidat n'ayant jamais initié de paiement n'y a aucune ligne, et resterait
   * invisible alors qu'il fait bien partie des « non payés ».
   */
  get candidaturesImpayees(): CandidatureResponse[] {
    return this.candidatures.filter(
      (c) => doitAvoirPaye(c.statut) && c.paiement?.statut !== 'SUCCES'
    );
  }

  /** Libellé lisible d'un statut de dossier : la liste affichait le code brut (« PRESELECTIONNE », « ENTRETIEN_EFFECTUE »). */
  libelle = libelleStatut;

  /**
   * Dossiers que « Relancer les impayés » écrira réellement — miroir de
   * {@code PaiementService#STATUTS_ELIGIBLES_RELANCE_PAIEMENT} : seuls les dossiers qui attendent encore le règlement
   * (présélectionnés, ou règlement déclaré mais non confirmé). Un dossier déjà convoqué ou entretenu sans avoir payé
   * figure bien dans la liste des impayés — pour être régularisé —, mais lui écrire « vos frais sont en attente » serait
   * faux. Le bouton annonçait pourtant tous les impayés (3) alors que le serveur n'en relançait que deux.
   */
  get candidaturesARelancer(): CandidatureResponse[] {
    return this.candidaturesImpayees.filter((c) => c.statut === 'PRESELECTIONNE' || c.statut === 'PAIEMENT_EN_ATTENTE');
  }

  relancerImpayes(): void {
    if (this.candidaturesARelancer.length === 0) return;
    this.relanceEnCours = true;
    this.relanceMessage = '';
    this.recrutementService.relancerPaiementsImpayes().subscribe({
      next: (resultat: ResultatRelance) => {
        this.relanceEnCours = false;
        this.relanceMessage = resultat.echecs.length === 0
          ? `${resultat.envoyes} relance(s) envoyée(s) avec succès.`
          : `${resultat.envoyes} relance(s) envoyée(s), ${resultat.echecs.length} échec(s) : ${resultat.echecs.join(', ')}.`;
      },
      error: () => {
        this.relanceEnCours = false;
        this.relanceMessage = 'Échec de la relance des paiements impayés.';
      },
    });
  }

  get paiementsFiltres(): PaiementResponse[] {
    const q = this.recherche.trim().toLowerCase();
    return this.paiements.filter((p) => {
      if (this.filtreStatut !== 'TOUS' && p.statut !== this.filtreStatut) return false;
      if (!q) return true;
      return (
        (p.numeroDossier || '').toLowerCase().includes(q) ||
        (p.candidatNom || '').toLowerCase().includes(q) ||
        (p.candidatPrenom || '').toLowerCase().includes(q) ||
        (p.referenceTransaction || '').toLowerCase().includes(q)
      );
    });
  }

  get montantTotal(): number {
    return this.paiements.filter((p) => p.statut === 'SUCCES').reduce((s, p) => s + (p.montant || 0), 0);
  }

  get nbReussis(): number {
    return this.paiements.filter((p) => p.statut === 'SUCCES').length;
  }

  get nbEnAttente(): number {
    return this.paiements.filter((p) => p.statut === 'EN_ATTENTE').length;
  }

  /**
   * Confirme le règlement d'un dossier impayé, quel que soit son état actuel (jamais
   * initié, tentative en attente ou en échec) — c'est la seule action qui couvre les
   * trois cas, le candidat ayant réglé hors ligne (espèces, virement).
   */
  confirmerDossier(c: CandidatureResponse): void {
    if (this.idEnCours) return;
    this.confirmation.demander({
      titre: 'Confirmer le paiement',
      message: `Confirmer le paiement des frais de dossier de ${c.candidatPrenom} ${c.candidatNom} (${c.numeroDossier}) ?`,
      libelleConfirmer: 'Confirmer le paiement',
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.idEnCours = c.id;
      this.errorMessage = '';
      this.recrutementService.enregistrerPaiementHorsLigne(c.id).subscribe({
        next: () => {
          this.idEnCours = null;
          this.charger();
        },
        error: (e) => {
          this.idEnCours = null;
          this.errorMessage = e?.error?.message || 'Échec de la confirmation du paiement.';
        },
      });
    });
  }

  valider(p: PaiementResponse): void {
    if (this.idEnCours) return;
    this.idEnCours = p.id;
    this.recrutementService.simulerSuccesPaiement(p.id).subscribe({
      next: () => {
        this.idEnCours = null;
        // On recharge tout : valider un règlement change aussi le statut du dossier, que la liste « en attente de paiement »
        // affichait encore tel quel — le même dossier apparaissait payé dans un tableau et impayé dans l'autre.
        this.charger();
      },
      error: () => {
        this.idEnCours = null;
        this.errorMessage = 'Échec de la validation du paiement.';
      },
    });
  }

  rejeter(p: PaiementResponse): void {
    if (this.idEnCours) return;
    // Le prompt() natif ne s'affiche ni dans un partage d'écran ni dans un enregistrement, et ignore la charte : voir
    // ConfirmationService#demanderSaisie.
    this.confirmation.demanderSaisie({
      titre: 'Rejeter le paiement',
      message: `Le règlement de ${p.candidatPrenom ?? ''} ${p.candidatNom ?? ''} (${p.numeroDossier ?? ''}) sera marqué en échec ; le candidat pourra en déclarer un nouveau.`,
      libelleConfirmer: 'Rejeter le paiement',
      destructif: true,
      saisie: { libelle: 'Motif du rejet (facultatif)', obligatoire: false, maxLength: 300 },
    }).subscribe((motif) => {
      if (motif === null) return;
      this.idEnCours = p.id;
      this.recrutementService.marquerPaiementEnEchec(p.id, motif || undefined).subscribe({
        next: () => {
          this.idEnCours = null;
          this.charger();
        },
        error: () => {
          this.idEnCours = null;
          this.errorMessage = 'Échec du rejet du paiement.';
        },
      });
    });
  }
}
