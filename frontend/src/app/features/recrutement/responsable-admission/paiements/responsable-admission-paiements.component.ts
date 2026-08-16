import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaiementResponse, RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-responsable-admission-paiements',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './responsable-admission-paiements.component.html',
})
export class ResponsableAdmissionPaiementsComponent implements OnInit {
  paiements: PaiementResponse[] = [];
  loading = false;
  errorMessage = '';
  recherche = '';
  filtreStatut: 'TOUS' | 'EN_ATTENTE' | 'SUCCES' = 'TOUS';

  constructor(private recrutementService: RecrutementService) {}

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.loading = true;
    this.errorMessage = '';
    this.recrutementService.getAllPaiements().subscribe({
      next: (data) => {
        this.paiements = data || [];
        this.loading = false;
      },
      error: () => {
        this.paiements = [];
        this.loading = false;
        this.errorMessage = 'Impossible de charger l\'historique des paiements.';
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
