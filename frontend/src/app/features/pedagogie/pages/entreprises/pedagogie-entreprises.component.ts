import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { switchMap } from 'rxjs';
import {
  DemandeEntrepriseDto,
  FicheModuleJour,
  FicheTechniqueModule,
  FicheTechniqueModuleSaisie,
  LIBELLES_STATUT_DEMANDE_ENTREPRISE,
  LIBELLES_STATUT_FICHE_MODULE,
  LIBELLES_TYPE_DEMANDE_ENTREPRISE,
  PedagogieEntrepriseService,
  StatutDemandeEntreprise,
  StatutFicheModule,
} from '../../services/pedagogie-entreprise.service';

/**
 * Écran « Entreprises » du responsable pédagogique (TDR §H, QMS-006/008/010) :
 * demandes reçues (besoin de formation / devis) et, pour une session sur
 * mesure, rédaction puis transmission de la Fiche Technique de Module.
 *
 * <p>Avant cet écran, {@code PedagogieEntrepriseController} existait côté
 * serveur sans qu'aucun front ne le consomme.
 */
@Component({
  selector: 'app-pedagogie-entreprises',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedagogie-entreprises.component.html',
})
export class PedagogieEntreprisesComponent implements OnInit {
  private pedagogieEntreprise = inject(PedagogieEntrepriseService);

  chargement = true;
  demandes: DemandeEntrepriseDto[] = [];

  libellesType = LIBELLES_TYPE_DEMANDE_ENTREPRISE;
  libellesStatutDemande = LIBELLES_STATUT_DEMANDE_ENTREPRISE;
  libellesStatutFiche = LIBELLES_STATUT_FICHE_MODULE;

  readonly classesStatutDemande: Record<StatutDemandeEntreprise, string> = {
    SOUMISE: 'bg-surface-container text-on-surface-variant',
    EN_COURS: 'bg-secondary-container text-on-secondary-container',
    REPONDUE: 'bg-success/15 text-success',
    CLOTUREE: 'bg-outline-variant/40 text-on-surface-variant',
  };

  readonly classesStatutFiche: Record<StatutFicheModule, string> = {
    BROUILLON: 'bg-surface-container text-on-surface-variant',
    TRANSMISE: 'bg-secondary-container text-on-secondary-container',
    ACCEPTEE: 'bg-success/15 text-success',
    AJUSTEMENTS_DEMANDES: 'bg-error-container/40 text-on-error-container',
  };

  // Panneau détail
  detailOuvert = false;
  demandeSelectionnee: DemandeEntrepriseDto | null = null;

  reponseTexte = '';
  statutSelectionne: StatutDemandeEntreprise = 'EN_COURS';
  enregistrementReponse = false;

  ficheChargement = false;
  ficheExistante: FicheTechniqueModule | null = null;
  fiche: FicheTechniqueModuleSaisie = this.ficheVierge();
  enregistrementFiche = false;

