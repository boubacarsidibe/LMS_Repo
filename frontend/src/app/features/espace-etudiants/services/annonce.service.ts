import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Annonce, AnnonceCreation } from '../models/annonce';
import { Promotion } from '../models/promotion';

@Injectable({ providedIn: 'root' })
export class AnnonceService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Annonce[]> {
    return this.http.get<Annonce[]>(`${this.api}/annonces`);
  }

  create(data: AnnonceCreation): Observable<Annonce> {
    return this.http.post<Annonce>(`${this.api}/formateur/annonces`, data);
  }

  getMesPromotions(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(`${this.api}/formateur/cohortes`);
  }
}
