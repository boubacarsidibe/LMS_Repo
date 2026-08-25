import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { CertificatVerificationService } from '../services/certificat-verification.service';
import { CertificatVerification } from '../models/certificat-verification';

type EtatVerification = 'chargement' | 'introuvable' | 'trouve' | 'erreur';

/**
 * Page publique (sans authentification) vers laquelle pointe le QR code imprimé
 * sur un certificat. Un tiers qui n'a pas de compte sur la plateforme (employeur,
 * établissement) peut ainsi confirmer qu'un certificat présenté est authentique.
 */
@Component({
  selector: 'app-verification-certificat',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent, FooterComponent],
  templateUrl: './verification-certificat.component.html',
})
export class VerificationCertificatComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private verificationService = inject(CertificatVerificationService);

  etat: EtatVerification = 'chargement';
  certificat: CertificatVerification | null = null;
  reference = '';

  ngOnInit(): void {
    this.reference = this.route.snapshot.paramMap.get('reference') || '';
    if (!this.reference) {
      this.etat = 'introuvable';
      return;
    }

    this.verificationService.verifier(this.reference).subscribe({
      next: (certificat) => {
        this.certificat = certificat;
        this.etat = 'trouve';
      },
      error: (error) => {
        this.etat = error?.status === 404 ? 'introuvable' : 'erreur';
      },
    });
  }
}
