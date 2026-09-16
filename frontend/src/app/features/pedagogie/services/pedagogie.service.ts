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

  /**
   * Ouvre un compte formateur. Le mot de passe est optionnel : laissé vide, le
   * serveur en génère un et l'envoie par courriel au titulaire.
   */
  creerFormateur(données: FormateurSaisie): Observable<{ id: number; message: string }> {
    return this.http.post<{ id: number; message: string }>(`${this.api}/formateurs`, données);
  }

  /** Fiche complète : coordonnées et cours enseignés, à la différence du répertoire. */
  getFormateurDetail(id: number): Observable<FormateurDetail> {
    return this.http.get<FormateurDetail>(`${this.api}/formateurs/${id}`);
  }

  /** Rapport de l'effectif formateurs (PDF) : coordonnées, cours affectés, formateurs sans cours signalés. */
  telechargerRapportFormateurs(): Observable<Blob> {
    return this.http.get(`${this.api}/formateurs/rapport/pdf`, { responseType: 'blob' });
  }

  /**
   * Refusée (409) s'il est seul titulaire d'un cours, s'il a un dossier de
   * candidature ou un historique alumni, ou s'il a publié dans le forum.
   */
  supprimerFormateur(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/formateurs/${id}`);
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

  /** Fait passer un étudiant côté alumni (Espace Alumni, TDR §C). Idempotent. */
  promouvoirAlumni(etudiantId: number): Observable<EtudiantScolarite> {
    return this.http.put<EtudiantScolarite>(`${this.api}/etudiants/${etudiantId}/promouvoir-alumni`, {});
  }

  /** Import CSV tolérant : les lignes invalides sont rejetées une à une, pas en bloc. */
  importerEtudiants(csv: string, promotionId: number | null): Observable<ResultatImport> {
    return this.http.post<ResultatImport>(`${this.api}/etudiants/import`, { csv, promotionId });
  }

  /** Fiche complète : coordonnées comprises, à la différence du répertoire. */
  getEtudiantDetail(id: number): Observable<EtudiantDetail> {
    return this.http.get<EtudiantDetail>(`${this.api}/etudiants/${id}`);
  }

  /**
   * Refusée (409) si un dossier de candidature ou un historique alumni existe,
   * ou si l'étudiant a publié dans le forum.
   */
  supprimerEtudiant(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/etudiants/${id}`);
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

  /**
   * Rapport PDF « Avis sur les cours » (téléchargé en Blob : le serveur pose un
   * `Content-Disposition: attachment` avec un nom de fichier daté).
   */
  telechargerRapportAvisCours(): Observable<Blob> {
    return this.http.get(`${this.api}/qualite/rapport/avis-cours`, { responseType: 'blob' });
  }

  /** Rapport PDF « Enquêtes de suivi de carrière ». */
  telechargerRapportEnquetesSuivi(): Observable<Blob> {
    return this.http.get(`${this.api}/qualite/rapport/enquetes-suivi`, { responseType: 'blob' });
  }

  // ===================== Enquêtes de suivi =====================

  getEnquetesSuivi(): Observable<SyntheseEnqueteSuivi[]> {
    return this.http.get<SyntheseEnqueteSuivi[]>(`${this.api}/enquetes-suivi`);
  }

  lancerEnqueteSuivi(titre: string, description: string | null): Observable<SyntheseEnqueteSuivi> {
    return this.http.post<SyntheseEnqueteSuivi>(`${this.api}/enquetes-suivi`, { titre, description });
  }

  cloturerEnqueteSuivi(id: number): Observable<SyntheseEnqueteSuivi> {
    return this.http.put<SyntheseEnqueteSuivi>(`${this.api}/enquetes-suivi/${id}/cloturer`, {});
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

  /** PDF officiel d'un relevé délivré (Blob : nom de fichier posé par le serveur). */
  telechargerRelevePdf(id: number): Observable<Blob> {
    return this.http.get(`${this.api}/releves/${id}/pdf`, { responseType: 'blob' });
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
  promotion: string;
  derniereConnexion: string;
  progression: number;
  /** null tant qu'aucune note n'est saisie — a ne pas confondre avec un zero. */
  noteMoyenne: number | null;
  statut: string;
  initiales: string;
  couleurAvatar: string;
  matricule: string | null;
  /** « Admission » si le compte vient d'un candidat admis, null s'il a été créé à la main. */
  origine: string | null;
  /** Vrai si ce compte est déjà passé côté alumni (voir promouvoirAlumni). */
  estAlumni: boolean;
}

/** Fiche complète d'un étudiant : coordonnées comprises, réservée à la pédagogie. */
export interface EtudiantDetail {
  id: number;
  nom: string;
  prenom: string;
  email: string | null;
  telephone: string | null;
  username: string | null;
  matricule: string | null;
  promotionId: number | null;
  promotion: string;
  noteMoyenne: number | null;
  statut: string;
  origine: string | null;
  estAlumni: boolean;
}

/** Fiche complète d'un formateur : coordonnées et cours enseignés. */
export interface FormateurDetail {
  id: number;
  nom: string;
  prenom: string;
  email: string | null;
  telephone: string | null;
  username: string | null;
  coursEnseignes: string[];
}

export interface EtudiantSaisie {
  username: string;
  password: string;
  email: string;
  nom: string;
  prenom: string;
  /** Requis : Etudiant.telephone est NOT NULL UNIQUE en base. */
  telephone: string;
}

/** `password` vide : le serveur en génère un et l'envoie par courriel. */
export interface FormateurSaisie {
  username: string;
  password: string;
  email: string;
  nom: string;
  prenom: string;
  /** Requis : Formateur.telephone est NOT NULL UNIQUE en base. */
  telephone: string;
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
  promotionLibelle: string | null;
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

export interface SyntheseEnqueteSuivi {
  enqueteId: number;
  titre: string;
  description: string | null;
  dateCreation: string;
  active: boolean;
  nombreReponses: number;
  nombreDestinataires: number;
  moyenneApplicationCompetences: number | null;
  moyenneImpactCarriere: number | null;
  derniersCommentaires: string[];
}
