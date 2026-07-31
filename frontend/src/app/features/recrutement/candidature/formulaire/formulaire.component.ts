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
  nom: string;
  prenom: string;
  email: string;
  telephone: string;
  adresse: string;
  dateNaissance: string;
  nationalite: string;
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
  showErrors = false;

  etape = 1;
  readonly totalEtapes = 4;

  data: CandidatureFormData = {
    etape: 1,
    nom: '',
    prenom: '',
    email: '',
    telephone: '',
    adresse: '',
    dateNaissance: '',
    nationalite: '',
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
    // Vérifier immédiatement si une candidature existe déjà → rediriger vers suivi
    this.recrutementService.getMesCandidatures().subscribe({
      next: (candidatures) => {
        const active = candidatures.find(c => c.statut !== 'REJETE' && c.statut !== 'BROUILLON');
        if (active) {
          this.router.navigate(['/recrutement/suivi']);
          return;
        }
        this.initCatalogue();
      },
      error: () => this.initCatalogue()
    });
  }

  private initCatalogue(): void {
    this.recrutementService.getCataloguePublic().subscribe({
      next: (data) => {
        this.sessions = data.filter(s => s.estActive);
      }
    });
    const saved = localStorage.getItem(STORAGE_KEY);
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
    if (!this.peutAvancer()) return;
    if (this.etape < this.totalEtapes) {
      this.etape++;
      this.saveDraft();
    }
  }

  peutAvancer(): boolean {
    this.showErrors = false;
    if (this.etape === 1) {
      if (!this.data.nom?.trim() || !this.data.prenom?.trim() || !this.data.email?.trim() || !this.data.telephone?.trim()) {
        this.showErrors = true;
        return false;
      }
    }
    if (this.etape === 2) {
      if (!this.data.programme?.trim()) {
        this.showErrors = true;
        return false;
      }
    }
    return true;
  }

  saveDraft(): void {
    this.data.etape = this.etape;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this.data));
  }

  soumettre(): void {
    if (!this.data.consentement) {
      alert('Veuillez accepter les conditions.');
      return;
    }
    this.loading = true;
    const sessionId = this.sessions.length > 0 ? this.sessions[0].id : 1;
    const request: CandidatureRequest = {
      sessionCandidatureId: sessionId,
      informationsPersonnelles: JSON.stringify({
        nom: this.data.nom,
        prenom: this.data.prenom,
        email: this.data.email,
        telephone: this.data.telephone,
        adresse: this.data.adresse,
        dateNaissance: this.data.dateNaissance,
        nationalite: this.data.nationalite,
        niveauEtude: this.data.niveauActuel,
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
    this.recrutementService.soumettreCandidature(request).subscribe({
      next: (response) => {
        this.candidature = response;
        this.loading = false;
        this.successMessage = 'Candidature soumise avec succès ! N° ' + response.numeroDossier;
        localStorage.removeItem(STORAGE_KEY);
        setTimeout(() => {
          this.router.navigate(['/recrutement/candidature/pieces'], { queryParams: { candidatureId: response.id } });
        }, 1500);
      },
      error: (err) => {
        this.loading = false;
        const msg = err.error?.message || err.message || 'Erreur inconnue';
        alert('Erreur lors de la soumission : ' + msg);
      }
    });
  }

  onFileSelected(event: Event, type: string): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const names = Array.from(input.files).map(f => f.name).join(', ');
      if (type === 'identite') {
        this.data.fichierIdentite = names;
      } else if (type === 'cv') {
        this.data.fichierCV = names;
      } else if (type === 'motivation') {
        this.data.fichierMotivation = names;
      } else if (type === 'photo') {
        this.data.fichierPhoto = names;
      }
      this.saveDraft();
    }
  }
}