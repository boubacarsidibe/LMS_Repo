import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Certificat, CertificatCreation } from '../models/certificat';

@Injectable({ providedIn: 'root' })
export class CertificatService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Certificat[]> {
    return this.http.get<Certificat[]>(`${this.api}/certificats`);
  }

  create(data: CertificatCreation): Observable<Certificat> {
    return this.http.post<Certificat>(`${this.api}/formateur/certificats`, data);
  }

  telechargerPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.api}/certificats/${id}/pdf`, { responseType: 'blob' });
  }
}
