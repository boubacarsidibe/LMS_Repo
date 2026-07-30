import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { RecrutementService, CandidatureResponse } from '../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-suivi',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './suivi.component.html'
})
export class SuiviComponent implements OnInit {
  candidatures: CandidatureResponse[] = [];
  loading = true;
  rechercheDossier = '';

  constructor(private recrutementService: RecrutementService) { }

  ngOnInit(): void {
    this.recrutementService.getMesCandidatures().subscribe({
      next: (data) => {
        this.candidatures = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  suivreDossier(): void {
    if (!this.rechercheDossier.trim()) return;
    this.recrutementService.suivreAvancement(this.rechercheDossier.trim()).subscribe({
      next: (data) => {
        const existsLocally = this.candidatures.find(c => c.id === data.id);
        if (!existsLocally) {
          this.candidatures = [data, ...this.candidatures];
        } else {
          this.candidatures = this.candidatures.map(c => c.id === data.id ? data : c);
        }
      },
      error: (err) => {
        alert('Dossier non trouvé: ' + (err.error?.message || ''));
      }
    });
  }

  getStatutColor(statut: string): string {
    switch (statut) {
      case 'ADMIS': return 'text-green-600';
      case 'REJETE': return 'text-red-600';
      case 'BROUILLON': return 'text-gray-500';
      default: return 'text-blue-600';
    }
  }
}