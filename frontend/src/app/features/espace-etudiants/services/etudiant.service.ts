import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Etudiant } from '../models/etudiant';

@Injectable({ providedIn: 'root' })
export class EtudiantService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Etudiant[]> {
    return this.http.get<Etudiant[]>(`${this.api}/formateur/etudiants`);
  }

  getById(id: number): Observable<Etudiant> {
    return this.http.get<Etudiant>(`${this.api}/formateur/etudiants/${id}`);
  }

  assignerCohorte(id: number, cohorteId: number): Observable<Etudiant> {
    return this.http.post<Etudiant>(`${this.api}/formateur/etudiants/${id}/cohorte`, { cohorteId });
  }

  contacter(id: number, objet: string, message: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.api}/formateur/etudiants/${id}/contacter`, { objet, message });
  }

  relancer(etudiantIds: number[], objet: string, message: string): Observable<{ envoyes: number; echecs: string[] }> {
    return this.http.post<{ envoyes: number; echecs: string[] }>(`${this.api}/formateur/etudiants/relance`, { etudiantIds, objet, message });
  }
}
