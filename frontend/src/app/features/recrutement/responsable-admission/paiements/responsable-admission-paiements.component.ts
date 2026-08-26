import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CandidatureResponse, PaiementResponse, RecrutementService, ResultatRelance } from '../../../../core/recrutement/services/recrutement.service';

/** Statuts à partir desquels le dossier est validé et le paiement des frais attendu. */
const STATUTS_DOSSIER_VALIDE = ['EN_COURS_DE_PRESELECTION', 'ADMIS1', 'ADMIS2', 'ADMIS3'];

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
  filtreStatut: 'TOUS' | 'EN_ATTENTE' | 'SUCCES' = 'TOUS';

  relanceEnCours = false;
  relanceMessage = '';

  constructor(private recrutementService: RecrutementService) {}

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
      (c) => STATUTS_DOSSIER_VALIDE.includes(c.statut) && c.paiement?.statut !== 'SUCCES'
    );
  }

  relancerImpayes(): void {
    if (this.candidaturesImpayees.length === 0) return;
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
}
