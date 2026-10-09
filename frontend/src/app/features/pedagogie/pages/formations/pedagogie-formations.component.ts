import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { PedagogieService, PromotionDetail } from '../../services/pedagogie.service';
import { Cours } from '../../../espace-etudiants/models/cours';
import { Formation } from '../../../espace-etudiants/models/formation';

interface FormationDetail {
  formation: Formation;
  cours: Cours[];
  promotions: PromotionDetail[];
}

type Filtre = 'toutes' | 'actives' | 'sans-cours' | 'sans-promotion';

/**
 * Lecture seule : le catalogue des formations n'a pas d'écran de gestion côté
 * serveur (seul `GET /pedagogie/formations` existe, ni création ni modification).
 * Cette page comble juste l'absence d'accès — le chiffre affiché sur l'écran
 * Cours n'était nulle part consultable.
 *
 * <p>Passée à 17 formations (dont 16 sont encore des coquilles vides côté
 * cours/promotions), un mur de cartes ou une liste rouge de 16 noms n'était plus
 * lisible : le tableau filtrable remplace les deux, et le statut se lit d'un
 * coup d'œil par ligne plutôt que dans un paragraphe d'alerte.
 */
@Component({
  selector: 'app-pedagogie-formations',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './pedagogie-formations.component.html',
})
export class PedagogieFormationsComponent implements OnInit {
  private pedagogie = inject(PedagogieService);

  chargement = true;
  erreur = '';
  detail: FormationDetail[] = [];

  recherche = '';
  filtre: Filtre = 'toutes';
  private elargies = new Set<number | string>();

  ngOnInit(): void {
    forkJoin({
      formations: this.pedagogie.getFormations(),
      cours: this.pedagogie.getCours(),
      promotions: this.pedagogie.getPromotions(),
    }).subscribe({
      next: ({ formations, cours, promotions }) => {
        this.detail = (formations || [])
          .map((formation) => ({
            formation,
            cours: (cours || []).filter((c) => (c.formations || []).some((f) => f.id === formation.id)),
            promotions: (promotions || []).filter((p) => p.formationCode === formation.code),
          }))
          .sort((a, b) => (a.formation.intitule || '').localeCompare(b.formation.intitule || ''));
        this.chargement = false;
      },
      error: () => {
        this.erreur = 'Impossible de charger les formations.';
        this.chargement = false;
      },
    });
  }

  get totalCours(): number {
    return this.detail.reduce((total, d) => total + d.cours.length, 0);
  }

  get totalPromotions(): number {
    return this.detail.reduce((total, d) => total + d.promotions.length, 0);
  }

  /** Cours et promotion : le programme est réellement exploité par des étudiants. */
  get formationsActives(): FormationDetail[] {
    return this.detail.filter((d) => d.cours.length > 0 && d.promotions.length > 0);
  }

  statut(d: FormationDetail): { libelle: string; classe: string } {
    if (d.cours.length > 0 && d.promotions.length > 0) {
      return { libelle: 'Active', classe: 'bg-success/15 text-success-strong' };
    }
    if (d.cours.length === 0 && d.promotions.length === 0) {
      return { libelle: 'Sans contenu', classe: 'bg-surface-container text-on-surface-variant' };
    }
    return { libelle: d.cours.length === 0 ? 'Sans cours' : 'Sans promotion', classe: 'bg-warning/15 text-warning-strong' };
  }

  get filtres(): { valeur: Filtre; libelle: string; nombre: number }[] {
    return [
      { valeur: 'toutes', libelle: 'Toutes', nombre: this.detail.length },
      { valeur: 'actives', libelle: 'Actives', nombre: this.formationsActives.length },
      { valeur: 'sans-cours', libelle: 'Sans cours', nombre: this.detail.filter((d) => d.cours.length === 0).length },
      {
        valeur: 'sans-promotion',
        libelle: 'Sans promotion',
        nombre: this.detail.filter((d) => d.promotions.length === 0).length,
      },
    ];
  }

  get detailFiltre(): FormationDetail[] {
    const terme = this.recherche.trim().toLowerCase();
    return this.detail.filter((d) => {
      if (terme) {
        const cible = `${d.formation.code} ${d.formation.intitule}`.toLowerCase();
        if (!cible.includes(terme)) return false;
      }
      switch (this.filtre) {
        case 'actives':
          return d.cours.length > 0 && d.promotions.length > 0;
        case 'sans-cours':
          return d.cours.length === 0;
        case 'sans-promotion':
          return d.promotions.length === 0;
        default:
          return true;
      }
    });
  }

  estElargie(d: FormationDetail): boolean {
    return this.elargies.has(d.formation.id!);
  }

  basculer(d: FormationDetail): void {
    const id = d.formation.id!;
    if (this.elargies.has(id)) {
      this.elargies.delete(id);
    } else {
      this.elargies.add(id);
    }
  }
}
