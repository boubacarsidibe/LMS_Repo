import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EnqueteSuivi, ReponseEnqueteSuiviSaisie } from '../../models/alumni.models';
import { AlumniService } from '../../services/alumni.service';

/**
 * Enquêtes de suivi (TDR §G) : mesurer l'impact à long terme de la formation sur
 * la carrière des diplômés. Répondre met à jour la réponse précédente si elle
 * existe déjà (unicité (enquête, alumni) côté serveur) — ce n'est pas signalé
 * comme une erreur, `dejaRepondu` sert seulement à orienter l'alumni.
 */
@Component({
  selector: 'app-alumni-enquetes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './enquetes-alumni.html',
})
export class EnquetesAlumni implements OnInit {
  chargement = true;
  enquetes: EnqueteSuivi[] = [];

  formulaireOuvert = false;
  enqueteEnCours: EnqueteSuivi | null = null;
  saisie: ReponseEnqueteSuiviSaisie = this.saisieVierge();
  envoiEnCours = false;
  reponseEnvoyee = false;
  messageErreur = '';

  constructor(private alumniService: AlumniService) {}

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.chargement = true;
    this.alumniService.getEnquetes().subscribe({
      next: (enquetes) => {
        this.enquetes = enquetes;
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
  }

  private saisieVierge(): ReponseEnqueteSuiviSaisie {
    return { noteApplicationCompetences: 3, noteImpactCarriere: 3, commentaire: '' };
  }

  ouvrirReponse(enquete: EnqueteSuivi): void {
    this.enqueteEnCours = enquete;
    this.saisie = this.saisieVierge();
    this.formulaireOuvert = true;
    this.reponseEnvoyee = false;
    this.messageErreur = '';
  }

  fermer(): void {
    this.formulaireOuvert = false;
    this.enqueteEnCours = null;
  }

  envoyer(): void {
    if (!this.enqueteEnCours || this.envoiEnCours) {
      return;
    }
    this.envoiEnCours = true;
    this.messageErreur = '';
    const donnees: ReponseEnqueteSuiviSaisie = {
      ...this.saisie,
      commentaire: this.saisie.commentaire?.trim() || null,
    };
    this.alumniService.repondreEnquete(this.enqueteEnCours.id, donnees).subscribe({
      next: () => {
        this.envoiEnCours = false;
        this.reponseEnvoyee = true;
        const idRepondu = this.enqueteEnCours!.id;
        this.enquetes = this.enquetes.map((e) => (e.id === idRepondu ? { ...e, dejaRepondu: true } : e));
      },
      error: (erreur) => {
        this.envoiEnCours = false;
        this.messageErreur = erreur.error?.message || "L'envoi de la réponse a échoué.";
      },
    });
  }
}
