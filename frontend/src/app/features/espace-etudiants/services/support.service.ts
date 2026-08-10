import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Support } from '../models/support';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SupportService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getCourseSupports(): Observable<Support[]> {
    return this.http.get<Support[]>(`${this.api}/supports/cours`);
  }

  getPedagogicalSupports(): Observable<Support[]> {
    return this.http.get<Support[]>(`${this.api}/supports/pedagogiques`);
  }

  getPedagogicalById(id: string) {
    return this.http.get<Support>(`${this.api}/supports/pedagogiques/${id}`);
  }

  uploadCourseSupport(
    coursId: number,
    typeRessource: string,
    datePublication: string,
    fichier: File
  ): Observable<Support> {
    const payload = new FormData();
    payload.append('coursId', String(coursId));
    payload.append('typeRessource', typeRessource);
    payload.append('datePublication', datePublication);
    payload.append('fichier', fichier);
    return this.http.post<Support>(`${this.api}/formateur/supports`, payload);
  }
}
