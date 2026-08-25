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
  /** Nom complet « Prénom Nom », pour l'affichage. */
  nom: string;
  prenom: string;
  /** Nom de famille seul — distinct de `nom`, qui est le nom complet. */
  nomFamille: string;
  telephone: string;
  email?: string;
  nbCours?: number;
}

/** Champs modifiables par le formateur. L'e-mail sert d'identifiant de connexion. */
export interface FormateurProfileUpdate {
  nom: string;
  prenom: string;
  telephone: string;
}

@Injectable({ providedIn: 'root' })
export class FormateurService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getProfile(): Observable<FormateurProfile> {
    return this.http.get<FormateurProfile>(`${this.api}/formateur/profile`);
  }

  /** Renvoie le profil complet à jour : l'écran se rafraîchit sans second appel. */
  updateProfile(modifications: FormateurProfileUpdate): Observable<FormateurProfile> {
    return this.http.put<FormateurProfile>(`${this.api}/formateur/profile`, modifications);
  }

  getDashboard(): Observable<FormateurDashboard> {
    return this.http.get<FormateurDashboard>(`${this.api}/formateur/dashboard`);
  }

  getMyStudents(): Observable<any> {
    return this.http.get<any>(`${this.api}/formateur/etudiants`);
  }
}
