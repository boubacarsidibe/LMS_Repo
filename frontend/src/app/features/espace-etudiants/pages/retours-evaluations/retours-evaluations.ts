import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { RETOURS_MOCK, RetourEvaluation } from '../../data/retours.mock';

@Component({
  selector: 'app-retours-evaluations',
  standalone: false,
  templateUrl: './retours-evaluations.html',
  styleUrl: './retours-evaluations.css',
})
export class RetoursEvaluations {
  retours: RetourEvaluation[] = RETOURS_MOCK;
  filtreActif: 'recents' | 'notes-basses' | 'actions-requises' = 'recents';

  constructor(private router: Router) {}

  get retoursFiltres(): RetourEvaluation[] {
    let liste = [...this.retours];
    if (this.filtreActif === 'notes-basses') {
      return liste.sort((a, b) => a.note - b.note);
    }
    if (this.filtreActif === 'actions-requises') {
      return liste.filter(r => r.statut === 'action-requise');
    }
    return liste; // déjà par ordre "plus récents" dans le mock
  }

  get nbActionsRequises(): number {
    return this.retours.filter(r => r.statut === 'action-requise').length;
  }

  togglePin(retour: RetourEvaluation) {
    retour.epingle = !retour.epingle;
  }

  ajusterCours() {
    this.router.navigate(['/espace-etudiant/formateur/mes-cours']);
  }

  etoiles(note: number): boolean[] {
    return Array.from({ length: 5 }, (_, i) => i < note);
  }
}
