import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  DemandeAccreditation,
  DemandeAccreditationSaisie,
  EcoleAccreditee,
  PieceAccreditation,
  TypeDocumentAccreditation,
} from '../models/accreditation.models';

/** Espace École de formation : soumission et suivi d'une demande d'accréditation (TDR §F). */
@Injectable({ providedIn: 'root' })
export class AccreditationService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/accreditation`;

  soumettreDemande(donnees: DemandeAccreditationSaisie): Observable<DemandeAccreditation> {
    return this.http.post<DemandeAccreditation>(`${this.api}/ecole/demande`, donnees);
  }

  getMesDemandes(): Observable<DemandeAccreditation[]> {
    return this.http.get<DemandeAccreditation[]>(`${this.api}/ecole/mes-demandes`);
  }

  getDemande(id: number): Observable<DemandeAccreditation> {
    return this.http.get<DemandeAccreditation>(`${this.api}/ecole/demande/${id}`);
  }

  televerserPiece(demandeId: number, fichier: File, typeDocument: TypeDocumentAccreditation): Observable<PieceAccreditation> {
    const donnees = new FormData();
    donnees.append('typeDocument', typeDocument);
    donnees.append('fichier', fichier);
    return this.http.post<PieceAccreditation>(`${this.api}/ecole/demande/${demandeId}/pieces`, donnees);
  }

  supprimerPiece(demandeId: number, pieceId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/ecole/demande/${demandeId}/pieces/${pieceId}`);
  }

  urlFichierPiece(demandeId: number, pieceId: number): string {
    return `/api/accreditation/ecole/demande/${demandeId}/pieces/${pieceId}/fichier`;
  }

  getEcolesAccreditees(): Observable<EcoleAccreditee[]> {
    return this.http.get<EcoleAccreditee[]>(`${this.api}/public/ecoles-accreditees`);
  }

  /** PDF de l'attestation d'accréditation (le jeton passe par l'intercepteur). */
  telechargerAttestationPdf(demandeId: number): Observable<Blob> {
    return this.http.get(`${this.api}/ecole/demande/${demandeId}/attestation/pdf`, { responseType: 'blob' });
  }
}
