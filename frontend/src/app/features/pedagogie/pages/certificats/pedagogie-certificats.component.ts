import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  Certificat,
  EtudiantScolarite,
  PedagogieService,
  PromotionDetail,
} from '../../services/pedagogie.service';

/**
 * Délivrance et registre des certificats.
 *
 * <p>Cette attribution relevait du formateur. Elle engage pourtant l'établissement
 * et non l'enseignant : le document porte désormais « INPG — Direction pédagogique »
 * et non le nom de la personne connectée.
 */
@Component({
  selector: 'app-pedagogie-certificats',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedagogie-certificats.component.html',
})
export class PedagogieCertificatsComponent implements OnInit {
  private pedagogie = inject(PedagogieService);

  chargement = true;
  certificats: Certificat[] = [];
  etudiants: EtudiantScolarite[] = [];
  promotions: PromotionDetail[] = [];

  formulaireOuvert = false;
  etudiantId: number | null = null;
  intitule = '';
  promotionId: number | null = null;
  enregistrement = false;

  messageErreur = '';
  messageSucces = '';

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.chargement = true;
    forkJoin({
      certificats: this.pedagogie.getCertificats(),
      etudiants: this.pedagogie.getEtudiants(),
      promotions: this.pedagogie.getPromotions(),
    }).subscribe({
      next: ({ certificats, etudiants, promotions }) => {
        this.certificats = certificats || [];
        this.etudiants = etudiants || [];
        this.promotions = (promotions as unknown as PromotionDetail[]) || [];
        this.chargement = false;
      },
      error: () => {
        this.chargement = false;
        this.messageErreur = 'Le registre n’a pas pu être chargé.';
      },
    });
  }

  ouvrirFormulaire(): void {
    this.etudiantId = null;
    this.intitule = '';
    this.promotionId = null;
    this.formulaireOuvert = true;
    this.messageErreur = '';
    this.messageSucces = '';
  }

  get saisieValide(): boolean {
    return !!this.etudiantId && !!this.intitule.trim();
  }

  delivrer(): void {
    if (!this.saisieValide || this.enregistrement) {
      return;
    }
    this.enregistrement = true;
    this.messageErreur = '';
    this.pedagogie
      .delivrerCertificat(this.etudiantId!, this.intitule.trim(), this.promotionId)
      .subscribe({
        next: (certificat) => {
          this.enregistrement = false;
          this.formulaireOuvert = false;
          this.messageSucces = `Certificat ${certificat.reference} délivré à ${certificat.etudiantNom}.`;
          this.charger();
        },
        error: (erreur) => {
          this.enregistrement = false;
          this.messageErreur = erreur.error?.message || 'La délivrance a échoué.';
        },
      });
  }
}
