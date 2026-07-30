import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { RecrutementService, CandidatureResponse, PieceJustificativeResponse } from '../../../../core/recrutement/services/recrutement.service';

@Component({
  selector: 'app-pieces',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './pieces.component.html'
})
export class PiecesComponent implements OnInit {
  candidature: CandidatureResponse | null = null;
  candidatureId: number | null = null;
  typesDocuments = ['CV', 'DIPLOME', 'RELEVE_NOTES', 'PIECE_IDENTITE', 'LETTRE_MOTIVATION'];
  uploading = false;

  constructor(private route: ActivatedRoute, private recrutementService: RecrutementService) { }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.candidatureId = params['candidatureId'] ? +params['candidatureId'] : null;
      if (this.candidatureId) {
        this.recrutementService.getCandidature(this.candidatureId).subscribe({
          next: (data) => this.candidature = data
        });
      }
    });
  }

  getPieceStatus(typeDocument: string): PieceJustificativeResponse | undefined {
    if (!this.candidature) return undefined;
    return this.candidature.piecesJustificatives.find(p => p.typeDocument === typeDocument);
  }

  onFileSelected(event: Event, typeDocument: string): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length || !this.candidatureId) return;

    const file = input.files[0];
    this.uploading = true;

    this.recrutementService.televerserPiece(this.candidatureId, file, typeDocument).subscribe({
      next: () => {
        alert(`Pièce "${typeDocument}" téléversée avec succès !`);
        this.uploading = false;
        // Recharger la candidature
        if (this.candidatureId) {
          this.recrutementService.getCandidature(this.candidatureId).subscribe({
            next: (data) => this.candidature = data
          });
        }
      },
      error: (err) => {
        alert('Erreur: ' + (err.error?.message || 'Échec du téléversement'));
        this.uploading = false;
      }
    });
  }
}