import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Cours, CoursCreation } from '../models/cours';
import { Support } from '../models/support';

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
  getById(id: number): Observable<Cours> {
    return this.http.get<Cours>(`${this.api}/formateur/cours/${id}`);
  }

  update(id: number, data: CoursCreation): Observable<Cours> {
    return this.http.put<Cours>(`${this.api}/formateur/cours/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/formateur/cours/${id}`);
  }

  getSupports(id: number): Observable<Support[]> {
    return this.http.get<Support[]>(`${this.api}/formateur/cours/${id}/supports`);
  }

  uploadImage(id: number, image: File): Observable<Cours> {
  const formData = new FormData();
  formData.append('image', image);
  return this.http.post<Cours>(`${this.api}/formateur/cours/${id}/image`, formData);
  }
}