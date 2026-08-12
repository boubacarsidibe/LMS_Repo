import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface FormateurDashboard {
  nbCours: number;
  nbRessources: number;
}

export interface FormateurProfile {
  id: number;
  nom: string;
  email?: string;
  nbCours?: number;
}

@Injectable({ providedIn: 'root' })
export class FormateurService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<FormateurProfile> {
    return this.http.get<FormateurProfile>(`${this.api}/formateur/profile`);
  }

  getDashboard(): Observable<FormateurDashboard> {
    return this.http.get<FormateurDashboard>(`${this.api}/formateur/dashboard`);
  }

  getMyStudents(): Observable<any> {
    return this.http.get<any>(`${this.api}/formateur/etudiants`);
  }
}
