import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { RecrutementService, SessionCandidatureResponse, CandidatureRequest, CandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { Auth } from '../../../../core/auth/services/authService';

const STORAGE_KEY = 'candidature_brouillon';

// Regex de validation — volontairement simples et permissives (pas de sur-validation
// bloquante), juste assez pour rejeter les cas manifestement invalides.
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const NOM_REGEX = /^[a-zA-ZÀ-ÖØ-öø-ÿ][a-zA-ZÀ-ÖØ-öø-ÿ' -]{1,}$/;

interface CandidatureFormData {
  etape: number;
  nom: string; prenom: string; email: string; telephone: string;
  adresse: string; phonePrefix: string; dateNaissance: string; nationalite: string;
  genre: string; profession: string; anneesExperience: string;
  sessionCandidatureId: number | null;
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
    genre: '', profession: '', anneesExperience: '',
    sessionCandidatureId: null,
    typeFormation: '', niveauVise: '', programme: '', campus: '',
    sessionRentree: '', niveauActuel: '', etablissement: '',
    diplome: '', anneeObtention: '',
    fichierIdentite: '', fichierCV: '', fichierMotivation: '', fichierReleves: '',
    consentement: false,
  };

  constructor(
    private recrutementService: RecrutementService,
    private router: Router,
    private auth: Auth
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
      next: (data) => {
        this.sessions = data.filter(s => s.estActive);
        // Présélectionne automatiquement s'il n'y a qu'une seule session ouverte
        // (cas le plus courant) ; sinon le candidat doit choisir explicitement.
        if (this.sessions.length === 1 && !this.data.sessionCandidatureId) {
          this.data.sessionCandidatureId = this.sessions[0].id;
        }
      }
    });

    // Brouillon serveur en priorité (permet la reprise depuis un autre appareil) ;
    // repli sur le localStorage si absent côté serveur ou si la requête échoue.
    this.recrutementService.getBrouillon().subscribe({
      next: (res) => {
        if (res && res.donneesJson) {
          this.chargerBrouillon(res.donneesJson);
        } else {
          this.chargerBrouillonLocal();
        }
      },
      error: () => this.chargerBrouillonLocal(),
    });
  }

  private chargerBrouillonLocal(): void {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) this.chargerBrouillon(saved);
  }

  private chargerBrouillon(json: string): void {
    try {
      const parsed = JSON.parse(json);
      this.data = { ...this.data, ...parsed };
      this.etape = this.data.etape || 1;
    } catch { /* ignore */ }
  }

  get etapeLabels(): string[] { return ['Profil', 'Formation', 'Parcours', 'Documents']; }

  // --- Validateurs étape 1 (Profil) : présence ET format, pas juste "non vide" ---

  get nomInvalide(): boolean {
    return !NOM_REGEX.test(this.data.nom.trim());
  }

  get prenomInvalide(): boolean {
    return !NOM_REGEX.test(this.data.prenom.trim());
  }

  get emailInvalide(): boolean {
    return !EMAIL_REGEX.test(this.data.email.trim());
  }

  /** Chiffres uniquement une fois espaces/points/tirets retirés, entre 7 et 12 chiffres (formats régionaux variables). */
  get telephoneInvalide(): boolean {
    const chiffres = this.data.telephone.replace(/[\s.-]/g, '');
    return !/^\d{7,12}$/.test(chiffres);
  }

  get adresseInvalide(): boolean {
    return this.data.adresse.trim().length < 5;
  }

  get genreInvalide(): boolean {
    return !['Homme', 'Femme', 'Autre'].includes(this.data.genre);
  }

  /** Champ optionnel : invalide seulement si rempli avec une valeur hors bornes plausibles. */
  get anneesExperienceInvalide(): boolean {
    if (!this.data.anneesExperience.trim()) return false;
    const n = Number(this.data.anneesExperience);
    return !Number.isFinite(n) || n < 0 || n > 60;
  }

  /** La session choisie doit exister parmi celles réellement publiées/ouvertes par l'admin. */
  get sessionInvalide(): boolean {
    return !this.sessions.some(s => s.id === this.data.sessionCandidatureId);
  }

  precedent(): void { if (this.etape > 1) { this.etape--; this.saveDraft(); } }
  suivant(): void { if (!this.peutAvancer()) return; if (this.etape < this.totalEtapes) { this.etape++; this.saveDraft(); } }

  peutAvancer(): boolean {
    this.showErrors = false;
    if (this.etape === 1) {
      if (this.nomInvalide || this.prenomInvalide || this.emailInvalide || this.telephoneInvalide
          || this.adresseInvalide || this.genreInvalide || this.anneesExperienceInvalide) {
        this.showErrors = true; return false;
      }
    }
    if (this.etape === 2) {
      if (this.sessionInvalide || !this.data.programme.trim()) { this.showErrors = true; return false; }
    }
    return true;
  }

  private brouillonServeurTimer?: ReturnType<typeof setTimeout>;

  saveDraft(): void {
    this.data.etape = this.etape;
    const json = JSON.stringify(this.data);
    localStorage.setItem(STORAGE_KEY, json);

    // Débounce l'écriture serveur (appelée à chaque frappe côté template) pour
    // éviter une requête HTTP par caractère saisi.
    if (this.brouillonServeurTimer) clearTimeout(this.brouillonServeurTimer);
    this.brouillonServeurTimer = setTimeout(() => {
      this.recrutementService.sauvegarderBrouillon(json).subscribe({ error: () => { /* best-effort */ } });
    }, 1000);
  }

  soumettre(): void {
    // Un brouillon restauré depuis le localStorage peut contenir des données
    // invalides sans être repassé par peutAvancer() : on revérifie ici avant
    // d'envoyer quoi que ce soit au backend.
    if (this.nomInvalide || this.prenomInvalide || this.emailInvalide || this.telephoneInvalide
        || this.adresseInvalide || this.genreInvalide || this.anneesExperienceInvalide) {
      this.etape = 1;
      this.showErrors = true;
      return;
    }
    if (this.sessionInvalide || !this.data.programme.trim()) {
      this.etape = 2;
      this.showErrors = true;
      return;
    }
    if (!this.data.consentement) { alert('Veuillez accepter les conditions.'); return; }
    this.loading = true;
    const request: CandidatureRequest = {
      sessionCandidatureId: this.data.sessionCandidatureId!,
      informationsPersonnelles: JSON.stringify({
        nom: this.data.nom, prenom: this.data.prenom, email: this.data.email,
        telephone: this.data.phonePrefix + ' ' + this.data.telephone, adresse: this.data.adresse,
        dateNaissance: this.data.dateNaissance, nationalite: this.data.nationalite,
        niveauEtude: this.data.niveauActuel,
        genre: this.data.genre, profession: this.data.profession,
        anneesExperience: this.data.anneesExperience,
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
    // La soumission vient d'accorder ROLE_CANDIDAT côté backend : on rafraîchit
    // currentUser AVANT de naviguer vers /suivi (gardée par candidatGuard), sinon
    // le signal encore obsolète ferait rejeter l'utilisateur qui vient de réussir.
    this.auth.getCurrentUser().subscribe({
      next: () => {
        setTimeout(() => {
          this.router.navigate(['/recrutement/suivi']);
        }, 1500);
      },
    });
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
