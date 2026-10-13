import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { RecrutementService, SessionCandidatureResponse, CandidatureRequest, CandidatureResponse, CritereAdmissionResponse } from '../../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { Auth } from '../../../../core/auth/services/authService';
import { NotificationService } from '../../../../shared/components/ui/notification.service';
import { NIVEAUX_ETUDES, diplomeRequis, niveauInsuffisant } from '../../niveaux-etudes';
import { EchecEnvoi, PieceAEnvoyer, envoyerPieces } from '../../envoi-pieces';
import { depotPossibleApres } from '../../parcours-candidature';

const STORAGE_KEY = 'candidature_brouillon';

// Regex de validation — volontairement simples et permissives (pas de sur-validation
// bloquante), juste assez pour rejeter les cas manifestement invalides.
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const NOM_REGEX = /^[a-zA-ZÀ-ÖØ-öø-ÿ][a-zA-ZÀ-ÖØ-öø-ÿ' -]{1,}$/;

interface CandidatureFormData {
  etape: number;
  nom: string; prenom: string; email: string; telephone: string;
  adresse: string; phonePrefix: string; dateNaissance: string; nationalite: string;
  genre: string; estProfessionnel: string; profession: string; anneesExperience: number | null;
  sessionCandidatureId: number | null;
  niveauActuel: string; etablissement: string;
  diplome: string; anneeObtention: string;
  fichierIdentite: string; fichierCV: string;
  consentement: boolean;
  /** Réponses aux critères personnalisés de la session choisie : clé = id du critère. */
  reponsesCriteres: { [critereId: number]: string };
  /** Formations/certifications déjà suivies par le candidat, déclarées librement. */
  formationsSuivies: FormationSuivie[];
  /** Relevés de notes des dernières années : 1 à MAX_ANNEES années, chacune portant un ou
   * plusieurs bulletins (ex: trimestres) — jamais un nombre illimité d'années ni de bulletins. */
  bulletinsParAnnee: AnneeBulletins[];
}

interface FormationSuivie {
  nom: string;
  etablissement: string;
  diplome: string;
  anneeObtention: string;
  pays: string;
}

/** Une année scolaire déclarée, avec le ou les bulletins qui la couvrent. */
interface AnneeBulletins {
  bulletins: BulletinAnnuel[];
}

/** Un bulletin (ex: trimestre) : la moyenne déclarée par le candidat, prouvée par le PDF joint. */
interface BulletinAnnuel {
  moyenne: number | null;
  fichierNom: string;
}

@Component({
  selector: 'app-formulaire',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FooterComponent, NavbarComponent],
  templateUrl: './formulaire.component.html'
})
export class FormulaireComponent implements OnInit {
  /** Niveaux d'études, puis « Autre » : une valeur hors liste ne déclenche jamais de rejet automatique. */
  readonly niveauxProposes = [...NIVEAUX_ETUDES, 'Autre'];

  sessions: SessionCandidatureResponse[] = [];
  loading = false;
  /** État de vérification initiale : empêche le flash du formulaire avant la redirection */
  verifierCandidatureExistante = true;
  /**
   * Le dossier tel que le serveur l'a enregistré. Non nul dès que le dépôt a réussi : le formulaire n'est alors plus
   * un formulaire mais le suivi de l'envoi des pièces, et il ne peut plus être soumis une seconde fois.
   */
  candidature: CandidatureResponse | null = null;
  successMessage = '';
  showErrors = false;

  /** Envoi des pièces vers le dossier déposé : pièces terminées / total, pour la barre de progression. */
  progressionEnvoi = { faites: 0, total: 0 };
  /** Pièces que le serveur n'a pas reçues, avec la raison : le candidat peut les renvoyer sans rien ressaisir. */
  piecesEnEchec: EchecEnvoi[] = [];
  /** Documents exigés que le serveur déclare encore manquants une fois l'envoi terminé (contrôle final). */
  documentsEncoreManquants: string[] = [];
  envoiEnCours = false;

