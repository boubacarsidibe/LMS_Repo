import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Etudiant } from '../../models/etudiant';
import { EtudiantService } from '../../services/etudiant.service';
import { Promotion } from '../../models/promotion';
import { PromotionService } from '../../services/promotion.service';

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

  etudiantEnContact: Etudiant | null = null;
  afficherRelanceCollective = false;
  objetMessage = '';
  contenuMessage = '';
  isEnvoiEnCours = false;
  messageErreur = '';
  messageSucces = '';

  constructor(
    private router: Router,
    private etudiantService: EtudiantService,
    private promotionService: PromotionService
  ) {}

  ngOnInit(): void {
    this.etudiantService.getAll().subscribe({ next: list => this.etudiants = list, error: () => this.etudiants = [] });
    this.promotionService.getAll().subscribe({ next: list => this.promotions = list, error: () => this.promotions = [] });
  }

  get etudiantsFiltres(): Etudiant[] {
    return this.etudiants.filter(e => {
      const correspondRecherche = e.nom.toLowerCase().includes(this.recherche.toLowerCase());
      const correspondPromotion = !this.promotionSelectionnee || e.promotion === this.promotionSelectionnee;
      return correspondRecherche && correspondPromotion;
    });
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
    this.objetMessage = '';
    this.contenuMessage = '';
    this.messageErreur = '';
    this.messageSucces = '';
  }

  ouvrirRelanceCollective() {
    this.etudiantEnContact = null;
    this.afficherRelanceCollective = true;
    this.objetMessage = '';
    this.contenuMessage = '';
    this.messageErreur = '';
    this.messageSucces = '';
  }

  fermerModalMessage() {
    this.etudiantEnContact = null;
    this.afficherRelanceCollective = false;
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
