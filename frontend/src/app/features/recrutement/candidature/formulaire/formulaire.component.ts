import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { RecrutementService, SessionCandidatureResponse, CandidatureRequest, CandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';

const STORAGE_KEY = 'candidature_brouillon';

interface CandidatureFormData {
  etape: number;
  sessionId: number | null;
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  adresse: string;
  typeFormation: string;
  niveauVise: string;
  programme: string;
  campus: string;
  sessionRentree: string;
  niveauActuel: string;
  etablissement: string;
  diplome: string;
  anneeObtention: string;
  fichierIdentite: string;
  fichierCV: string;
  fichierMotivation: string;
  fichierPhoto: string;
  consentement: boolean;
}

@Component({
  selector: 'app-formulaire',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FooterComponent, NavbarComponent],
  templateUrl: './formulaire.component.html'
})
export class FormulaireComponent implements OnInit {
  sessions: SessionCandidatureResponse[] = [];
  loading = false;
  candidature: CandidatureResponse | null = null;
  successMessage = '';

  etape = 1;
  readonly totalEtapes = 4;

  data: CandidatureFormData = {
    etape: 1,
    sessionId: null,
    nom: '',
    prenom: '',
    email: '',
    telephone: '',
    adresse: '',
    typeFormation: '',
    niveauVise: '',
    programme: '',
    campus: '',
    sessionRentree: '',
    niveauActuel: '',
    etablissement: '',
    diplome: '',
    anneeObtention: '',
    fichierIdentite: '',
    fichierCV: '',
    fichierMotivation: '',
    fichierPhoto: '',
    consentement: false,
  };

  constructor(
    private recrutementService: RecrutementService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.recrutementService.getCataloguePublic().subscribe({
      next: (data) => {
        this.sessions = data.filter(s => s.estActive);
      }
    });

    const saved = sessionStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        this.data = { ...this.data, ...parsed };
        this.etape = this.data.etape || 1;
      } catch { /* ignore */ }
    }
  }

  get etapeLabels(): string[] {
    return ['Profil', 'Formation', 'Parcours', 'Documents'];
  }

  precedent(): void {
    if (this.etape > 1) {
      this.etape--;
      this.saveDraft();
    }
  }

  suivant(): void {
    if (this.etape < this.totalEtapes) {
      this.etape++;
      this.saveDraft();
    }
  }

  saveDraft(): void {
    this.data.etape = this.etape;
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(this.data));
  }

  creerBrouillon(): void {
    if (!this.data.sessionId) {
      alert('Veuillez sélectionner une session.');
      return;
    }
    this.loading = true;
    const request: CandidatureRequest = {
      sessionCandidatureId: this.data.sessionId,
      informationsPersonnelles: JSON.stringify({
        nom: this.data.nom,
        prenom: this.data.prenom,
        email: this.data.email,
        telephone: this.data.telephone,
        adresse: this.data.adresse,
      }),
      informationsAcademiques: JSON.stringify({
        typeFormation: this.data.typeFormation,
        niveauVise: this.data.niveauVise,
        programme: this.data.programme,
        niveauActuel: this.data.niveauActuel,
        etablissement: this.data.etablissement,
        diplome: this.data.diplome,
        anneeObtention: this.data.anneeObtention,
      }),
      informationsProfessionnelles: JSON.stringify({
        campus: this.data.campus,
        sessionRentree: this.data.sessionRentree,
      }),
    };
    this.recrutementService.creerBrouillon(request).subscribe({
      next: (response) => {
        this.candidature = response;
        this.loading = false;
        sessionStorage.removeItem(STORAGE_KEY);
      },
      error: (err) => {
        alert('Erreur : ' + (err.error?.message || 'Erreur inconnue'));
        this.loading = false;
      }
    });
  }

  soumettre(): void {
    if (!this.candidature) return;
    if (!this.data.consentement) {
      alert('Veuillez accepter les conditions.');
      return;
    }
    this.loading = true;
    this.recrutementService.soumettreCandidature(this.candidature.id).subscribe({
      next: (response) => {
        this.candidature = response;
        this.loading = false;
        this.successMessage = 'Candidature soumise avec succès !';
        setTimeout(() => {
          this.router.navigate(['/recrutement/candidature/pieces'], { queryParams: { candidatureId: response.id } });
        }, 1500);
      },
      error: (err) => {
        alert('Erreur lors de la soumission : ' + (err.error?.message || 'Erreur inconnue'));
        this.loading = false;
      }
    });
  }
}