  /** Stocke les vrais fichiers sélectionnés (pas juste leurs noms) */
  filesToUpload: Map<string, File[]> = new Map();

  /**
   * Diplômes/certificats ajoutés en nombre libre (à l'image d'un formulaire de
   * candidature UNESCO où l'on ajoute autant de compétences que l'on veut) : un
   * document + sa description, avec un bouton « + » qui n'apparaît qu'une fois la
   * dernière entrée complète. Volontairement hors de `data` : les `File` ne
   * survivent pas à la sérialisation JSON du brouillon.
   */
  diplomes: { file: File | null; description: string }[] = [{ file: null, description: '' }];

  etape = 1;
  readonly totalEtapes = 3;

  data: CandidatureFormData = {
    etape: 1, nom: '', prenom: '', email: '', telephone: '', adresse: '',
    phonePrefix: '+221', dateNaissance: '', nationalite: '',
    genre: '', estProfessionnel: '', profession: '', anneesExperience: null,
    sessionCandidatureId: null,
    niveauActuel: '', etablissement: '',
    diplome: '', anneeObtention: '',
    fichierIdentite: '', fichierCV: '',
    consentement: false,
    reponsesCriteres: {},
    formationsSuivies: [],
    bulletinsParAnnee: [{ bulletins: [{ moyenne: null, fichierNom: '' }] }],
  };

  /** Fichiers PDF réels des bulletins, indexés [année][bulletin] comme data.bulletinsParAnnee —
   * hors du brouillon JSON comme les autres champs fichier*, un File ne survit pas à la sérialisation. */
  bulletinFiles: (File | null)[][] = [[null]];

  readonly MAX_ANNEES = 3;
  readonly MAX_BULLETINS_PAR_ANNEE = 3;

  /** Session imposée via l'URL (ex: venant de la page « Sessions ouvertes ») : prioritaire sur un brouillon existant. */
  private sessionImposeeParUrl: number | null = null;

  constructor(
    private recrutementService: RecrutementService,
    private router: Router,
    private route: ActivatedRoute,
    private auth: Auth,
    private notifications: NotificationService
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
        const active = candidatures.find(c => !depotPossibleApres(c.statut));
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
    // Un File ne survit pas à JSON.stringify/parse : le brouillon restauré connaît les
    // noms de fichiers de bulletins mais plus les fichiers eux-mêmes, qu'il faut
    // resélectionner. On réaligne seulement la forme [année][bulletin] du tableau.
    if (!this.data.bulletinsParAnnee || this.data.bulletinsParAnnee.length === 0) {
      this.data.bulletinsParAnnee = [{ bulletins: [{ moyenne: null, fichierNom: '' }] }];
    }
    this.bulletinFiles = this.data.bulletinsParAnnee.map(annee =>
      (annee.bulletins && annee.bulletins.length > 0 ? annee.bulletins : (annee.bulletins = [{ moyenne: null, fichierNom: '' }])).map(() => null)
    );
  }

  get etapeLabels(): string[] { return ['Profil', 'Formation & Parcours', 'Documents']; }

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

  /** Alimente Candidat#niveauEtude, utilisé par le filtre de critères côté serveur : le laisser vide désactive silencieusement ce critère. */
  get niveauActuelInvalide(): boolean {
    return !this.data.niveauActuel;
  }

  readonly anneeCourante = new Date().getFullYear();

  /** Libellés des documents requis (badge « Requis » en étape Documents) non encore sélectionnés. */
  get documentsManquants(): string[] {
    const requis: { type: string; label: string }[] = [
      { type: 'identite', label: "Pièce d'identité" },
      { type: 'cv', label: 'CV' },
    ];
    return requis.filter(r => !this.filesToUpload.get(r.type)?.length).map(r => r.label);
  }

