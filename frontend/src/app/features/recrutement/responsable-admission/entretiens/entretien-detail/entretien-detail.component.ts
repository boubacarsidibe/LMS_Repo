import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ConfirmationService } from '../../../../../shared/components/ui/confirmation.service';
import { CandidatureResponse, EntretienResponse, RecrutementService } from '../../../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-entretien-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './entretien-detail.component.html',
})
export class EntretienDetailComponent implements OnInit {
  entretien: EntretienResponse | null = null;
  groupe: EntretienResponse[] = [];
  candidatures: CandidatureResponse[] = [];
  loading = true;
  error = '';

  constructor(private route: ActivatedRoute, private router: Router, private service: RecrutementService, private confirmation: ConfirmationService) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    forkJoin({ entretiens: this.service.getAllEntretiens(), candidatures: this.service.getAllCandidatures() }).subscribe({
      next: ({ entretiens, candidatures }) => {
        this.entretien = (entretiens || []).find((item) => item.id === id) || null;
        this.candidatures = candidatures || [];
        this.groupe = this.entretien ? (entretiens || []).filter((item) => this.cleGroupe(item) === this.cleGroupe(this.entretien!)) : [];
        this.loading = false;
        if (!this.entretien) this.error = 'Entretien introuvable.';
      },
      error: () => { this.loading = false; this.error = 'Impossible de charger cet entretien.'; },
    });
  }

  candidat(entretien: EntretienResponse): CandidatureResponse | undefined {
    return this.candidatures.find((c) => c.candidatId === entretien.candidatId);
  }

  supprimer(): void {
    if (!this.entretien) return;
    this.confirmation.demander({
      titre: "Supprimer l'entretien",
      message: 'Cet entretien et les convocations de tous ses candidats seront supprimés.',
      libelleConfirmer: "Supprimer l'entretien",
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      forkJoin(this.groupe.map((item) => this.service.supprimerEntretien(item.id))).subscribe({
        next: () => this.router.navigate(['/responsable-admission/entretiens']),
        error: () => this.error = 'La suppression du groupe a échoué.',
      });
    });
  }

  private cleGroupe(entretien: EntretienResponse): string {
    return entretien.groupeEntretienId || `${entretien.dateHeure}|${entretien.lieuOuLien}`;
  }
}
