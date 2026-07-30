import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { RecrutementService, CandidatureResponse, PaiementResponse } from '../../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-paiement',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './paiement.component.html'
})
export class PaiementComponent implements OnInit {
  candidature: CandidatureResponse | null = null;
  candidatureId: number | null = null;
  paiement: PaiementResponse | null = null;
  montant = 25000;
  devise = 'XOF';
  loading = false;

  constructor(private route: ActivatedRoute, private recrutementService: RecrutementService) { }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.candidatureId = params['candidatureId'] ? +params['candidatureId'] : null;
      if (this.candidatureId) {
        this.recrutementService.getCandidature(this.candidatureId).subscribe({
          next: (data) => {
            this.candidature = data;
            this.paiement = data.paiement;
          }
        });
      }
    });
  }

  effectuerPaiement(): void {
    if (!this.candidatureId) return;
    this.loading = true;

    this.recrutementService.initierPaiement({
      candidatureId: this.candidatureId,
      montant: this.montant,
      devise: this.devise
    }).subscribe({
      next: (response) => {
        this.paiement = response;
        this.loading = false;
        alert('Paiement initié avec succès ! Référence : ' + response.referenceTransaction);
      },
      error: (err) => {
        alert('Erreur de paiement: ' + (err.error?.message || 'Erreur inconnue'));
        this.loading = false;
      }
    });
  }
}