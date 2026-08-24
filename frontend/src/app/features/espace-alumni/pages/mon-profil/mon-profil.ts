import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlumniProfil, AlumniProfilUpdate } from '../../models/alumni.models';
import { AlumniService } from '../../services/alumni.service';

/**
 * Fiche personnelle de l'alumni : identité (lecture seule, héritée de sa
 * scolarité) et situation professionnelle courante.
 */
@Component({
  selector: 'app-alumni-mon-profil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './mon-profil.html',
})
export class MonProfil implements OnInit {
  chargement = true;
  profil: AlumniProfil | null = null;
  saisie: AlumniProfilUpdate = { posteActuel: '', entreprise: '', secteurActivite: '', visible: true };
  enregistrement = false;
  messageSucces = '';
  messageErreur = '';

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
      },
      error: () => {
        this.enregistrement = false;
        this.messageErreur = "La mise à jour n'a pas pu être enregistrée.";
      },
    });
  }
}
