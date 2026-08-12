import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SessionCandidatureResponse {
  id: number;
  libelle: string;
  dateOuverture: string;
  dateFermeture: string;
  estOuverte: boolean;
  estActive: boolean;
}

export interface CandidatureRequest {
  sessionCandidatureId: number;
  informationsPersonnelles: string;
  informationsAcademiques: string;
  informationsProfessionnelles: string;
}

export interface CandidatureResponse {
  id: number;
  numeroDossier: string;
  statut: string;
  dateSoumission: string;
  motifRejet: string | null;
  candidatId: number;
  candidatNom: string;
  candidatPrenom: string;
  candidatEmail: string | null;
  sessionCandidatureId: number;
  sessionCandidatureLibelle: string;
  piecesJustificatives: PieceJustificativeResponse[];
  paiement: PaiementResponse | null;
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
}

export interface PaiementRequest {
  candidatureId: number;
  montant: number;
  devise: string;
}

export interface SessionCandidatureRequest {
  libelle: string;
  dateOuverture: string;
  dateFermeture: string;
  estOuverte: boolean;
}

export interface TestAcademiqueRequest {
  nomEpreuve: string;
  coefficient: number;
  noteObtenue: number;
  dateEpreuve: string;
  candidatId: number;
}

export interface TestAcademiqueResponse {
  id: number;
  nomEpreuve: string;
  coefficient: number;
  noteObtenue: number;
  dateEpreuve: string;
  candidatId: number;
}

export interface EntretienRequest {
  dateHeure: string;
  lieuOuLien: string;
  noteEntretien: number;
  appreciationGlobale: string;
  candidatId: number;
}

// ===================== SESSIONS TYPES =====================

export interface SessionsKpi {
  totalSessionsActives: number;
  totalCandidatures: number;
  tauxRemplissage: number;
  prochaineCloture: string;
}

export interface SessionRow {
  id: number;
  libelle: string;
  dateOuverture: string;
  dateFermeture: string;
  estOuverte: boolean;
  quotaMax: number;
  quotaActuel: number;
  status: string;
  nombreCandidatures: number;
  tauxRemplissage: number;
  program: Program | null;
}

export interface Program {
  id: number;
  title: string;
  level: string;
  department: string;
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
  noteEntretien: number;
  appreciationGlobale: string;
  candidatId: number;
}

// ===================== DASHBOARD TYPES =====================

export interface DashboardOverview {
  sessionActive: SessionSummary | null;
  totalCandidatures: number;
  candidaturesValidees: number;
  candidaturesEnAttente: number;
  candidaturesRejetees: number;
  candidaturesRecentes: CandidatureResume[];
  testsAcademiques: TestSummary[];
  entretiens: EntretienSummary[];
}

export interface SessionSummary {
  id: number;
  libelle: string;
  estOuverte: boolean;
  dateOuverture: string;
  dateFermeture: string;
  quotaMax: number;
  quotaActuel: number;
}

export interface CandidatureResume {
  id: number;
  numeroDossier: string;
  nomCandidat: string;
  prenomCandidat: string;
  email: string;
  statut: string;
  formation: string;
  dateSoumission: string;
}

export interface TestSummary {
  id: number;
  nomEpreuve: string;
  dateEpreuve: string;
  nombreCandidats: number;
  seuilReussite: number;
}

export interface EntretienSummary {
  id: number;
  commission: string;
  salle: string;
  jury: string;
  dateEntretien: string;
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

  // ===================== SETTINGS =====================

  getProgramsFromSessions(): Observable<Program[]> {
    return this.http.get<Program[]>(`${this.baseUrl}/sessions/programs`);
  }

  getPrograms(): Observable<Program[]> {
    return this.http.get<Program[]>(`${this.baseUrl}/settings/programs`);
  }

  updateProgram(id: number, body: Program): Observable<Program> {
    return this.http.put<Program>(`${this.baseUrl}/settings/programs/${id}`, body);
  }

