import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  AlumniProfil,
  AlumniProfilUpdate,
  ReleveDelivre,
  SituationProfessionnelle,
  SituationProfessionnelleSaisie,
} from '../models/alumni.models';

/**
 * Espace Alumni de l'alumni connecté : sa fiche, son historique professionnel et
 * ses relevés délivrés (TDR §C).
 */
@Injectable({ providedIn: 'root' })
export class AlumniService {
  private api = `${environment.apiUrl}/espace-alumni`;

  constructor(private http: HttpClient) {}

  getMonProfil(): Observable<AlumniProfil> {
    return this.http.get<AlumniProfil>(`${this.api}/mon-profil`);
  }

  mettreAJourProfil(donnees: AlumniProfilUpdate): Observable<AlumniProfil> {
    return this.http.put<AlumniProfil>(`${this.api}/mon-profil`, donnees);
  }

  getMesSituations(): Observable<SituationProfessionnelle[]> {
    return this.http.get<SituationProfessionnelle[]>(`${this.api}/mes-situations`);
  }

  ajouterSituation(donnees: SituationProfessionnelleSaisie): Observable<SituationProfessionnelle> {
    return this.http.post<SituationProfessionnelle>(`${this.api}/mes-situations`, donnees);
  }

  modifierSituation(id: number, donnees: SituationProfessionnelleSaisie): Observable<SituationProfessionnelle> {
    return this.http.put<SituationProfessionnelle>(`${this.api}/mes-situations/${id}`, donnees);
  }

  supprimerSituation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/mes-situations/${id}`);
  }

  getMesReleves(): Observable<ReleveDelivre[]> {
    return this.http.get<ReleveDelivre[]>(`${this.api}/mes-releves`);
  }
}
