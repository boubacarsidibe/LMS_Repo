import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Etudiant } from '../../models/etudiant';
import { EtudiantService } from '../../services/etudiant.service';
import { Promotion } from '../../models/promotion';
import { PromotionService } from '../../services/promotion.service';
import { Cours } from '../../models/cours';
import { CoursService } from '../../services/cours.service';
import { SujetService } from '../../services/sujet.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-suivi-etudiants',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './suivi-etudiants.html',
  styleUrl: './suivi-etudiants.css',
})
export class SuiviEtudiants implements OnInit {
  etudiants: Etudiant[] = [];
  promotions: Promotion[] = [];
  recherche: string = '';
  promotionSelectionnee: string = '';
  statutSelectionne: '' | Etudiant['statut'] = '';
  chargement = true;
  erreurChargement = '';

  etudiantEnContact: Etudiant | null = null;
  afficherRelanceCollective = false;
  objetMessage = '';
  contenuMessage = '';
  isEnvoiEnCours = false;
  messageErreur = '';
  messageSucces = '';

  /** Relance collective par sujet de forum, plutôt que par e-mail (voir ouvrirRelanceForum). */
  afficherRelanceForum = false;
  mesCours: Cours[] = [];
  chargementCoursForum = false;
  coursIdForum: number | null = null;
  titreForum = '';
  messageForum = '';
  isEnvoiForumEnCours = false;
  messageErreurForum = '';
  messageSuccesForum = '';

  constructor(
    private router: Router,
    private etudiantService: EtudiantService,
    private promotionService: PromotionService,
    private coursService: CoursService,
    private sujetService: SujetService
  ) {}

  ngOnInit(): void {
    this.chargerDonnees();
  }

  chargerDonnees(): void {
    this.chargement = true;
    this.erreurChargement = '';
    forkJoin({
      etudiants: this.etudiantService.getAll(),
      promotions: this.promotionService.getMesPromotionsFormateur(),
    }).subscribe({
      next: ({ etudiants, promotions }) => {
        this.etudiants = etudiants || [];
        this.promotions = promotions || [];
        this.chargement = false;
      },
      error: () => {
        this.etudiants = [];
        this.promotions = [];
        this.chargement = false;
        this.erreurChargement = 'Le suivi des étudiants est indisponible. Réessayez dans un instant.';
      },
    });
  }

  get etudiantsFiltres(): Etudiant[] {
    return this.etudiants.filter(e => {
      const correspondRecherche = e.nom.toLowerCase().includes(this.recherche.toLowerCase());
      const correspondPromotion = !this.promotionSelectionnee || e.promotion === this.promotionSelectionnee;
      const correspondStatut = !this.statutSelectionne || e.statut === this.statutSelectionne;
      return correspondRecherche && correspondPromotion && correspondStatut;
    });
  }

  get progressionMoyenne(): number {
    if (!this.etudiants.length) return 0;
    return Math.round(this.etudiants.reduce((total, etudiant) => total + etudiant.progression, 0) / this.etudiants.length);
  }

  get etudiantsPrioritaires(): number {
    return this.etudiants.filter(({ statut }) => statut === 'À risque' || statut === 'En retard').length;
  }

  get engagementMoyen(): number {
    if (!this.etudiants.length) return 0;
    return Math.round((this.etudiants.filter(({ statut }) => statut === 'Actif').length / this.etudiants.length) * 100);
  }

  reinitialiserFiltres(): void {
    this.recherche = '';
    this.promotionSelectionnee = '';
    this.statutSelectionne = '';
  }

  voirProfil(id: number) {
    this.router.navigate(['/espace-etudiant/formateur/profil-etudiant', id]);
  }

  classeProgression(progression: number): string {
    if (progression < 50) return 'bg-error';
    return 'bg-secondary';
  }

  classeStatut(statut: string): string {
    switch (statut) {
      case 'En retard': return 'bg-error-container text-on-error-container';
      case 'Actif': return 'bg-secondary-container text-on-secondary-container';
      case 'À risque': return 'bg-surface-container-high text-on-surface-variant';
      default: return '';
    }
  }

