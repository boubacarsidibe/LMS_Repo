import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FormationOption {
  id: number;
  code: string;
  intitule: string;
}

export interface SessionCandidatureResponse {
  id: number;
  libelle: string;
  formationId: number | null;
  formationCode: string | null;
  formationIntitule: string | null;
  formationNiveau: string | null;
  dateOuverture: string;
  dateFermeture: string;
  estOuverte: boolean;
  estActive: boolean;
  criteresActifs: boolean;
  criteresNiveauEtudeMinimum: string | null;
  criteresAgeMinimum: number | null;
  criteresAgeMaximum: number | null;
  criteresGenre: string | null;
  criteresExperienceMinimale: number | null;
  seuilReussiteTest: number;
  nbCandidatures: number;
  /** Informations "Prochaines étapes" affichées au candidat admis, null si non renseignées. */
  infoFraisScolarite: string | null;
  infoCarteEtudiante: string | null;
  infoRentree: string | null;
  /** Critères ajoutés librement par le responsable admission, en plus des critères de base ci-dessus. */
  criteresPersonnalises: CritereAdmissionResponse[];
}

export type EnumTypeCritere = 'NUMERIQUE' | 'TEXTE' | 'BOOLEEN' | 'CHOIX';

export interface CritereAdmissionResponse {
  id: number;
  libelle: string;
  type: EnumTypeCritere;
  obligatoire: boolean;
  /** Options proposées (type CHOIX), séparées par des virgules. */
  options: string | null;
  valeurMinimale: number | null;
  valeurAttendue: string | null;
  ordre: number;
  actif: boolean;
}

export interface CritereAdmissionRequest {
  libelle: string;
  type: EnumTypeCritere;
  obligatoire: boolean;
  options: string | null;
  valeurMinimale: number | null;
  valeurAttendue: string | null;
  ordre: number;
}

export interface FiltreCriteresResponse {
  total: number;
  preselectionnes: number;
  rejetes: number;
}

/** Compte rendu d'un envoi groupé (relance dossiers incomplets ou paiements impayés). */
export interface ResultatRelance {
  envoyes: number;
  echecs: string[];
}

export interface CandidatureRequest {
  sessionCandidatureId: number;
  informationsPersonnelles: string;
  informationsAcademiques: string;
  informationsProfessionnelles: string;
  /** Réponses aux critères personnalisés de la session : clé = id du critère. */
  reponsesCriteres?: { [critereId: number]: string };
}

export interface ReponseCritereResponse {
  critereId: number;
  libelle: string;
  valeur: string | null;
}

export interface CandidatureResponse {
  id: number;
  numeroDossier: string;
  statut: string;
  dateSoumission: string;
  motifRejet: string | null;
  /** Non nul si la dernière décision peut être annulée (bouton "Annuler"). */
  statutPrecedent: string | null;
  candidatId: number;
  candidatNom: string;
  candidatPrenom: string;
  candidatEmail: string | null;
  candidatGenre: string | null;
  candidatProfession: string | null;
  candidatAnneesExperience: number | null;
  candidatDateNaissance: string | null;
  candidatNiveauEtude: string | null;
  candidatNationalite: string | null;
  sessionCandidatureId: number;
  sessionCandidatureLibelle: string;
  /** Informations "Prochaines étapes" de la session, saisies par le responsable admission. */
  sessionInfoFraisScolarite: string | null;
  sessionInfoCarteEtudiante: string | null;
  sessionInfoRentree: string | null;
  piecesJustificatives: PieceJustificativeResponse[];
  paiement: PaiementResponse | null;
  reponsesCriteres: ReponseCritereResponse[];
}

export interface PieceJustificativeResponse {
  id: number;
  typeDocument: string;
  estConforme: boolean;
  fichierStockeId: number | null;
  fichierNomOriginal: string | null;
}

