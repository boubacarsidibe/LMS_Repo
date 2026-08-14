import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Etudiant } from '../data/etudiants.mock';

export interface EtudiantIdentite {
  nom: string;
  prenom: string;
  telephone: string;
}

export interface EtudiantProfile {
  userId: number;
  username: string;
  email: string;
  etudiant?: { id: number } & EtudiantIdentite;
}

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

  /** Profil de l'étudiant actuellement connecté (pour la salutation du dashboard). */
  getMyProfile(): Observable<EtudiantProfile> {
    return this.http.get<EtudiantProfile>(`${this.api}/profile`);
  }

  /**
   * Met à jour l'identité de l'étudiant connecté. L'email n'est pas modifiable :
   * il sert d'identifiant de connexion.
   */
  updateMyProfile(identite: EtudiantIdentite): Observable<EtudiantProfile> {
    return this.http.put<EtudiantProfile>(`${this.api}/profile`, identite);
  }
}
