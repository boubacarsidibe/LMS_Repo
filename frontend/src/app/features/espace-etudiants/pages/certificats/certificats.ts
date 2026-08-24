import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Certificat } from '../../models/certificat';
import { CertificatService } from '../../services/certificat.service';
import { Auth } from '../../../../core/auth/services/authService';

@Component({
  selector: 'app-certificats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './certificats.html',
  styleUrl: './certificats.css',
})
export class Certificats implements OnInit {
  private auth = inject(Auth);

  // Voir Annonces pour le detail de ce mecanisme.

  certificats: Certificat[] = [];
  telechargementEnCoursId: number | null = null;

  constructor(private certificatService: CertificatService) {}

  ngOnInit(): void {
    this.chargerCertificats();
  }

  private chargerCertificats(): void {
    this.certificatService.getAll().subscribe({
      next: (data) => (this.certificats = data || []),
      error: () => (this.certificats = []),
    });
  }

  get isFormateurRole(): boolean {
    return this.auth.isFormateur();
  }

  telecharger(certificat: Certificat): void {
    this.telechargementEnCoursId = certificat.id;
    this.certificatService.telechargerPdf(certificat.id).subscribe({
      next: (blob) => {
        this.telechargementEnCoursId = null;
        const url = window.URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = `certificat-${certificat.reference}.pdf`;
        lien.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.telechargementEnCoursId = null;
      },
    });
  }
}
