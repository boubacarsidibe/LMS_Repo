import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Cours, CoursCreation } from '../models/cours';

@Injectable({ providedIn: 'root' })
export class CoursService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Cours[]> {
    return this.http.get<Cours[]>(`${this.api}/formateur/cours`);
  }

  create(data: CoursCreation): Observable<Cours> {
    return this.http.post<Cours>(`${this.api}/formateur/cours`, data);
  }

  update(coursId: number, data: CoursCreation): Observable<Cours> {
    return this.http.patch<Cours>(`${this.api}/formateur/cours/${coursId}`, data);
  }

  updateCohortes(coursId: number, cohorteIds: number[]): Observable<Cours> {
    return this.http.patch<Cours>(`${this.api}/formateur/cours/${coursId}/cohortes`, { cohorteIds });
  }
}
