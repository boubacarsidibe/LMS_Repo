import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SujetForum } from '../models/sujet';

@Injectable({ providedIn: 'root' })
export class SujetService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<SujetForum[]> {
    return this.http.get<SujetForum[]>(`${this.api}/sujets`);
  }

  repondre(sujetId: number, contenu: string, marquerCommeResolu: boolean, fichier: File | null): Observable<SujetForum> {
    const payload = new FormData();
    payload.append('contenu', contenu);
    payload.append('marquerCommeResolu', String(marquerCommeResolu));
    if (fichier) {
      payload.append('fichier', fichier);
    }
    return this.http.post<SujetForum>(`${this.api}/sujets/${sujetId}/messages`, payload);
  }

  telechargerPieceJointe(messageId: number): Observable<Blob> {
    return this.http.get(`${this.api}/sujets/messages/${messageId}/piece-jointe`, { responseType: 'blob' });
  }

  toggleUrgence(sujetId: number): Observable<SujetForum> {
    return this.http.patch<SujetForum>(`${this.api}/formateur/sujets/${sujetId}/urgence`, {});
  }

  marquerResolu(sujetId: number): Observable<SujetForum> {
    return this.http.patch<SujetForum>(`${this.api}/formateur/sujets/${sujetId}/resoudre`, {});
  }

  supprimer(sujetId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/formateur/sujets/${sujetId}`);
  }
}