  deleteProgram(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/settings/programs/${id}`);
  }

  getRules(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/settings/rules`);
  }

  updateRules(body: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/settings/rules`, body);
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

  sendReminder(): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/applications/reminder-incomplets`, {});
  }

  exportApplications(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/applications/export`, { responseType: 'blob' });
  }

  updateDocumentStatus(applicationId: number, pieceId: number, valide: boolean): Observable<any> {
    return this.http.patch<any>(`${this.baseUrl}/applications/${applicationId}/documents/status`, { pieceId, valide });
  }

  // ===================== SESSIONS =====================

  getSessionKpis(): Observable<SessionsKpi> {
    return this.http.get<SessionsKpi>(`${this.baseUrl}/sessions/kpis`);
  }

  listSessions(filters?: { q?: string; status?: string; level?: string }): Observable<SessionRow[]> {
    let params = new (window as any).URLSearchParams();
    if (filters) {
      if (filters.q) params.set('q', filters.q);
      if (filters.status) params.set('status', filters.status);
      if (filters.level) params.set('level', filters.level);
    }
    return this.http.get<SessionRow[]>(`${this.baseUrl}/sessions?${params.toString()}`);
  }

  createSessionApi(body: any): Observable<SessionRow> {
    return this.http.post<SessionRow>(`${this.baseUrl}/sessions`, body);
  }

  updateSession(id: number, body: any): Observable<SessionRow> {
    return this.http.put<SessionRow>(`${this.baseUrl}/sessions/${id}`, body);
  }

  changeSessionStatus(id: number, status: string): Observable<SessionRow> {
    return this.http.patch<SessionRow>(`${this.baseUrl}/sessions/${id}/status`, { status });
  }

  deleteSession(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/sessions/${id}`);
  }

  duplicateSession(id: number): Observable<SessionRow> {
    return this.http.post<SessionRow>(`${this.baseUrl}/sessions/${id}/duplicate`, {});
  }

  exportSessions(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/sessions/export`, { responseType: 'blob' });
  }

  // ===================== DASHBOARD =====================

  getDashboardOverview(): Observable<DashboardOverview> {
    return this.http.get<DashboardOverview>(`${this.baseUrl}/dashboard/overview`);
  }

  createSession(body: { libelle: string; dateOuverture: string; dateFermeture: string; quotaMax: number }): Observable<SessionSummary> {
    return this.http.post<SessionSummary>(`${this.baseUrl}/dashboard/sessions/new`, body);
  }

  closeSession(id: number): Observable<SessionSummary> {
    return this.http.patch<SessionSummary>(`${this.baseUrl}/dashboard/sessions/${id}/close`, {});
  }

  getRecentApplications(limit: number = 5): Observable<CandidatureResume[]> {
    return this.http.get<CandidatureResume[]>(`${this.baseUrl}/dashboard/applications/recent?limit=${limit}`);
  }

  searchApplications(q: string): Observable<CandidatureResume[]> {
    return this.http.get<CandidatureResume[]>(`${this.baseUrl}/dashboard/applications/search?q=${encodeURIComponent(q)}`);
  }

  // ===================== PUBLIC =====================

  getCataloguePublic(): Observable<SessionCandidatureResponse[]> {
    return this.http.get<SessionCandidatureResponse[]>(`${this.baseUrl}/recrutement/public/catalogue`);
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

  getMesCandidatures(): Observable<CandidatureResponse[]> {
    return this.http.get<CandidatureResponse[]>(`${this.baseUrl}/recrutement/candidat/candidatures`);
  }

  getCandidature(id: number): Observable<CandidatureResponse> {
    return this.http.get<CandidatureResponse>(`${this.baseUrl}/recrutement/candidat/candidature/${id}`);
  }

  suivreAvancement(numeroDossier: string): Observable<CandidatureResponse> {
    return this.http.get<CandidatureResponse>(`${this.baseUrl}/recrutement/candidat/suivi/${numeroDossier}`);
  }

  // ===================== ADMIN =====================

  // Sessions
  getAllSessions(): Observable<SessionCandidatureResponse[]> {
    return this.http.get<SessionCandidatureResponse[]>(`${this.baseUrl}/admin/recrutement/sessions`);
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

  // Candidatures admin
  getAllCandidatures(): Observable<CandidatureResponse[]> {
    return this.http.get<CandidatureResponse[]>(`${this.baseUrl}/admin/recrutement/candidatures`);
  }

  getCandidatureAdmin(id: number): Observable<CandidatureResponse> {
    return this.http.get<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}`);
  }

  validerPreselection(id: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}/valider-preselection`, {});
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

  simulerSuccesPaiement(id: number): Observable<PaiementResponse> {
    return this.http.put<PaiementResponse>(`${this.baseUrl}/admin/recrutement/paiements/${id}/simuler-succes`, {});
  }

  // Tests Académiques
  getAllTests(): Observable<TestAcademiqueResponse[]> {
    return this.http.get<TestAcademiqueResponse[]>(`${this.baseUrl}/admin/recrutement/tests`);
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
