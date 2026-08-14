import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RetourEvaluation } from '../../models/retour-evaluation';
import { RetourService } from '../../services/retour.service';
import { Cours } from '../../models/cours';
import { CoursService } from '../../services/cours.service';

@Component({
  selector: 'app-retours-evaluations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './retours-evaluations.html',
  styleUrl: './retours-evaluations.css',
})
export class RetoursEvaluations implements OnInit {
  retours: RetourEvaluation[] = [];
  mesCours: Cours[] = [];
  filtreActif: 'recents' | 'notes-basses' | 'actions-requises' = 'recents';
  coursSelectionne: string = '';

  constructor(private router: Router, private retourService: RetourService, private coursService: CoursService) {}

  ngOnInit(): void {
    this.retourService.getAll().subscribe({ next: list => this.retours = list, error: () => this.retours = [] });
    this.coursService.getAll().subscribe({ next: list => this.mesCours = list || [], error: () => this.mesCours = [] });
  }

  get retoursFiltres(): RetourEvaluation[] {
    let liste = this.retours.filter(r => !this.coursSelectionne || r.cours === this.coursSelectionne);
    if (this.filtreActif === 'notes-basses') {
      return [...liste].sort((a, b) => a.note - b.note);
    }
    if (this.filtreActif === 'actions-requises') {
      return liste.filter(r => r.statut === 'action-requise');
    }
    return liste;
  }

  get nbActionsRequises(): number {
    return this.retours.filter(r => r.statut === 'action-requise').length;
  }

  get noteMoyenne(): number {
    if (!this.retours.length) return 0;
    return this.retours.reduce((total, r) => total + r.note, 0) / this.retours.length;
  }

  get tauxSatisfaction(): number {
    if (!this.retours.length) return 0;
    return Math.round((this.retours.filter(r => r.note >= 4).length / this.retours.length) * 100);
  }

  togglePin(retour: RetourEvaluation) {
    this.retourService.togglePin(retour.id).subscribe({
      next: (misAJour) => {
        retour.epingle = misAJour.epingle;
      }
    });
  }

  ajusterCours() {
    this.router.navigate(['/espace-etudiant/formateur/mes-cours']);
  }

  etoiles(note: number): boolean[] {
    return Array.from({ length: 5 }, (_, i) => i < note);
  }
}
