import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface ColonneExport {
  key: string;
  label: string;
}

/**
 * Modal générique de sélection des colonnes avant un export Excel.
 * Réutilisé par les pages Sessions et Candidatures du back-office.
 */
@Component({
  selector: 'app-export-columns-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div *ngIf="open" class="fixed inset-0 bg-primary/20 backdrop-blur-sm z-[60] flex items-center justify-center p-4" (click)="fermer()">
      <div class="bg-surface rounded-xl shadow-lg w-full max-w-md border border-outline-variant" (click)="$event.stopPropagation()">
        <div class="px-6 py-4 border-b border-outline-variant flex items-center justify-between">
          <h3 class="font-headline-sm text-headline-sm text-primary">Colonnes à exporter</h3>
          <button type="button" (click)="fermer()" class="p-1 text-on-surface-variant hover:bg-surface-variant rounded-full transition-colors"><span class="material-symbols-outlined">close</span></button>
        </div>
        <div class="p-6 space-y-2 max-h-80 overflow-y-auto">
          <label *ngFor="let c of colonnes" class="flex items-center gap-3 py-1.5 cursor-pointer">
            <input type="checkbox" [checked]="selection.has(c.key)" (change)="toggle(c.key)" class="w-4 h-4 accent-secondary">
            <span class="font-body-sm text-body-sm text-on-surface">{{ c.label }}</span>
          </label>
          <p class="font-body-sm text-body-sm text-on-surface-variant pt-2">Un graphique de synthèse est automatiquement inclus dans le fichier exporté.</p>
        </div>
        <div class="p-6 border-t border-outline-variant flex justify-end gap-3">
          <button type="button" class="px-4 py-2 font-label-md text-label-md text-on-surface-variant bg-surface border border-outline-variant rounded-lg hover:bg-surface-variant transition-colors" (click)="fermer()">Annuler</button>
          <button type="button" [disabled]="selection.size === 0" class="px-4 py-2 font-label-md text-label-md text-on-primary bg-secondary rounded-lg hover:bg-secondary/90 transition-colors shadow-sm disabled:opacity-60" (click)="valider()">
            Exporter
          </button>
        </div>
      </div>
    </div>
  `,
})
export class ExportColumnsModalComponent implements OnChanges {
  @Input() open = false;
  @Input() colonnes: ColonneExport[] = [];
  @Input() selectionnees: string[] = [];
  @Output() openChange = new EventEmitter<boolean>();
  @Output() exporter = new EventEmitter<string[]>();

  selection = new Set<string>();

  ngOnChanges(): void {
    if (this.selection.size === 0 && this.selectionnees.length) {
      this.selection = new Set(this.selectionnees);
    }
  }

  toggle(key: string): void {
    if (this.selection.has(key)) this.selection.delete(key);
    else this.selection.add(key);
  }

  fermer(): void {
    this.open = false;
    this.openChange.emit(false);
  }

  valider(): void {
    this.exporter.emit(Array.from(this.selection));
    this.fermer();
  }
}
