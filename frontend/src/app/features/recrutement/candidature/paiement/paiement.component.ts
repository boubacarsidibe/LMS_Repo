import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { RecrutementService, CandidatureResponse, PaiementResponse } from '../../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-paiement',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FooterComponent, NavbarComponent],
  templateUrl: './paiement.component.html'
})
export class PaiementComponent implements OnInit {
  candidature: CandidatureResponse | null = null;
  candidatureId: number | null = null;
  paiement: PaiementResponse | null = null;
  loading = false;
  currentDate = '';

  // Méthode de paiement
  paymentMethod = 'mobile_money';
  selectedOperator = 'wave';
  phonePrefix = '+221';
  phoneNumber = '';
  cardHolder = '';
  cardNumber = '';
  cardExpiry = '';
  cardCvc = '';

  mobileOperators = [
    { code: 'wave', name: 'Wave', logo: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCu5Fx6MGPwXW1O6nXZUIge8lJW5ETKGegBrzOxfjTArVgilG1l6cVQzr73utkVc2reBbvFe1yzClqxmu0utfk8CrZd0HvguvXgRa1U8Yr5CArrl1dlgQoJHVOv0Om_iY78aycE8tjPh__lfbRhx93Z7JeYR8r9vmBWdbTsqgbQUgJlDVlPT30k1dKoma5woDthMs9XhYRylfBt94rrYtZOfPZlL5PWSyMQnhgPez76zbbE9aoieDMN' },
    { code: 'orange', name: 'Orange', logo: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCS72PLqCwDeZ6ADiQq5TPqsToYiGsQlsOhaUDl_qSSEhXsbE2idfScJxM0b1gq6exDxMhmJGWcESqRXI_7ApJN8qflXqh2qtgV-3XmIExGSoNRgUBesD69RHbLHT4Ci89rSAfwF5z5bCcnbnCfHyT6x0oWS3H69P3L58-DKRRlHTcbA8avZ1e-PxKIaMSj6w76IVmpQ2wAOQ-uouoQvIrfNdSziOj2EymKUH19uGyQNTlB-m84xxi2' },
    { code: 'mtn', name: 'MTN', logo: 'https://lh3.googleusercontent.com/aida-public/AB6AXuB6osJqLr9ZkEOAQJ5KMm95-zApcBkptb6UU_w_LM2Ma5Wip8U9emKPr1pduKMaOoKglLE8nAZG95uJ8ofHpEA-RVVUlaflkRElODOC-58FQTycjhOaWpIa4Wt3yWmzCpV0AuuVs17X9akL3wG5Or7zWSd7_a_3XYTelayBrHJpyutM5bAtiQ0t9YZiMB2IZW2XWyig8XVxY4b-4TBHxBOnrMjjDYgRczBnFkjxqeSOivTZykZYchL5' },
    { code: 'free', name: 'Free', logo: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCkUjVymICefMY8aerJhlNHovbVuhqrUOCVh-IdpWcWMe5lWnUTmPuj5dNx64-F80RUwer8MJjih3HP9-ND8OI4liwofu0EDYZfBprvCf3IgD68NfhkqacmyeY10hpDMkMX-GWXJ8JHda8b8lOZe7hHzk7-rJXHh1BGG4FVS81nWAKVcAsmNHNs01NTEctltstklG1tBQ6PZEoxC-_9jDfRdbSryGPNWQMbu4Y42vvi2URvZfWbDCku' },
  ];

  constructor(
    private route: ActivatedRoute,
    private recrutementService: RecrutementService,
    private router: Router
  ) { }

  get paiementReussi(): boolean {
    return this.paiement?.statut === 'SUCCES' || this.paiement?.statut === 'CONFIRME';
  }

  get nomComplet(): string {
    return [this.candidature?.candidatPrenom, this.candidature?.candidatNom].filter(Boolean).join(' ');
  }

  get libelleFormation(): string {
    return this.candidature?.sessionCandidatureLibelle || '—';
  }

  get referenceTransaction(): string {
    return this.paiement?.referenceTransaction || 'TXN-2024-99482';
  }

  get montantFormate(): string {
    const montant = this.paiement?.montant || 15000;
    return montant.toLocaleString('fr-FR') + ' FCFA';
  }

  get methodePaiement(): string {
    if (this.paiement?.statut === 'SUCCES' || this.paiement?.statut === 'CONFIRME') {
      return this.paymentMethod === 'card' ? 'Carte Bancaire'
        : this.paymentMethod === 'transfer' ? 'Virement Bancaire'
        : this.selectedOperator.charAt(0).toUpperCase() + this.selectedOperator.slice(1);
    }
    return '—';
  }

  ngOnInit(): void {
    this.updateCurrentDate();
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

  private updateCurrentDate(): void {
    const now = new Date();
    this.currentDate = now.toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  onMethodChange(): void {
    // Réinitialiser les champs au changement de méthode
  }

  initierPaiement(): void {
    if (!this.candidatureId) return;
    this.loading = true;

    this.recrutementService.initierPaiement({
      candidatureId: this.candidatureId,
      montant: 15000,
      devise: 'XOF'
    }).subscribe({
      next: (response) => {
        this.paiement = response;
        this.loading = false;
        this.updateCurrentDate();
        // La vue de confirmation s'affichera automatiquement via paiementReussi
      },
      error: (err) => {
        alert('Erreur de paiement: ' + (err.error?.message || 'Erreur inconnue'));
        this.loading = false;
      }
    });
  }

  telechargerRecu(): void {
    // TODO: implémenter le téléchargement PDF via l'API
    alert('Téléchargement du reçu à venir...');
  }

  accederEspaceCandidat(): void {
    this.router.navigate(['/recrutement/suivi']);
  }
}