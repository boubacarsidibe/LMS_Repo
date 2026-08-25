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
