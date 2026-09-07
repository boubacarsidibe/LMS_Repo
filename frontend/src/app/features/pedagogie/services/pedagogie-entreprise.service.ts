import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type TypeDemandeEntreprise = 'BESOIN_FORMATION' | 'DEVIS_SESSION';
export type StatutDemandeEntreprise = 'SOUMISE' | 'EN_COURS' | 'REPONDUE' | 'CLOTUREE';
export type StatutFicheModule = 'BROUILLON' | 'TRANSMISE' | 'ACCEPTEE' | 'AJUSTEMENTS_DEMANDES';

export const LIBELLES_TYPE_DEMANDE_ENTREPRISE: Record<TypeDemandeEntreprise, string> = {
  BESOIN_FORMATION: 'Besoin de formation',
  DEVIS_SESSION: 'Devis / session sur mesure',
};

export const LIBELLES_STATUT_DEMANDE_ENTREPRISE: Record<StatutDemandeEntreprise, string> = {
  SOUMISE: 'Soumise',
  EN_COURS: 'En cours',
  REPONDUE: 'Répondue',
  CLOTUREE: 'Clôturée',
};

export const LIBELLES_STATUT_FICHE_MODULE: Record<StatutFicheModule, string> = {
  BROUILLON: 'Brouillon',
  TRANSMISE: 'Transmise',
  ACCEPTEE: 'Acceptée',
  AJUSTEMENTS_DEMANDES: 'Ajustements demandés',
};

/** Demande entreprise, vue pédagogie — toutes, tous statuts confondus. */
export interface DemandeEntrepriseDto {
  id: number;
  type: TypeDemandeEntreprise;
  statut: StatutDemandeEntreprise;
  entrepriseId: number;
  raisonSociale: string;
  formationId: number | null;
  intituleFormation: string | null;
  description: string;
  effectifConcerne: number | null;
  datesSouhaitees: string | null;
  dateSoumission: string;
  dateReponse: string | null;
  reponseEquipe: string | null;
  /** Statut de la fiche technique rattachée, ou null si aucune n'a encore été créée. */
  ficheStatut: StatutFicheModule | null;
}

/** Une journée du planning (§4 du gabarit). */
export interface FicheModuleJour {
  id: number | null;
  numero: number;
  contenus: string | null;
  dureeHeures: number | null;
  typeEnseignement: string | null;
  modalites: string | null;
}

/** Fiche Technique de Module telle que renvoyée par le serveur. */
export interface FicheTechniqueModule {
  id: number;
  demandeId: number;
  statut: StatutFicheModule;
  dateTransmission: string | null;
  commentaireEntreprise: string | null;
  dateDecisionEntreprise: string | null;
  code: string | null;
  intitule: string | null;
  version: string | null;
  dateRevision: string | null;
  responsableModule: string | null;
  departement: string | null;
  atelierService: string | null;
  fonctionsCibles: string | null;
  thematiques: string | null;
  prerequis: string | null;
  langue: string | null;
  niveau: string | null;
  effectif: number | null;
  dureeHeures: number | null;
  dureeJours: number | null;
  periodeProbable: string | null;
  objectifGeneral: string | null;
  objectifsSpecifiques: string | null;
  modalitesTestPrerequis: string | null;
  formationSurSite: string | null;
  typesEvaluation: string | null;
  ressourcesPedagogiques: string | null;
  bibliographie: string | null;
  profilFormateur: string | null;
  coutTotal: string | null;
  modalitesFinancieres: string | null;
  jours: FicheModuleJour[];
}

/** Corps d'un enregistrement (upsert) de fiche — le statut n'y est pas piloté. */
export interface FicheTechniqueModuleSaisie {
  code: string | null;
  intitule: string | null;
  version: string | null;
  dateRevision: string | null;
  responsableModule: string | null;
  departement: string | null;
  atelierService: string | null;
  fonctionsCibles: string | null;
  thematiques: string | null;
  prerequis: string | null;
  langue: string | null;
  niveau: string | null;
  effectif: number | null;
  dureeHeures: number | null;
  dureeJours: number | null;
  periodeProbable: string | null;
  objectifGeneral: string | null;
  objectifsSpecifiques: string | null;
  modalitesTestPrerequis: string | null;
  formationSurSite: string | null;
  typesEvaluation: string | null;
  ressourcesPedagogiques: string | null;
  bibliographie: string | null;
  profilFormateur: string | null;
  coutTotal: string | null;
  modalitesFinancieres: string | null;
  jours: FicheModuleJour[];
}

/**
 * Volet pédagogique de l'espace Entreprise (TDR §H, QMS-006/008/010) : demandes
 * reçues et Fiche Technique de Module. Distinct de {@code PedagogieService},
 * déjà volumineux, comme {@code EntrepriseService} l'est côté espace entreprise.
 */
@Injectable({ providedIn: 'root' })
export class PedagogieEntrepriseService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/pedagogie/entreprises`;

  getDemandes(): Observable<DemandeEntrepriseDto[]> {
    return this.http.get<DemandeEntrepriseDto[]>(`${this.api}/demandes`);
  }

  repondreDemande(
    id: number,
    statut: StatutDemandeEntreprise,
    reponse: string | null
  ): Observable<DemandeEntrepriseDto> {
    return this.http.put<DemandeEntrepriseDto>(`${this.api}/demandes/${id}`, { statut, reponse });
  }

  /** 404 tant qu'aucune fiche n'a encore été créée pour cette demande. */
  getFiche(demandeId: number): Observable<FicheTechniqueModule> {
    return this.http.get<FicheTechniqueModule>(`${this.api}/demandes/${demandeId}/fiche`);
  }

  /** Crée la fiche en BROUILLON si elle n'existe pas encore, sinon la remplace. */
  enregistrerFiche(demandeId: number, donnees: FicheTechniqueModuleSaisie): Observable<FicheTechniqueModule> {
    return this.http.put<FicheTechniqueModule>(`${this.api}/demandes/${demandeId}/fiche`, donnees);
  }

  transmettreFiche(demandeId: number): Observable<FicheTechniqueModule> {
    return this.http.put<FicheTechniqueModule>(`${this.api}/demandes/${demandeId}/fiche/transmettre`, {});
  }
}
