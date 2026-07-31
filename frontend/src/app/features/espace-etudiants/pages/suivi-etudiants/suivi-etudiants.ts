import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { ETUDIANTS_MOCK, Etudiant } from '../../data/etudiants.mock';

@Component({
  selector: 'app-suivi-etudiants',
  standalone: false,
  templateUrl: './suivi-etudiants.html',
  styleUrl: './suivi-etudiants.css',
})
export class SuiviEtudiants {
  etudiants: Etudiant[] = ETUDIANTS_MOCK;
  recherche: string = '';

  constructor(private router: Router) {}

  get etudiantsFiltres(): Etudiant[] {
    return this.etudiants.filter(e =>
      e.nom.toLowerCase().includes(this.recherche.toLowerCase())
    );
  }

  voirProfil(id: number) {
    this.router.navigate(['/espace-etudiant/formateur/profil-etudiant', id]);
  }

  classeProgression(progression: number): string {
    if (progression < 50) return 'bg-error';
    return 'bg-secondary';
  }

  classeStatut(statut: string): string {
    switch (statut) {
      case 'En retard': return 'bg-error-container text-on-error-container';
      case 'Actif': return 'bg-secondary-container text-on-secondary-container';
      case 'À risque': return 'bg-surface-container-high text-on-surface-variant';
      default: return '';
    }
  }
}