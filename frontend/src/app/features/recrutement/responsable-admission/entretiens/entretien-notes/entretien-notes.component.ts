import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import {
  EntretienResponse,
  RecrutementService,
} from '../../../../../core/recrutement/services/recrutement.service';

interface LigneNote extends EntretienResponse {
  candidat: string;
  numeroDossierAffiche: string;
}

@Component({
  selector: 'app-entretien-notes',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './entretien-notes.component.html',
})
export class EntretienNotesComponent implements OnInit {
  entretien: EntretienResponse | null = null;
  lignes: LigneNote[] = [];
  original = new Map<number, number | null>();
  loading = true;
  saving = false;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: RecrutementService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.service.getEntretien(id).subscribe({
      next: entretien => {
        this.entretien = entretien;
        if (!entretien.groupeEntretienId) {
          this.appliquerLignes([entretien]);
          this.error = 'Cet entretien ancien ne possede pas encore de groupe de notation.';
          return;
        }
        this.service.getNotesGroupeEntretien(entretien.groupeEntretienId).subscribe({
          next: lignes => this.appliquerLignes(lignes),
          error: e => {
            this.appliquerLignes([entretien]);
            this.error = e.error?.message || 'Impossible de charger la liste complete des candidats.';
          },
        });
      },
      error: () => {
        this.loading = false;
        this.error = 'Entretien introuvable.';
      },
    });
  }

  private appliquerLignes(lignes: EntretienResponse[]): void {
    this.lignes = lignes.map(ligne => Object.assign(ligne, {
      candidat: `${ligne.candidatPrenom || ''} ${ligne.candidatNom || ''}`.trim()
        || 'Candidat non disponible',
      numeroDossierAffiche: ligne.numeroDossier || '-',
    }));
    this.original.clear();
    this.lignes.forEach(ligne => this.original.set(ligne.id, ligne.noteEntretien));
    this.loading = false;
  }

  get modifie(): boolean {
    return this.lignes.some(ligne => ligne.noteEntretien !== this.original.get(ligne.id));
  }

  enregistrer(): void {
    if (!this.entretien?.groupeEntretienId) return;
    const notes: Record<number, number | null> = {};
    this.lignes
      .filter(ligne => ligne.noteEntretien !== this.original.get(ligne.id))
      .forEach(ligne => notes[ligne.id] = ligne.noteEntretien);
    if (!Object.keys(notes).length) return;
    if (Object.values(notes).some(note => note !== null && (note < 0 || note > 20))) {
      this.error = 'Chaque note doit etre comprise entre 0 et 20.';
      return;
    }

    this.saving = true;
    this.error = '';
    this.service.enregistrerNotesGroupeEntretien(this.entretien.groupeEntretienId, notes).subscribe({
      next: () => this.router.navigate(['/responsable-admission/entretiens', this.entretien!.id]),
      error: e => {
        this.saving = false;
        this.error = e.error?.message || 'L\'enregistrement des notes a echoue.';
      },
    });
  }
}
