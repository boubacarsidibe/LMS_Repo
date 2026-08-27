import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Seance } from '../../models/seance';
import { SeanceCreation, SeanceService } from '../../services/seance.service';
import { Cours } from '../../models/cours';
import { CoursService } from '../../services/cours.service';

/**
 * Vue calendrier d'une {@link Seance} : la date/heure API (un seul champ
 * {@code dateHeure} ISO) est éclatée en {@code date}/{@code heure} pour que le
 * gabarit — hérité de l'ancien calendrier — puisse continuer à grouper par jour
 * et trier par heure sans réécriture.
 */
interface SeanceVM {
  id: number;
  coursId: number;
  coursTitre: string;
  titre: string;
  type: string;
  date: Date;
  heure: string;
  dureeMinutes: number;
  lieuOuLien: string | null;
}

interface JourCalendrier {
  numero: number;
  date: Date;
  horsMois: boolean;
  seances: SeanceVM[];
}

@Component({
  selector: 'app-planning-calendrier',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './planning-calendrier.html',
  styleUrl: './planning-calendrier.css',
})
export class PlanningCalendrier implements OnInit {
  moisAffiche: Date = new Date();
  jours: JourCalendrier[] = [];
  seances: SeanceVM[] = [];
  mesCours: Cours[] = [];
  nomsMois = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];
  nomsJours = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'];

  vueActive: 'mois' | 'semaine' | 'jour' | 'liste' = 'mois';

  afficherFormulaire = false;
  seanceForm: FormGroup;
  isSubmitting = false;
  errorMessage = '';
  seanceEnEdition: SeanceVM | null = null;

  constructor(private seanceService: SeanceService, private coursService: CoursService, private fb: FormBuilder) {
    this.seanceForm = this.fb.group({
      coursId: ['', Validators.required],
      titre: ['', Validators.required],
      date: ['', Validators.required],
      heure: ['', Validators.required],
      type: ['cours', Validators.required],
      dureeMinutes: [60, [Validators.required, Validators.min(1)]],
      lieuOuLien: [''],
    });
  }

  ngOnInit(): void {
    forkJoin({
      cours: this.coursService.getAll(),
      seances: this.seanceService.getMesSeances(),
    }).subscribe({
      next: ({ cours, seances }) => {
        this.mesCours = cours;
        this.seances = seances.map((s) => this.versVM(s));
        this.genererCalendrier();
      },
      error: () => {
        this.mesCours = [];
        this.seances = [];
        this.genererCalendrier();
      },
    });
  }

  private versVM(s: Seance): SeanceVM {
    const date = new Date(s.dateHeure);
    return {
      id: s.id,
      coursId: s.coursId,
      coursTitre: s.coursTitre,
      titre: s.titre,
      type: s.type,
      date,
      heure: `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`,
      dureeMinutes: s.dureeMinutes,
      lieuOuLien: s.lieuOuLien,
    };
  }

  genererCalendrier() {
    const annee = this.moisAffiche.getFullYear();
    const mois = this.moisAffiche.getMonth();

    const premierJourMois = new Date(annee, mois, 1);
    const dernierJourMois = new Date(annee, mois + 1, 0);

    // getDay() renvoie 0=Dimanche...6=Samedi. On veut 0=Lundi...6=Dimanche
    const decalage = (premierJourMois.getDay() + 6) % 7;

    const jours: JourCalendrier[] = [];

    // Jours du mois précédent pour compléter la première semaine
    for (let i = decalage; i > 0; i--) {
      const date = new Date(annee, mois, 1 - i);
      jours.push({ numero: date.getDate(), date, horsMois: true, seances: [] });
    }

    // Jours du mois affiché
    for (let jour = 1; jour <= dernierJourMois.getDate(); jour++) {
      const date = new Date(annee, mois, jour);
      jours.push({
        numero: jour,
        date,
        horsMois: false,
        seances: this.seances.filter(s => this.memesDates(s.date, date))
      });
    }

    // Jours du mois suivant pour compléter la dernière semaine (grille multiple de 7)
    const resteAJours = (7 - (jours.length % 7)) % 7;
    for (let jour = 1; jour <= resteAJours; jour++) {
      const date = new Date(annee, mois + 1, jour);
      jours.push({ numero: jour, date, horsMois: true, seances: [] });
    }

    this.jours = jours;
  }

  memesDates(a: Date, b: Date): boolean {
    return a.getFullYear() === b.getFullYear() &&
           a.getMonth() === b.getMonth() &&
           a.getDate() === b.getDate();
  }

  moisPrecedent() {
    this.moisAffiche = new Date(this.moisAffiche.getFullYear(), this.moisAffiche.getMonth() - 1, 1);
    this.genererCalendrier();
  }

  moisSuivant() {
    this.moisAffiche = new Date(this.moisAffiche.getFullYear(), this.moisAffiche.getMonth() + 1, 1);
    this.genererCalendrier();
  }

  changerVue(vue: 'mois' | 'semaine' | 'jour' | 'liste') {
    this.vueActive = vue;
  }

  precedent() {
    if (this.vueActive === 'semaine') {
      this.moisAffiche = this.ajouterJours(this.moisAffiche, -7);
    } else if (this.vueActive === 'jour') {
      this.moisAffiche = this.ajouterJours(this.moisAffiche, -1);
    } else {
      this.moisPrecedent();
      return;
    }
    this.genererCalendrier();
  }

  suivant() {
    if (this.vueActive === 'semaine') {
      this.moisAffiche = this.ajouterJours(this.moisAffiche, 7);
    } else if (this.vueActive === 'jour') {
      this.moisAffiche = this.ajouterJours(this.moisAffiche, 1);
    } else {
      this.moisSuivant();
      return;
    }
    this.genererCalendrier();
  }

  private ajouterJours(date: Date, n: number): Date {
    const resultat = new Date(date);
    resultat.setDate(resultat.getDate() + n);
    return resultat;
  }

  private debutSemaine(date: Date): Date {
    const resultat = new Date(date);
    const decalage = (resultat.getDay() + 6) % 7; // 0 = lundi
    resultat.setDate(resultat.getDate() - decalage);
    resultat.setHours(0, 0, 0, 0);
    return resultat;
  }

  get joursSemaine(): JourCalendrier[] {
    const lundi = this.debutSemaine(this.moisAffiche);
    const jours: JourCalendrier[] = [];
    for (let i = 0; i < 7; i++) {
      const date = this.ajouterJours(lundi, i);
      jours.push({
        numero: date.getDate(),
        date,
        horsMois: false,
        seances: this.seances.filter(s => this.memesDates(s.date, date))
      });
    }
    return jours;
  }

  get seancesDuJour(): SeanceVM[] {
    return this.seances
      .filter(s => this.memesDates(s.date, this.moisAffiche))
      .sort((a, b) => a.heure.localeCompare(b.heure));
  }

  get seancesListe(): SeanceVM[] {
    return [...this.seances].sort((a, b) =>
      a.date.getTime() - b.date.getTime() || a.heure.localeCompare(b.heure)
    );
  }

  get libellePeriode(): string {
    if (this.vueActive === 'semaine') {
      const lundi = this.debutSemaine(this.moisAffiche);
      const dimanche = this.ajouterJours(lundi, 6);
      const memeMois = lundi.getMonth() === dimanche.getMonth();
      const debut = memeMois ? `${lundi.getDate()}` : `${lundi.getDate()} ${this.nomsMois[lundi.getMonth()]}`;
      return `Semaine du ${debut} au ${dimanche.getDate()} ${this.nomsMois[dimanche.getMonth()]} ${dimanche.getFullYear()}`;
    }
    if (this.vueActive === 'jour') {
      return `${this.nomsJours[(this.moisAffiche.getDay() + 6) % 7]} ${this.moisAffiche.getDate()} ${this.nomsMois[this.moisAffiche.getMonth()]} ${this.moisAffiche.getFullYear()}`;
    }
    if (this.vueActive === 'liste') {
      return 'Toutes les séances';
    }
    return `${this.nomsMois[this.moisAffiche.getMonth()]} ${this.moisAffiche.getFullYear()}`;
  }

  estAujourdhui(date: Date): boolean {
    return this.memesDates(date, new Date());
  }

  classeEvenement(type: string): string {
    switch (type) {
      case 'cours': return 'bg-primary-container text-on-primary-container';
      case 'echeance': return 'bg-error-container text-error border border-error/20';
      case 'tutorat': return 'bg-secondary-container text-on-secondary-container';
      default: return '';
    }
  }

  get prochainesSeances(): (SeanceVM & { libelleDate: string })[] {
    const maintenant = new Date();
    maintenant.setHours(0, 0, 0, 0);

    return this.seances
      .filter(s => s.date >= maintenant)
      .sort((a, b) => a.date.getTime() - b.date.getTime())
      .slice(0, 3)
      .map(s => ({ ...s, libelleDate: this.libelleDateRelative(s.date) }));
  }

  libelleDateRelative(date: Date): string {
    const aujourdhui = new Date();
    aujourdhui.setHours(0, 0, 0, 0);
    const demain = new Date(aujourdhui);
    demain.setDate(demain.getDate() + 1);

    if (this.memesDates(date, aujourdhui)) return "Aujourd'hui";
    if (this.memesDates(date, demain)) return 'Demain';
    return `${date.getDate()} ${this.nomsMois[date.getMonth()].toLowerCase().slice(0, 3)}.`;
  }

  iconeEvenement(type: string): string {
    switch (type) {
      case 'cours': return 'video_camera_front';
      case 'tutorat': return 'psychology';
      case 'echeance': return 'assignment';
      default: return 'event';
    }
  }

  fondIconeEvenement(type: string): string {
    switch (type) {
      case 'cours': return 'bg-primary-container text-on-primary-container';
      case 'tutorat': return 'bg-secondary-container/30 text-secondary';
      case 'echeance': return 'bg-error-container/20 text-error';
      default: return '';
    }
  }

  ouvrirFormulaire() {
    this.errorMessage = '';
    this.seanceEnEdition = null;
    this.seanceForm.reset({
      coursId: this.mesCours[0]?.id ?? '',
      titre: '',
      date: '',
      heure: '',
      type: 'cours',
      dureeMinutes: 60,
      lieuOuLien: '',
    });
    this.afficherFormulaire = true;
  }

  ouvrirEdition(seance: SeanceVM) {
    this.errorMessage = '';
    this.seanceEnEdition = seance;
    this.seanceForm.setValue({
      coursId: seance.coursId,
      titre: seance.titre,
      date: this.formatDateForInput(seance.date),
      heure: seance.heure,
      type: seance.type,
      dureeMinutes: seance.dureeMinutes,
      lieuOuLien: seance.lieuOuLien || '',
    });
    this.afficherFormulaire = true;
  }

  private formatDateForInput(date: Date): string {
    const annee = date.getFullYear();
    const mois = String(date.getMonth() + 1).padStart(2, '0');
    const jour = String(date.getDate()).padStart(2, '0');
    return `${annee}-${mois}-${jour}`;
  }

  fermerFormulaire() {
    this.afficherFormulaire = false;
    this.seanceEnEdition = null;
  }

  lancerSeance(seance: SeanceVM) {
    if (seance.lieuOuLien) {
      window.open(seance.lieuOuLien, '_blank');
    } else {
      this.ouvrirEdition(seance);
    }
  }

  onSubmit() {
    if (this.seanceForm.invalid) {
      this.seanceForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    const valeurs = this.seanceForm.getRawValue();
    const donnees: SeanceCreation = {
      coursId: Number(valeurs.coursId),
      titre: valeurs.titre,
      type: valeurs.type,
      dateHeure: `${valeurs.date}T${valeurs.heure}:00`,
      dureeMinutes: Number(valeurs.dureeMinutes),
      lieuOuLien: valeurs.lieuOuLien || null,
    };
    const requete = this.seanceEnEdition
      ? this.seanceService.modifier(this.seanceEnEdition.id, donnees)
      : this.seanceService.creerSeance(donnees);

    requete.subscribe({
      next: (seance) => {
        this.isSubmitting = false;
        const vm = this.versVM(seance);
        if (this.seanceEnEdition) {
          this.seances = this.seances.map(s => s.id === vm.id ? vm : s);
        } else {
          this.seances = [...this.seances, vm];
        }
        this.genererCalendrier();
        this.afficherFormulaire = false;
        this.seanceEnEdition = null;
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error.error?.message || "L'enregistrement de la séance a échoué.";
      },
    });
  }

  supprimerSeance(seance: SeanceVM, evenement: Event) {
    evenement.stopPropagation();
    if (!confirm(`Supprimer la séance « ${seance.titre} » ?`)) {
      return;
    }
    this.seanceService.supprimer(seance.id).subscribe({
      next: () => {
        this.seances = this.seances.filter(s => s.id !== seance.id);
        this.genererCalendrier();
        if (this.seanceEnEdition?.id === seance.id) {
          this.fermerFormulaire();
        }
      },
    });
  }
}
