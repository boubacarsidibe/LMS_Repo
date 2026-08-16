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

  getPromotions(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(`${this.api}/promotions`);
  }

  getCours(): Observable<Cours[]> {
    return this.http.get<Cours[]>(`${this.api}/cours`);
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

  publierSupport(coursId: number, typeRessource: string, fichier: File): Observable<SupportDetail> {
    const payload = new FormData();
    payload.append('coursId', String(coursId));
    payload.append('typeRessource', typeRessource);
    payload.append('fichier', fichier);
    return this.http.post<SupportDetail>(`${this.api}/supports`, payload);
  }

  supprimerSupport(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/supports/${id}`);
  }
}
