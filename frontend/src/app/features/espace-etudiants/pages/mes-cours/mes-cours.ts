import { Component, ViewChild, ElementRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Cours } from '../../models/cours';
import { Cohorte } from '../../models/cohorte';
import { Support } from '../../models/support';
import { Seance } from '../../models/seance';
import { PresenceEtudiant } from '../../models/presence';
import { CoursService } from '../../services/cours.service';
import { CohorteService } from '../../services/cohorte.service';
import { SupportService } from '../../services/support.service';
import { SeanceService } from '../../services/seance.service';
import { PresenceService } from '../../services/presence.service';
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';
import { NotificationService } from '../../../../shared/components/ui/notification.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-mes-cours',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './mes-cours.html',
  styleUrl: './mes-cours.css',
})
export class MesCours implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('depotRapide') depotRapide!: ElementRef<HTMLElement>;

  cours: Cours[] = [];
  toutesCohortes: Cohorte[] = [];

  coursEnGestion: Cours | null = null;
  cohorteIdsGestion = new Set<number>();
  isSavingCohortes = false;
  gestionError = '';

  coursEnEdition: Cours | null = null;
  editionForm: FormGroup;
  isSavingEdition = false;
  editionError = '';

  coursEnPresence: Cours | null = null;
  seancesDuCours: Seance[] = [];
  seanceSelectionnee: Seance | null = null;
  rosterSeance: PresenceEtudiant[] = [];
  chargementRoster = false;
  nouvelleSeanceTitre = '';
  nouvelleSeanceDate = this.aujourdhuiIso();
  isSavingSeance = false;
  isSavingPresences = false;
  presenceError = '';

  depotForm: FormGroup;
  fichierSelectionne: File | null = null;
  isUploading = false;
  uploadMessage = '';
  uploadError = '';

  suppressionCoursEnCoursId: number | null = null;

  /**
   * Cours à mettre en avant, transmis par « Ajuster le cours » depuis la page des
   * retours : sans cela, on arrivait sur la liste complète sans savoir lequel
   * l'avis visait.
   */
  coursMisEnAvantId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private coursService: CoursService,
    private cohorteService: CohorteService,
    private supportService: SupportService,
    private seanceService: SeanceService,
    private presenceService: PresenceService,
    private confirmation: ConfirmationService,
    private notifications: NotificationService,
    private route: ActivatedRoute
  ) {
    // datePublication n'est pas obligatoire : par defaut on publie immediatement.
    // La renseigner dans le futur programme la mise en ligne (la ressource reste
    // invisible des etudiants jusqu'a cette date).
    this.depotForm = this.fb.group({
      typeRessource: ['pdf', Validators.required],
      coursCible: ['', Validators.required],
      datePublication: [this.aujourdhuiIso()]
    });
    this.editionForm = this.fb.group({
      code: ['', Validators.required],
      titre: ['', Validators.required],
      semestre: [''],
      description: [''],
      volumeHoraire: [0, [Validators.required, Validators.min(0)]]
    });
  }

  mesSupports: Support[] = [];
  chargementSupports = false;
  suppressionEnCoursId: string | null = null;

  private chargerSupports(): void {
    this.chargementSupports = true;
    this.supportService.getMesSupports().subscribe({
      next: (data) => { this.mesSupports = data || []; this.chargementSupports = false; },
      error: () => { this.mesSupports = []; this.chargementSupports = false; }
    });
  }

  supprimerSupport(support: Support): void {
    this.confirmation.demander({
      titre: 'Supprimer la ressource',
      message: `« ${support.titre} » sera définitivement supprimée de ce cours.`,
      libelleConfirmer: 'Supprimer',
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.suppressionEnCoursId = support.id;
      this.supportService.supprimer(support.id).subscribe({
        next: () => {
          this.suppressionEnCoursId = null;
          this.chargerSupports();
          this.loadCours(); // le compteur de ressources de la carte cours change
          this.notifications.succes('Ressource supprimée.');
        },
        error: () => {
          this.suppressionEnCoursId = null;
          this.notifications.erreur('La suppression de la ressource a échoué.');
        }
      });
    });
  }

  iconeTypeRessource(type?: string | null): string {
    switch (type) {
      case 'lien': return 'link';
      case 'video': return 'videocam';
      default: return 'description';
    }
  }

  ngOnInit(): void {
    // ?cours=<id> vient de « Ajuster le cours » sur la page des retours.
    const cible = this.route.snapshot.queryParamMap.get('cours');
    this.coursMisEnAvantId = cible ? Number(cible) : null;

    this.loadCours();
    this.chargerSupports();
    this.cohorteService.getAll().subscribe({
      next: (data) => (this.toutesCohortes = data || []),
      error: () => (this.toutesCohortes = [])
    });
  }

  /**
   * Amène la carte ciblée à l'écran une fois la liste rendue. Le délai laisse
   * Angular peindre les cartes : sans lui, l'élément n'existe pas encore.
   */
  private mettreEnAvantLeCoursCible(): void {
    if (this.coursMisEnAvantId === null) return;
    setTimeout(() => {
      document.getElementById('cours-' + this.coursMisEnAvantId)
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 100);
  }

  ouvrirGestionCohortes(cours: Cours) {
    this.coursEnGestion = cours;
    this.gestionError = '';
    this.cohorteIdsGestion = new Set((cours.cohortes || []).map(c => Number(c.id)));
  }

  fermerGestionCohortes() {
    this.coursEnGestion = null;
  }

  toggleCohorteGestion(id: number, checked: boolean) {
    if (checked) {
      this.cohorteIdsGestion.add(id);
    } else {
      this.cohorteIdsGestion.delete(id);
    }
  }

  enregistrerCohortes() {
    if (!this.coursEnGestion) {
      return;
    }
    this.isSavingCohortes = true;
    this.gestionError = '';
    this.coursService.updateCohortes(this.coursEnGestion.id, Array.from(this.cohorteIdsGestion)).subscribe({
      next: () => {
        this.isSavingCohortes = false;
        this.coursEnGestion = null;
        this.loadCours();
      },
      error: (error) => {
        this.isSavingCohortes = false;
        this.gestionError = error.error?.message || "L'enregistrement des cohortes a échoué.";
      }
    });
  }

  preRemplirDepot(cours: Cours) {
    this.depotForm.patchValue({ coursCible: cours.id });
    this.depotRapide?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  ouvrirEditionCours(cours: Cours) {
    this.coursEnEdition = cours;
    this.editionError = '';
    this.editionForm.setValue({
      code: cours.code,
      titre: cours.titre,
      semestre: cours.semestre || '',
      description: cours.description || '',
      volumeHoraire: cours.volumeHoraire || 0
    });
  }

  fermerEditionCours() {
    this.coursEnEdition = null;
  }

  enregistrerEdition() {
    if (!this.coursEnEdition || this.editionForm.invalid) {
      this.editionForm.markAllAsTouched();
      return;
    }
    this.isSavingEdition = true;
    this.editionError = '';
    this.coursService.update(this.coursEnEdition.id, this.editionForm.getRawValue()).subscribe({
      next: () => {
        this.isSavingEdition = false;
        this.coursEnEdition = null;
        this.loadCours();
      },
      error: (error) => {
        this.isSavingEdition = false;
        this.editionError = error.error?.message || "La modification du cours a échoué.";
      }
    });
  }

  supprimerCours(cours: Cours): void {
    this.confirmation.demander({
      titre: 'Supprimer le cours',
      message: `« ${cours.titre} » et toutes ses ressources seront définitivement supprimés.`,
      libelleConfirmer: 'Supprimer le cours',
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.suppressionCoursEnCoursId = cours.id;
      this.coursService.delete(cours.id).subscribe({
        next: () => {
          this.suppressionCoursEnCoursId = null;
          this.loadCours();
          this.notifications.succes(`Le cours « ${cours.titre} » a été supprimé.`);
        },
        error: () => {
          this.suppressionCoursEnCoursId = null;
          this.notifications.erreur('La suppression du cours a échoué.');
        }
      });
    });
  }

  ouvrirPresence(cours: Cours) {
    this.coursEnPresence = cours;
    this.seanceSelectionnee = null;
    this.rosterSeance = [];
    this.presenceError = '';
    this.nouvelleSeanceTitre = '';
    this.nouvelleSeanceDate = this.aujourdhuiIso();
    this.seanceService.getByCours(cours.id).subscribe({
      next: (list) => (this.seancesDuCours = list),
      error: () => (this.seancesDuCours = []),
    });
  }

  fermerPresence() {
    this.coursEnPresence = null;
    this.seanceSelectionnee = null;
  }

  creerSeance() {
    if (!this.coursEnPresence || !this.nouvelleSeanceTitre.trim() || !this.nouvelleSeanceDate) return;

    this.isSavingSeance = true;
    this.presenceError = '';
    this.seanceService.creer(this.coursEnPresence.id, this.nouvelleSeanceTitre.trim(), this.nouvelleSeanceDate).subscribe({
      next: (seance) => {
        this.isSavingSeance = false;
        this.seancesDuCours = [seance, ...this.seancesDuCours];
        this.nouvelleSeanceTitre = '';
        this.selectionnerSeance(seance);
      },
      error: (error) => {
        this.isSavingSeance = false;
        this.presenceError = error.error?.message || "La création de la séance a échoué.";
      },
    });
  }

  selectionnerSeance(seance: Seance) {
    this.seanceSelectionnee = seance;
    this.chargementRoster = true;
    this.presenceService.getRosterSeance(seance.id).subscribe({
      next: (roster) => {
        this.rosterSeance = roster;
        this.chargementRoster = false;
      },
      error: () => {
        this.rosterSeance = [];
        this.chargementRoster = false;
      },
    });
  }

  retourListeSeances() {
    this.seanceSelectionnee = null;
  }

  marquerPresence(etudiant: PresenceEtudiant, present: boolean) {
    etudiant.present = present;
  }

  enregistrerPresences() {
    if (!this.seanceSelectionnee) return;
    const presences = this.rosterSeance
      .filter(e => e.present !== null)
      .map(e => ({ etudiantId: e.etudiantId, present: e.present as boolean }));
    if (!presences.length) return;

    this.isSavingPresences = true;
    this.presenceError = '';
    this.presenceService.enregistrer(this.seanceSelectionnee.id, presences).subscribe({
      next: () => {
        this.isSavingPresences = false;
      },
      error: (error) => {
        this.isSavingPresences = false;
        this.presenceError = error.error?.message || "L'enregistrement des présences a échoué.";
      },
    });
  }

  private loadCours(): void {
    this.coursService.getAll().subscribe({
      next: (data) => {
        this.cours = data || [];
        this.mettreEnAvantLeCoursCible();
      },
      error: () => {
        this.cours = [];
      }
    });
  }

  ouvrirExplorateur() {
    this.fileInput.nativeElement.click();
  }

  onFichierSelectionne(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.fichierSelectionne = input.files[0];
    }
  }

  onSubmit() {
    if (this.depotForm.invalid || !this.fichierSelectionne) {
      this.depotForm.markAllAsTouched();
      return;
    }

    this.isUploading = true;
    this.uploadError = '';
    this.uploadMessage = '';
    const { coursCible, typeRessource, datePublication } = this.depotForm.getRawValue();
    this.supportService.uploadCourseSupport(
      Number(coursCible),
      typeRessource,
      datePublication,
      this.fichierSelectionne
    ).subscribe({
      next: () => {
        this.isUploading = false;
        this.uploadMessage = 'La ressource a été publiée.';
        this.fichierSelectionne = null;
        this.depotForm.reset({ typeRessource: 'pdf', coursCible: '', datePublication: this.aujourdhuiIso() });
        this.fileInput.nativeElement.value = '';
        this.loadCours();
        this.chargerSupports();
      },
      error: (error) => {
        this.isUploading = false;
        this.uploadError = error.error?.message || 'La publication de la ressource a échoué.';
      }
    });
  }

  /** Date du jour au format attendu par <input type="date"> (AAAA-MM-JJ). */
  private aujourdhuiIso(): string {
    const maintenant = new Date();
    const mois = String(maintenant.getMonth() + 1).padStart(2, '0');
    const jour = String(maintenant.getDate()).padStart(2, '0');
    return `${maintenant.getFullYear()}-${mois}-${jour}`;
  }

  /** Vrai si la date saisie est posterieure a aujourd'hui : la publication sera differee. */
  get publicationDifferee(): boolean {
    const saisie = this.depotForm?.get('datePublication')?.value;
    return !!saisie && saisie > this.aujourdhuiIso();
  }

  recherche: string = '';
  semestreSelectionne: string = ''; // '' = tous les semestres
  cohorteSelectionnee: string = ''; // '' = toutes les cohortes

  /** Semestres reellement presents dans les cours du formateur (pas de liste figee). */
  get semestresDisponibles(): string[] {
    const semestres = this.cours
      .map(c => c.semestre)
      .filter((s): s is string => !!s && s.trim().length > 0);
    return [...new Set(semestres)].sort();
  }

  /** Idem pour les cohortes : seules celles rattachees a au moins un cours sont proposees. */
  get cohortesDisponibles(): Cohorte[] {
    const parId = new Map<string, Cohorte>();
    this.cours.forEach(c => (c.cohortes || []).forEach(coh => parId.set(String(coh.id), coh)));
    return [...parId.values()];
  }

  get coursFiltres(): Cours[] {
    const recherche = this.recherche.trim().toLowerCase();
    return this.cours.filter(c => {
      const correspondRecherche = !recherche
        || c.titre.toLowerCase().includes(recherche)
        || c.code.toLowerCase().includes(recherche);
      const correspondSemestre = !this.semestreSelectionne || c.semestre === this.semestreSelectionne;
      const correspondCohorte = !this.cohorteSelectionnee
        || (c.cohortes || []).some(coh => String(coh.id) === this.cohorteSelectionnee);
      return correspondRecherche && correspondSemestre && correspondCohorte;
    });
  }

  urlImage(cours: Cours): string {
    if (!cours.imageUrl) return '';
    const baseUrl = environment.apiUrl.replace('/api', '');
    return `${baseUrl}${cours.imageUrl}`;
  }
}
