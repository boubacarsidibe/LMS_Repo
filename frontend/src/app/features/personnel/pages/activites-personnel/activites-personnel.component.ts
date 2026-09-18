import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Auth } from '../../../../core/auth/services/authService';
import { Observable } from 'rxjs';
import {
  Activite,
  ActiviteSaisie,
  Departement,
  LIBELLES_TYPE_ACTIVITE,
  TypeActivite,
} from '../../models/personnel.models';
import { PersonnelService } from '../../services/personnel.service';
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';

const MOIS_LIBELLES = [
  'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
  'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre',
];

/**
 * Suivi des activités du personnel INPG (TDR §I) : saisie mensuelle par
 * département, consultable et exportable en rapport (par département ou
 * consolidé pour l'ensemble de l'INPG).
 *
 * <p>Un seul composant, partagé par les trois espaces internes (formateur,
 * pédagogie, admission) qui y accèdent chacun depuis leur propre navigation —
 * même modélisation, pas de triplication par rôle. La création de département
 * est réservée à la pédagogie côté serveur (contrôleur) ; côté client,
 * {@link peutGererDepartements} masque simplement le formulaire correspondant
 * aux deux autres rôles — un simple confort d'affichage, l'autorisation réelle
 * reste vérifiée par le serveur.
 */
@Component({
  selector: 'app-activites-personnel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './activites-personnel.component.html',
})
export class ActivitesPersonnelComponent implements OnInit {
  private personnelService = inject(PersonnelService);
  private auth = inject(Auth);
  private confirmation = inject(ConfirmationService);

  telechargementRapport = false;

  moisLibelles = MOIS_LIBELLES;
  libellesType = LIBELLES_TYPE_ACTIVITE;
  types: TypeActivite[] = ['FORMATION', 'PROJET', 'RECHERCHE', 'AUTRE'];

  departements: Departement[] = [];
  activites: Activite[] = [];
  chargement = true;

  filtreDepartementId: number | null = null;
  filtreMois = new Date().getMonth() + 1;
  filtreAnnee = new Date().getFullYear();

  formulaireOuvert = false;
  saisie: ActiviteSaisie = this.saisieVierge();
  enregistrement = false;

  gestionDepartementOuverte = false;
  nouveauCode = '';
  nouveauLibelle = '';

  messageErreur = '';
  messageSucces = '';

  ngOnInit(): void {
    this.personnelService.getDepartements().subscribe({
      next: (departements) => (this.departements = departements),
      error: () => (this.departements = []),
    });
    this.charger();
  }

  private saisieVierge(): ActiviteSaisie {
    return {
      departementId: this.filtreDepartementId,
      typeActivite: 'FORMATION',
      titre: '',
      description: '',
      mois: this.filtreMois,
      annee: this.filtreAnnee,
    };
  }

  charger(): void {
    this.chargement = true;
    this.personnelService
      .getActivites(this.filtreMois, this.filtreAnnee, this.filtreDepartementId ?? undefined)
      .subscribe({
        next: (activites) => {
          this.activites = activites;
          this.chargement = false;
        },
        error: () => {
          this.activites = [];
          this.chargement = false;
        },
      });
  }

  get anneesDisponibles(): number[] {
    const anneeActuelle = new Date().getFullYear();
    return [anneeActuelle, anneeActuelle - 1, anneeActuelle - 2];
  }

  ouvrirFormulaire(): void {
    this.saisie = this.saisieVierge();
    this.formulaireOuvert = true;
    this.reinitialiserMessages();
  }

  get saisieValide(): boolean {
    return !!this.saisie.departementId && !!this.saisie.titre.trim() && this.saisie.mois >= 1
      && this.saisie.mois <= 12 && !!this.saisie.annee;
  }

  enregistrer(): void {
    if (!this.saisieValide || this.enregistrement) {
      return;
    }
    this.enregistrement = true;
    this.reinitialiserMessages();
    this.personnelService.saisirActivite(this.saisie).subscribe({
      next: () => {
        this.enregistrement = false;
        this.formulaireOuvert = false;
        this.messageSucces = 'Activité enregistrée.';
        this.charger();
      },
      error: (erreur) => {
        this.enregistrement = false;
        this.messageErreur = erreur.error?.message || "L'enregistrement a échoué.";
      },
    });
  }

  supprimer(activite: Activite): void {
    this.confirmation.demander({
      titre: "Retirer l'activité",
      message: `Retirer l'activité « ${activite.titre} » ?`,
      libelleConfirmer: 'Retirer',
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.reinitialiserMessages();
      this.personnelService.supprimerActivite(activite.id).subscribe({
        next: () => this.charger(),
        error: (erreur) =>
          (this.messageErreur = erreur.error?.message || 'Seul l’auteur ou la direction peut retirer cette activité.'),
      });
    });
  }

  creerDepartement(): void {
    if (!this.nouveauCode.trim() || !this.nouveauLibelle.trim()) {
      return;
    }
    this.reinitialiserMessages();
    this.personnelService.creerDepartement(this.nouveauCode.trim(), this.nouveauLibelle.trim()).subscribe({
      next: (departement) => {
        this.departements = [...this.departements, departement].sort((a, b) => a.libelle.localeCompare(b.libelle));
        this.nouveauCode = '';
        this.nouveauLibelle = '';
        this.messageSucces = `Département « ${departement.libelle} » créé.`;
      },
      error: (erreur) => (this.messageErreur = erreur.error?.message || 'La création du département a échoué.'),
    });
  }

  ouvrirRapportDepartement(): void {
    if (!this.filtreDepartementId) {
      return;
    }
    const departement = this.departements.find((d) => d.id === this.filtreDepartementId);
    const code = departement ? departement.code : this.filtreDepartementId;
    this.telechargerRapport(
      this.personnelService.telechargerRapportDepartement(this.filtreDepartementId, this.filtreMois, this.filtreAnnee),
      `INPG-activites-${code}-${this.filtreAnnee}-${this.deuxChiffres(this.filtreMois)}.pdf`
    );
  }

  ouvrirRapportGlobal(): void {
    this.telechargerRapport(
      this.personnelService.telechargerRapportGlobal(this.filtreMois, this.filtreAnnee),
      `INPG-activites-global-${this.filtreAnnee}-${this.deuxChiffres(this.filtreMois)}.pdf`
    );
  }

  /** Récupère le PDF en Blob (le jeton passe par l'intercepteur) puis force un téléchargement nommé. */
  private telechargerRapport(flux: Observable<Blob>, nom: string): void {
    this.telechargementRapport = true;
    flux.subscribe({
      next: (blob) => {
        this.telechargementRapport = false;
        const url = URL.createObjectURL(blob);
        const lien = document.createElement('a');
        lien.href = url;
        lien.download = nom;
        lien.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.telechargementRapport = false;
        this.messageErreur = 'Le rapport n’a pas pu être généré.';
      },
    });
  }

  private deuxChiffres(n: number): string {
    return n < 10 ? `0${n}` : `${n}`;
  }

  get peutGererDepartements(): boolean {
    return (this.auth.currentUser()?.roles ?? []).includes('ROLE_RESPONSABLE_PEDAGOGIQUE');
  }

  private reinitialiserMessages(): void {
    this.messageErreur = '';
    this.messageSucces = '';
  }
}
