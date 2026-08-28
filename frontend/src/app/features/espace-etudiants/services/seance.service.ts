import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Seance } from '../models/seance';

export interface SeanceCreation {
  coursId: number;
  titre: string;
  type: string;
  dateHeure: string;
  dureeMinutes: number;
  lieuOuLien?: string | null;
}

@Injectable({ providedIn: 'root' })
export class SeanceService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getByCours(coursId: number): Observable<Seance[]> {
    return this.http.get<Seance[]>(`${this.api}/formateur/cours/${coursId}/seances`);
  }

  /** Toutes les séances des cours du formateur connecté, tous cours confondus. */
  getMesSeances(): Observable<Seance[]> {
    return this.http.get<Seance[]>(`${this.api}/formateur/seances`);
  }

  creerSeance(data: SeanceCreation): Observable<Seance> {
    return this.http.post<Seance>(`${this.api}/formateur/seances`, data);
  }

  modifier(id: number, data: SeanceCreation): Observable<Seance> {
    return this.http.put<Seance>(`${this.api}/formateur/seances/${id}`, data);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/formateur/seances/${id}`);
  }

  /**
   * Creation rapide depuis la prise de presence : seul le titre et la date
   * comptent pour ce flux, le reste (type/duree/lieu) prend une valeur par
   * defaut raisonnable plutot que d'alourdir la modale de presence.
   */
  creer(coursId: number, titre: string, date: string): Observable<Seance> {
    return this.http.post<Seance>(`${this.api}/formateur/seances`, {
      coursId,
      titre,
      type: 'cours',
      dateHeure: `${date}T09:00:00`,
      dureeMinutes: 60,
      lieuOuLien: null,
    });
  }

  /**
   * Met à jour uniquement le lieu / lien de visioconférence d'une séance
   * existante : l'API attend le reste des champs, on les renvoie donc
   * inchangés plutôt que d'exposer un PATCH partiel côté backend.
   */
  mettreAJourLien(seance: Seance, lieuOuLien: string): Observable<Seance> {
    return this.http.put<Seance>(`${this.api}/formateur/seances/${seance.id}`, {
      coursId: seance.coursId,
      titre: seance.titre,
      type: seance.type,
      dateHeure: seance.dateHeure,
      dureeMinutes: seance.dureeMinutes,
      lieuOuLien: lieuOuLien.trim(),
    });
  }
}
