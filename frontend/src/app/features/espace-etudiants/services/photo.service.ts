import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Photo } from '../models/photo';

@Injectable({ providedIn: 'root' })
export class PhotoService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Photo[]> {
    return this.http.get<Photo[]>(`${this.api}/photos`);
  }

  upload(titre: string, cohorteId: number | null, fichier: File): Observable<Photo> {
    const formData = new FormData();
    formData.append('titre', titre);
    if (cohorteId) {
      formData.append('cohorteId', String(cohorteId));
    }
    formData.append('fichier', fichier);
    return this.http.post<Photo>(`${this.api}/formateur/photos`, formData);
  }
}
