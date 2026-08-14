import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CoursService } from '../../services/cours.service';
import { Cours } from '../../models/cours';
import { EspaceNavbarComponent } from '../../../../shared/components/espace-navbar/espace-navbar.component';

/**
 * Liste des cours accessibles à l'étudiant, point d'entrée vers le contenu de
 * chacun. Distincte de MesCours, qui liste les cours dont le formateur connecté
 * est propriétaire.
 */
@Component({
  selector: 'app-mes-cours-etudiant',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, EspaceNavbarComponent],
  templateUrl: './mes-cours-etudiant.html',
})
export class MesCoursEtudiant implements OnInit {
  private coursService = inject(CoursService);

  cours: Cours[] = [];
  chargement = true;
  recherche = '';

  ngOnInit(): void {
    this.coursService.getAllPublic().subscribe({
      next: (data) => {
        this.cours = data || [];
        this.chargement = false;
      },
      error: () => {
        this.cours = [];
        this.chargement = false;
      },
    });
  }

  get coursFiltres(): Cours[] {
    const terme = this.recherche.trim().toLowerCase();
    if (!terme) return this.cours;
    return this.cours.filter((c) =>
      (c.titre || '').toLowerCase().includes(terme) || (c.code || '').toLowerCase().includes(terme));
  }
}
