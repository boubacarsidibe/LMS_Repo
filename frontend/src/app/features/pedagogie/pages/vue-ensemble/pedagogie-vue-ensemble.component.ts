import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Cours } from '../../../espace-etudiants/models/cours';
import { Formation } from '../../../espace-etudiants/models/formation';
import { PedagogieService, Promotion } from '../../services/pedagogie.service';

/**
 * Vue d'ensemble de l'offre d'enseignement.
 *
 * <p>Tous les chiffres sont dérivés des données réelles : aucun indicateur n'est
 * écrit en dur, et une base vide produit des états explicites plutôt que des
 * valeurs de remplissage.
 *
 * <p>Le point d'attention mis en avant — les cours hors programme — n'est pas
 * décoratif : depuis l'unification du rattachement, un cours qui n'appartient à
 * aucune formation n'est visible d'aucun étudiant. C'est le défaut le plus coûteux
 * de cet espace, et le seul que le responsable pédagogique puisse corriger.
 */
@Component({
  selector: 'app-pedagogie-vue-ensemble',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './pedagogie-vue-ensemble.component.html',
})
export class PedagogieVueEnsembleComponent implements OnInit {
  private pedagogie = inject(PedagogieService);

  chargement = true;
  cours: Cours[] = [];
  formations: Formation[] = [];
  promotions: Promotion[] = [];

  ngOnInit(): void {
    forkJoin({
      cours: this.pedagogie.getCours(),
      formations: this.pedagogie.getFormations(),
      promotions: this.pedagogie.getPromotions(),
    }).subscribe({
      next: ({ cours, formations, promotions }) => {
        this.cours = cours || [];
        this.formations = formations || [];
        this.promotions = promotions || [];
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  get tuiles(): { libelle: string; valeur: number; icone: string; lien: string }[] {
    return [
      { libelle: 'Cours', valeur: this.cours.length, icone: 'menu_book', lien: '../cours' },
      { libelle: 'Formations', valeur: this.formations.length, icone: 'school', lien: '../formations' },
      { libelle: 'Promotions', valeur: this.promotions.length, icone: 'groups', lien: '../promotions' },
      { libelle: 'Ressources', valeur: this.totalSupports, icone: 'folder_open', lien: '../cours' },
    ];
  }

  get totalSupports(): number {
    return this.cours.reduce((somme, c) => somme + (c.nbRessources || 0), 0);
  }

  /** Cours qui n'appartiennent à aucune formation : invisibles pour les étudiants. */
  get coursHorsProgramme(): Cours[] {
    return this.cours.filter((c) => !c.promotions || c.promotions.length === 0);
  }

  /** Cours sans aucune ressource déposée. */
  get coursSansRessource(): Cours[] {
    return this.cours.filter((c) => !c.nbRessources);
  }

  /** Promotions dont aucun cours n'a encore été rattaché au programme. */
  get promotionsSansCours(): Promotion[] {
    const rattachees = new Set<number>();
    this.cours.forEach((c) => (c.promotions || []).forEach((p) => rattachees.add(Number(p.id))));
    return this.promotions.filter((p) => !rattachees.has(Number(p.id)));
  }
}
