import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormatRapport } from '../../../core/recrutement/services/recrutement.service';

/**
 * Bouton « Rapport » avec choix de format (PDF ou Excel) — réutilisé par les
 * pages Candidatures, Tests et Entretiens du back-office recrutement, pour ne
 * pas dupliquer le petit menu déroulant trois fois.
 */
@Component({
  selector: 'app-report-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="relative inline-block">
      <button
        type="button"
        [disabled]="generationEnCours"
        (click)="toggleMenu()"
        class="flex items-center gap-2 px-4 py-2 bg-surface-container-lowest border border-outline-variant rounded-lg font-label-md text-label-md text-primary hover:bg-surface-container-low transition-colors shadow-sm disabled:opacity-60">
        <span class="material-symbols-outlined text-[18px]">description</span>
        {{ generationEnCours ? 'Génération…' : label }}
        <span class="material-symbols-outlined text-[18px]">{{ menuOuvert ? 'expand_less' : 'expand_more' }}</span>
      </button>

      <div *ngIf="menuOuvert" class="fixed inset-0 z-10" (click)="menuOuvert = false"></div>

      <div *ngIf="menuOuvert" class="absolute right-0 mt-2 w-44 bg-surface border border-outline-variant rounded-lg shadow-lg z-20 py-1 origin-top-right">
        <button type="button" (click)="choisir('pdf')" class="w-full flex items-center gap-3 px-4 py-2.5 font-body-sm text-body-sm text-on-surface-variant hover:bg-surface-container-low hover:text-primary transition-colors">
          <span class="material-symbols-outlined text-[18px]">picture_as_pdf</span>
          PDF
        </button>
        <button type="button" (click)="choisir('xlsx')" class="w-full flex items-center gap-3 px-4 py-2.5 font-body-sm text-body-sm text-on-surface-variant hover:bg-surface-container-low hover:text-primary transition-colors">
          <span class="material-symbols-outlined text-[18px]">table_chart</span>
          Excel
        </button>
      </div>
    </div>
  `,
})
export class ReportButtonComponent {
  @Input() label = 'Rapport';
  @Input() generationEnCours = false;
  @Output() genererRapport = new EventEmitter<FormatRapport>();

  menuOuvert = false;

  toggleMenu(): void {
    if (this.generationEnCours) return;
    this.menuOuvert = !this.menuOuvert;
  }

  choisir(format: FormatRapport): void {
    this.menuOuvert = false;
    this.genererRapport.emit(format);
  }
}
