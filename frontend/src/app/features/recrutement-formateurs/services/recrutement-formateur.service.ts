import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  CandidatureFormateur,
  CandidatureFormateurSaisie,
  OffreFormateur,
  PieceCandidatureFormateur,
  TypeDocumentCandidatureFormateur,
} from '../models/recrutement-formateur.models';

/** Espace candidat formateur : consultation des offres, dépôt et suivi d'une candidature (QMS-007). */
@Injectable({ providedIn: 'root' })
export class RecrutementFormateurService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/recrutement-formateurs`;

  getOffres(): Observable<OffreFormateur[]> {
    return this.http.get<OffreFormateur[]>(`${this.api}/public/offres`);
  }

  getOffre(id: number): Observable<OffreFormateur> {
    return this.http.get<OffreFormateur>(`${this.api}/public/offres/${id}`);
  }

  /** Domaines d'expertise proposés dans le dossier, dérivés du catalogue (offres + formations). */
  getDomainesExpertise(): Observable<string[]> {
    return this.http.get<string[]>(`${this.api}/public/domaines`);
  }

  postuler(donnees: CandidatureFormateurSaisie): Observable<CandidatureFormateur> {
    return this.http.post<CandidatureFormateur>(`${this.api}/candidatures`, donnees);
  }

  getMesCandidatures(): Observable<CandidatureFormateur[]> {
    return this.http.get<CandidatureFormateur[]>(`${this.api}/mes-candidatures`);
  }

  getCandidature(id: number): Observable<CandidatureFormateur> {
    return this.http.get<CandidatureFormateur>(`${this.api}/candidatures/${id}`);
  }

  televerserPiece(
    candidatureId: number,
    fichier: File,
    typeDocument: TypeDocumentCandidatureFormateur
  ): Observable<PieceCandidatureFormateur> {
    const donnees = new FormData();
    donnees.append('typeDocument', typeDocument);
    donnees.append('fichier', fichier);
    return this.http.post<PieceCandidatureFormateur>(`${this.api}/candidatures/${candidatureId}/pieces`, donnees);
  }

  supprimerPiece(candidatureId: number, pieceId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/candidatures/${candidatureId}/pieces/${pieceId}`);
  }

  urlFichierPiece(candidatureId: number, pieceId: number): string {
    return `/api/recrutement-formateurs/candidatures/${candidatureId}/pieces/${pieceId}/fichier`;
  }
}
