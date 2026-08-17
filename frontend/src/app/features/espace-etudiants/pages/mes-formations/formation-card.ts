import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Formation } from '../../models/formation';
import { FavoritesService } from '../../services/favorites.service';
import { NotificationService } from '../../../../shared/components/ui/notification.service';

@Component({
  selector: 'app-formation-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './formation-card.html',
  styleUrls: ['./formation-card.css']
})
export class FormationCard {
  @Input() formation!: Formation;

  constructor(private favorites: FavoritesService,
    private notifications: NotificationService
  ) {}
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

  // Aucune page de contenu de cours n'existe encore côté étudiant : on prévient
  // honnêtement plutôt que de naviguer vers une page sans rapport (ex. le
  // catalogue public de recrutement).
  continuer(event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.notifications.info("Le contenu de ce cours n'est pas encore disponible en ligne.");
  }
}