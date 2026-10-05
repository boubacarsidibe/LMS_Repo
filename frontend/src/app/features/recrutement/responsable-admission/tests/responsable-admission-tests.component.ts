import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { RecrutementService, TestAcademiqueResponse, FormatRapport } from '../../../../core/recrutement/services/recrutement.service';
import { ReportButtonComponent } from '../../../../shared/components/report-button/report-button.component';
import { downloadBlob } from '../../../../shared/utils/download-file';

interface TestListe {
  id: number;
  groupeId: string;
  nom: string;
  session: string | null;
  date: string | null;
  lieu: string | null;
  coefficient: number;
  inscrits: number;
  notesSaisies: number;
}

@Component({
  selector: 'app-responsable-admission-tests',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ReportButtonComponent],
  templateUrl: './responsable-admission-tests.component.html',
})
export class ResponsableAdmissionTestsComponent implements OnInit {
  tests: TestListe[] = [];
  recherche = '';
  loading = true;
  errorMessage = '';
  generatingReport = false;

  constructor(private recrutementService: RecrutementService) {}

  ngOnInit(): void {
    this.recrutementService.getAllTests().subscribe({
      next: (lignes) => {
        this.tests = this.regrouper(lignes || []);
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les tests académiques.';
        this.loading = false;
      },
    });
  }

  get testsFiltres(): TestListe[] {
    const q = this.recherche.trim().toLocaleLowerCase();
    return !q ? this.tests : this.tests.filter((test) =>
      `${test.nom} ${test.session ?? ''} ${test.lieu ?? ''}`.toLocaleLowerCase().includes(q));
  }

  private regrouper(lignes: TestAcademiqueResponse[]): TestListe[] {
    const groupes = new Map<string, TestAcademiqueResponse[]>();
    lignes.forEach((ligne) => {
      const cle = ligne.groupeEpreuveId || String(ligne.id);
      groupes.set(cle, [...(groupes.get(cle) ?? []), ligne]);
    });
    return Array.from(groupes.entries()).map(([groupeId, items]) => {
      const test = items[0];
      return {
        id: test.id, groupeId, nom: test.nomEpreuve, session: test.sessionCandidatureLibelle,
        date: test.dateEpreuve, lieu: test.lieuOuLien, coefficient: test.coefficient,
        inscrits: items.length, notesSaisies: items.filter((item) => item.noteObtenue != null).length,
      };
    }).sort((a, b) => (a.date ?? '').localeCompare(b.date ?? ''));
  }

  genererRapport(format: FormatRapport): void {
    this.generatingReport = true;
    this.recrutementService.genererRapportTests(format).subscribe({
      next: (blob) => { downloadBlob(blob, `rapport_tests_${new Date().toISOString().slice(0, 10)}.${format}`); this.generatingReport = false; },
      error: () => { this.errorMessage = 'Le rapport n’a pas pu être généré.'; this.generatingReport = false; },
    });
  }
}
