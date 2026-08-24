import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Certificat } from '../models/certificat';

@Injectable({ providedIn: 'root' })
export class CertificatService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Certificat[]> {
    return this.http.get<Certificat[]>(`${this.api}/certificats`);
  }

  telechargerPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.api}/certificats/${id}/pdf`, { responseType: 'blob' });
  }
}