  ouvrirContact(e: Etudiant) {
    this.etudiantEnContact = e;
    this.afficherRelanceCollective = false;
    this.afficherRelanceForum = false;
    this.objetMessage = '';
    this.contenuMessage = '';
    this.messageErreur = '';
    this.messageSucces = '';
  }

  ouvrirRelanceCollective() {
    this.etudiantEnContact = null;
    this.afficherRelanceCollective = true;
    this.afficherRelanceForum = false;
    this.objetMessage = '';
    this.contenuMessage = '';
    this.messageErreur = '';
    this.messageSucces = '';
  }

  fermerModalMessage() {
    this.etudiantEnContact = null;
    this.afficherRelanceCollective = false;
  }

  ouvrirRelanceForum(): void {
    this.etudiantEnContact = null;
    this.afficherRelanceCollective = false;
    this.afficherRelanceForum = true;
    this.coursIdForum = null;
    this.titreForum = '';
    this.messageForum = '';
    this.messageErreurForum = '';
    this.messageSuccesForum = '';
    if (this.mesCours.length === 0) {
      this.chargementCoursForum = true;
      this.coursService.getAll().subscribe({
        next: (cours) => {
          this.mesCours = cours || [];
          this.chargementCoursForum = false;
        },
        error: () => {
          this.mesCours = [];
          this.chargementCoursForum = false;
        },
      });
    }
  }

  fermerRelanceForum(): void {
    this.afficherRelanceForum = false;
  }

  envoyerRelanceForum(): void {
    if (!this.coursIdForum) {
      this.messageErreurForum = 'Choisissez un cours.';
      return;
    }
    if (!this.titreForum.trim() || !this.messageForum.trim()) {
      this.messageErreurForum = 'Le titre et le message sont requis.';
      return;
    }
    this.isEnvoiForumEnCours = true;
    this.messageErreurForum = '';
    this.messageSuccesForum = '';
    this.sujetService.creerSujet(this.titreForum.trim(), this.coursIdForum, this.messageForum.trim()).subscribe({
      next: () => {
        this.isEnvoiForumEnCours = false;
        this.messageSuccesForum = 'Sujet publié : ses étudiants le verront dans leur forum.';
        this.titreForum = '';
        this.messageForum = '';
        this.coursIdForum = null;
      },
      error: (erreur) => {
        this.isEnvoiForumEnCours = false;
        this.messageErreurForum = erreur.error?.message || "La publication sur le forum a échoué.";
      },
    });
  }

  envoyerMessage() {
    if (!this.objetMessage.trim() || !this.contenuMessage.trim()) {
      this.messageErreur = "L'objet et le message sont requis.";
      return;
    }
    this.isEnvoiEnCours = true;
    this.messageErreur = '';
    this.messageSucces = '';

    if (this.etudiantEnContact) {
      this.etudiantService.contacter(this.etudiantEnContact.id, this.objetMessage, this.contenuMessage).subscribe({
        next: (res) => {
          this.isEnvoiEnCours = false;
          this.messageSucces = res.message;
          this.fermerApresEnvoi();
        },
        error: (error) => {
          this.isEnvoiEnCours = false;
          this.messageErreur = error.error?.message || "L'envoi a échoué.";
        }
      });
    } else if (this.afficherRelanceCollective) {
      const ids = this.etudiantsFiltres.map(e => e.id);
      this.etudiantService.relancer(ids, this.objetMessage, this.contenuMessage).subscribe({
        next: (res) => {
          this.isEnvoiEnCours = false;
          this.messageSucces = res.echecs.length
            ? `${res.envoyes} email(s) envoyé(s), ${res.echecs.length} échec(s).`
            : `${res.envoyes} email(s) envoyé(s) avec succès.`;
          this.fermerApresEnvoi();
        },
        error: (error) => {
          this.isEnvoiEnCours = false;
          this.messageErreur = error.error?.message || "L'envoi a échoué.";
        }
      });
    }
  }

  /**
   * Referme le formulaire une fois l'envoi abouti, en conservant le message de
   * succes affiche sur la page. Sans cela la carte restait ouverte apres un envoi
   * reussi, laissant croire que rien ne s'etait passe.
   */
  private fermerApresEnvoi(): void {
    this.etudiantEnContact = null;
    this.afficherRelanceCollective = false;
    this.objetMessage = '';
    this.contenuMessage = '';
  }
}
