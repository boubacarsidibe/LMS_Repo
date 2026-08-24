import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  CritereAccreditation,
  DemandeAccreditation,
  StatutDemandeAccreditation,
} from '../../accreditation/models/accreditation.models';

/** Espace Évaluateur : consultation et décision sur les demandes d'accréditation (TDR §F). */
@Injectable({ providedIn: 'root' })
export class EvaluateurAccreditationService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/admin/accreditation`;

  getDemandes(statut?: StatutDemandeAccreditation): Observable<DemandeAccreditation[]> {
    const params = statut ? { params: { statut } } : {};
    return this.http.get<DemandeAccreditation[]>(`${this.api}/demandes`, params);
  }

  getDemande(id: number): Observable<DemandeAccreditation> {
    return this.http.get<DemandeAccreditation>(`${this.api}/demandes/${id}`);
  }

  urlFichierPiece(demandeId: number, pieceId: number): string {
    return `/api/admin/accreditation/demandes/${demandeId}/pieces/${pieceId}/fichier`;
  }

  prendreEnCharge(id: number): Observable<DemandeAccreditation> {
    return this.http.put<DemandeAccreditation>(`${this.api}/demandes/${id}/prendre-en-charge`, {});
  }

  accrediter(id: number, motif?: string): Observable<DemandeAccreditation> {
    return this.http.put<DemandeAccreditation>(`${this.api}/demandes/${id}/accrediter`, { motif: motif || null });
  }

  rejeter(id: number, motif: string): Observable<DemandeAccreditation> {
    return this.http.put<DemandeAccreditation>(`${this.api}/demandes/${id}/rejeter`, { motif });
  }

  getCriteres(): Observable<CritereAccreditation[]> {
    return this.http.get<CritereAccreditation[]>(`${this.api}/criteres`);
  }

  creerCritere(libelle: string, description: string | null): Observable<CritereAccreditation> {
    return this.http.post<CritereAccreditation>(`${this.api}/criteres`, { libelle, description });
  }

  desactiverCritere(id: number): Observable<CritereAccreditation> {
    return this.http.put<CritereAccreditation>(`${this.api}/criteres/${id}/desactiver`, {});
  }

  supprimerCritere(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/criteres/${id}`);
  }
}
