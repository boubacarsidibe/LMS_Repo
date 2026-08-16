import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RetourEvaluation } from '../../models/retour-evaluation';
import { RetourService } from '../../services/retour.service';
import { Cours } from '../../models/cours';
import { CoursService } from '../../services/cours.service';
import { NotificationService } from '../../../../shared/components/ui/notification.service';

type Onglet = 'recents' | 'notes-basses' | 'actions-requises';

@Component({
  selector: 'app-retours-evaluations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './retours-evaluations.html',
  styleUrl: './retours-evaluations.css',
})
export class RetoursEvaluations implements OnInit {
  private router = inject(Router);
  private retourService = inject(RetourService);
  private coursService = inject(CoursService);
  private notifications = inject(NotificationService);

  retours: RetourEvaluation[] = [];
  mesCours: Cours[] = [];
  chargement = true;
  erreur = '';

  filtreActif: Onglet = 'recents';
  /** Id du cours sélectionné, ou null pour « tous ». */
  coursSelectionne: number | null = null;

  ngOnInit(): void {
    this.charger();
    this.coursService.getAll().subscribe({
      next: (list) => (this.mesCours = list || []),
      error: () => (this.mesCours = []),
    });
  }

  private charger(): void {
    this.chargement = true;
    this.retourService.getAll().subscribe({
      next: (list) => {
        this.retours = list;
        this.chargement = false;
      },
      error: () => {
        this.retours = [];
        this.erreur = 'Impossible de charger les retours pour le moment.';
        this.chargement = false;
      },
    });
  }

  /** Avis du cours sélectionné — base commune aux indicateurs et à la liste. */
  get retoursDuPerimetre(): RetourEvaluation[] {
    if (this.coursSelectionne === null) return this.retours;
    return this.retours.filter((r) => r.coursId === this.coursSelectionne);
  }

  get retoursFiltres(): RetourEvaluation[] {
    const liste = [...this.retoursDuPerimetre];
    // Un avis épinglé reste en tête quel que soit l'onglet : c'est tout l'intérêt
    // de l'épingler.
    const epinglesDAbord = (a: RetourEvaluation, b: RetourEvaluation) =>
      Number(b.epingle) - Number(a.epingle);

    if (this.filtreActif === 'notes-basses') {
      return liste.sort((a, b) => epinglesDAbord(a, b) || a.note - b.note);
    }
    if (this.filtreActif === 'actions-requises') {
      return liste
        .filter((r) => r.statut === 'action-requise')
        .sort((a, b) => epinglesDAbord(a, b) || this.parDateDesc(a, b));
    }
    return liste.sort((a, b) => epinglesDAbord(a, b) || this.parDateDesc(a, b));
  }

  private parDateDesc(a: RetourEvaluation, b: RetourEvaluation): number {
    return new Date(b.dateIso).getTime() - new Date(a.dateIso).getTime();
  }

  get nbActionsRequises(): number {
    return this.retoursDuPerimetre.filter((r) => r.statut === 'action-requise').length;
  }

  get noteMoyenne(): number {
    const liste = this.retoursDuPerimetre;
    if (!liste.length) return 0;
    return liste.reduce((total, r) => total + r.note, 0) / liste.length;
  }

  get tauxSatisfaction(): number {
    const liste = this.retoursDuPerimetre;
    if (!liste.length) return 0;
    return Math.round((liste.filter((r) => r.note >= 4).length / liste.length) * 100);
  }

  /** Libellé des indicateurs : ils suivent le cours sélectionné. */
  get libellePerimetre(): string {
    if (this.coursSelectionne === null) return 'Tous vos cours';
    return this.mesCours.find((c) => Number(c.id) === this.coursSelectionne)?.titre ?? 'Cours sélectionné';
  }

  togglePin(retour: RetourEvaluation): void {
    const etatPrecedent = retour.epingle;
    // Bascule optimiste : la carte remonte immédiatement, on revient en arrière
    // si le serveur refuse.
    retour.epingle = !etatPrecedent;
    this.retourService.togglePin(retour.id).subscribe({
      next: (misAJour) => (retour.epingle = misAJour.epingle),
      error: () => {
        retour.epingle = etatPrecedent;
        this.notifications.erreur("L'épinglage n'a pas pu être enregistré.");
      },
    });
  }

  /** Export CSV des avis affichés, ouvrable directement dans Excel. */
  exporter(): void {
    const liste = this.retoursFiltres;
    if (!liste.length) {
      this.notifications.info('Aucun avis à exporter pour cette sélection.');
      return;
    }
    const echapper = (valeur: string | number) => `"${String(valeur).replace(/"/g, '""')}"`;
    const lignes = [
      ['Date', 'Cours', 'Note sur 5', 'Statut', 'Commentaire'].join(';'),
      ...liste.map((r) =>
        [
          echapper(r.date),
          echapper(r.cours),
          r.note,
          echapper(r.statut === 'action-requise' ? 'Action requise' : 'Validé'),
          echapper(r.commentaire),
        ].join(';'),
      ),
    ];
    // Le BOM force Excel à lire le fichier en UTF-8 (sinon les accents cassent).
    const blob = new Blob(['﻿' + lignes.join('\r\n')], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const lien = document.createElement('a');
    lien.href = url;
    lien.download = `retours_evaluations_${new Date().toISOString().slice(0, 10)}.csv`;
    lien.click();
    URL.revokeObjectURL(url);
    this.notifications.succes(`${liste.length} avis exporté(s).`);
  }

  /** Ouvre « Mes cours » directement sur le cours visé par l'avis. */
  ajusterCours(retour: RetourEvaluation): void {
    this.router.navigate(['/espace-etudiant/formateur/mes-cours'], {
      queryParams: { cours: retour.coursId },
    });
  }

  etoiles(note: number): boolean[] {
    return Array.from({ length: 5 }, (_, i) => i < note);
  }

  /**
   * Répartition des notes de 5 à 1.
   *
   * <p>La moyenne seule masque les avis polarisés : 1 et 5 en nombre égal donnent
   * la même moyenne que des 3 partout, alors que les deux situations n'appellent
   * pas la même réaction.
   */
  get repartition(): { note: number; nombre: number; pourcentage: number }[] {
    const liste = this.retoursDuPerimetre;
    return [5, 4, 3, 2, 1].map((note) => {
      const nombre = liste.filter((r) => r.note === note).length;
      return {
        note,
        nombre,
        pourcentage: liste.length ? Math.round((nombre / liste.length) * 100) : 0,
      };
    });
  }
}
