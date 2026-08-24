import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CertificatVerification } from '../../models/certificat-verification';
import { VerificationCertificatService } from '../../services/verification-certificat.service';

type EtatVerification = 'chargement' | 'authentique' | 'introuvable';

@Component({
  selector: 'app-verification-certificat',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './verification-certificat.html',
  styleUrl: './verification-certificat.css',
})
export class VerificationCertificat implements OnInit {
  private route = inject(ActivatedRoute);
  private verificationService = inject(VerificationCertificatService);

  etat: EtatVerification = 'chargement';
  certificat: CertificatVerification | null = null;
  reference = '';

  ngOnInit(): void {
    this.reference = this.route.snapshot.paramMap.get('reference') ?? '';
    if (!this.reference) {
      this.etat = 'introuvable';
      return;
    }

    this.verificationService.verifier(this.reference).subscribe({
      next: (certificat) => {
        this.certificat = certificat;
        this.etat = 'authentique';
      },
      error: () => {
        this.certificat = null;
        this.etat = 'introuvable';
      },
    });
  }
}
