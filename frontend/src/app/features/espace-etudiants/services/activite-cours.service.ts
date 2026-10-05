import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ActiviteCours, ActiviteCreation, RemiseActivite } from '../models/activite-cours';

@Injectable({ providedIn: 'root' })
export class ActiviteCoursService {
  private readonly api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getFormateur(coursId: number): Observable<ActiviteCours[]> {
    return this.http.get<ActiviteCours[]>(`${this.api}/formateur/cours/${coursId}/activites`);
  }

  creer(coursId: number, activite: ActiviteCreation): Observable<ActiviteCours> {
    return this.http.post<ActiviteCours>(`${this.api}/formateur/cours/${coursId}/activites`, activite);
  }

  getEtudiant(coursId: number): Observable<ActiviteCours[]> {
    return this.http.get<ActiviteCours[]>(`${this.api}/cours/${coursId}/activites`);
  }

  remettre(activiteId: number, contenu: string): Observable<RemiseActivite> {
    return this.http.post<RemiseActivite>(`${this.api}/activites/${activiteId}/remises`, { contenu });
  }

  getRemises(activiteId: number): Observable<RemiseActivite[]> {
    return this.http.get<RemiseActivite[]>(`${this.api}/formateur/activites/${activiteId}/remises`);
  }

  corriger(remiseId: number, note: number, commentaire: string): Observable<RemiseActivite> {
    return this.http.put<RemiseActivite>(`${this.api}/formateur/remises/${remiseId}/note`, { note, commentaire });
  }
}
