import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CoursService } from '../../services/cours.service';
import { Cours } from '../../models/cours';
import { SupportDetail } from '../../../pedagogie/services/pedagogie.service';
import { EspaceNavbarComponent } from '../../../../shared/components/espace-navbar/espace-navbar.component';

/**
 * Contenu d'un cours vu par l'étudiant : la fiche et les supports publiés par
 * le responsable pédagogique ou le formateur. Remplace le message
 * « contenu non disponible » qui bloquait l'accès depuis les cartes.
 */
@Component({
  selector: 'app-cours-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, EspaceNavbarComponent],
  templateUrl: './cours-detail.html',
})
export class CoursDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private coursService = inject(CoursService);

  cours: Cours | null = null;
  supports: SupportDetail[] = [];
  chargement = true;
  erreur: string | null = null;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.erreur = 'Cours introuvable.';
      this.chargement = false;
      return;
    }

    this.coursService.getCoursPublic(id).subscribe({
      next: (cours) => {
        this.cours = cours;
        this.chargement = false;
      },
      error: () => {
        this.erreur = "Ce cours n'existe pas ou n'est plus accessible.";
        this.chargement = false;
      },
    });

    this.coursService.getSupportsPublic(id).subscribe({
      next: (supports) => (this.supports = supports || []),
      error: () => (this.supports = []),
    });
  }

  /** Icône Material selon le type de ressource déclaré à la publication. */
  iconeSupport(type: string | null): string {
    switch ((type || '').toLowerCase()) {
      case 'video': return 'play_circle';
      case 'exercice': return 'assignment';
      case 'td':
      case 'tp': return 'science';
      case 'lien': return 'link';
      default: return 'description';
    }
  }
}
