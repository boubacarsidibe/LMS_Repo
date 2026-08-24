import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SituationProfessionnelle, SituationProfessionnelleSaisie } from '../../models/alumni.models';
import { AlumniService } from '../../services/alumni.service';

/**
 * Historique professionnel de l'alumni (TDR §C, « suivi de leur situation
 * professionnelle ») : une ligne par poste occupé, la plus récente en tête.
 */
@Component({
  selector: 'app-alumni-mon-parcours',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mon-parcours.html',
})
export class MonParcours implements OnInit {
  chargement = true;
  situations: SituationProfessionnelle[] = [];

  formulaireOuvert = false;
  idEnEdition: number | null = null;
  saisie: SituationProfessionnelleSaisie = this.saisieVierge();
  enregistrement = false;

  messageErreur = '';

  constructor(private alumniService: AlumniService) {}

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.chargement = true;
    this.alumniService.getMesSituations().subscribe({
      next: (situations) => {
        this.situations = situations;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  private saisieVierge(): SituationProfessionnelleSaisie {
    return { intitulePoste: '', entreprise: '', dateDebut: '', dateFin: null };
  }

  ouvrirAjout(): void {
    this.idEnEdition = null;
    this.saisie = this.saisieVierge();
    this.formulaireOuvert = true;
    this.messageErreur = '';
  }

  ouvrirEdition(situation: SituationProfessionnelle): void {
    this.idEnEdition = situation.id;
    this.saisie = {
      intitulePoste: situation.intitulePoste,
      entreprise: situation.entreprise,
      dateDebut: situation.dateDebut,
      dateFin: situation.dateFin,
    };
    this.formulaireOuvert = true;
    this.messageErreur = '';
  }

  get saisieValide(): boolean {
    return !!this.saisie.intitulePoste.trim() && !!this.saisie.entreprise.trim() && !!this.saisie.dateDebut;
  }

  enregistrer(): void {
    if (!this.saisieValide || this.enregistrement) {
      return;
    }
    this.enregistrement = true;
    this.messageErreur = '';
    const donnees: SituationProfessionnelleSaisie = {
      ...this.saisie,
      dateFin: this.saisie.dateFin || null,
    };
    const requete = this.idEnEdition
      ? this.alumniService.modifierSituation(this.idEnEdition, donnees)
      : this.alumniService.ajouterSituation(donnees);
    requete.subscribe({
      next: () => {
        this.enregistrement = false;
        this.formulaireOuvert = false;
        this.charger();
      },
      error: (erreur) => {
        this.enregistrement = false;
        this.messageErreur = erreur.error?.message || "L'enregistrement a échoué.";
      },
    });
  }

  supprimer(situation: SituationProfessionnelle): void {
    this.alumniService.supprimerSituation(situation.id).subscribe({
      next: () => this.charger(),
      error: () => (this.messageErreur = 'La suppression a échoué.'),
    });
  }
}
