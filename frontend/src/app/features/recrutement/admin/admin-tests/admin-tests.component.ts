import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RecrutementService, TestAcademiqueResponse } from '../../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-admin-tests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-tests.component.html'
})
export class AdminTestsComponent implements OnInit {
  tests: TestAcademiqueResponse[] = [];
  nomEpreuve = '';
  coefficient = 1;
  noteObtenue: number | null = null;
  dateEpreuve = '';
  candidatId: number | null = null;

  constructor(private service: RecrutementService) { }

  ngOnInit(): void { this.service.getAllTests().subscribe(data => this.tests = data); }

  creer(): void {
    if (!this.candidatId) return;
    this.service.creerTest({
      nomEpreuve: this.nomEpreuve, coefficient: this.coefficient,
      noteObtenue: this.noteObtenue || 0, dateEpreuve: this.dateEpreuve, candidatId: this.candidatId
    }).subscribe(() => this.ngOnInit());
  }

  supprimer(id: number): void {
    if (confirm('Supprimer ?')) this.service.supprimerTest(id).subscribe(() => this.ngOnInit());
  }
}