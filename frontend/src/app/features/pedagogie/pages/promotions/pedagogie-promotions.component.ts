import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Formation } from '../../../espace-etudiants/models/formation';
import {
  PedagogieService,
  PromotionDetail,
  PromotionSaisie,
} from '../../services/pedagogie.service';

/**
 * Gestion des promotions.
 *
 * <p>Le rattachement à une formation est mis en avant, et son absence signalée :
 * une promotion tire son programme de sa formation. Sans elle, ses étudiants
 * n'accèdent à aucun cours — sans que rien d'autre ne le leur indique.
 */
@Component({
  selector: 'app-pedagogie-promotions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedagogie-promotions.component.html',
})
export class PedagogiePromotionsComponent implements OnInit {
  private pedagogie = inject(PedagogieService);

  chargement = true;
  promotions: PromotionDetail[] = [];
  formations: Formation[] = [];

  formulaireOuvert = false;
  enEdition: PromotionDetail | null = null;
  saisie: PromotionSaisie = this.saisieVierge();
  enregistrement = false;

  messageErreur = '';
  messageSucces = '';

  ngOnInit(): void {
    this.charger();
  }

  private saisieVierge(): PromotionSaisie {
    return { code: '', libelle: '', formationId: null, dateDebut: null, dateFin: null };
  }

  private charger(): void {
    this.chargement = true;
    forkJoin({
      promotions: this.pedagogie.getPromotions(),
      formations: this.pedagogie.getFormations(),
    }).subscribe({
      next: ({ promotions, formations }) => {
        this.promotions = (promotions as unknown as PromotionDetail[]) || [];
        this.formations = formations || [];
        this.chargement = false;
      },
      error: () => {
        this.chargement = false;
        this.messageErreur = 'Les promotions n’ont pas pu être chargées.';
      },
    });
  }

  /** Promotions dont le programme n'est pas défini : leurs étudiants n'ont aucun cours. */
  get sansFormation(): PromotionDetail[] {
    return this.promotions.filter((p) => !p.formationIntitule);
  }

  ouvrirCreation(): void {
    this.enEdition = null;
    this.saisie = this.saisieVierge();
    this.formulaireOuvert = true;
    this.reinitialiserMessages();
  }

  ouvrirEdition(promotion: PromotionDetail): void {
    this.enEdition = promotion;
    const formation = this.formations.find((f) => f.code === promotion.formationCode);
    this.saisie = {
      code: promotion.code,
      libelle: promotion.libelle,
      formationId: formation?.id ? Number(formation.id) : null,
      dateDebut: promotion.dateDebut,
      dateFin: promotion.dateFin,
      programme: promotion.programme,
      activitesParascolaires: promotion.activitesParascolaires,
    };
    this.formulaireOuvert = true;
    this.reinitialiserMessages();
  }

  fermerFormulaire(): void {
    this.formulaireOuvert = false;
    this.enEdition = null;
  }

  get saisieValide(): boolean {
    return !!this.saisie.code.trim() && !!this.saisie.libelle.trim();
  }

  enregistrer(): void {
    if (!this.saisieValide || this.enregistrement) {
      return;
    }
    this.enregistrement = true;
    this.reinitialiserMessages();

    const requete = this.enEdition
      ? this.pedagogie.modifierPromotion(this.enEdition.id, this.saisie)
      : this.pedagogie.creerPromotion(this.saisie);

    requete.subscribe({
      next: (promotion) => {
        this.enregistrement = false;
        this.messageSucces = this.enEdition
          ? `Promotion « ${promotion.libelle} » modifiée.`
          : `Promotion « ${promotion.libelle} » créée.`;
        this.fermerFormulaire();
        this.charger();
      },
      error: (erreur) => {
        this.enregistrement = false;
        this.messageErreur = erreur.error?.message || 'L’enregistrement a échoué.';
      },
    });
  }

  supprimer(promotion: PromotionDetail): void {
    this.reinitialiserMessages();
    this.pedagogie.supprimerPromotion(promotion.id).subscribe({
      next: () => {
        this.messageSucces = `Promotion « ${promotion.libelle} » supprimée.`;
        this.charger();
      },
      // Le serveur refuse (409) tant que des étudiants y sont rattachés : son message
      // indique combien, il est donc plus utile qu'un texte générique.
      error: (erreur) => (this.messageErreur = erreur.error?.message || 'La suppression a échoué.'),
    });
  }

  private reinitialiserMessages(): void {
    this.messageErreur = '';
    this.messageSucces = '';
  }
}
