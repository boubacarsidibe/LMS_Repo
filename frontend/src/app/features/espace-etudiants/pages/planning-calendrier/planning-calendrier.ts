import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Evenement } from '../../models/evenement';
import { EvenementService } from '../../services/evenement.service';

interface JourCalendrier {
  numero: number;
  date: Date;
  horsMois: boolean;
  evenements: Evenement[];
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
  evenements: Evenement[] = [];
  nomsMois = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];
  nomsJours = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'];

  vueActive: 'mois' | 'semaine' | 'jour' | 'liste' = 'mois';

  afficherFormulaire = false;
  evenementForm: FormGroup;
  isSubmitting = false;
  errorMessage = '';
  seanceEnEdition: Evenement | null = null;

  constructor(private evenementService: EvenementService, private fb: FormBuilder) {
    this.evenementForm = this.fb.group({
      titre: ['', Validators.required],
      date: ['', Validators.required],
      heure: ['', Validators.required],
      type: ['cours', Validators.required],
      lienVisio: [''],
    });
  }

  ngOnInit(): void {
    this.evenementService.getAll().subscribe({
      next: (data) => {
        this.evenements = data;
        this.genererCalendrier();
      },
      error: () => {
        this.evenements = [];
        this.genererCalendrier();
      },
    });
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
      jours.push({ numero: date.getDate(), date, horsMois: true, evenements: [] });
    }

    // Jours du mois affiché
    for (let jour = 1; jour <= dernierJourMois.getDate(); jour++) {
      const date = new Date(annee, mois, jour);
      jours.push({
        numero: jour,
        date,
        horsMois: false,
        evenements: this.evenements.filter(e => this.memesDates(e.date, date))
      });
    }

    // Jours du mois suivant pour compléter la dernière semaine (grille multiple de 7)
    const resteAJours = (7 - (jours.length % 7)) % 7;
    for (let jour = 1; jour <= resteAJours; jour++) {
      const date = new Date(annee, mois + 1, jour);
      jours.push({ numero: jour, date, horsMois: true, evenements: [] });
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
        evenements: this.evenements.filter(e => this.memesDates(e.date, date))
      });
    }
    return jours;
  }

  get evenementsDuJour(): Evenement[] {
    return this.evenements
      .filter(e => this.memesDates(e.date, this.moisAffiche))
      .sort((a, b) => a.heure.localeCompare(b.heure));
  }

  get evenementsListe(): Evenement[] {
    return [...this.evenements].sort((a, b) =>
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
      return 'Tous les événements';
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

  get prochainesSeances(): (Evenement & { libelleDate: string })[] {
    const maintenant = new Date();
    maintenant.setHours(0, 0, 0, 0);

    return this.evenements
      .filter(e => e.date >= maintenant)
      .sort((a, b) => a.date.getTime() - b.date.getTime())
      .slice(0, 3)
      .map(e => ({ ...e, libelleDate: this.libelleDateRelative(e.date) }));
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
      case 'cours': return 'bg-primary-container text-on-primary';
      case 'tutorat': return 'bg-secondary-container/30 text-secondary';
      case 'echeance': return 'bg-error-container/20 text-error';
      default: return '';
    }
  }

  ouvrirFormulaire() {
    this.errorMessage = '';
    this.seanceEnEdition = null;
    this.evenementForm.reset({ titre: '', date: '', heure: '', type: 'cours', lienVisio: '' });
    this.afficherFormulaire = true;
  }

  ouvrirEdition(seance: Evenement) {
    this.errorMessage = '';
    this.seanceEnEdition = seance;
    this.evenementForm.setValue({
      titre: seance.titre,
      date: this.formatDateForInput(seance.date),
      heure: seance.heure,
      type: seance.type,
      lienVisio: seance.lienVisio || '',
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

  lancerSeance(seance: Evenement) {
    if (seance.lienVisio) {
      window.open(seance.lienVisio, '_blank');
    } else {
      this.ouvrirEdition(seance);
    }
  }

  onSubmit() {
    if (this.evenementForm.invalid) {
      this.evenementForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    const donnees = this.evenementForm.getRawValue();
    const requete = this.seanceEnEdition
      ? this.evenementService.update(this.seanceEnEdition.id, donnees)
      : this.evenementService.create(donnees);

    requete.subscribe({
      next: (evenement) => {
        this.isSubmitting = false;
        if (this.seanceEnEdition) {
          this.evenements = this.evenements.map(e => e.id === evenement.id ? evenement : e);
        } else {
          this.evenements = [...this.evenements, evenement];
        }
        this.genererCalendrier();
        this.afficherFormulaire = false;
        this.seanceEnEdition = null;
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error.error?.message || "L'enregistrement de l'événement a échoué.";
      },
    });
  }
}