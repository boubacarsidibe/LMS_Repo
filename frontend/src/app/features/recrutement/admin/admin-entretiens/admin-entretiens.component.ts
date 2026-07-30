import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RecrutementService, EntretienResponse } from '../../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-admin-entretiens',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-entretiens.component.html'
})
export class AdminEntretiensComponent implements OnInit {
  entretiens: EntretienResponse[] = [];
  dateHeure = '';
  lieuOuLien = '';
  noteEntretien: number | null = null;
  appreciationGlobale = '';
  candidatId: number | null = null;

  constructor(private service: RecrutementService) { }

  ngOnInit(): void { this.service.getAllEntretiens().subscribe(data => this.entretiens = data); }

  creer(): void {
    if (!this.candidatId) return;
    this.service.creerEntretien({
      dateHeure: this.dateHeure, lieuOuLien: this.lieuOuLien,
      noteEntretien: this.noteEntretien || 0, appreciationGlobale: this.appreciationGlobale,
      candidatId: this.candidatId
    }).subscribe(() => this.ngOnInit());
  }

  supprimer(id: number): void {
    if (confirm('Supprimer ?')) this.service.supprimerEntretien(id).subscribe(() => this.ngOnInit());
  }
}