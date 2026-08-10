import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { RecrutementService, SessionCandidatureResponse, CandidatureRequest, CandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';

const STORAGE_KEY = 'candidature_brouillon';

interface CandidatureFormData {
  etape: number;
  nom: string; prenom: string; email: string; telephone: string;
  adresse: string; phonePrefix: string; dateNaissance: string; nationalite: string;
  typeFormation: string; niveauVise: string; programme: string; campus: string;
  sessionRentree: string; niveauActuel: string; etablissement: string;
  diplome: string; anneeObtention: string;
  fichierIdentite: string; fichierCV: string; fichierMotivation: string; fichierReleves: string;
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
  /** État de vérification initiale : empêche le flash du formulaire avant la redirection */
  verifierCandidatureExistante = true;
  candidature: CandidatureResponse | null = null;
  successMessage = '';
  showErrors = false;

  /** Stocke les vrais fichiers sélectionnés (pas juste leurs noms) */
  filesToUpload: Map<string, File[]> = new Map();

  etape = 1;
  readonly totalEtapes = 4;

  data: CandidatureFormData = {
    etape: 1, nom: '', prenom: '', email: '', telephone: '', adresse: '',
    phonePrefix: '+221', dateNaissance: '', nationalite: '',
    typeFormation: '', niveauVise: '', programme: '', campus: '',
    sessionRentree: '', niveauActuel: '', etablissement: '',
    diplome: '', anneeObtention: '',
    fichierIdentite: '', fichierCV: '', fichierMotivation: '', fichierReleves: '',
    consentement: false,
  };

  constructor(
    private recrutementService: RecrutementService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Vérification immédiate : si une candidature active existe, rediriger sans afficher le formulaire
    this.recrutementService.getMesCandidatures().subscribe({
      next: (candidatures) => {
        const active = candidatures.find(c => c.statut !== 'REJETE' && c.statut !== 'BROUILLON');
        if (active) {
          this.router.navigate(['/recrutement/suivi'], { replaceUrl: true });
          return;
        }
        this.initCatalogue();
        this.verifierCandidatureExistante = false;
      },
      error: () => {
        this.initCatalogue();
        this.verifierCandidatureExistante = false;
      }
    });
  }

  private initCatalogue(): void {
    this.recrutementService.getCataloguePublic().subscribe({
      next: (data) => { this.sessions = data.filter(s => s.estActive); }
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

  get etapeLabels(): string[] { return ['Profil', 'Formation', 'Parcours', 'Documents']; }

  precedent(): void { if (this.etape > 1) { this.etape--; this.saveDraft(); } }
  suivant(): void { if (!this.peutAvancer()) return; if (this.etape < this.totalEtapes) { this.etape++; this.saveDraft(); } }

  peutAvancer(): boolean {
    this.showErrors = false;
    if (this.etape === 1) {
      if (!this.data.nom.trim() || !this.data.prenom.trim() || !this.data.email.trim() || !this.data.telephone.trim()) {
        this.showErrors = true; return false;
      }
    }
    if (this.etape === 2) {
      if (!this.data.programme.trim()) { this.showErrors = true; return false; }
    }
    return true;
  }

  saveDraft(): void {
    this.data.etape = this.etape;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this.data));
  }

  soumettre(): void {
    if (!this.data.consentement) { alert('Veuillez accepter les conditions.'); return; }
    this.loading = true;
    const sessionId = this.sessions.length > 0 ? this.sessions[0].id : 1;
    const request: CandidatureRequest = {
      sessionCandidatureId: sessionId,
      informationsPersonnelles: JSON.stringify({
        nom: this.data.nom, prenom: this.data.prenom, email: this.data.email,
        telephone: this.data.phonePrefix + ' ' + this.data.telephone, adresse: this.data.adresse,
        dateNaissance: this.data.dateNaissance, nationalite: this.data.nationalite,
        niveauEtude: this.data.niveauActuel,
      }),
      informationsAcademiques: JSON.stringify({
        typeFormation: this.data.typeFormation, niveauVise: this.data.niveauVise,
        programme: this.data.programme, niveauActuel: this.data.niveauActuel,
        etablissement: this.data.etablissement, diplome: this.data.diplome,
        anneeObtention: this.data.anneeObtention,
      }),
      informationsProfessionnelles: JSON.stringify({
        campus: this.data.campus, sessionRentree: this.data.sessionRentree,
      }),
    };
    this.recrutementService.soumettreCandidature(request).subscribe({
      next: (response) => {
        this.candidature = response;
        this.successMessage = 'Candidature soumise ! Téléversement des pièces en cours...';
        this.uploadAllFiles(response.id);
      },
      error: (err) => {
        this.loading = false;
        const msg = err.error?.message || err.message || 'Erreur inconnue';
        alert('Erreur lors de la soumission : ' + msg);
      }
    });
  }

  private uploadAllFiles(candidatureId: number): void {
    const typeMap: Record<string, string> = {
      'identite': 'PIECE_IDENTITE',
      'cv': 'CV',
      'motivation': 'LETTRE_MOTIVATION',
      'releves': 'RELEVE_NOTES',
    };
    const uploads: Promise<any>[] = [];
    this.filesToUpload.forEach((files, type) => {
      files.forEach(file => {
        const docType = typeMap[type] || 'PIECE_IDENTITE';
        const upload$ = this.recrutementService.televerserPiece(candidatureId, file, docType);
        uploads.push(
          firstValueFrom(upload$).catch((err) => {
            console.error(`[uploadAllFiles] Échec upload "${type}" (${file.name}) comme ${docType}:`, err);
            return Promise.resolve(null);
          })
        );
      });
    });
    if (uploads.length > 0) {
      Promise.all(uploads).finally(() => this.onSubmitComplete());
    } else {
      this.onSubmitComplete();
    }
  }

  private onSubmitComplete(): void {
    this.loading = false;
    this.successMessage = 'Candidature soumise avec succès ! N° ' + (this.candidature?.numeroDossier || '');
    localStorage.removeItem(STORAGE_KEY);
    setTimeout(() => {
      this.router.navigate(['/recrutement/suivi']);
    }, 1500);
  }

  onFileSelected(event: Event, type: string): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const MAX_SIZE_MO = 5;
      const MAX_SIZE_BYTES = MAX_SIZE_MO * 1024 * 1024;
      const allFiles = Array.from(input.files);

      const oversized = allFiles.filter(f => f.size > MAX_SIZE_BYTES);
      if (oversized.length > 0) {
        const names = oversized.map(f => `${f.name} (${(f.size / (1024 * 1024)).toFixed(1)} Mo)`).join(', ');
        alert(`❌ Fichier(s) trop volumineux (max ${MAX_SIZE_MO} Mo) :\n${names}`);
        input.value = '';
        return;
      }

      this.filesToUpload.set(type, allFiles);
      const names = allFiles.map(f => f.name).join(', ');
      if (type === 'identite') this.data.fichierIdentite = names;
      else if (type === 'cv') this.data.fichierCV = names;
      else if (type === 'motivation') this.data.fichierMotivation = names;
      else if (type === 'releves') this.data.fichierReleves = names;
      this.saveDraft();
    }
  }
}
