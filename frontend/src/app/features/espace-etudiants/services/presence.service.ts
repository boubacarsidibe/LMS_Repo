import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { PresenceEtudiant, PresenceHistorique } from '../models/presence';

@Injectable({ providedIn: 'root' })
export class PresenceService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getRosterSeance(seanceId: number): Observable<PresenceEtudiant[]> {
    return this.http.get<PresenceEtudiant[]>(`${this.api}/formateur/seances/${seanceId}/presences`);
  }

  enregistrer(seanceId: number, presences: { etudiantId: number; present: boolean }[]): Observable<void> {
    return this.http.post<void>(`${this.api}/formateur/seances/${seanceId}/presences`, { presences });
  }

  /** Envoie une demande de justification aux étudiants marqués absents à cette séance. */
  relancerAbsences(seanceId: number): Observable<{ envoyes: number }> {
    return this.http.post<{ envoyes: number }>(`${this.api}/formateur/seances/${seanceId}/relancer-absences`, {});
  }

  getByEtudiant(etudiantId: number): Observable<PresenceHistorique[]> {
    return this.http.get<PresenceHistorique[]>(`${this.api}/formateur/etudiants/${etudiantId}/presences`);
  }
}
