import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Etudiant } from '../../models/etudiant';
import { EtudiantService } from '../../services/etudiant.service';
import { Cohorte } from '../../models/cohorte';
import { CohorteService } from '../../services/cohorte.service';

@Component({
  selector: 'app-suivi-etudiants',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './suivi-etudiants.html',
  styleUrl: './suivi-etudiants.css',
})
export class SuiviEtudiants implements OnInit {
  etudiants: Etudiant[] = [];
  cohortes: Cohorte[] = [];
  recherche: string = '';
  cohorteSelectionnee: string = '';

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
    private cohorteService: CohorteService
  ) {}

  ngOnInit(): void {
    this.etudiantService.getAll().subscribe({ next: list => this.etudiants = list, error: () => this.etudiants = [] });
    this.cohorteService.getAll().subscribe({ next: list => this.cohortes = list, error: () => this.cohortes = [] });
  }

  get etudiantsFiltres(): Etudiant[] {
    return this.etudiants.filter(e => {
      const correspondRecherche = e.nom.toLowerCase().includes(this.recherche.toLowerCase());
      const correspondCohorte = !this.cohorteSelectionnee || e.cohorte === this.cohorteSelectionnee;
      return correspondRecherche && correspondCohorte;
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
        },
        error: (error) => {
          this.isEnvoiEnCours = false;
          this.messageErreur = error.error?.message || "L'envoi a échoué.";
        }
      });
    }
  }
}