export interface PaiementResponse {
  id: number;
  referenceTransaction: string;
  montant: number;
  devise: string;
  statut: string;
  datePaiement: string | null;
  methodePaiement: string | null;
  candidatureId: number | null;
  numeroDossier: string | null;
  candidatNom: string | null;
  candidatPrenom: string | null;
  candidatureStatut: string | null;
}

/**
 * Le montant et la devise ne sont volontairement pas transmis : les frais de
 * dossier sont fixés par le serveur. Les laisser à la main du client revenait à
 * le laisser choisir le prix qu'il paie.
 */
export interface PaiementRequest {
  candidatureId: number;
  methodePaiement?: string;
}

export interface SessionCandidatureRequest {
  libelle: string;
  formationId: number | null;
  dateOuverture: string;
  dateFermeture: string;
  estOuverte: boolean;
  criteresActifs: boolean;
  criteresNiveauEtudeMinimum: string | null;
  criteresAgeMinimum: number | null;
  criteresAgeMaximum: number | null;
  criteresGenre: string | null;
  criteresExperienceMinimale: number | null;
  seuilReussiteTest: number;
  infoFraisScolarite: string | null;
  infoCarteEtudiante: string | null;
  infoRentree: string | null;
}

export type EnumTypeNotification =
  | 'VALIDATION_DOSSIER' | 'REJET_DOSSIER' | 'PAIEMENT_SUCCES'
  | 'CONVOCATION_TEST' | 'CONVOCATION_ENTRETIEN'
  | 'ADMISSION_TEST' | 'ADMISSION_ENTRETIEN' | 'ADMISSION_FINALE' | 'NON_ADMISSION'
  | 'DOCUMENT_CONFORME' | 'DOCUMENT_NON_CONFORME';

export interface NotificationTemplate {
  type: EnumTypeNotification;
  sujet: string;
  corps: string;
  actif: boolean;
}

export interface ConvocationTestGroupeeRequest {
  candidatIds: number[];
  nomEpreuve: string;
  coefficient: number;
  dateEpreuve: string;
  lieuOuLien: string | null;
}

export interface ConvocationEntretienGroupeeRequest {
  candidatIds: number[];
  dateHeure: string;
  lieuOuLien: string;
}

export interface TestAcademiqueRequest {
  nomEpreuve: string;
  coefficient: number;
  noteObtenue: number | null;
  dateEpreuve: string;
  lieuOuLien: string | null;
  candidatId: number;
}

export interface TestAcademiqueResponse {
  id: number;
  nomEpreuve: string;
  coefficient: number;
  noteObtenue: number | null;
  dateEpreuve: string;
  lieuOuLien: string | null;
  candidatId: number;
  sessionCandidatureId: number | null;
  sessionCandidatureLibelle: string | null;
}

export interface EntretienRequest {
  dateHeure: string;
  lieuOuLien: string;
  noteEntretien: number;
  appreciationGlobale: string;
  candidatId: number;
}

/** Formation du catalogue public. */
export interface FormationPublique {
  id: number;
  code: string;
  intitule: string;
  description: string;
  niveau: string | null;
  domaine: string | null;
  duree: string | null;
  imageUrl: string | null;
}

/**
 * Fiche détaillée. `ficheDetaillee` porte le contenu éditorial (modules, points
 * clés, débouchés, encadré latéral) dans la forme attendue par le gabarit
 * existant : il n'a donc pas eu à changer.
 */
export interface FormationDetailPublique extends FormationPublique {
  ficheDetaillee: Record<string, any> | null;
}

/** Épreuve académique telle que le candidat peut la consulter. */
export interface TestCandidat {
  nomEpreuve: string;
  coefficient: number | null;
  noteObtenue: number | null;
  dateEpreuve: string | null;
  lieu: string | null;
}

/** Convocation à un entretien. Les appréciations du jury ne sont pas exposées. */
export interface EntretienCandidat {
  id: number;
  dateHeure: string | null;
  lieuOuLien: string | null;
  confirmeParCandidat: boolean;
  motifReport: string | null;
}

