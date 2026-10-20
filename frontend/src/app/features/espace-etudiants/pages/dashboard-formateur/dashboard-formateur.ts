import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormateurService, FormateurProfile, FormateurDashboard } from '../../services/formateur.service';
import { CoursService } from '../../services/cours.service';
import { Cours } from '../../models/cours';
import { SeanceService } from '../../services/seance.service';
import { Seance } from '../../models/seance';
import { SujetService } from '../../services/sujet.service';
import { SujetForum } from '../../models/sujet';
import { EtudiantService } from '../../services/etudiant.service';
import { Etudiant } from '../../models/etudiant';
import { RetourService } from '../../services/retour.service';
import { RetourEvaluation } from '../../models/retour-evaluation';

/** Vue d'aperçu d'une {@link Seance} : la date/heure ISO unique de l'API est
 * éclatée en {@code date}/{@code heure} pour l'affichage du tableau de bord. */
interface SeanceApercu {
  titre: string;
  type: string;
  date: Date;
  heure: string;
}

@Component({
  selector: 'app-dashboard-formateur',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard-formateur.html',
  styleUrl: './dashboard-formateur.css',
})
export class DashboardFormateur implements OnInit {
  profile: FormateurProfile | null = null;
  dashboard: FormateurDashboard | null = null;
  loading = false;

  mesCours: Cours[] = [];
  chargementCours = false;

  prochainesSeances: SeanceApercu[] = [];
  chargementSeances = false;

  derniersSujets: SujetForum[] = [];
  sujetsNonLus = 0;
  chargementSujets = false;

  mesEtudiants: Etudiant[] = [];
  chargementEtudiants = false;

  retours: RetourEvaluation[] = [];
  chargementRetours = false;

  private readonly nomsMois = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Août', 'Sep', 'Oct', 'Nov', 'Déc'];

  constructor(
    private formateurService: FormateurService,
    private coursService: CoursService,
    private seanceService: SeanceService,
    private sujetService: SujetService,
    private etudiantService: EtudiantService,
    private retourService: RetourService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.formateurService.getProfile().subscribe({
      next: p => { this.profile = p; this.loading = false; },
      error: () => { this.profile = null; this.loading = false; }
    });

    this.formateurService.getDashboard().subscribe({
      next: dashboard => this.dashboard = dashboard,
      error: () => this.dashboard = null
    });

    this.chargementCours = true;
    this.coursService.getAll().subscribe({
      next: list => { this.mesCours = list || []; this.chargementCours = false; },
      error: () => { this.mesCours = []; this.chargementCours = false; }
    });

    this.chargementSeances = true;
    this.seanceService.getMesSeances().subscribe({
      next: list => {
        const aujourdhui = new Date();
        aujourdhui.setHours(0, 0, 0, 0);
        this.prochainesSeances = (list || [])
          .map(s => this.versApercu(s))
          .filter(s => s.date >= aujourdhui)
          .sort((a, b) => a.date.getTime() - b.date.getTime())
          .slice(0, 3);
        this.chargementSeances = false;
      },
      error: () => { this.prochainesSeances = []; this.chargementSeances = false; }
    });

    this.chargementSujets = true;
    this.sujetService.getAll().subscribe({
      next: list => {
        const tousLesSujets = list || [];
        this.sujetsNonLus = tousLesSujets.filter(s => s.statut === 'nouveau').length;
        this.derniersSujets = [...tousLesSujets]
          .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
          .slice(0, 2);
        this.chargementSujets = false;
      },
      error: () => { this.derniersSujets = []; this.chargementSujets = false; }
    });

    this.chargementEtudiants = true;
    this.etudiantService.getAll().subscribe({
      next: list => { this.mesEtudiants = list || []; this.chargementEtudiants = false; },
      error: () => { this.mesEtudiants = []; this.chargementEtudiants = false; }
    });

    this.chargementRetours = true;
    this.retourService.getAll().subscribe({
      next: list => { this.retours = list || []; this.chargementRetours = false; },
      error: () => { this.retours = []; this.chargementRetours = false; }
    });
  }

  /** Part moyenne des supports consultés, tous cours confondus (même règle que l'espace de l'étudiant). */
  get progressionMoyenne(): number {
    if (!this.mesEtudiants.length) return 0;
    return Math.round(this.mesEtudiants.reduce((somme, e) => somme + e.progression, 0) / this.mesEtudiants.length);
  }

  /** Étudiants dont la moyenne appelle un accompagnement : même définition que l'écran « Suivi des étudiants ». */
  get etudiantsAAccompagner(): number {
    return this.mesEtudiants.filter(({ statut }) => statut === 'À risque' || statut === 'En retard').length;
  }

  /** Moyenne des avis sur 5, au dixième ; null tant qu'aucun avis n'a été déposé (à ne pas confondre avec 0). */
  get noteMoyenneRetours(): number | null {
    if (!this.retours.length) return null;
    return Math.round((this.retours.reduce((somme, r) => somme + r.note, 0) / this.retours.length) * 10) / 10;
  }

  get retoursAAgir(): number {
    return this.retours.filter(r => r.statut === 'action-requise').length;
  }

  private versApercu(s: Seance): SeanceApercu {
    const date = new Date(s.dateHeure);
    return {
      titre: s.titre,
      type: s.type,
      date,
      heure: `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`,
    };
  }

  get nombreRessourcesTotal(): number {
    return this.mesCours.reduce((total, c) => total + (c.nbRessources || 0), 0);
  }

  jourCourt(date: Date): string {
    return String(date.getDate()).padStart(2, '0');
  }

  moisCourt(date: Date): string {
    return this.nomsMois[date.getMonth()];
  }

  iconeTypeEvenement(type: string): string {
    switch (type) {
      case 'cours': return 'video_camera_front';
      case 'tutorat': return 'psychology';
      case 'echeance': return 'assignment';
      default: return 'event';
    }
  }

  libelleTypeEvenement(type: string): string {
    switch (type) {
      case 'cours': return 'Classe Virtuelle';
      case 'tutorat': return 'Tutorat';
      case 'echeance': return 'Échéance';
      default: return type;
    }
  }

  classeTypeEvenement(type: string): string {
    switch (type) {
      case 'echeance': return 'text-on-error-container';
      default: return 'text-secondary';
    }
  }
}