  /**
   * Une à trois années, chacune avec un à trois bulletins — jamais un nombre illimité
   * ni d'années ni de bulletins. Chaque bulletin doit porter sa moyenne déclarée (sur
   * 20) ET son PDF de preuve : les deux vont ensemble, l'un sans l'autre est une
   * déclaration invérifiable ou une preuve sans valeur.
   */
  get bulletinsIncomplets(): boolean {
    if (this.data.bulletinsParAnnee.length === 0) return true;
    return this.data.bulletinsParAnnee.some((annee, ai) =>
      annee.bulletins.length === 0 || annee.bulletins.some((b, bi) =>
        b.moyenne === null || !Number.isFinite(b.moyenne) || b.moyenne < 0 || b.moyenne > 20 || !this.bulletinFiles[ai]?.[bi]
      )
    );
  }

  get sessionSelectionnee(): SessionCandidatureResponse | undefined {
    return this.sessions.find(s => s.id === this.data.sessionCandidatureId);
  }

  /** Critères ajoutés librement par le responsable admission pour la session choisie, en plus des critères de base. */
  get criteresPersonnalises(): CritereAdmissionResponse[] {
    return this.sessionSelectionnee?.criteresPersonnalises || [];
  }

  optionsCritere(critere: CritereAdmissionResponse): string[] {
    return (critere.options || '').split(',').map(o => o.trim()).filter(o => o.length > 0);
  }

