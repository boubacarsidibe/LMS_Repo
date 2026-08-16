import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormateurService, FormateurProfile, FormateurDashboard } from '../../services/formateur.service';
import { CoursService } from '../../services/cours.service';
import { Cours } from '../../models/cours';
import { EvenementService } from '../../services/evenement.service';
import { Evenement } from '../../models/evenement';
import { SujetService } from '../../services/sujet.service';
import { SujetForum } from '../../models/sujet';
import { EtudiantService } from '../../services/etudiant.service';
import { Etudiant } from '../../models/etudiant';

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

  prochainesSeances: Evenement[] = [];
  chargementSeances = false;

  derniersSujets: SujetForum[] = [];
  sujetsNonLus = 0;
  chargementSujets = false;

  mesEtudiants: Etudiant[] = [];
  chargementEtudiants = false;

  private readonly nomsMois = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Août', 'Sep', 'Oct', 'Nov', 'Déc'];

  constructor(
    private formateurService: FormateurService,
    private coursService: CoursService,
    private evenementService: EvenementService,
    private sujetService: SujetService,
    private etudiantService: EtudiantService
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
    this.evenementService.getAll().subscribe({
      next: list => {
        const aujourdhui = new Date();
        aujourdhui.setHours(0, 0, 0, 0);
        this.prochainesSeances = (list || [])
          .filter(e => e.date >= aujourdhui)
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
