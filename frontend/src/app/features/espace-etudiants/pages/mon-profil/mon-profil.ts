import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormateurService, FormateurProfile } from '../../services/formateur.service';
import { NotificationService } from '../../../../shared/components/ui/notification.service';

@Component({
  selector: 'app-mon-profil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './mon-profil.html',
  styleUrl: './mon-profil.css',
})
export class MonProfil implements OnInit {
  private formateurService = inject(FormateurService);
  private notifications = inject(NotificationService);
  private fb = inject(FormBuilder);

  profil: FormateurProfile | null = null;
  chargement = true;
  erreur: string | null = null;

  /** La page s'ouvre en lecture ; le formulaire n'apparaît qu'à la demande. */
  edition = false;
  enregistrement = false;
  erreurFormulaire = '';

  readonly form: FormGroup = this.fb.group({
    prenom: ['', [Validators.required, Validators.maxLength(80)]],
    nom: ['', [Validators.required, Validators.maxLength(80)]],
    // Le backend impose l'unicité du numéro ; on vérifie seulement la forme ici.
    telephone: ['', [Validators.required, Validators.pattern(/^[0-9+\s().-]{6,20}$/)]],
  });

  ngOnInit() {
    this.charger();
  }

  private charger() {
    this.chargement = true;
    this.formateurService.getProfile().subscribe({
      next: (data) => {
        this.appliquer(data);
        this.chargement = false;
      },
      error: () => {
        this.erreur = 'Impossible de charger votre profil pour le moment.';
        this.chargement = false;
      },
    });
  }

  private appliquer(data: FormateurProfile) {
    this.profil = data;
    this.form.reset({
      prenom: data.prenom ?? '',
      nom: data.nomFamille ?? '',
      telephone: data.telephone ?? '',
    });
  }

  ouvrirEdition() {
    this.erreurFormulaire = '';
    if (this.profil) this.appliquer(this.profil);
    this.edition = true;
  }

  annulerEdition() {
    // Repartir des valeurs serveur : une saisie abandonnée ne doit rien laisser.
    if (this.profil) this.appliquer(this.profil);
    this.erreurFormulaire = '';
    this.edition = false;
  }

  enregistrer() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enregistrement = true;
    this.erreurFormulaire = '';
    this.formateurService.updateProfile(this.form.getRawValue()).subscribe({
      next: (profilAJour) => {
        this.appliquer(profilAJour);
        this.enregistrement = false;
        this.edition = false;
        this.notifications.succes('Votre profil a été mis à jour.');
      },
      error: (err) => {
        this.enregistrement = false;
        // 409 = numéro déjà pris : le message du serveur est le plus précis.
        this.erreurFormulaire = err?.error?.message || "L'enregistrement du profil a échoué.";
      },
    });
  }

  /** Message d'aide affiché sous un champ, une fois celui-ci touché. */
  messageErreur(champ: 'prenom' | 'nom' | 'telephone'): string | null {
    const c = this.form.get(champ);
    if (!c || c.valid || !c.touched) return null;
    if (c.hasError('required')) return 'Ce champ est obligatoire.';
    if (c.hasError('maxlength')) return '80 caractères maximum.';
    if (c.hasError('pattern')) return 'Numéro invalide (chiffres, espaces, + ( ) - ).';
    return null;
  }

  initiales(nom: string): string {
    return (nom || '')
      .split(' ')
      .filter(Boolean)
      .map((mot) => mot.charAt(0))
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }
}
