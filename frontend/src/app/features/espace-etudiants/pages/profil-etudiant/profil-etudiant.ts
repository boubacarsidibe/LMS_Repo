import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Etudiant } from '../../models/etudiant';
import { EtudiantService } from '../../services/etudiant.service';
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
  /** Vrai tant que la fiche est demandée à l'API : sans lui, « Étudiant introuvable » s'affichait pendant l'attente. */
  chargementEtudiant = false;
  isSaving = false;
  errorMessage = '';

  notes: NoteModule[] = [];
  mesCours: Cours[] = [];
  nouvelleNoteCoursId: number | null = null;
  nouvelleNoteModule = '';
  /**
   * Saisie conservée en texte, et non en {@code type="number"} lié à un nombre.
   *
   * <p>Sur un champ numérique, la spécification HTML impose de vider {@code value}
   * dès que le contenu n'est pas un décimal <i>à point</i> : un « 15,5 » tapé au
   * pavé numérique français y était lu comme vide, la note restait à {@code null},
   * et le bouton « Ajouter la note » demeurait grisé sans qu'aucun message
   * n'explique pourquoi. La virgule est désormais convertie avant analyse.
   */
  nouvelleNoteTexte = '';
  nouvelleNoteCommentaire = '';
  isSavingNote = false;
  noteErrorMessage = '';

  /** Note saisie, ou {@code null} si elle n'est pas encore exploitable. */
  get nouvelleNoteValeur(): number | null {
    const brut = this.nouvelleNoteTexte.trim().replace(',', '.');
    if (brut === '' || !/^\d+(\.\d+)?$/.test(brut)) return null;
    const valeur = Number(brut);
    return valeur >= 0 && valeur <= 100 ? valeur : null;
  }

  /** Explique le refus plutôt que de laisser le bouton grisé sans raison. */
  get messageNoteSaisie(): string {
    if (this.nouvelleNoteTexte.trim() === '' || this.nouvelleNoteValeur !== null) return '';
    return 'Entrez une note comprise entre 0 et 100 (la virgule est acceptée).';
  }

  presences: PresenceHistorique[] = [];

  private etudiantId = 0;

  constructor(
    private route: ActivatedRoute,
    private etudiantService: EtudiantService,
    private coursService: CoursService,
    private noteModuleService: NoteModuleService,
    private presenceService: PresenceService,
    private confirmation: ConfirmationService
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.etudiantId = id;
      this.chargementEtudiant = true;
      this.etudiantService.getById(id).subscribe({
        next: e => { this.etudiant = e; this.chargementEtudiant = false; },
        error: () => { this.etudiant = undefined; this.chargementEtudiant = false; }
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
      detail: `${n.note}/100 (${n.note / 5}/20) · ${n.coursCode}`,
    }));
    const itemsPresences: ActiviteItem[] = this.presences.map(p => ({
      type: 'presence',
      date: this.parseDateFr(p.date),
      dateAffichee: p.date,
      titre: p.present ? 'Présent' : 'Absent',
      detail: `${p.seanceTitre} · ${p.coursCode}`,
    }));
    return [...itemsNotes, ...itemsPresences].sort((a, b) => b.date.getTime() - a.date.getTime());
  }

  /**
   * Analyse une date « jj/mm/aaaa », avec ou sans heure accolee.
   *
   * <p>Les presences arrivent au format « 06/08/2026 09:00 » : sans le retrait de
   * l'heure, l'annee valait « 2026 09:00 » et la date etait invalide, ce qui
   * desordonnait le fil d'activite.
   */
  private parseDateFr(dateStr: string): Date {
    if (!dateStr) {
      return new Date(0);
    }
    const [jour, mois, annee] = dateStr.trim().split(' ')[0].split('/').map(Number);
    if ([jour, mois, annee].some(isNaN)) {
      return new Date(0);
    }
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
          this.nouvelleNoteTexte = '';
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
      message: `La note de ${note.note}/100 (${note.note / 5}/20) en « ${note.module} » sera supprimée et la moyenne recalculée.`,
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

}
