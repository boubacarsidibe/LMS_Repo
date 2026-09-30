import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  CandidatureFormateur,
  DecisionCandidatureFormateur,
  OffreFormateur,
  OffreFormateurSaisie,
  StatutCandidatureFormateur,
} from '../models/recrutement-formateur.models';

/** Volet responsable admission du recrutement des formateurs (QMS-007 §1, §3, §4). */
@Injectable({ providedIn: 'root' })
export class AdminRecrutementFormateurService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/admin/recrutement-formateurs`;

  // ----- Offres -----
  getOffres(): Observable<OffreFormateur[]> {
    return this.http.get<OffreFormateur[]>(`${this.api}/offres`);
  }

  getOffre(id: number): Observable<OffreFormateur> {
    return this.http.get<OffreFormateur>(`${this.api}/offres/${id}`);
  }

  creerOffre(donnees: OffreFormateurSaisie): Observable<OffreFormateur> {
    return this.http.post<OffreFormateur>(`${this.api}/offres`, donnees);
  }

  modifierOffre(id: number, donnees: OffreFormateurSaisie): Observable<OffreFormateur> {
    return this.http.put<OffreFormateur>(`${this.api}/offres/${id}`, donnees);
  }

  publierOffre(id: number): Observable<OffreFormateur> {
    return this.http.put<OffreFormateur>(`${this.api}/offres/${id}/publier`, {});
  }

  cloturerOffre(id: number): Observable<OffreFormateur> {
    return this.http.put<OffreFormateur>(`${this.api}/offres/${id}/cloturer`, {});
  }

  supprimerOffre(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/offres/${id}`);
  }

  // ----- Candidatures -----
  getCandidatures(filtres?: { offreId?: number; statut?: StatutCandidatureFormateur }): Observable<CandidatureFormateur[]> {
    const params: Record<string, string> = {};
    if (filtres?.offreId) params['offreId'] = String(filtres.offreId);
    if (filtres?.statut) params['statut'] = filtres.statut;
    return this.http.get<CandidatureFormateur[]>(`${this.api}/candidatures`, { params });
  }

  getCandidature(id: number): Observable<CandidatureFormateur> {
    return this.http.get<CandidatureFormateur>(`${this.api}/candidatures/${id}`);
  }

  changerStatut(id: number, statut: StatutCandidatureFormateur): Observable<CandidatureFormateur> {
    return this.http.put<CandidatureFormateur>(`${this.api}/candidatures/${id}/statut`, { statut });
  }

  decider(id: number, decision: DecisionCandidatureFormateur, motif: string | null): Observable<CandidatureFormateur> {
    return this.http.put<CandidatureFormateur>(`${this.api}/candidatures/${id}/decision`, { decision, motif });
  }

  urlFichierPiece(candidatureId: number, pieceId: number): string {
    return `/api/admin/recrutement-formateurs/candidatures/${candidatureId}/pieces/${pieceId}/fichier`;
  }
}
