import { validPassword } from '../../../../core/auth/validators/password-policy';
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EtudiantService, EtudiantProfile, EtudiantIdentite } from '../../services/etudiant.service';
import { Auth } from '../../../../core/auth/services/authService';

/**
 * Profil de l'étudiant connecté : consultation et modification de son identité,
 * plus changement de mot de passe. Distincte de la page ProfilEtudiant, qui est
 * la fiche d'un étudiant vue par un formateur.
 */
@Component({
  selector: 'app-mon-profil-etudiant',
  standalone: true,
  // La navigation est fournie par EtudiantLayout : la page ne porte plus de barre.
  imports: [CommonModule, FormsModule],
  templateUrl: './mon-profil-etudiant.html',
})
export class MonProfilEtudiant implements OnInit {
  private etudiantService = inject(EtudiantService);
  private auth = inject(Auth);

  profil: EtudiantProfile | null = null;
  chargement = true;
  erreur: string | null = null;

  /** Copie éditable : le profil chargé reste la référence tant que rien n'est enregistré. */
  identite: EtudiantIdentite = { nom: '', prenom: '', telephone: '' };
  enregistrement = false;
  messageIdentite: string | null = null;
  erreurIdentite: string | null = null;

  motDePasseActuel = '';
  nouveauMotDePasse = '';
  confirmationMotDePasse = '';
  changementMotDePasse = false;
  messageMotDePasse: string | null = null;
  erreurMotDePasse: string | null = null;

  ngOnInit(): void {
    this.etudiantService.getMyProfile().subscribe({
      next: (profil) => {
        this.profil = profil;
        this.identite = {
          nom: profil.etudiant?.nom || '',
          prenom: profil.etudiant?.prenom || '',
          telephone: profil.etudiant?.telephone || '',
        };
        this.chargement = false;
      },
      error: () => {
        this.erreur = "Impossible de charger votre profil pour le moment.";
        this.chargement = false;
      },
    });
  }

  /** Aucune fiche étudiant rattachée au compte : les champs d'identité n'ont rien à éditer. */
  get ficheAbsente(): boolean {
    return !!this.profil && !this.profil.etudiant;
  }

  get initiales(): string {
    const source = [this.identite.prenom, this.identite.nom].filter(Boolean).join(' ')
      || this.profil?.username?.replace('_', ' ')
      || this.profil?.email
      || '';
    const lettres = source.trim().split(/[\s_]+/).map((mot) => mot.charAt(0));
    return (lettres.join('') || '?').toUpperCase().slice(0, 2);
  }

  get identiteModifiee(): boolean {
    const initial = this.profil?.etudiant;
    if (!initial) return false;
    return this.identite.nom !== initial.nom
      || this.identite.prenom !== initial.prenom
      || this.identite.telephone !== initial.telephone;
  }

  get identiteComplete(): boolean {
    return !!this.identite.nom.trim() && !!this.identite.prenom.trim() && !!this.identite.telephone.trim();
  }

  enregistrerIdentite(): void {
    if (!this.identiteComplete || this.enregistrement) return;
    this.enregistrement = true;
    this.messageIdentite = null;
    this.erreurIdentite = null;

    this.etudiantService.updateMyProfile({
      nom: this.identite.nom.trim(),
      prenom: this.identite.prenom.trim(),
      telephone: this.identite.telephone.trim(),
    }).subscribe({
      next: (profil) => {
        this.profil = profil;
        this.identite = {
          nom: profil.etudiant?.nom || '',
          prenom: profil.etudiant?.prenom || '',
          telephone: profil.etudiant?.telephone || '',
        };
        this.enregistrement = false;
        this.messageIdentite = 'Vos informations ont été mises à jour.';
      },
      error: (err) => {
        this.enregistrement = false;
        this.erreurIdentite = this.messageErreur(err, "L'enregistrement a échoué.");
      },
    });
  }

  annulerIdentite(): void {
    const initial = this.profil?.etudiant;
    if (!initial) return;
    this.identite = { nom: initial.nom, prenom: initial.prenom, telephone: initial.telephone };
    this.messageIdentite = null;
    this.erreurIdentite = null;
  }

  changerMotDePasse(): void {
    if (this.changementMotDePasse) return;
    this.messageMotDePasse = null;
    this.erreurMotDePasse = null;

    if (!validPassword(this.nouveauMotDePasse)) {
      this.erreurMotDePasse = 'Le nouveau mot de passe doit contenir au moins 12 caractères et ne pas dépasser 72 octets UTF-8.';
      return;
    }
    if (this.nouveauMotDePasse !== this.confirmationMotDePasse) {
      this.erreurMotDePasse = 'La confirmation ne correspond pas au nouveau mot de passe.';
      return;
    }

    this.changementMotDePasse = true;
    this.auth.ChangePassword(this.motDePasseActuel, this.nouveauMotDePasse).subscribe({
      next: () => {
        this.changementMotDePasse = false;
        this.motDePasseActuel = '';
        this.nouveauMotDePasse = '';
        this.confirmationMotDePasse = '';
        this.messageMotDePasse = 'Mot de passe modifié.';
      },
      error: (err) => {
        this.changementMotDePasse = false;
        this.erreurMotDePasse = this.messageErreur(err, 'Le changement de mot de passe a échoué.');
      },
    });
  }

  /**
   * Accepte les erreurs JSON de l'API et les réponses textuelles d'un proxy.
   */
  private messageErreur(err: any, defaut: string): string {
    const corps = err?.error;
    if (typeof corps === 'string') {
      try {
        return JSON.parse(corps)?.message || defaut;
      } catch {
        return corps || defaut;
      }
    }
    return corps?.message || defaut;
  }
}
