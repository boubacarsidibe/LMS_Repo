import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  EtudiantDetail,
  EtudiantSaisie,
  EtudiantScolarite,
  PedagogieService,
  PromotionDetail,
} from '../../services/pedagogie.service';
import { ConfirmationService } from '../../../../shared/components/ui/confirmation.service';

/**
 * Répertoire des étudiants : recherche, ouverture de compte, affectation à une
 * promotion, import en masse et export.
 *
 * <p>Le rattachement à une promotion est signalé quand il manque : un étudiant sans
 * promotion n'a ni cours, ni calendrier, ni relevé — et rien ne le lui indique.
 */
@Component({
  selector: 'app-pedagogie-etudiants',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedagogie-etudiants.component.html',
})
export class PedagogieEtudiantsComponent implements OnInit {
  private pedagogie = inject(PedagogieService);
  private confirmation = inject(ConfirmationService);

  @ViewChild('champCsv') champCsv?: ElementRef<HTMLInputElement>;

  chargement = true;
  etudiants: EtudiantScolarite[] = [];
  promotions: PromotionDetail[] = [];
  recherche = '';

  // Filtres (appliqués côté client sur la liste déjà chargée/recherchée)
  filtrePromotion = '';
  filtreStatut = '';
  filtreSansPromotion = false;
  filtreAlumni = false;

  // Création
  formulaireOuvert = false;
  saisie: EtudiantSaisie = this.saisieVierge();
  enregistrement = false;

  // Import
  importOuvert = false;
  csv = '';
  promotionImport: number | null = null;
  importEnCours = false;
  rejets: string[] = [];

  // Détail / suppression
  detailOuvert = false;
  chargementDetail = false;
  etudiantDetail: EtudiantDetail | null = null;
  suppressionEnCours = false;

  messageErreur = '';
  messageSucces = '';

  ngOnInit(): void {
    this.charger();
  }

  private saisieVierge(): EtudiantSaisie {
    return { username: '', password: '', email: '', nom: '', prenom: '', telephone: '' };
  }

  private charger(): void {
    this.chargement = true;
    forkJoin({
      etudiants: this.pedagogie.getEtudiants(this.recherche),
      promotions: this.pedagogie.getPromotions(),
    }).subscribe({
      next: ({ etudiants, promotions }) => {
        this.etudiants = etudiants || [];
        this.promotions = (promotions as unknown as PromotionDetail[]) || [];
        this.chargement = false;
      },
      error: () => {
        this.chargement = false;
        this.messageErreur = 'Le répertoire n’a pas pu être chargé.';
      },
    });
  }

  rechercher(): void {
    this.charger();
  }

  /** Étudiants sans promotion : ils n'ont ni cours, ni calendrier, ni relevé. */
  get sansPromotion(): EtudiantScolarite[] {
    return this.etudiants.filter((e) => !e.promotion || e.promotion === 'Sans promotion');
  }

  /**
   * Candidats définitivement admis dont l'inscription reste à faire.
   *
   * <p>L'admission ouvre l'accès, elle n'inscrit pas : le compte est créé sans
   * promotion ni matricule, et c'est l'affectation faite ici qui achève l'inscription.
   * Sans cette distinction, un admis de la veille et un compte oublié depuis des mois
   * se présentaient à l'identique — or l'un appelle une inscription, l'autre une
   * correction.
   */
  get admisEnAttente(): EtudiantScolarite[] {
    return this.sansPromotion.filter((e) => !!e.origine);
  }

  /** Statuts réellement présents dans la liste chargée (pas de liste figée). */
  get statutsDisponibles(): string[] {
    return [...new Set(this.etudiants.map((e) => e.statut).filter((s): s is string => !!s))].sort();
  }

  /** Répertoire affiché une fois les filtres appliqués sur la liste chargée. */
  get etudiantsFiltres(): EtudiantScolarite[] {
    return this.etudiants.filter((e) => {
      const correspondPromotion = !this.filtrePromotion || e.promotion === this.filtrePromotion;
      const correspondStatut = !this.filtreStatut || e.statut === this.filtreStatut;
      const correspondSansPromotion = !this.filtreSansPromotion || !e.promotion || e.promotion === 'Sans promotion';
      const correspondAlumni = !this.filtreAlumni || e.estAlumni;
      return correspondPromotion && correspondStatut && correspondSansPromotion && correspondAlumni;
    });
  }

  reinitialiserFiltres(): void {
    this.filtrePromotion = '';
    this.filtreStatut = '';
    this.filtreSansPromotion = false;
    this.filtreAlumni = false;
  }

