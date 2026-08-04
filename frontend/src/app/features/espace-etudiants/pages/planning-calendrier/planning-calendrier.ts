import { Component } from '@angular/core';
import { EVENEMENTS_MOCK, EvenementCalendrier } from '../../data/evenements.mock';

interface JourCalendrier {
  numero: number;
  date: Date;
  horsMois: boolean;
  evenements: EvenementCalendrier[];
}

@Component({
  selector: 'app-planning-calendrier',
  standalone: false,
  templateUrl: './planning-calendrier.html',
  styleUrl: './planning-calendrier.css',
})
export class PlanningCalendrier {
  moisAffiche: Date = new Date();
  jours: JourCalendrier[] = [];
  evenements: EvenementCalendrier[] = EVENEMENTS_MOCK;
  nomsMois = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];

  constructor() {
    this.genererCalendrier();
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

  get prochainesSeances(): (EvenementCalendrier & { libelleDate: string })[] {
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
}