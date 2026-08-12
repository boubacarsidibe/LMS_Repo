import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormateurService, FormateurProfile } from '../../services/formateur.service';

@Component({
  selector: 'app-mon-profil',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mon-profil.html',
  styleUrl: './mon-profil.css',
})
export class MonProfil implements OnInit {
  profil: FormateurProfile | null = null;
  chargement = true;
  erreur: string | null = null;

  constructor(private formateurService: FormateurService) {}

  ngOnInit() {
    this.formateurService.getProfile().subscribe({
      next: (data) => {
        this.profil = data;
        this.chargement = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement du profil :', err);
        this.erreur = "Impossible de charger votre profil pour le moment.";
        this.chargement = false;
      }
    });
  }

  initiales(nom: string): string {
    return nom
      .split(' ')
      .map(mot => mot.charAt(0))
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }
}