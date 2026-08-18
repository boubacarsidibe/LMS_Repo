import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { FormateurBref, PedagogieService, SupportDetail } from '../../services/pedagogie.service';
import { Cours } from '../../../espace-etudiants/models/cours';
import { NotificationService } from '../../../../shared/components/ui/notification.service';

/**
 * Fiche d'un cours, côté responsable pédagogique.
 *
 * <p>Cet écran servait à publier et retirer des supports. Ce n'est pas le rôle du
 * responsable pédagogique : le contenu d'un cours est conçu par celui qui l'enseigne.
 * S'il le faisait, c'est qu'il n'avait <strong>aucun moyen de désigner l'enseignant</strong> —
 * il bouchait le trou à la place du formateur.
 *
 * <p>L'écran fait donc désormais ce qui lui revient : composer l'équipe pédagogique,
 * et constater ce qui a été publié sans y toucher.
 */
@Component({
  selector: 'app-pedagogie-cours-supports',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './pedagogie-cours-supports.component.html',
})
export class PedagogieCoursSupportsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private pedagogie = inject(PedagogieService);
  private notifications = inject(NotificationService);

  coursId = 0;
  cours: Cours | null = null;
  supports: SupportDetail[] = [];
  formateurs: FormateurBref[] = [];
  chargement = true;
  erreur: string | null = null;

  /** Identifiants cochés dans le panneau d'affectation, avant enregistrement. */
  selection = new Set<number>();
  affectation = false;

  ngOnInit(): void {
    this.coursId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.coursId) {
      this.erreur = 'Cours introuvable.';
      this.chargement = false;
      return;
    }
    forkJoin({
      cours: this.pedagogie.getCours(),
      formateurs: this.pedagogie.getFormateurs(),
      supports: this.pedagogie.getSupports(this.coursId),
    }).subscribe({
      next: ({ cours, formateurs, supports }) => {
        this.cours = (cours || []).find((c) => c.id === this.coursId) || null;
        this.formateurs = formateurs || [];
        this.supports = supports || [];
        this.reinitialiserSelection();
        this.chargement = false;
      },
      error: () => {
        this.erreur = 'La fiche du cours n’a pas pu être chargée.';
        this.chargement = false;
      },
    });
  }

  private reinitialiserSelection(): void {
    this.selection = new Set((this.cours?.formateurs || []).map((f) => f.id));
  }

  /** Un cours sans formateur n'apparaît dans aucun espace enseignant. */
  get sansFormateur(): boolean {
    return !this.cours?.formateurs?.length;
  }

  estSelectionne(id: number): boolean {
    return this.selection.has(id);
  }

  basculer(id: number): void {
    if (this.selection.has(id)) {
      this.selection.delete(id);
    } else {
      this.selection.add(id);
    }
  }

  /** Vrai tant que la sélection diffère de l'équipe enregistrée. */
  get selectionModifiee(): boolean {
    const enregistres = (this.cours?.formateurs || []).map((f) => f.id);
    return (
      enregistres.length !== this.selection.size ||
      enregistres.some((id) => !this.selection.has(id))
    );
  }

  enregistrerEquipe(): void {
    if (this.affectation || this.selection.size === 0) {
      return;
    }
    this.affectation = true;
    this.erreur = null;

    this.pedagogie.affecterFormateurs(this.coursId, [...this.selection]).subscribe({
      next: (cours) => {
        this.affectation = false;
        this.cours = cours;
        this.reinitialiserSelection();
        this.notifications.succes('L’équipe pédagogique a été enregistrée.');
      },
      error: (err) => {
        this.affectation = false;
        this.erreur = err?.error?.message || 'L’affectation a échoué.';
      },
    });
  }

  annulerSelection(): void {
    this.reinitialiserSelection();
  }
}
