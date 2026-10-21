import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { FooterComponent } from '../../../shared/components/footer/footer.component';
import { AttestationVerificationService } from '../services/attestation-verification.service';
import { AttestationAccreditationVerification } from '../models/attestation-verification';
import { LIBELLES_NIVEAU_PROGRAMME, NiveauProgramme } from '../models/accreditation.models';

type EtatVerification = 'chargement' | 'introuvable' | 'trouve' | 'erreur';

/**
 * Page publique (sans authentification) vers laquelle pointe le QR code imprimé
 * sur une attestation d'accréditation. Sert aussi de fiche détaillée depuis la
 * liste des écoles accréditées (TDR §F) : un tiers sans compte (futur étudiant,
 * entreprise partenaire) peut confirmer qu'une accréditation présentée est
 * authentique, sans jamais exposer une liste énumérable — même principe que
 * verification-certificat.component pour les certificats de formation.
 */
@Component({
  selector: 'app-verification-accreditation',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent, FooterComponent],
  templateUrl: './verification-accreditation.component.html',
})
export class VerificationAccreditationComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private verificationService = inject(AttestationVerificationService);

  etat: EtatVerification = 'chargement';
  attestation: AttestationAccreditationVerification | null = null;
  reference = '';
  libellesNiveau = LIBELLES_NIVEAU_PROGRAMME;

  ngOnInit(): void {
    this.reference = this.route.snapshot.paramMap.get('reference') || '';
    if (!this.reference) {
      this.etat = 'introuvable';
      return;
    }

    this.verificationService.verifier(this.reference).subscribe({
      next: (attestation) => {
        this.attestation = attestation;
        this.etat = 'trouve';
      },
      error: (error) => {
        this.etat = error?.status === 404 ? 'introuvable' : 'erreur';
      },
    });
  }

  libelleNiveau(niveau: string | null): string {
    return niveau ? this.libellesNiveau[niveau as NiveauProgramme] ?? niveau : '—';
  }
}
