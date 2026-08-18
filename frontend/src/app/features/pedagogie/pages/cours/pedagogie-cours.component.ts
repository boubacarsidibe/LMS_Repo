import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PedagogieService, Promotion } from '../../services/pedagogie.service';
import { Cours, CoursCreation } from '../../../espace-etudiants/models/cours';
import { Formation } from '../../../espace-etudiants/models/formation';
import { Auth } from '../../../../core/auth/services/authService';
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';
import { NotificationService } from '../../../../shared/components/ui/notification.service';

/**
 * Pilotage de l'offre d'enseignement par le responsable pédagogique : création,
 * modification et suppression des cours, tous formateurs confondus.
 */
@Component({
  selector: 'app-pedagogie-cours',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './pedagogie-cours.component.html',
})
export class PedagogieCoursComponent implements OnInit {
  private pedagogie = inject(PedagogieService);
  private auth = inject(Auth);
  private router = inject(Router);
  private confirmation = inject(ConfirmationService);
  private notifications = inject(NotificationService);

  cours: Cours[] = [];
  formations: Formation[] = [];
  promotions: Promotion[] = [];
  chargement = true;
  erreur: string | null = null;

  /** Formulaire unique : en création quand coursEnEdition est null, sinon en modification. */
  formulaireOuvert = false;
  coursEnEdition: Cours | null = null;
  saisie: CoursCreation = { code: '', titre: '', semestre: '', description: '', volumeHoraire: 0 };
  enregistrement = false;

  ngOnInit(): void {
    this.charger();
    this.pedagogie.getFormations().subscribe({
      next: (data) => (this.formations = data || []),
      error: () => (this.formations = []),
    });
    this.pedagogie.getPromotions().subscribe({
      next: (data) => (this.promotions = data || []),
      error: () => (this.promotions = []),
    });
  }

  private charger(): void {
    this.chargement = true;
    this.pedagogie.getCours().subscribe({
      next: (data) => {
        this.cours = data || [];
        this.chargement = false;
      },
      error: () => {
        this.erreur = "Impossible de charger les cours.";
        this.chargement = false;
      },
    });
  }

  get totalSupports(): number {
    return this.cours.reduce((somme, c) => somme + (c.nbRessources || 0), 0);
  }

  /**
   * Cours sans équipe pédagogique.
   *
   * <p>Le cloisonnement enseignant se lit dans la table cours_formateur : un cours
   * qui n'y figure pas n'apparaît dans l'espace d'aucun formateur, donc rien ne peut
   * y être publié, planifié ni noté. Rien ne le signalait jusqu'ici.
   */
  get coursSansFormateur(): Cours[] {
    return this.cours.filter((c) => !c.formateurs?.length);
  }

  nomsFormateurs(cours: Cours): string {
    return (cours.formateurs || []).map((f) => f.nomComplet).join(', ');
  }

  get saisieValide(): boolean {
    return !!this.saisie.code.trim() && !!this.saisie.titre.trim() && (this.saisie.volumeHoraire ?? 0) >= 0;
  }

  ouvrirCreation(): void {
    this.coursEnEdition = null;
    this.saisie = { code: '', titre: '', semestre: '', description: '', volumeHoraire: 0 };
    this.formulaireOuvert = true;
  }

  ouvrirEdition(cours: Cours): void {
    this.coursEnEdition = cours;
    this.saisie = {
      code: cours.code,
      titre: cours.titre,
      semestre: cours.semestre || '',
      description: cours.description || '',
      volumeHoraire: cours.volumeHoraire || 0,
    };
    this.formulaireOuvert = true;
  }

  fermerFormulaire(): void {
    this.formulaireOuvert = false;
    this.coursEnEdition = null;
  }

  enregistrer(): void {
    if (!this.saisieValide || this.enregistrement) return;
    this.enregistrement = true;

    const requete = this.coursEnEdition
      ? this.pedagogie.modifierCours(this.coursEnEdition.id, this.saisie)
      : this.pedagogie.creerCours(this.saisie);

    requete.subscribe({
      next: () => {
        this.enregistrement = false;
        this.fermerFormulaire();
        this.charger();
      },
      error: (err) => {
        this.enregistrement = false;
        this.notifications.erreur(err?.error?.message || "L'enregistrement du cours a échoué.");
      },
    });
  }

  supprimer(cours: Cours): void {
    this.confirmation.demander({
      titre: 'Supprimer le cours',
      message: `« ${cours.titre} » et tous ses supports seront définitivement supprimés.`,
      libelleConfirmer: 'Supprimer le cours',
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.pedagogie.supprimerCours(cours.id).subscribe({
        next: () => {
          this.charger();
          this.notifications.succes(`Le cours « ${cours.titre} » a été supprimé.`);
        },
        error: () => this.notifications.erreur('La suppression du cours a échoué.'),
      });
    });
  }

  get userDisplayName(): string {
    const user = this.auth.currentUser();
    if (!user) return '';
    return (user as any).username ? (user as any).username.replace('_', ' ') : user.email;
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/auth/login']),
      error: () => {
        this.auth.clearSession();
        this.router.navigate(['/auth/login']);
      },
    });
  }
}
