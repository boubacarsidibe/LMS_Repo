import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Cours, CoursCreation } from '../../espace-etudiants/models/cours';
import { Formation } from '../../espace-etudiants/models/formation';

/** Support tel que renvoyé par /api/pedagogie et /api/espace-etudiant/cours/{id}/supports. */
export interface SupportDetail {
  id: number;
  titre: string;
  typeRessource: string | null;
  url: string | null;
  datePublication: string | null;
  telechargeable: boolean;
}

export interface Promotion {
  id: number;
  code: string;
  libelle: string;
}

/**
 * Espace du responsable pédagogique. Distinct de CoursService, qui adresse les
 * endpoints /formateur/** cadrés par la propriété du cours : ici les cours sont
 * pilotés quel que soit le formateur rattaché.
 */
@Injectable({ providedIn: 'root' })
export class PedagogieService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/pedagogie`;

  getFormations(): Observable<Formation[]> {
    return this.http.get<Formation[]>(`${this.api}/formations`);
  }

  /**
   * Promotions avec leur programme et leurs effectifs.
   *
   * <p>Renvoyait auparavant une forme réduite sans la formation rattachée : l'écran
   * affichait « Aucun programme » pour toutes, et l'édition effaçait le rattachement
   * faute de le retrouver.
   */
  getPromotions(): Observable<PromotionDetail[]> {
    return this.http.get<PromotionDetail[]>(`${this.api}/promotions`);
  }

  getCours(): Observable<Cours[]> {
    return this.http.get<Cours[]>(`${this.api}/cours`);
  }

  /** Formateurs affectables à un cours. */
  getFormateurs(): Observable<FormateurBref[]> {
    return this.http.get<FormateurBref[]>(`${this.api}/formateurs`);
  }

  /** Remplace l'équipe pédagogique du cours. Une liste vide est refusée (400). */
  affecterFormateurs(coursId: number, formateurIds: number[]): Observable<Cours> {
    return this.http.put<Cours>(`${this.api}/cours/${coursId}/formateurs`, { formateurIds });
  }

  creerCours(data: CoursCreation): Observable<Cours> {
    return this.http.post<Cours>(`${this.api}/cours`, data);
  }

  modifierCours(id: number, data: CoursCreation): Observable<Cours> {
    return this.http.put<Cours>(`${this.api}/cours/${id}`, data);
  }

  supprimerCours(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/cours/${id}`);
  }

  getSupports(coursId: number): Observable<SupportDetail[]> {
    return this.http.get<SupportDetail[]>(`${this.api}/cours/${coursId}/supports`);
  }

  // La publication et le retrait d'un support sont rendus au formateur : c'est lui
  // qui conçoit le contenu de son cours. Le responsable pédagogique garde la
  // consultation, qui lui sert à vérifier qu'un cours n'est pas vide.

  // ===================== Promotions =====================

  creerPromotion(données: PromotionSaisie): Observable<PromotionDetail> {
    return this.http.post<PromotionDetail>(`${this.api}/promotions`, données);
  }

  modifierPromotion(id: number, données: PromotionSaisie): Observable<PromotionDetail> {
    return this.http.put<PromotionDetail>(`${this.api}/promotions/${id}`, données);
  }

  /** Refusé (409) tant que des étudiants sont rattachés à la promotion. */
  supprimerPromotion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/promotions/${id}`);
  }

  // ===================== Étudiants =====================

  getEtudiants(recherche?: string): Observable<EtudiantScolarite[]> {
    const url = recherche?.trim()
      ? `${this.api}/etudiants?recherche=${encodeURIComponent(recherche.trim())}`
      : `${this.api}/etudiants`;
    return this.http.get<EtudiantScolarite[]>(url);
  }

  creerEtudiant(données: EtudiantSaisie): Observable<EtudiantScolarite> {
    return this.http.post<EtudiantScolarite>(`${this.api}/etudiants`, données);
  }

  /** `promotionId` à null détache l'étudiant de sa promotion. */
  affecterPromotion(etudiantId: number, promotionId: number | null): Observable<EtudiantScolarite> {
    return this.http.put<EtudiantScolarite>(`${this.api}/etudiants/${etudiantId}/promotion`, { promotionId });
  }

  /** Import CSV tolérant : les lignes invalides sont rejetées une à une, pas en bloc. */
  importerEtudiants(csv: string, promotionId: number | null): Observable<ResultatImport> {
    return this.http.post<ResultatImport>(`${this.api}/etudiants/import`, { csv, promotionId });
  }

  // ===================== Certificats =====================

  getCertificats(): Observable<Certificat[]> {
    return this.http.get<Certificat[]>(`${this.api}/certificats`);
  }

  delivrerCertificat(etudiantId: number, intituleFormation: string, cohorteId: number | null): Observable<Certificat> {
    return this.http.post<Certificat>(`${this.api}/certificats`, { etudiantId, intituleFormation, cohorteId });
  }

  // ===================== Qualité et rappels =====================

  getQualite(): Observable<Qualite> {
    return this.http.get<Qualite>(`${this.api}/qualite`);
  }

  envoyerRappel(objet: string, message: string, etudiantIds: number[]): Observable<ResultatRappel> {
    return this.http.post<ResultatRappel>(`${this.api}/rappels`, { objet, message, etudiantIds });
  }

  // ===================== Relevés de notes =====================

  /** Relevé COURANT d'un étudiant : recalculé, pas figé. Sert à vérifier avant de délivrer. */
  getReleveCourant(etudiantId: number): Observable<ReleveCourant> {
    return this.http.get<ReleveCourant>(`${this.api}/etudiants/${etudiantId}/releve`);
  }

  /** Registre des relevés délivrés. */
  getReleves(): Observable<ReleveDelivre[]> {
    return this.http.get<ReleveDelivre[]>(`${this.api}/releves`);
  }

  /**
   * Délivre un relevé. Les moyennes sont figées à cet instant : le document remis ne
   * changera plus si un formateur corrige une note ensuite.
   */
  delivrerReleves(
    periode: string,
    etudiantIds: number[],
    promotionId: number | null
  ): Observable<ResultatDelivrance> {
    return this.http.post<ResultatDelivrance>(`${this.api}/releves`, {
      periode,
      etudiantIds,
      promotionId,
    });
  }
}

/** Relevé recalculé à chaque appel — celui que l'étudiant voit en continu. */
export interface ReleveCourant {
  moyenneGenerale: number | null;
  nombreNotes: number;
  parCours: {
    coursId: number;
    coursCode: string;
    coursTitre: string;
    moyenne: number | null;
    coefficient: number;
  }[];
}

/** Relevé délivré : document daté, référencé, aux valeurs figées. */
export interface ReleveDelivre {
  id: number;
  reference: string;
  etudiantId: number;
  etudiantNom: string;
  matricule: string | null;
  promotionLibelle: string | null;
  periode: string;
  dateEmission: string;
  delivrePar: string;
  moyenneGenerale: number | null;
  nombreNotes: number;
  lignes: { coursCode: string; coursTitre: string; coefficient: number; moyenne: number | null }[];
}

export interface ResultatDelivrance {
  delivres: ReleveDelivre[];
  /** Étudiants écartés faute de note : leur délivrer un relevé vide n'attesterait de rien. */
  sansNote: string[];
  /** Étudiants qui avaient déjà un relevé pour cette période. Signalé, jamais bloqué. */
  reemissions: string[];
}

export interface PromotionSaisie {
  code: string;
  libelle: string;
  formationId: number | null;
  dateDebut?: string | null;
  dateFin?: string | null;
  programme?: string | null;
  activitesParascolaires?: string | null;
}

export interface PromotionDetail {
  id: number;
  code: string;
  libelle: string;
  dateDebut: string | null;
  dateFin: string | null;
  enCours: boolean;
  programme: string | null;
  activitesParascolaires: string | null;
  formationCode: string | null;
  formationIntitule: string | null;
  formationDescription: string | null;
  /** Nommé « effectif » côté serveur (PromotionDetailDto), partagé avec l'espace étudiant. */
  effectif: number;
  nbCours: number;
}

/** Identité d'un formateur, telle que renvoyée avec un cours. */
export interface FormateurBref {
  id: number;
  nomComplet: string;
}

export interface EtudiantScolarite {
  id: number;
  nom: string;
  cohorte: string;
  derniereConnexion: string;
  progression: number;
  /** null tant qu'aucune note n'est saisie — a ne pas confondre avec un zero. */
  noteMoyenne: number | null;
  statut: string;
  initiales: string;
  couleurAvatar: string;
  matricule: string | null;
}

export interface EtudiantSaisie {
  username: string;
  password: string;
  email: string;
  nom: string;
  prenom: string;
  telephone?: string | null;
}

export interface ResultatImport {
  crees: number;
  rejets: string[];
}

export interface ResultatRappel {
  envoyes: number;
  echecs: string[];
}

export interface Certificat {
  id: number;
  reference: string;
  etudiantId: number | null;
  etudiantNom: string | null;
  etudiantMatricule: string | null;
  intituleFormation: string;
  cohorteLibelle: string | null;
  dateEmission: string | null;
  formateurNom: string | null;
}

export interface LigneQualite {
  coursId: number;
  coursCode: string;
  coursTitre: string;
  moyenne: number | null;
  nombreAvis: number;
  actionsRequises: number;
  derniersCommentaires: string[];
}

export interface Qualite {
  moyenneGenerale: number | null;
  nombreAvis: number;
  coursEvalues: number;
  coursASurveiller: number;
  parCours: LigneQualite[];
}
