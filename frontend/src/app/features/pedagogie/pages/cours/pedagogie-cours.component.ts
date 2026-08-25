import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
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

  // Le formulaire s'ouvre en haut de page : sur une longue liste, un clic sur
  // « Modifier » tout en bas ne produisait aucun changement visible à l'écran
  // sans ce défilement, et passait pour un bouton mort.
  @ViewChild('formulaire') private formulaireRef?: ElementRef<HTMLElement>;

  cours: Cours[] = [];
  formations: Formation[] = [];
  promotions: Promotion[] = [];
  chargement = true;
  erreur: string | null = null;

  /** Formulaire unique : en création quand coursEnEdition est null, sinon en modification. */
  formulaireOuvert = false;
  coursEnEdition: Cours | null = null;
  saisie: CoursCreation = this.saisieVierge();
  enregistrement = false;

  /** Formations cochées dans le formulaire, avant enregistrement. */
  formationsChoisies = new Set<number>();

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

  /**
   * Cours inscrits à aucun programme.
   *
   * <p>Un cours tient sa place dans le cursus de sa formation, et les promotions en
   * héritent. Sans formation, aucune promotion ne le reçoit : le cours existe en base
   * et n'est visible d'aucun étudiant. Le formulaire ne demandait pas la formation, si
   * bien que tout cours créé ici naissait dans cet état.
   */
  get coursSansFormation(): Cours[] {
    return this.cours.filter((c) => !c.formations?.length);
  }

  nomsFormateurs(cours: Cours): string {
    return (cours.formateurs || []).map((f) => f.nomComplet).join(', ');
  }

  nomsFormations(cours: Cours): string {
    return (cours.formations || []).map((f) => f.intitule).join(', ');
  }

  get saisieValide(): boolean {
    return !!this.saisie.code.trim() && !!this.saisie.titre.trim() && (this.saisie.volumeHoraire ?? 0) >= 0;
  }

  private saisieVierge(): CoursCreation {
    // Coefficient 1 par defaut : un cours pese autant que les autres tant que la
    // maquette n'en decide pas autrement.
    return { code: '', titre: '', semestre: '', description: '', volumeHoraire: 0, coefficient: 1 };
  }

  ouvrirCreation(): void {
    this.coursEnEdition = null;
    this.saisie = this.saisieVierge();
    this.formationsChoisies = new Set();
    this.formulaireOuvert = true;
    this.defilerVersFormulaire();
  }

  ouvrirEdition(cours: Cours): void {
    this.coursEnEdition = cours;
    this.saisie = {
      code: cours.code,
      titre: cours.titre,
      semestre: cours.semestre || '',
      description: cours.description || '',
      volumeHoraire: cours.volumeHoraire || 0,
      coefficient: cours.coefficient ?? 1,
    };
    this.formationsChoisies = new Set((cours.formations || []).map((f) => f.id));
    this.formulaireOuvert = true;
    this.defilerVersFormulaire();
  }

  /** setTimeout : le formulaire n'existe dans le DOM qu'après le prochain cycle de détection. */
  private defilerVersFormulaire(): void {
    setTimeout(() => this.formulaireRef?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  fermerFormulaire(): void {
    this.formulaireOuvert = false;
    this.coursEnEdition = null;
  }

  /** Formation.id est typé « number | string » dans le modèle partagé : on normalise ici. */
  private idFormation(id: number | string | undefined): number | null {
    const valeur = Number(id);
    return id === undefined || id === null || Number.isNaN(valeur) ? null : valeur;
  }

  formationChoisie(id: number | string | undefined): boolean {
    const valeur = this.idFormation(id);
    return valeur !== null && this.formationsChoisies.has(valeur);
  }

  basculerFormation(id: number | string | undefined): void {
    const valeur = this.idFormation(id);
    if (valeur === null) {
      return;
    }
    if (this.formationsChoisies.has(valeur)) {
      this.formationsChoisies.delete(valeur);
    } else {
      this.formationsChoisies.add(valeur);
    }
  }

  enregistrer(): void {
    if (!this.saisieValide || this.enregistrement) return;
    this.enregistrement = true;

    // Toujours transmis, y compris vide : le serveur distingue « tableau vide »
    // (retirer de tout programme) de « champ absent » (ne pas y toucher).
    const saisie: CoursCreation = { ...this.saisie, formationIds: [...this.formationsChoisies] };

    const requete = this.coursEnEdition
      ? this.pedagogie.modifierCours(this.coursEnEdition.id, saisie)
      : this.pedagogie.creerCours(saisie);

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
