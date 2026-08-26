import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FormateurBref, FormateurDetail, FormateurSaisie, PedagogieService } from '../../services/pedagogie.service';

/**
 * Ouverture des comptes formateurs par la pédagogie.
 *
 * <p>Seul {@code POST /api/auth/formateurs} existait avant cet écran, réservé au
 * responsable admission — la pédagogie, qui affecte ensuite les formateurs aux
 * cours, ne pouvait ouvrir leur compte elle-même. Un compte créé ici n'apparaît
 * dans aucun espace formateur tant qu'il n'est pas rattaché à un cours (page
 * « Cours » → équipe pédagogique) : le message de succès le rappelle.
 */
@Component({
  selector: 'app-pedagogie-formateurs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedagogie-formateurs.component.html',
})
export class PedagogieFormateursComponent implements OnInit {
  private pedagogie = inject(PedagogieService);

  chargement = true;
  formateurs: FormateurBref[] = [];

  formulaireOuvert = false;
  saisie: FormateurSaisie = this.saisieVierge();
  enregistrement = false;

  // Détail / suppression
  detailOuvert = false;
  chargementDetail = false;
  formateurDetail: FormateurDetail | null = null;
  suppressionEnCours = false;

  messageErreur = '';
  messageSucces = '';

  ngOnInit(): void {
    this.charger();
  }

  ouvrirDetail(formateur: FormateurBref): void {
    this.detailOuvert = true;
    this.chargementDetail = true;
    this.formateurDetail = null;
    this.pedagogie.getFormateurDetail(formateur.id).subscribe({
      next: (detail) => {
        this.chargementDetail = false;
        this.formateurDetail = detail;
      },
      error: () => {
        this.chargementDetail = false;
        this.detailOuvert = false;
        this.messageErreur = 'La fiche de ce formateur n’a pas pu être chargée.';
      },
    });
  }

  fermerDetail(): void {
    this.detailOuvert = false;
    this.formateurDetail = null;
  }

  supprimerDepuisDetail(): void {
    const formateur = this.formateurDetail;
    if (!formateur || this.suppressionEnCours) {
      return;
    }
    if (!confirm(`Supprimer définitivement le compte de ${formateur.prenom} ${formateur.nom} ?`)) {
      return;
    }
    this.suppressionEnCours = true;
    this.reinitialiserMessages();
    this.pedagogie.supprimerFormateur(formateur.id).subscribe({
      next: () => {
        this.suppressionEnCours = false;
        this.detailOuvert = false;
        this.formateurDetail = null;
        this.messageSucces = `Le compte de ${formateur.prenom} ${formateur.nom} a été supprimé.`;
        this.charger();
      },
      error: (erreur) => {
        this.suppressionEnCours = false;
        this.messageErreur = erreur.error?.message || 'La suppression a échoué.';
      },
    });
  }

  private saisieVierge(): FormateurSaisie {
    return { username: '', password: '', email: '', nom: '', prenom: '', telephone: '' };
  }

  private charger(): void {
    this.chargement = true;
    this.pedagogie.getFormateurs().subscribe({
      next: (formateurs) => {
        this.formateurs = formateurs || [];
        this.chargement = false;
      },
      error: () => {
        this.chargement = false;
        this.messageErreur = 'Le répertoire n’a pas pu être chargé.';
      },
    });
  }

  ouvrirCreation(): void {
    this.saisie = this.saisieVierge();
    this.formulaireOuvert = true;
    this.reinitialiserMessages();
  }

  /** L'identifiant se déduit de l'adresse : une saisie de moins, une faute de moins. */
  surEmailSaisi(): void {
    if (this.saisie.email.includes('@') && !this.saisie.username.trim()) {
      this.saisie.username = this.saisie.email.split('@')[0];
    }
  }

  get saisieValide(): boolean {
    return (
      !!this.saisie.nom.trim() &&
      !!this.saisie.prenom.trim() &&
      !!this.saisie.email.trim() &&
      !!this.saisie.telephone?.trim()
    );
  }

  creer(): void {
    if (!this.saisieValide || this.enregistrement) {
      return;
    }
    this.enregistrement = true;
    this.reinitialiserMessages();
    this.pedagogie.creerFormateur(this.saisie).subscribe({
      next: (resultat) => {
        this.enregistrement = false;
        this.formulaireOuvert = false;
        this.messageSucces = resultat.message;
        this.charger();
      },
      error: (erreur) => {
        this.enregistrement = false;
        this.messageErreur = erreur.error?.message || 'La création du compte a échoué.';
      },
    });
  }

  private reinitialiserMessages(): void {
    this.messageErreur = '';
    this.messageSucces = '';
  }
}
