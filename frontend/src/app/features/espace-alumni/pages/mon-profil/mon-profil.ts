import { validPassword } from '../../../../core/auth/validators/password-policy';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Auth } from '../../../../core/auth/services/authService';
import { AlumniMetriques, AlumniProfil, AlumniProfilUpdate } from '../../models/alumni.models';
import { AlumniService } from '../../services/alumni.service';

/**
 * Fiche personnelle de l'alumni : temps forts du parcours (métriques réelles,
 * pas des réglages), identité (lecture seule, héritée de sa scolarité),
 * situation professionnelle courante, et mot de passe — cette dernière
 * section manquait alors que la page sert de point d'entrée après connexion
 * (endpoint générique `/api/auth/change-password`, même patron que les
 * autres rôles).
 */
@Component({
  selector: 'app-alumni-mon-profil',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mon-profil.html',
})
export class MonProfil implements OnInit {
  chargement = true;
  profil: AlumniProfil | null = null;
  metriques: AlumniMetriques | null = null;
  saisie: AlumniProfilUpdate = { posteActuel: '', entreprise: '', secteurActivite: '', visible: true };
  enregistrement = false;
  messageSucces = '';
  messageErreur = '';

  private auth = inject(Auth);
  motDePasseActuel = '';
  nouveauMotDePasse = '';
  confirmationMotDePasse = '';
  enregistrementMotDePasse = false;
  messageSuccesMotDePasse = '';
  messageErreurMotDePasse = '';

  constructor(private alumniService: AlumniService) {}

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.chargement = true;
    this.alumniService.getMonProfil().subscribe({
      next: (profil) => {
        this.profil = profil;
        this.saisie = {
          posteActuel: profil.posteActuel ?? '',
          entreprise: profil.entreprise ?? '',
          secteurActivite: profil.secteurActivite ?? '',
          visible: profil.visible,
        };
        this.chargement = false;
      },
      error: () => (this.chargement = false),
    });
    this.alumniService.getMesMetriques().subscribe({
      next: (metriques) => (this.metriques = metriques),
      error: () => (this.metriques = null),
    });
  }

  enregistrer(): void {
    if (this.enregistrement) {
      return;
    }
    this.enregistrement = true;
    this.messageSucces = '';
    this.messageErreur = '';
    this.alumniService.mettreAJourProfil(this.saisie).subscribe({
      next: (profil) => {
        this.profil = profil;
        this.enregistrement = false;
        this.messageSucces = 'Profil mis à jour.';
        this.alumniService.getMesMetriques().subscribe({
          next: (metriques) => (this.metriques = metriques),
          error: () => {},
        });
      },
      error: () => {
        this.enregistrement = false;
        this.messageErreur = "La mise à jour n'a pas pu être enregistrée.";
      },
    });
  }

  changerMotDePasse(): void {
    if (this.enregistrementMotDePasse) {
      return;
    }
    this.messageSuccesMotDePasse = '';
    this.messageErreurMotDePasse = '';
    if (!this.motDePasseActuel || !this.nouveauMotDePasse || !this.confirmationMotDePasse) {
      this.messageErreurMotDePasse = 'Tous les champs sont obligatoires.';
      return;
    }
    if (!validPassword(this.nouveauMotDePasse)) {
      this.messageErreurMotDePasse = 'Le nouveau mot de passe doit compter au moins 8 caractères.';
      return;
    }
    if (this.nouveauMotDePasse !== this.confirmationMotDePasse) {
      this.messageErreurMotDePasse = 'La confirmation ne correspond pas au nouveau mot de passe.';
      return;
    }
    this.enregistrementMotDePasse = true;
    this.auth.ChangePassword(this.motDePasseActuel, this.nouveauMotDePasse).subscribe({
      next: () => {
        this.enregistrementMotDePasse = false;
        this.messageSuccesMotDePasse = 'Mot de passe modifié.';
        this.motDePasseActuel = '';
        this.nouveauMotDePasse = '';
        this.confirmationMotDePasse = '';
      },
      error: (err) => {
        this.enregistrementMotDePasse = false;
        this.messageErreurMotDePasse = err?.error?.message || 'Le mot de passe actuel est incorrect.';
      },
    });
  }
}
