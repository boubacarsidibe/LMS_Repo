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

export interface EntretienResponse {
  id: number;
  dateHeure: string;
  lieuOuLien: string;
  noteEntretien: number;
  appreciationGlobale: string;
  candidatId: number;
}

@Injectable({
  providedIn: 'root'
})
export class RecrutementService {

  private baseUrl = '/api';

  constructor(private http: HttpClient) { }

  // ===================== PUBLIC =====================

  getCataloguePublic(): Observable<SessionCandidatureResponse[]> {
    return this.http.get<SessionCandidatureResponse[]>(`${this.baseUrl}/recrutement/public/catalogue`);
  }

  poserQuestion(email: string, message: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/recrutement/public/question`, { email, message });
  }

  // ===================== CANDIDAT =====================

  creerBrouillon(request: CandidatureRequest): Observable<CandidatureResponse> {
    return this.http.post<CandidatureResponse>(`${this.baseUrl}/recrutement/candidat/candidature/brouillon`, request);
  }

  soumettreCandidature(id: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/recrutement/candidat/candidature/${id}/soumettre`, {});
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

  admettreCandidat(id: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.baseUrl}/admin/recrutement/candidatures/${id}/admettre`, {});
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
}