  // ===================== Création =====================

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
      !!this.saisie.password.trim() &&
      !!this.saisie.telephone?.trim()
    );
  }

  creer(): void {
    if (!this.saisieValide || this.enregistrement) {
      return;
    }
    this.enregistrement = true;
    this.reinitialiserMessages();
    this.pedagogie.creerEtudiant(this.saisie).subscribe({
      next: (etudiant) => {
        this.enregistrement = false;
        this.formulaireOuvert = false;
        this.messageSucces = `Compte créé pour ${etudiant.nom} — matricule ${etudiant.matricule}.`;
        this.charger();
      },
      error: (erreur) => {
        this.enregistrement = false;
        this.messageErreur = erreur.error?.message || 'La création du compte a échoué.';
      },
    });
  }

  // ===================== Affectation =====================

  affecter(etudiant: EtudiantScolarite, valeur: string): void {
    const promotionId = valeur ? Number(valeur) : null;
    this.reinitialiserMessages();
    this.pedagogie.affecterPromotion(etudiant.id, promotionId).subscribe({
      next: (misAJour) => {
        this.messageSucces = `${misAJour.nom} → ${misAJour.promotion}.`;
        this.charger();
      },
      error: (erreur) => (this.messageErreur = erreur.error?.message || 'L’affectation a échoué.'),
    });
  }

  // ===================== Alumni =====================

  promouvoirAlumni(etudiant: EtudiantScolarite): void {
    if (etudiant.estAlumni) {
      return;
    }
    this.reinitialiserMessages();
    this.pedagogie.promouvoirAlumni(etudiant.id).subscribe({
      next: (misAJour) => {
        this.messageSucces = `${misAJour.nom} a rejoint l’espace Alumni.`;
        this.charger();
      },
      error: (erreur) => (this.messageErreur = erreur.error?.message || 'Le passage en alumni a échoué.'),
    });
  }

  // ===================== Détail / suppression =====================

  ouvrirDetail(etudiant: EtudiantScolarite): void {
    this.detailOuvert = true;
    this.chargementDetail = true;
    this.etudiantDetail = null;
    this.pedagogie.getEtudiantDetail(etudiant.id).subscribe({
      next: (detail) => {
        this.chargementDetail = false;
        this.etudiantDetail = detail;
      },
      error: () => {
        this.chargementDetail = false;
        this.detailOuvert = false;
        this.messageErreur = 'La fiche de cet étudiant n’a pas pu être chargée.';
      },
    });
  }

  fermerDetail(): void {
    this.detailOuvert = false;
    this.etudiantDetail = null;
  }

  supprimerDepuisDetail(): void {
    const etudiant = this.etudiantDetail;
    if (!etudiant || this.suppressionEnCours) {
      return;
    }
    this.confirmation.demander({
      titre: 'Supprimer le compte étudiant',
      message: `Supprimer définitivement le compte de ${etudiant.prenom} ${etudiant.nom} ?`,
      libelleConfirmer: 'Supprimer le compte',
      destructif: true,
    }).subscribe((confirme) => {
      if (!confirme) return;
      this.suppressionEnCours = true;
      this.reinitialiserMessages();
      this.pedagogie.supprimerEtudiant(etudiant.id).subscribe({
        next: () => {
          this.suppressionEnCours = false;
          this.detailOuvert = false;
          this.etudiantDetail = null;
          this.messageSucces = `Le compte de ${etudiant.prenom} ${etudiant.nom} a été supprimé.`;
          this.charger();
        },
        error: (erreur) => {
          this.suppressionEnCours = false;
          this.messageErreur = erreur.error?.message || 'La suppression a échoué.';
        },
      });
    });
  }

  // ===================== Import =====================

  ouvrirImport(): void {
    this.importOuvert = true;
    this.csv = '';
    this.promotionImport = null;
    this.rejets = [];
    this.reinitialiserMessages();
  }

  choisirFichier(): void {
    this.champCsv?.nativeElement.click();
  }

  fichierChoisi(evenement: Event): void {
    const champ = evenement.target as HTMLInputElement;
    const fichier = champ.files?.[0];
    if (!fichier) {
      return;
    }
    const lecteur = new FileReader();
    lecteur.onload = () => (this.csv = String(lecteur.result || ''));
    lecteur.readAsText(fichier);
  }

  importer(): void {
    if (!this.csv.trim() || this.importEnCours) {
      return;
    }
    this.importEnCours = true;
    this.rejets = [];
    this.reinitialiserMessages();
    this.pedagogie.importerEtudiants(this.csv, this.promotionImport).subscribe({
      next: (resultat) => {
        this.importEnCours = false;
        this.rejets = resultat.rejets || [];
        this.messageSucces = `${resultat.crees} compte(s) créé(s).`;
        if (this.rejets.length === 0) {
          this.importOuvert = false;
        }
        this.charger();
      },
      error: (erreur) => {
        this.importEnCours = false;
        this.messageErreur = erreur.error?.message || 'L’import a échoué.';
      },
    });
  }

  // ===================== Export =====================

  /** Export du répertoire tel qu'affiché, recherche et filtres compris. */
  exporter(): void {
    const entete = 'Matricule;Nom;Promotion;Statut';
    const lignes = this.etudiantsFiltres.map((e) =>
      [e.matricule || '', e.nom, e.promotion, e.statut].map((c) => this.cellule(c)).join(';')
    );
    const contenu = '﻿' + [entete, ...lignes].join('\r\n');
    const lien = document.createElement('a');
    lien.href = URL.createObjectURL(new Blob([contenu], { type: 'text/csv;charset=utf-8;' }));
    lien.download = `etudiants-${new Date().toISOString().slice(0, 10)}.csv`;
    lien.click();
    URL.revokeObjectURL(lien.href);
  }

  /** Échappe les guillemets en les doublant, comme l'attend le format CSV. */
  private cellule(valeur: string): string {
    return `"${(valeur || '').replace(/"/g, '""')}"`;
  }

  private reinitialiserMessages(): void {
    this.messageErreur = '';
    this.messageSucces = '';
  }
}
