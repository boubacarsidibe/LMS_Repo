import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Etudiant } from '../../data/etudiants.mock';
import { EtudiantService } from '../../services/etudiant.service';

@Component({
  selector: 'app-suivi-etudiants',
  standalone: false,
  templateUrl: './suivi-etudiants.html',
  styleUrl: './suivi-etudiants.css',
})
export class SuiviEtudiants implements OnInit {
  etudiants: Etudiant[] = [];
  recherche: string = '';

  constructor(private router: Router, private etudiantService: EtudiantService) {}

  ngOnInit(): void {
    this.etudiantService.getAll().subscribe({ next: list => this.etudiants = list, error: () => this.etudiants = [] });
  }

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