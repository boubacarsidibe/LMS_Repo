import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from '../../../core/auth/services/authService';

interface EntreeNavigation {
  libelle: string;
  lien: string;
  icone: string;
}

/**
 * Shell de l'espace du responsable pédagogique.
 *
 * <p>Les deux écrans de cet espace portaient chacun leur propre barre horizontale,
 * dupliquée et bornée à 1280 px : sur un écran large, les éléments de droite
 * s'arrêtaient loin du bord. Ils partagent désormais une barre latérale, comme
 * l'espace formateur, ce qui règle l'alignement et laisse la place aux rubriques
 * à venir.
 */
@Component({
  selector: 'app-pedagogie-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './pedagogie-layout.component.html',
})
export class PedagogieLayoutComponent implements OnInit {
  private auth = inject(Auth);
  private router = inject(Router);

  nomAffiche = '';
  initiales = '?';
  sidebarMobileOuverte = false;
  menuProfilOuvert = false;

  readonly navigation: EntreeNavigation[] = [
    { libelle: 'Vue d’ensemble', lien: 'vue-ensemble', icone: 'dashboard' },
    { libelle: 'Cours', lien: 'cours', icone: 'menu_book' },
    { libelle: 'Formations', lien: 'formations', icone: 'school' },
    { libelle: 'Promotions', lien: 'promotions', icone: 'groups' },
    { libelle: 'Étudiants', lien: 'etudiants', icone: 'badge' },
    { libelle: 'Formateurs', lien: 'formateurs', icone: 'co_present' },
    { libelle: 'Relevés', lien: 'releves', icone: 'description' },
    { libelle: 'Certificats', lien: 'certificats', icone: 'workspace_premium' },
    { libelle: 'Suivi qualité', lien: 'qualite', icone: 'analytics' },
    { libelle: 'Activités du personnel', lien: 'activites-personnel', icone: 'apartment' },
    { libelle: 'Rendez-vous', lien: 'rendez-vous', icone: 'event' },
  ];

  ngOnInit(): void {
    const utilisateur = this.auth.currentUser();
    this.nomAffiche = utilisateur?.username || 'Responsable pédagogique';
    this.initiales = this.calculerInitiales(this.nomAffiche);
  }

  private calculerInitiales(nom: string): string {
    const morceaux = nom.trim().split(/[\s_.-]+/).filter(Boolean);
    if (morceaux.length === 0) {
      return '?';
    }
    const deux = morceaux.slice(0, 2).map((m) => m[0]).join('');
    return deux.toUpperCase();
  }

  basculerSidebarMobile(): void {
    this.sidebarMobileOuverte = !this.sidebarMobileOuverte;
  }

  basculerMenuProfil(): void {
    this.menuProfilOuvert = !this.menuProfilOuvert;
  }

  seDeconnecter(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/auth/login']),
      error: () => this.router.navigate(['/auth/login']),
    });
  }
}
