import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Etudiant } from '../../models/etudiant';
import { EtudiantService } from '../../services/etudiant.service';
import { Cohorte } from '../../models/cohorte';
import { CohorteService } from '../../services/cohorte.service';
import { Cours } from '../../models/cours';
import { CoursService } from '../../services/cours.service';
import { NoteModule } from '../../models/note-module';
import { NoteModuleService } from '../../services/note-module.service';
import { PresenceHistorique } from '../../models/presence';
import { PresenceService } from '../../services/presence.service';
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';

interface ActiviteItem {
  type: 'note' | 'presence';
  date: Date;
  dateAffichee: string;
  titre: string;
  detail: string;
}

@Component({
  selector: 'app-profil-etudiant',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './profil-etudiant.html',
  styleUrl: './profil-etudiant.css',
})
export class ProfilEtudiant implements OnInit {
  etudiant: Etudiant | undefined;
  cohortes: Cohorte[] = [];
  cohorteSelectionneeId: number | null = null;
  isSaving = false;
  errorMessage = '';

  notes: NoteModule[] = [];
  mesCours: Cours[] = [];
  nouvelleNoteCoursId: number | null = null;
  nouvelleNoteModule = '';
  nouvelleNoteValeur: number | null = null;
  nouvelleNoteCommentaire = '';
  isSavingNote = false;
  noteErrorMessage = '';

  presences: PresenceHistorique[] = [];

  private etudiantId = 0;

  constructor(
    private route: ActivatedRoute,
    private etudiantService: EtudiantService,
    private cohorteService: CohorteService,
    private coursService: CoursService,
    private noteModuleService: NoteModuleService,
    private presenceService: PresenceService,
    private confirmation: ConfirmationService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.etudiantId = id;
      this.etudiantService.getById(id).subscribe({
        next: e => this.etudiant = e,
        error: () => this.etudiant = undefined
      });
      this.cohorteService.getAll().subscribe({
        next: list => this.cohortes = list,
        error: () => this.cohortes = []
      });
      this.coursService.getAll().subscribe({
        next: list => this.mesCours = list,
        error: () => this.mesCours = []
      });
      this.chargerNotes();
      this.chargerPresences();
    }
  }

  chargerNotes() {
    this.noteModuleService.getByEtudiant(this.etudiantId).subscribe({
      next: list => this.notes = list,
      error: () => this.notes = []
    });
  }

  chargerPresences() {
    this.presenceService.getByEtudiant(this.etudiantId).subscribe({
      next: list => this.presences = list,
      error: () => this.presences = []
    });
  }

  get tauxPresence(): number | null {
    if (!this.presences.length) return null;
    const presents = this.presences.filter(p => p.present).length;
    return Math.round((presents / this.presences.length) * 100);
  }

  get activiteTimeline(): ActiviteItem[] {
    const itemsNotes: ActiviteItem[] = this.notes.map(n => ({
      type: 'note',
      date: this.parseDateFr(n.dateEvaluation),
      dateAffichee: n.dateEvaluation,
      titre: `Note ajoutée — ${n.module}`,
      detail: `${n.note}/20 · ${n.coursCode}`,
    }));
    const itemsPresences: ActiviteItem[] = this.presences.map(p => ({
      type: 'presence',
      date: new Date(p.date),
      dateAffichee: this.formatDateIso(p.date),
      titre: p.present ? 'Présent' : 'Absent',
      detail: `${p.seanceTitre} · ${p.coursCode}`,
    }));
    return [...itemsNotes, ...itemsPresences].sort((a, b) => b.date.getTime() - a.date.getTime());
  }

  private parseDateFr(dateStr: string): Date {
    const [jour, mois, annee] = dateStr.split('/').map(Number);
    return new Date(annee, mois - 1, jour);
  }

  private formatDateIso(iso: string): string {
    const [annee, mois, jour] = iso.split('-');
    return `${jour}/${mois}/${annee}`;
  }

  ajouterNote() {
    if (!this.nouvelleNoteCoursId || !this.nouvelleNoteModule.trim() || this.nouvelleNoteValeur === null) return;

    this.isSavingNote = true;
    this.noteErrorMessage = '';
    this.noteModuleService
      .ajouter(this.etudiantId, this.nouvelleNoteCoursId, this.nouvelleNoteModule.trim(), this.nouvelleNoteValeur, this.nouvelleNoteCommentaire.trim())
      .subscribe({
        next: (note) => {
          this.isSavingNote = false;
          this.notes = [note, ...this.notes];
          this.nouvelleNoteCoursId = null;
          this.nouvelleNoteModule = '';
          this.nouvelleNoteValeur = null;
          this.nouvelleNoteCommentaire = '';
          this.rafraichirNoteMoyenne();
        },
        error: (error) => {
          this.isSavingNote = false;
          this.noteErrorMessage = error.error?.message || "L'ajout de la note a échoué.";
        },
      });
  }

  supprimerNote(note: NoteModule) {
    this.confirmation.demander({
      titre: 'Supprimer la note',
      message: `La note de ${note.note}/20 en « ${note.module} » sera supprimée et la moyenne recalculée.`,
      libelleConfirmer: 'Supprimer',
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.noteModuleService.supprimer(note.id).subscribe({
        next: () => {
          this.notes = this.notes.filter(n => n.id !== note.id);
          this.rafraichirNoteMoyenne();
        },
      });
    });
  }

  private rafraichirNoteMoyenne() {
    this.etudiantService.getById(this.etudiantId).subscribe({
      next: e => this.etudiant = e,
    });
  }

  assignerCohorte() {
    if (!this.etudiant || !this.cohorteSelectionneeId) return;

    this.isSaving = true;
    this.errorMessage = '';
    this.etudiantService.assignerCohorte(this.etudiant.id, this.cohorteSelectionneeId).subscribe({
      next: e => {
        this.isSaving = false;
        this.etudiant = e;
      },
      error: (error) => {
        this.isSaving = false;
        this.errorMessage = error.error?.message || "L'affectation à la cohorte a échoué.";
      }
    });
  }
}