export interface ParcoursCandidature {
  tests: TestCandidat[];
  entretiens: EntretienCandidat[];
  seuilReussiteTest: number | null;
}

export interface NotificationCandidatResponse {
  id: number;
  message: string;
  type: string;
  lu: boolean;
  dateCreation: string;
  lien: string | null;
}

export interface EntretienResponse {
  id: number;
  dateHeure: string;
  lieuOuLien: string;
  noteEntretien: number | null;
  appreciationGlobale: string;
  candidatId: number;
}

@Injectable({
  providedIn: 'root'
})
export class RecrutementService {

  private baseUrl = '/api';

  constructor(private http: HttpClient) { }

  // ===================== ACADEMIC TESTS =====================

  getAcademicTestKpis(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/academic-tests/kpis`);
  }

  listAcademicExams(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/academic-tests/exams`);
  }

  createAcademicExam(body: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/academic-tests/exams`, body);
  }

  changeExamStatus(nom: string, status: string): Observable<any> {
    return this.http.patch<any>(`${this.baseUrl}/academic-tests/exams/${encodeURIComponent(nom)}/status`, { status });
  }

  getExamScores(nom: string, seuil: number = 60): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/academic-tests/exams/${encodeURIComponent(nom)}/scores?seuil=${seuil}`);
  }

  saveExamScores(nom: string, scores: any[]): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/academic-tests/exams/${encodeURIComponent(nom)}/scores/batch`, scores);
  }

  /** Seuil de réussite (note/20) de la session de candidature actuellement ouverte. */
  getRules(): Observable<{ seuilTests: number | null; sessionId: number | null }> {
    return this.http.get<{ seuilTests: number | null; sessionId: number | null }>(`${this.baseUrl}/settings/rules`);
  }

  updateRules(seuilTests: number): Observable<{ seuilTests: number | null; sessionId: number | null }> {
    return this.http.put<{ seuilTests: number | null; sessionId: number | null }>(
      `${this.baseUrl}/settings/rules`, { seuilTests });
  }

  // ===================== ANALYTICS =====================

  getAnalyticsKpis(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/analytics/kpis`);
  }

  getVolumeByMonth(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/analytics/volume-by-month`);
  }

  getGeographicDistribution(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/analytics/geographic-distribution`);
  }

  getConversionFunnel(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/analytics/conversion-funnel`);
  }

  exportAnalyticsReport(annee: string = '2026-2027'): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/analytics/export-report?annee=${annee}`, { responseType: 'blob' });
  }

  // ===================== INTERVIEWS =====================

  getInterviewKpis(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/interviews/kpis`);
  }

  listInterviews(tab: string = 'ALL', search?: string, page: number = 0, limit: number = 10): Observable<any[]> {
    const params = new URLSearchParams();
    params.set('tab', tab);
    if (search) params.set('search', search);
    params.set('page', String(page));
    params.set('limit', String(limit));
    return this.http.get<any[]>(`${this.baseUrl}/interviews?${params.toString()}`);
  }

  createInterview(body: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/interviews`, body);
  }

  updateInterview(id: number, body: any): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/interviews/${id}`, body);
  }

  saveEvaluation(id: number, body: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/interviews/${id}/evaluation`, body);
  }

  deleteInterview(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/interviews/${id}`);
  }

  // ===================== APPLICATIONS =====================

  getApplicationKpis(filters?: { status?: string; sessionId?: number }): Observable<any> {
    const params = new URLSearchParams();
    if (filters?.status) params.set('status', filters.status);
    if (filters?.sessionId) params.set('sessionId', String(filters.sessionId));
    const qs = params.toString();
    return this.http.get<any>(`${this.baseUrl}/applications/kpis${qs ? '?' + qs : ''}`);
  }

  listApplications(filters?: { search?: string; status?: string; programId?: number; sessionId?: number }): Observable<any[]> {
    const params = new URLSearchParams();
    if (filters?.search) params.set('search', filters.search);
    if (filters?.status) params.set('status', filters.status);
    if (filters?.sessionId) params.set('sessionId', String(filters.sessionId));
    const qs = params.toString();
    return this.http.get<any[]>(`${this.baseUrl}/applications${qs ? '?' + qs : ''}`);
  }

  sendReminder(): Observable<ResultatRelance> {
    return this.http.post<ResultatRelance>(`${this.baseUrl}/applications/reminder-incomplets`, {});
  }

  exportApplications(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/applications/export`, { responseType: 'blob' });
  }

  updateDocumentStatus(applicationId: number, pieceId: number, valide: boolean): Observable<any> {
    return this.http.patch<any>(`${this.baseUrl}/applications/${applicationId}/documents/status`, { pieceId, valide });
  }


  // ===================== PUBLIC =====================

  getCataloguePublic(): Observable<SessionCandidatureResponse[]> {
    return this.http.get<SessionCandidatureResponse[]>(`${this.baseUrl}/recrutement/public/catalogue`);
  }

  /**
   * Parcours d'évaluation du candidat : épreuves passées et entretiens planifiés.
   * Remplace les notes et rendez-vous qui étaient écrits en dur dans la page de suivi.
   */
  getParcoursCandidature(candidatureId: number): Observable<ParcoursCandidature> {
    return this.http.get<ParcoursCandidature>(
      `${this.baseUrl}/recrutement/candidat/candidature/${candidatureId}/parcours`
    );
  }

  /** Catalogue public des formations, servi par la base et non plus codé en dur. */
  getFormationsPubliques(): Observable<FormationPublique[]> {
    return this.http.get<FormationPublique[]>(`${this.baseUrl}/recrutement/public/formations`);
  }

  /** Fiche détaillée d'une formation publiée. */
  getFormationPublique(id: number): Observable<FormationDetailPublique> {
    return this.http.get<FormationDetailPublique>(
      `${this.baseUrl}/recrutement/public/formations/${id}`
    );
  }

  poserQuestion(email: string, message: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/recrutement/public/question`, { email, message });
  }

  // ===================== CANDIDAT =====================

  /**
   * Route unique : crée le Candidat (si nécessaire) et soumet la candidature.
   */
  soumettreCandidature(request: CandidatureRequest): Observable<CandidatureResponse> {
    return this.http.post<CandidatureResponse>(`${this.baseUrl}/recrutement/candidat/candidature/soumettre`, request);
  }

  televerserPiece(candidatureId: number, file: File, typeDocument: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('typeDocument', typeDocument);
    return this.http.post(`${this.baseUrl}/recrutement/candidat/candidature/${candidatureId}/pieces`, formData);
  }

  initierPaiement(request: PaiementRequest): Observable<PaiementResponse> {
    return this.http.post<PaiementResponse>(`${this.baseUrl}/recrutement/candidat/paiement`, request);
  }

  /** Reçu PDF des frais de dossier. Le backend refuse (403) le reçu d'un autre candidat. */
  telechargerRecuPaiement(paiementId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/recrutement/candidat/paiement/${paiementId}/recu`, {
      responseType: 'blob',
    });
  }

  confirmerPresenceEntretien(entretienId: number): Observable<EntretienResponse> {
    return this.http.put<EntretienResponse>(`${this.baseUrl}/recrutement/candidat/entretien/${entretienId}/confirmer-presence`, {});
  }

  demanderReportEntretien(entretienId: number, motif: string): Observable<EntretienResponse> {
    return this.http.put<EntretienResponse>(`${this.baseUrl}/recrutement/candidat/entretien/${entretienId}/demander-report`, { motif });
  }

  /** Convocation PDF à l'entretien. Le backend refuse (403) la convocation d'un autre candidat. */
  telechargerConvocationEntretien(entretienId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/recrutement/candidat/entretien/${entretienId}/convocation`, {
      responseType: 'blob',
    });
  }

  getMesCandidatures(): Observable<CandidatureResponse[]> {
    return this.http.get<CandidatureResponse[]>(`${this.baseUrl}/recrutement/candidat/candidatures`);
  }

  /** Lettre d'admission PDF. Le backend refuse (403) la lettre d'un autre candidat, et (409) si la candidature n'est pas admise. */
  telechargerLettreAdmission(candidatureId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/recrutement/candidat/candidature/${candidatureId}/lettre-admission`, {
      responseType: 'blob',
    });
  }

  getCandidature(id: number): Observable<CandidatureResponse> {
    return this.http.get<CandidatureResponse>(`${this.baseUrl}/recrutement/candidat/candidature/${id}`);
  }

  suivreAvancement(numeroDossier: string): Observable<CandidatureResponse> {
    return this.http.get<CandidatureResponse>(`${this.baseUrl}/recrutement/candidat/suivi/${numeroDossier}`);
  }

  /** Brouillon du formulaire de candidature persisté côté serveur (reprise depuis un autre appareil). */
  getBrouillon(): Observable<{ donneesJson?: string }> {
    return this.http.get<{ donneesJson?: string }>(`${this.baseUrl}/recrutement/candidat/brouillon`);
  }

  sauvegarderBrouillon(donneesJson: string): Observable<any> {
    return this.http.put(`${this.baseUrl}/recrutement/candidat/brouillon`, { donneesJson });
  }

  // ===================== ADMIN =====================

  // Sessions
  getAllSessions(): Observable<SessionCandidatureResponse[]> {
    return this.http.get<SessionCandidatureResponse[]>(`${this.baseUrl}/admin/recrutement/sessions`);
  }

  /** Formations proposables pour l'ouverture d'une session. */
  getFormationsDisponibles(): Observable<FormationOption[]> {
    return this.http.get<FormationOption[]>(`${this.baseUrl}/admin/recrutement/formations`);
  }

  getSession(id: number): Observable<SessionCandidatureResponse> {
    return this.http.get<SessionCandidatureResponse>(`${this.baseUrl}/admin/recrutement/sessions/${id}`);
  }

  creerSession(request: SessionCandidatureRequest): Observable<SessionCandidatureResponse> {
    return this.http.post<SessionCandidatureResponse>(`${this.baseUrl}/admin/recrutement/sessions`, request);
  }

  modifierSession(id: number, request: SessionCandidatureRequest): Observable<SessionCandidatureResponse> {
    return this.http.put<SessionCandidatureResponse>(`${this.baseUrl}/admin/recrutement/sessions/${id}`, request);
  }

  supprimerSession(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/admin/recrutement/sessions/${id}`);
  }

  exporterSessions(colonnes?: string[]): Observable<Blob> {
    let params = new HttpParams();
    if (colonnes && colonnes.length) {
      colonnes.forEach((c) => (params = params.append('colonnes', c)));
    }
    return this.http.get(`${this.baseUrl}/admin/recrutement/sessions/export`, { responseType: 'blob', params });
  }

  /** Filtre automatique de premier niveau (TDR 3.1.1) : présélectionne ou rejette les dossiers SOUMIS de la session. */
  appliquerFiltreCriteres(sessionId: number): Observable<FiltreCriteresResponse> {
    return this.http.post<FiltreCriteresResponse>(
      `${this.baseUrl}/admin/recrutement/sessions/${sessionId}/appliquer-filtre-criteres`, {});
  }

  // Critères personnalisés
  getCriteresDeSession(sessionId: number): Observable<CritereAdmissionResponse[]> {
    return this.http.get<CritereAdmissionResponse[]>(`${this.baseUrl}/admin/recrutement/sessions/${sessionId}/criteres`);
  }

  creerCritere(sessionId: number, request: CritereAdmissionRequest): Observable<CritereAdmissionResponse> {
    return this.http.post<CritereAdmissionResponse>(`${this.baseUrl}/admin/recrutement/sessions/${sessionId}/criteres`, request);
  }

  getCritere(critereId: number): Observable<CritereAdmissionResponse> {
    return this.http.get<CritereAdmissionResponse>(`${this.baseUrl}/admin/recrutement/criteres/${critereId}`);
  }

  modifierCritere(critereId: number, request: CritereAdmissionRequest): Observable<CritereAdmissionResponse> {
    return this.http.put<CritereAdmissionResponse>(`${this.baseUrl}/admin/recrutement/criteres/${critereId}`, request);
  }

  retirerCritere(critereId: number): Observable<CritereAdmissionResponse> {
    return this.http.delete<CritereAdmissionResponse>(`${this.baseUrl}/admin/recrutement/criteres/${critereId}`);
  }

  reactiverCritere(critereId: number): Observable<CritereAdmissionResponse> {
    return this.http.put<CritereAdmissionResponse>(`${this.baseUrl}/admin/recrutement/criteres/${critereId}/reactiver`, {});
  }

  // Candidatures admin
  getAllCandidatures(): Observable<CandidatureResponse[]> {
    return this.http.get<CandidatureResponse[]>(`${this.baseUrl}/admin/recrutement/candidatures`);
  }

  getCandidatureAdmin(id: number): Observable<CandidatureResponse> {
    return this.http.get<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}`);
  }

  exporterCandidatures(statut?: string, colonnes?: string[]): Observable<Blob> {
    let params = new HttpParams();
    if (statut) {
      params = params.set('statut', statut);
    }
    if (colonnes && colonnes.length) {
      colonnes.forEach((c) => (params = params.append('colonnes', c)));
    }
    return this.http.get(`${this.baseUrl}/admin/recrutement/candidatures/export`, { responseType: 'blob', params });
  }

  /** Télécharge, en une archive .zip, l'ensemble des documents des candidatures (filtrées par statut). */
  exporterDocuments(statut?: string): Observable<Blob> {
    let params = new HttpParams();
    if (statut) {
      params = params.set('statut', statut);
    }
    return this.http.get(`${this.baseUrl}/admin/recrutement/candidatures/documents/export`, { responseType: 'blob', params });
  }

  telechargerPiece(pieceId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/admin/recrutement/pieces/${pieceId}/telecharger`, { responseType: 'blob' });
  }

  modifierConformitePiece(pieceId: number, estConforme: boolean): Observable<{ id: number; estConforme: boolean }> {
    return this.http.put<{ id: number; estConforme: boolean }>(
      `${this.baseUrl}/admin/recrutement/pieces/${pieceId}/conformite`, { estConforme });
  }

  annulerDerniereAction(id: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}/annuler`, {});
  }

  reculerStatut(id: number, statut: string): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}/reculer-statut`, { statut });
  }

  appliquerFiltreAutomatique(id: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}/filtrer`, {});
  }

  validerPreselection(id: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}/valider-preselection`, {});
  }

  mettreEnListeAttente(id: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}/liste-attente`, {});
  }

  rejeterDossier(id: number, motif: string): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}/rejeter`, { motif });
  }

  admettreTestAcademique(id: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}/admettre-test`, {});
  }

  admettreEntretien(id: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}/admettre-entretien`, {});
  }

  admettreFinal(id: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}/admettre-final`, {});
  }

  enregistrerPaiementHorsLigne(candidatureId: number, methodePaiement?: string): Observable<PaiementResponse> {
    return this.http.post<PaiementResponse>(
      `${this.baseUrl}/admin/recrutement/candidatures/${candidatureId}/paiement/enregistrer`,
      { methodePaiement });
  }

  simulerSuccesPaiement(id: number): Observable<PaiementResponse> {
    return this.http.put<PaiementResponse>(`${this.baseUrl}/admin/recrutement/paiements/${id}/simuler-succes`, {});
  }

  marquerPaiementEnEchec(id: number, motif?: string): Observable<PaiementResponse> {
    return this.http.put<PaiementResponse>(`${this.baseUrl}/admin/recrutement/paiements/${id}/echec`, { motif });
  }

  getAllPaiements(): Observable<PaiementResponse[]> {
    return this.http.get<PaiementResponse[]>(`${this.baseUrl}/admin/recrutement/paiements`);
  }

  /** Relance (email + notification) les candidats dont le dossier est validé mais impayé. */
  relancerPaiementsImpayes(): Observable<ResultatRelance> {
    return this.http.post<ResultatRelance>(`${this.baseUrl}/admin/recrutement/paiements/relance`, {});
  }

  // Tests Académiques
  getAllTests(): Observable<TestAcademiqueResponse[]> {
    return this.http.get<TestAcademiqueResponse[]>(`${this.baseUrl}/admin/recrutement/tests`);
  }

  getTest(id: number): Observable<TestAcademiqueResponse> {
    return this.http.get<TestAcademiqueResponse>(`${this.baseUrl}/admin/recrutement/tests/${id}`);
  }

  creerTest(request: TestAcademiqueRequest): Observable<TestAcademiqueResponse> {
    return this.http.post<TestAcademiqueResponse>(`${this.baseUrl}/admin/recrutement/tests`, request);
  }

  modifierTest(id: number, request: TestAcademiqueRequest): Observable<TestAcademiqueResponse> {
    return this.http.put<TestAcademiqueResponse>(`${this.baseUrl}/admin/recrutement/tests/${id}`, request);
  }

  supprimerTest(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/admin/recrutement/tests/${id}`);
  }

  convoquerTestsGroupe(request: ConvocationTestGroupeeRequest): Observable<TestAcademiqueResponse[]> {
    return this.http.post<TestAcademiqueResponse[]>(`${this.baseUrl}/admin/recrutement/tests/convocation-groupee`, request);
  }

  // Entretiens
  getAllEntretiens(): Observable<EntretienResponse[]> {
    return this.http.get<EntretienResponse[]>(`${this.baseUrl}/admin/recrutement/entretiens`);
  }

  creerEntretien(request: EntretienRequest): Observable<EntretienResponse> {
    return this.http.post<EntretienResponse>(`${this.baseUrl}/admin/recrutement/entretiens`, request);
  }

  modifierEntretien(id: number, request: EntretienRequest): Observable<EntretienResponse> {
    return this.http.put<EntretienResponse>(`${this.baseUrl}/admin/recrutement/entretiens/${id}`, request);
  }

  supprimerEntretien(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/admin/recrutement/entretiens/${id}`);
  }

  convoquerEntretiensGroupe(request: ConvocationEntretienGroupeeRequest): Observable<EntretienResponse[]> {
    return this.http.post<EntretienResponse[]>(`${this.baseUrl}/admin/recrutement/entretiens/convocation-groupee`, request);
  }

  // Modèles de notifications
  getAllTemplates(): Observable<NotificationTemplate[]> {
    return this.http.get<NotificationTemplate[]>(`${this.baseUrl}/admin/recrutement/notifications/templates`);
  }

  modifierTemplate(type: EnumTypeNotification, request: NotificationTemplate): Observable<NotificationTemplate> {
    return this.http.put<NotificationTemplate>(`${this.baseUrl}/admin/recrutement/notifications/templates/${type}`, request);
  }

  // ===================== NOTIFICATIONS CANDIDAT =====================

  getNotifications(): Observable<NotificationCandidatResponse[]> {
    return this.http.get<NotificationCandidatResponse[]>(`${this.baseUrl}/recrutement/candidat/notifications`);
  }

  countNotificationsNonLues(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.baseUrl}/recrutement/candidat/notifications/count`);
  }

  marquerNotificationLue(id: number): Observable<NotificationCandidatResponse> {
    return this.http.post<NotificationCandidatResponse>(`${this.baseUrl}/recrutement/candidat/notifications/${id}/lu`, {});
  }

  marquerToutesNotificationsLues(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/recrutement/candidat/notifications/tout-lu`, {});
  }
}
