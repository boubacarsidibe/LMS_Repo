import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Formation } from '../../models/formation';
import { FormationService } from '../../services/formation.service';
import { mesFormationsMock } from '../../data/formations.data';
import { FavoritesService } from '../../services/favorites.service';
import { FormationCard } from './formation-card';

@Component({
  selector: 'app-mes-formations',
  standalone: true,
  imports: [CommonModule, FormsModule, FormationCard],
  templateUrl: './mes-formations.html',
  styleUrls: ['./mes-formations.css'],
})
export class MesFormations implements OnInit {
  formations: Formation[] = [];
  filtre: 'ALL' | 'EN_COURS' | 'TERMINE' | 'FAVORI' = 'ALL';
  recherche: string = '';

  constructor(private formationService: FormationService, private favorites: FavoritesService) {}

  ngOnInit(): void {
    this.loadFormations();
  }

  private loadFormations(): void {
    this.formationService.getAll().subscribe({
      next: (data) => {
        this.formations = data ?? mesFormationsMock;
        this.marquerFavoris();
      },
      error: () => {
        this.formations = mesFormationsMock;
        this.marquerFavoris();
      },
    });
  }

  /** Récupère les favoris du serveur et les reporte sur la liste courante. */
  private marquerFavoris(): void {
    this.favorites.getFavorites().subscribe({
      next: (ids) => {
        const favoris = new Set(ids.map((i) => Number(i)));
        this.formations.forEach((f) => (f.favorite = f.id !== undefined && favoris.has(Number(f.id))));
      },
      error: () => this.formations.forEach((f) => (f.favorite = false)),
    });
  }

  get formationsFiltres(): Formation[] {
    return this.formations.filter((f) => {
      const textMatch = `${f.intitule || ''} ${f.code || ''}`.toLowerCase().includes(this.recherche.toLowerCase());
      if (!textMatch) return false;
      if (this.filtre === 'ALL') return true;
      if (this.filtre === 'FAVORI') return !!f.favorite;
      return f.status === this.filtre;
    });
  }
}
