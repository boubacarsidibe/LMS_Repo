import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Seance } from '../models/seance';

@Injectable({ providedIn: 'root' })
export class SeanceService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getByCours(coursId: number): Observable<Seance[]> {
    return this.http.get<Seance[]>(`${this.api}/formateur/cours/${coursId}/seances`);
  }

  creer(coursId: number, titre: string, date: string): Observable<Seance> {
    return this.http.post<Seance>(`${this.api}/formateur/cours/${coursId}/seances`, { titre, date });
  }
}
