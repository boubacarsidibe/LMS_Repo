import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from '../../../../core/auth/services/authService';
import { AlumniProfil } from '../../models/alumni.models';
import { AlumniService } from '../../services/alumni.service';

interface EntreeNavigation {
  libelle: string;
  lien: string;
  icone: string;
}

/**
 * Cadre de l'espace Alumni : mêmes conventions visuelles que
 * {@code EtudiantLayout} (charte graphique CLAUDE.md §1), en plus simple —
 * trois rubriques, pas de cloche d'annonces (aucune n'est adressée aux alumni
 * aujourd'hui).
 */
@Component({
  selector: 'app-alumni-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './alumni-layout.html',
})
export class AlumniLayout implements OnInit {
  profil: AlumniProfil | null = null;
  chargement = true;

  readonly navigation: EntreeNavigation[] = [
    { libelle: 'Mon profil', lien: 'mon-profil', icone: 'account_circle' },
    { libelle: 'Parcours professionnel', lien: 'mon-parcours', icone: 'work_history' },
    { libelle: 'Mes relevés', lien: 'mes-releves', icone: 'description' },
  ];

  constructor(
    private alumniService: AlumniService,
    private auth: Auth,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.alumniService.getMonProfil().subscribe({
      next: (profil) => {
        this.profil = profil;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  get nomAffiche(): string {
    if (!this.profil?.prenom) {
      return 'Alumni';
    }
    return `${this.profil.prenom} ${this.profil.nom}`;
  }

  get initiales(): string {
    if (!this.profil?.prenom || !this.profil?.nom) {
      return '?';
    }
    return (this.profil.prenom.charAt(0) + this.profil.nom.charAt(0)).toUpperCase();
  }

  deconnexion(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/accueil']),
      error: () => this.router.navigate(['/accueil']),
    });
  }
}
