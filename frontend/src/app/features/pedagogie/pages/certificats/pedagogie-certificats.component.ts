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
  civilite = '';
  dateNaissance = '';
  dateDebut = '';
  dateFin = '';
  code = '';
  enregistrement = false;
  telechargementEnCoursId: number | null = null;

  readonly civilites = ['Monsieur', 'Madame', 'Mademoiselle'];

  messageErreur = '';
  messageSucces = '';
  /** Cours que l'étudiant n'a pas terminés (renvoyés par le 409 du serveur). */
  coursNonTermines: string[] = [];

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
    this.civilite = '';
    this.dateNaissance = '';
    this.dateDebut = '';
    this.dateFin = '';
    this.code = '';
    this.formulaireOuvert = true;
    this.messageErreur = '';
    this.messageSucces = '';
    this.coursNonTermines = [];
  }

  /** Le modèle INPG imprime la civilité et la période : elles sont donc demandées à la délivrance. */
  get saisieValide(): boolean {
    return !!this.etudiantId && !!this.intitule.trim() && !!this.civilite && !!this.dateDebut && !!this.dateFin
      && !this.periodeInversee;
  }

  get periodeInversee(): boolean {
    return !!this.dateDebut && !!this.dateFin && this.dateFin < this.dateDebut;
  }

  delivrer(): void {
    if (!this.saisieValide || this.enregistrement) {
      return;
    }
    this.enregistrement = true;
    this.messageErreur = '';
    this.coursNonTermines = [];
    this.pedagogie
      .delivrerCertificat({
        etudiantId: this.etudiantId!,
        intituleFormation: this.intitule.trim(),
        cohorteId: this.promotionId,
        civilite: this.civilite,
        dateNaissance: this.dateNaissance || null,
        dateDebut: this.dateDebut,
        dateFin: this.dateFin,
        code: this.code.trim() || null,
      })
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
          this.coursNonTermines = Array.isArray(erreur.error?.coursNonTermines)
            ? erreur.error.coursNonTermines
            : [];
        },
      });
  }

  /** Télécharge le PDF au modèle INPG (le jeton passe par l'intercepteur). */
  telecharger(certificat: Certificat): void {
    this.telechargementEnCoursId = certificat.id;
    this.messageErreur = '';
    this.pedagogie.telechargerCertificatPdf(certificat.id).subscribe({
      next: (blob) => {
        this.telechargementEnCoursId = null;
        const url = window.URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = `INPG-certificat-${certificat.reference}.pdf`;
        lien.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.telechargementEnCoursId = null;
        this.messageErreur = 'Le certificat n’a pas pu être téléchargé.';
      },
    });
  }
}