  /** Libellés des critères personnalisés obligatoires non encore répondus. */
  get criteresPersonnalisesManquants(): string[] {
    return this.criteresPersonnalises
      .filter(c => c.obligatoire)
      .filter(c => !this.data.reponsesCriteres[c.id]?.trim())
      .map(c => c.libelle);
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

    if (session.criteresNiveauEtudeMinimum && niveauInsuffisant(this.data.niveauActuel, session.criteresNiveauEtudeMinimum)) {
      avertissements.push(`Niveau d'études minimum requis pour cette session : ${session.criteresNiveauEtudeMinimum}.`);
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

  /** Un candidat sans activité professionnelle n'a ni profession ni ancienneté à déclarer. */
  onEstProfessionnelChange(): void {
    if (this.data.estProfessionnel !== 'Oui') {
      this.data.profession = '';
      this.data.anneesExperience = null;
    }
    this.saveDraft();
  }

  ajouterFormationSuivie(): void {
    this.data.formationsSuivies.push({ nom: '', etablissement: '', diplome: '', anneeObtention: '', pays: '' });
    this.saveDraft();
  }

  retirerFormationSuivie(index: number): void {
    this.data.formationsSuivies.splice(index, 1);
    this.saveDraft();
  }

  /** Une année de plus (jusqu'à MAX_ANNEES, jamais au-delà), avec un premier bulletin vide. */
  ajouterAnnee(): void {
    if (this.data.bulletinsParAnnee.length >= this.MAX_ANNEES) return;
    this.data.bulletinsParAnnee.push({ bulletins: [{ moyenne: null, fichierNom: '' }] });
    this.bulletinFiles.push([null]);
    this.saveDraft();
  }

  retirerAnnee(anneeIndex: number): void {
    if (this.data.bulletinsParAnnee.length <= 1) return; // au moins une année reste obligatoire
    this.data.bulletinsParAnnee.splice(anneeIndex, 1);
    this.bulletinFiles.splice(anneeIndex, 1);
    this.saveDraft();
  }

  /** Un bulletin de plus dans cette année (ex: trimestre suivant), jusqu'à MAX_BULLETINS_PAR_ANNEE. */
  ajouterBulletin(anneeIndex: number): void {
    const bulletins = this.data.bulletinsParAnnee[anneeIndex].bulletins;
    if (bulletins.length >= this.MAX_BULLETINS_PAR_ANNEE) return;
    bulletins.push({ moyenne: null, fichierNom: '' });
    this.bulletinFiles[anneeIndex].push(null);
    this.saveDraft();
  }

  retirerBulletin(anneeIndex: number, bulletinIndex: number): void {
    const bulletins = this.data.bulletinsParAnnee[anneeIndex].bulletins;
    if (bulletins.length <= 1) return; // au moins un bulletin reste obligatoire par année
    bulletins.splice(bulletinIndex, 1);
    this.bulletinFiles[anneeIndex].splice(bulletinIndex, 1);
    this.saveDraft();
  }

  onBulletinFileSelected(event: Event, anneeIndex: number, bulletinIndex: number): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const MAX_SIZE_MO = 5;
      if (file.size > MAX_SIZE_MO * 1024 * 1024) {
        this.notifications.erreur(`Fichier trop volumineux (max ${MAX_SIZE_MO} Mo) : ${file.name}.`);
        input.value = '';
        return;
      }
      this.bulletinFiles[anneeIndex][bulletinIndex] = file;
      this.data.bulletinsParAnnee[anneeIndex].bulletins[bulletinIndex].fichierNom = file.name;
      this.saveDraft();
    }
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
      if (this.sessionInvalide || this.criteresPersonnalisesManquants.length > 0 || this.niveauActuelInvalide) {
        this.showErrors = true; return false;
      }
    }
    return true;
  }

  private brouillonServeurTimer?: ReturnType<typeof setTimeout>;

  saveDraft(): void {
    // Une fois le dossier déposé, plus rien ne doit recréer de brouillon : l'écriture serveur est différée d'une
    // seconde, et la dernière frappe partait après le dépôt — qui venait justement de supprimer le brouillon.
    if (this.candidature) return;
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
    // Deux clics rapprochés, ou un second clic pendant l'envoi des pièces, ne déposent pas deux fois le dossier.
    if (this.loading || this.candidature) return;
    // Un brouillon restauré depuis le localStorage peut contenir des données
    // invalides sans être repassé par peutAvancer() : on revérifie ici avant
    // d'envoyer quoi que ce soit au backend.
    if (this.nomInvalide || this.prenomInvalide || this.emailInvalide || this.telephoneInvalide
        || this.adresseInvalide || this.genreInvalide || this.anneesExperienceInvalide) {
      this.etape = 1;
      this.showErrors = true;
      return;
    }
    if (this.sessionInvalide || this.criteresPersonnalisesManquants.length > 0 || this.niveauActuelInvalide) {
      this.etape = 2;
      this.showErrors = true;
      return;
    }
    if (this.documentsManquants.length > 0 || this.diplomeManquant || this.bulletinsIncomplets || !this.data.consentement) {
      this.etape = 3;
      this.showErrors = true;
      return;
    }
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
        niveauActuel: this.data.niveauActuel,
        etablissement: this.data.etablissement, diplome: this.data.diplome,
        anneeObtention: this.data.anneeObtention,
        formationsSuivies: this.data.formationsSuivies.filter(f => f.nom.trim()),
      }),
      informationsProfessionnelles: JSON.stringify({
        estProfessionnel: this.data.estProfessionnel,
      }),
      reponsesCriteres: this.data.reponsesCriteres,
      consentement: this.data.consentement,
    };
    this.recrutementService.soumettreCandidature(request).subscribe({
      next: (response) => {
        this.candidature = response;
        this.abandonnerBrouillon();
        this.successMessage = 'Candidature enregistrée ! Envoi des pièces en cours...';
        this.envoyerLesPieces();
      },
      error: (err) => {
        this.loading = false;
        const msg = err.error?.message || err.message || 'Erreur inconnue';
        this.notifications.erreur('Erreur lors de la soumission : ' + msg);
      }
    });
  }

  /** Le dossier est déposé : le brouillon, local comme serveur, n'a plus lieu d'être et ne doit pas ressusciter. */
  private abandonnerBrouillon(): void {
    if (this.brouillonServeurTimer) clearTimeout(this.brouillonServeurTimer);
    this.brouillonServeurTimer = undefined;
    localStorage.removeItem(STORAGE_KEY);
  }

  /** Les pièces choisies dans le formulaire, chacune avec le type de document que le serveur attend. */
  private piecesAEnvoyer(): PieceAEnvoyer[] {
    const typeServeur: Record<string, { type: string; libelle: string }> = {
      identite: { type: 'PIECE_IDENTITE', libelle: "Pièce d'identité" },
      cv: { type: 'CV', libelle: 'CV' },
    };
    const pieces: PieceAEnvoyer[] = [];
    this.filesToUpload.forEach((files, cle) => {
      const attendu = typeServeur[cle];
      if (!attendu) return;
      files.forEach((file) => pieces.push({ libelle: `${attendu.libelle} (${file.name})`, file, typeDocument: attendu.type }));
    });
    // Bulletins annuels : chacun est un relevé de notes distinct, avec sa moyenne déclarée.
    this.data.bulletinsParAnnee.forEach((annee, ai) => {
      annee.bulletins.forEach((bulletin, bi) => {
        const file = this.bulletinFiles[ai]?.[bi];
        if (!file) return;
        pieces.push({
          libelle: `Relevé de notes — année ${ai + 1}, bulletin ${bi + 1} (${file.name})`,
          file, typeDocument: 'RELEVE_NOTES', moyenne: bulletin.moyenne,
        });
      });
    });
    this.diplomes
      .filter((d) => d.file && d.description.trim())
      .forEach((d) => pieces.push({
        libelle: `Diplôme ou certification (${d.file!.name})`,
        file: d.file!, typeDocument: 'DIPLOME', description: d.description.trim(),
      }));
    return pieces;
  }

  private envoyerUnePiece(piece: PieceAEnvoyer) {
    const id = this.candidature!.id;
    if (piece.typeDocument === 'DIPLOME') {
      return this.recrutementService.televerserPiece(id, piece.file, piece.typeDocument, piece.description);
    }
    return this.recrutementService.televerserPiece(id, piece.file, piece.typeDocument, piece.moyenne);
  }

  /**
   * Envoie les pièces vers le dossier déposé, puis interroge le serveur pour savoir ce qu'il a réellement reçu.
   *
   * <p>Les pièces partent après le dépôt, une requête chacune : une coupure peut en perdre une. L'ancien code
   * l'annonçait par une alerte fugitive, puis redirigeait aussitôt vers le suivi — qui redemandait les
   * documents au candidat sans qu'il ait compris pourquoi. Désormais : pannes passagères reprises
   * automatiquement, échecs listés avec leur raison, bouton pour renvoyer, et jamais « tout est transmis »
   * sans que le serveur le confirme.
   */
  private async envoyerLesPieces(
    pieces: PieceAEnvoyer[] = this.piecesAEnvoyer(),
    refusDefinitifs: EchecEnvoi[] = [],
  ): Promise<void> {
    this.loading = false;
    this.envoiEnCours = true;
    this.piecesEnEchec = [];
    this.documentsEncoreManquants = [];
    this.progressionEnvoi = { faites: 0, total: pieces.length };

    const bilan = await envoyerPieces(pieces, (piece) => this.envoyerUnePiece(piece), {
      surProgression: (faites, total) => (this.progressionEnvoi = { faites, total }),
    });
    this.piecesEnEchec = [...bilan.echecs, ...refusDefinitifs];

    await this.controlerAupresDuServeur();
    this.envoiEnCours = false;

    if (this.piecesEnEchec.length === 0 && this.documentsEncoreManquants.length === 0) {
      this.successMessage = 'Candidature soumise avec succès ! N° ' + (this.candidature?.numeroDossier || '');
      this.continuerVersLeSuivi(1500);
    } else {
      this.successMessage = '';
    }
  }

  /**
   * Relit le dossier côté serveur : c'est lui, et non le compte rendu du navigateur, qui dit si le dossier est complet.
   * Si la lecture échoue, on s'en tient au bilan de l'envoi.
   */
  private async controlerAupresDuServeur(): Promise<void> {
    try {
      const dossiers = await firstValueFrom(this.recrutementService.getMesCandidatures());
      const enregistre = dossiers.find((c) => c.id === this.candidature?.id);
      if (!enregistre) return;
      this.candidature = enregistre;
      this.documentsEncoreManquants = (enregistre.documentsAttendus ?? [])
        .filter((d) => d.etat !== 'FOURNI')
        // Une pièce que l'on vient de signaler en échec est déjà listée plus haut : ne pas la compter deux fois.
        .filter((d) => !this.piecesEnEchec.some((e) => e.piece.typeDocument === d.type))
        .map((d) => d.libelle);
    } catch {
      /* contrôle indisponible : le bilan de l'envoi fait foi */
    }
  }

  /** Renvoie uniquement ce que le serveur n'a pas reçu, avec les fichiers encore en mémoire. */
  reessayerEnvoi(): void {
    if (this.envoiEnCours) return;
    const aRenvoyer = this.piecesEnEchec.filter((e) => !e.definitif).map((e) => e.piece);
    // Un refus définitif ne se règle pas en renvoyant : il reste affiché tant que la page est ouverte.
    const definitifs = this.piecesEnEchec.filter((e) => e.definitif);
    void this.envoyerLesPieces(aRenvoyer, definitifs);
  }

  /**
   * Quitte le formulaire pour le suivi, où le candidat peut compléter ce qui manque.
   * La soumission vient d'accorder ROLE_CANDIDAT côté backend : on rafraîchit currentUser AVANT de naviguer vers
   * /suivi (gardée par candidatGuard), sinon le signal encore obsolète ferait rejeter l'utilisateur qui vient de réussir.
   */
  continuerVersLeSuivi(delaiMs = 0): void {
    this.auth.getCurrentUser().subscribe({
      next: () => setTimeout(() => this.router.navigate(['/recrutement/suivi']), delaiMs),
      error: () => this.router.navigate(['/recrutement/suivi']),
    });
  }

  /** Au moins une pièce en échec peut être renvoyée : un refus définitif (format, dossier figé) ne le peut pas. */
  get aDesEchecsRenvoyables(): boolean {
    return this.piecesEnEchec.some((e) => !e.definitif);
  }

  get progressionEnvoiPourcent(): number {
    const { faites, total } = this.progressionEnvoi;
    return total === 0 ? 100 : Math.round((faites / total) * 100);
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
        this.notifications.erreur(`Fichier(s) trop volumineux (max ${MAX_SIZE_MO} Mo) : ${names}.`);
        input.value = '';
        return;
      }

      this.filesToUpload.set(type, allFiles);
      const names = allFiles.map(f => f.name).join(', ');
      if (type === 'identite') this.data.fichierIdentite = names;
      else if (type === 'cv') this.data.fichierCV = names;
      this.saveDraft();
    }
  }

  /** La dernière entrée doit être complète avant de pouvoir en ajouter une autre. */
  /** Attestation de diplôme exigée, sauf d'un candidat de niveau 4e secondaire (CPS). */
  get diplomeObligatoire(): boolean {
    return diplomeRequis(this.data.niveauActuel);
  }

  /** Vrai si un diplôme est exigé et qu'aucune entrée n'a à la fois son fichier et sa description. */
  get diplomeManquant(): boolean {
    return this.diplomeObligatoire && !this.diplomes.some((d) => d.file && d.description.trim().length > 0);
  }

  get dernierDiplomeComplet(): boolean {
    const dernier = this.diplomes[this.diplomes.length - 1];
    return !!dernier?.file && dernier.description.trim().length > 0;
  }

  onDiplomeFileSelected(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    const MAX_SIZE_MO = 5;
    if (file.size > MAX_SIZE_MO * 1024 * 1024) {
      this.notifications.erreur(`Fichier trop volumineux (max ${MAX_SIZE_MO} Mo) : ${file.name}.`);
      input.value = '';
      return;
    }
    this.diplomes[index].file = file;
  }

  ajouterDiplome(): void {
    if (!this.dernierDiplomeComplet) return;
    this.diplomes.push({ file: null, description: '' });
  }

  retirerDiplome(index: number): void {
    this.diplomes.splice(index, 1);
    if (this.diplomes.length === 0) this.diplomes.push({ file: null, description: '' });
  }
}
