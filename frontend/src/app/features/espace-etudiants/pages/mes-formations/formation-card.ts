import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Formation } from '../../models/formation';
import { FavoritesService } from '../../services/favorites.service';

@Component({
  selector: 'app-formation-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './formation-card.html',
  styleUrls: ['./formation-card.css']
})
export class FormationCard {
  @Input() formation!: Formation;

  constructor(private favorites: FavoritesService) {}
  toggleFavorite(event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    if (!this.formation || this.formation.id === undefined) return;
    this.favorites.toggleFavorite(this.formation.id).subscribe({
      next: (res) => this.formation.favorite = !!res.favorite,
      error: () => {
        // optionally show an error toast
      }
    });
  }
}