  messageErreur = '';
  messageSucces = '';

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.chargement = true;
    this.pedagogieEntreprise.getDemandes().subscribe({
      next: (demandes) => {
        this.demandes = demandes || [];
        this.chargement = false;
      },
      error: () => {
        this.chargement = false;
        this.messageErreur = 'Les demandes n’ont pas pu être chargées.';
      },
    });
  }

  private ficheVierge(): FicheTechniqueModuleSaisie {
    return {
      code: null,
      intitule: null,
      version: null,
      dateRevision: null,
      responsableModule: null,
      departement: null,
      atelierService: null,
      fonctionsCibles: null,
      thematiques: null,
      prerequis: null,
      langue: 'Français',
      niveau: null,
      effectif: null,
      dureeHeures: null,
      dureeJours: null,
      periodeProbable: null,
      objectifGeneral: null,
      objectifsSpecifiques: null,
      modalitesTestPrerequis: null,
      formationSurSite: null,
      typesEvaluation: null,
      ressourcesPedagogiques: null,
      bibliographie: null,
      profilFormateur: null,
      coutTotal: null,
      modalitesFinancieres: null,
      jours: [],
    };
  }

  ouvrirDetail(demande: DemandeEntrepriseDto): void {
    this.detailOuvert = true;
    this.demandeSelectionnee = demande;
    this.reponseTexte = demande.reponseEquipe || '';
    this.statutSelectionne = demande.statut;
    this.reinitialiserMessages();
    this.chargerFiche(demande.id);
  }

  fermerDetail(): void {
    this.detailOuvert = false;
    this.demandeSelectionnee = null;
    this.ficheExistante = null;
    this.fiche = this.ficheVierge();
  }

  private chargerFiche(demandeId: number): void {
    this.ficheChargement = true;
    this.ficheExistante = null;
    this.fiche = this.ficheVierge();
    this.pedagogieEntreprise.getFiche(demandeId).subscribe({
      next: (fiche) => {
        this.ficheChargement = false;
        this.ficheExistante = fiche;
        this.fiche = this.saisieDepuis(fiche);
      },
      error: (erreur: HttpErrorResponse) => {
        this.ficheChargement = false;
        if (erreur.status !== 404) {
          this.messageErreur = 'La fiche technique n’a pas pu être chargée.';
        }
      },
    });
  }

  private saisieDepuis(f: FicheTechniqueModule): FicheTechniqueModuleSaisie {
    return {
      code: f.code,
      intitule: f.intitule,
      version: f.version,
      dateRevision: f.dateRevision,
      responsableModule: f.responsableModule,
      departement: f.departement,
      atelierService: f.atelierService,
      fonctionsCibles: f.fonctionsCibles,
      thematiques: f.thematiques,
      prerequis: f.prerequis,
      langue: f.langue,
      niveau: f.niveau,
      effectif: f.effectif,
      dureeHeures: f.dureeHeures,
      dureeJours: f.dureeJours,
      periodeProbable: f.periodeProbable,
      objectifGeneral: f.objectifGeneral,
      objectifsSpecifiques: f.objectifsSpecifiques,
      modalitesTestPrerequis: f.modalitesTestPrerequis,
      formationSurSite: f.formationSurSite,
      typesEvaluation: f.typesEvaluation,
      ressourcesPedagogiques: f.ressourcesPedagogiques,
      bibliographie: f.bibliographie,
      profilFormateur: f.profilFormateur,
      coutTotal: f.coutTotal,
      modalitesFinancieres: f.modalitesFinancieres,
      jours: f.jours.map((j) => ({ ...j })),
    };
  }

  enregistrerReponse(): void {
    const demande = this.demandeSelectionnee;
    if (!demande || this.enregistrementReponse) {
      return;
    }
    this.enregistrementReponse = true;
    this.reinitialiserMessages();
    this.pedagogieEntreprise.repondreDemande(demande.id, this.statutSelectionne, this.reponseTexte.trim() || null).subscribe({
      next: (miseAJour) => {
        this.enregistrementReponse = false;
        this.demandeSelectionnee = miseAJour;
        this.demandes = this.demandes.map((d) => (d.id === miseAJour.id ? miseAJour : d));
        this.messageSucces = 'Réponse enregistrée.';
      },
      error: (erreur) => {
        this.enregistrementReponse = false;
        this.messageErreur = erreur.error?.message || 'L’enregistrement de la réponse a échoué.';
      },
    });
  }

  ajouterJour(): void {
    this.fiche.jours.push({
      id: null,
      numero: this.fiche.jours.length + 1,
      contenus: '',
      dureeHeures: null,
      typeEnseignement: '',
      modalites: '',
    });
  }

  retirerJour(index: number): void {
    this.fiche.jours.splice(index, 1);
    this.fiche.jours.forEach((j: FicheModuleJour, i: number) => (j.numero = i + 1));
  }

  get ficheTransmissible(): boolean {
    return !!this.fiche.intitule?.trim() && !!this.fiche.objectifGeneral?.trim() && this.fiche.jours.length > 0;
  }

  get ficheModifiable(): boolean {
    return this.ficheExistante?.statut !== 'ACCEPTEE';
  }

  enregistrerBrouillon(): void {
    const demande = this.demandeSelectionnee;
    if (!demande || this.enregistrementFiche) {
      return;
    }
    this.enregistrementFiche = true;
    this.reinitialiserMessages();
    this.pedagogieEntreprise.enregistrerFiche(demande.id, this.fiche).subscribe({
      next: (fiche) => {
        this.enregistrementFiche = false;
        this.ficheExistante = fiche;
        this.messageSucces = 'Brouillon enregistré.';
        this.majFicheStatutDansListe(demande.id, fiche.statut);
      },
      error: (erreur) => {
        this.enregistrementFiche = false;
        this.messageErreur = erreur.error?.message || 'L’enregistrement de la fiche a échoué.';
      },
    });
  }

  transmettre(): void {
    const demande = this.demandeSelectionnee;
    if (!demande || !this.ficheTransmissible || this.enregistrementFiche) {
      return;
    }
    this.enregistrementFiche = true;
    this.reinitialiserMessages();
    this.pedagogieEntreprise
      .enregistrerFiche(demande.id, this.fiche)
      .pipe(switchMap(() => this.pedagogieEntreprise.transmettreFiche(demande.id)))
      .subscribe({
        next: (fiche) => {
          this.enregistrementFiche = false;
          this.ficheExistante = fiche;
          this.fiche = this.saisieDepuis(fiche);
          this.messageSucces = 'Fiche transmise à l’entreprise.';
          this.majFicheStatutDansListe(demande.id, fiche.statut);
        },
        error: (erreur) => {
          this.enregistrementFiche = false;
          this.messageErreur = erreur.error?.message || 'La transmission de la fiche a échoué.';
        },
      });
  }

  private majFicheStatutDansListe(demandeId: number, ficheStatut: StatutFicheModule): void {
    this.demandes = this.demandes.map((d) => (d.id === demandeId ? { ...d, ficheStatut } : d));
  }

  private reinitialiserMessages(): void {
    this.messageErreur = '';
    this.messageSucces = '';
  }
}
