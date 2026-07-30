import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RecrutementService, CandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-admin-candidatures',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-candidatures.component.html'
})
export class AdminCandidaturesComponent implements OnInit {
  candidatures: CandidatureResponse[] = [];
  motifRejet = '';

  constructor(private service: RecrutementService) { }

  ngOnInit(): void {
    this.service.getAllCandidatures().subscribe(data => this.candidatures = data);
  }

  validerPreselection(id: number): void {
    this.service.validerPreselection(id).subscribe(() => this.ngOnInit());
  }

  rejeter(id: number): void {
    const motif = prompt('Motif du rejet :') || 'Non spécifié';
    this.service.rejeterDossier(id, motif).subscribe(() => this.ngOnInit());
  }

  admettre(id: number): void {
    this.service.admettreCandidat(id).subscribe(() => this.ngOnInit());
  }

  simulerPaiement(id: number): void {
    this.service.simulerSuccesPaiement(id).subscribe(() => this.ngOnInit());
  }
}