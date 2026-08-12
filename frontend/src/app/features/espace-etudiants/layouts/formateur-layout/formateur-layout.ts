import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { FormateurService, FormateurProfile } from '../../services/formateur.service';

@Component({
  selector: 'app-formateur-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './formateur-layout.html',
  styleUrl: './formateur-layout.css',
})
export class FormateurLayout implements OnInit {
  profile: FormateurProfile | null = null;
  loading = false;

  constructor(private formateurService: FormateurService) {}

  ngOnInit(): void {
    this.loading = true;
    this.formateurService.getProfile().subscribe({ next: p => { this.profile = p; this.loading = false; }, error: () => { this.profile = null; this.loading = false; } });
  }

  // Pas encore de page de profil self-service : on prévient honnêtement plutôt
  // que de laisser un lien href="#" qui ne fait que remonter en haut de page.
  profilBientotDisponible(event: Event): void {
    event.preventDefault();
    alert('La page "Mon profil" sera bientôt disponible.');
  }
}
