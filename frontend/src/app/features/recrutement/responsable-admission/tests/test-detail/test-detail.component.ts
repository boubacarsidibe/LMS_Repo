import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { RecrutementService, TestAcademiqueRequest, TestAcademiqueResponse } from '../../../../../core/recrutement/services/recrutement.service';
import { downloadBlob } from '../../../../../shared/utils/download-file';
import { ConfirmationService } from '../../../../../shared/components/ui/confirmation.service';

@Component({ selector: 'app-test-detail', standalone: true, imports: [CommonModule, RouterModule, ReactiveFormsModule], templateUrl: './test-detail.component.html' })
export class TestDetailComponent implements OnInit {
  test: TestAcademiqueResponse | null = null;
  loading = true; saving = false; edition = false; error = '';
  form: FormGroup;
  constructor(private route: ActivatedRoute, private router: Router, private service: RecrutementService, private fb: FormBuilder, private confirmation: ConfirmationService) {
    this.form = this.fb.group({ nomEpreuve: ['', Validators.required], coefficient: [1, [Validators.required, Validators.min(0.1)]], dateEpreuve: ['', Validators.required], lieuOuLien: [''] });
  }
  ngOnInit(): void { this.charger(); }
  charger(): void { const id = Number(this.route.snapshot.paramMap.get('id')); this.service.getTest(id).subscribe({ next: test => { this.test = test; this.form.patchValue({ nomEpreuve: test.nomEpreuve, coefficient: test.coefficient, dateEpreuve: test.dateEpreuve?.slice(0, 16), lieuOuLien: test.lieuOuLien ?? '' }); this.loading = false; }, error: () => { this.error = 'Impossible de charger ce test.'; this.loading = false; } }); }
  enregistrer(): void { if (!this.test || this.form.invalid) { this.form.markAllAsTouched(); return; } this.saving = true; const value = this.form.getRawValue(); const request: TestAcademiqueRequest = { nomEpreuve: value.nomEpreuve!, coefficient: Number(value.coefficient), dateEpreuve: value.dateEpreuve!, lieuOuLien: value.lieuOuLien ?? null, candidatId: this.test.candidatId, noteObtenue: this.test.noteObtenue }; this.service.modifierGroupeTest(this.test.groupeEpreuveId, request).subscribe({ next: lignes => { this.test = lignes[0]; this.edition = false; this.saving = false; }, error: e => { this.error = e.error?.message || 'Modification impossible.'; this.saving = false; } }); }
  supprimer(): void {
    if (!this.test) return;
    const test = this.test;
    this.confirmation.demander({
      titre: "Supprimer l'épreuve",
      message: `Le test « ${test.nomEpreuve} » et toutes ses notes seront supprimés définitivement.`,
      libelleConfirmer: "Supprimer l'épreuve",
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.service.supprimerGroupeTest(test.groupeEpreuveId).subscribe({ next: () => this.router.navigate(['/responsable-admission/tests']), error: e => this.error = e.error?.message || 'Suppression impossible.' });
    });
  }
  exporterNotes(): void {
    if (!this.test) return;
    this.service.exporterNotesTest(this.test.id).subscribe({
      next: fichier => downloadBlob(fichier, `releve_notes_${this.nomFichier(this.test!.nomEpreuve)}.xlsx`),
      error: () => this.error = 'L’export des notes est impossible.',
    });
  }
  private nomFichier(nom: string): string {
    return nom.trim().replace(/[\\/:*?"<>|]+/g, '-').replace(/\s+/g, '_') || 'test';
  }
}
