import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { RecrutementService, SessionCandidatureResponse, CandidatureRequest, CandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { CandidatNavbarComponent } from '../../../../shared/components/candidat-navbar/candidat-navbar.component';
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
  genre: string; profession: string; anneesExperience: number | null;
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
  imports: [CommonModule, FormsModule, RouterModule, FooterComponent, CandidatNavbarComponent],
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
    genre: '', profession: '', anneesExperience: null,
    sessionCandidatureId: null,
    typeFormation: '', niveauVise: '', programme: '', campus: '',
    sessionRentree: '', niveauActuel: '', etablissement: '',
    diplome: '', anneeObtention: '',
    fichierIdentite: '', fichierCV: '', fichierMotivation: '', fichierReleves: '',
    consentement: false,
  };

  /** Session imposée via l'URL (ex: venant de la page « Sessions ouvertes ») : prioritaire sur un brouillon existant. */
  private sessionImposeeParUrl: number | null = null;

  constructor(
    private recrutementService: RecrutementService,
    private router: Router,
    private route: ActivatedRoute,
    private auth: Auth
  ) {}

  ngOnInit(): void {
    // Session choisie depuis la page « Sessions ouvertes » (?session=<id>) : prioritaire
    // sur toute session déjà présente dans un brouillon restauré.
    const sessionParam = this.route.snapshot.queryParamMap.get('session');
    if (sessionParam) {
      this.sessionImposeeParUrl = Number(sessionParam);
      this.data.sessionCandidatureId = this.sessionImposeeParUrl;
    }

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
    // Une session choisie explicitement depuis l'URL prime sur celle d'un brouillon restauré.
    if (this.sessionImposeeParUrl !== null) {
      this.data.sessionCandidatureId = this.sessionImposeeParUrl;
    }
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

  /** Champ optionnel (input number : ngModel le lie déjà à un number|null) : invalide seulement si hors bornes plausibles. */
  get anneesExperienceInvalide(): boolean {
    const n = this.data.anneesExperience;
    if (n === null || n === undefined) return false;
    return !Number.isFinite(n) || n < 0 || n > 60;
  }

  /** La session choisie doit exister parmi celles réellement publiées/ouvertes par l'admin. */
  get sessionInvalide(): boolean {
    return !this.sessions.some(s => s.id === this.data.sessionCandidatureId);
  }

  get sessionSelectionnee(): SessionCandidatureResponse | undefined {
    return this.sessions.find(s => s.id === this.data.sessionCandidatureId);
  }

  private calculerAge(dateNaissance: string): number | null {
    if (!dateNaissance) return null;
    const naissance = new Date(dateNaissance);
    if (Number.isNaN(naissance.getTime())) return null;
    const aujourdhui = new Date();
    let age = aujourdhui.getFullYear() - naissance.getFullYear();
    const moisEcoule = aujourdhui.getMonth() - naissance.getMonth();
    if (moisEcoule < 0 || (moisEcoule === 0 && aujourdhui.getDate() < naissance.getDate())) age--;
    return age;
  }

  /**
   * Avertit le candidat, dès qu'il a rempli assez d'informations, s'il ne semble pas
   * remplir les critères de la session choisie — plutôt que de le laisser découvrir
   * un rejet automatique après la soumission (TDR : filtre appliqué "sur ces champs").
   * Purement informatif : ne bloque jamais la progression, la décision finale reste
   * celle du serveur au moment de la soumission.
   */
  get avertissementsCriteres(): string[] {
    const session = this.sessionSelectionnee;
    if (!session || !session.criteresActifs) return [];
    const avertissements: string[] = [];

    if (session.criteresNiveauEtudeMinimum && this.data.niveauActuel) {
      const niveaux = ['Baccalauréat', 'Licence (L3)', 'Master 1 (M1)', 'Master 2 (M2)'];
      const seuil = niveaux.indexOf(session.criteresNiveauEtudeMinimum);
      const actuel = niveaux.indexOf(this.data.niveauActuel);
      if (seuil >= 0 && actuel >= 0 && actuel < seuil) {
        avertissements.push(`Niveau d'études minimum requis pour cette session : ${session.criteresNiveauEtudeMinimum}.`);
      }
    }

    const age = this.calculerAge(this.data.dateNaissance);
    if (age !== null) {
      if (session.criteresAgeMinimum && age < session.criteresAgeMinimum) {
        avertissements.push(`Âge minimum requis pour cette session : ${session.criteresAgeMinimum} ans.`);
      }
      if (session.criteresAgeMaximum && age > session.criteresAgeMaximum) {
        avertissements.push(`Âge maximum autorisé pour cette session : ${session.criteresAgeMaximum} ans.`);
      }
    }

    if (session.criteresGenre && this.data.genre && session.criteresGenre !== this.data.genre) {
      avertissements.push(`Cette session est réservée aux candidats de genre "${session.criteresGenre}".`);
    }

    if (session.criteresExperienceMinimale && this.data.anneesExperience !== null
        && this.data.anneesExperience < session.criteresExperienceMinimale) {
      avertissements.push(`Expérience minimale requise pour cette session : ${session.criteresExperienceMinimale} an(s).`);
    }

    return avertissements;
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
        anneesExperience: this.data.anneesExperience !== null ? String(this.data.anneesExperience) : '',
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
