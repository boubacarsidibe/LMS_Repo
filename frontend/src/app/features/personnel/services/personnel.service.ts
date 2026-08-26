import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Activite, ActiviteSaisie, Departement } from '../models/personnel.models';

/**
 * Suivi des activités du personnel INPG (TDR §I). Commun aux trois espaces
 * internes (formateur, pédagogie, admission) — un seul service, une seule
 * modélisation, pas de duplication par rôle.
 */
@Injectable({ providedIn: 'root' })
export class PersonnelService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/personnel`;

  getDepartements(): Observable<Departement[]> {
    return this.http.get<Departement[]>(`${this.api}/departements`);
  }

  /** Réservé à la pédagogie côté serveur. */
  creerDepartement(code: string, libelle: string): Observable<Departement> {
    return this.http.post<Departement>(`${this.api}/departements`, { code, libelle });
  }

  getActivites(mois: number, annee: number, departementId?: number): Observable<Activite[]> {
    let url = `${this.api}/activites?mois=${mois}&annee=${annee}`;
    if (departementId) {
      url += `&departementId=${departementId}`;
    }
    return this.http.get<Activite[]>(url);
  }

  getMesActivites(): Observable<Activite[]> {
    return this.http.get<Activite[]>(`${this.api}/activites/mes-activites`);
  }

  saisirActivite(saisie: ActiviteSaisie): Observable<Activite> {
    return this.http.post<Activite>(`${this.api}/activites`, saisie);
  }

  /** L'auteur peut retirer sa propre saisie ; la pédagogie et l'admission peuvent modérer celle d'un tiers. */
  supprimerActivite(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/activites/${id}`);
  }

  urlRapportDepartement(departementId: number, mois: number, annee: number): string {
    return `/api/personnel/rapports/departement/${departementId}?mois=${mois}&annee=${annee}`;
  }

  urlRapportGlobal(mois: number, annee: number): string {
    return `/api/personnel/rapports/global?mois=${mois}&annee=${annee}`;
  }
}
