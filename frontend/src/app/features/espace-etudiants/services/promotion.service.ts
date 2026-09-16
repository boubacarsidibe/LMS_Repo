import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Promotion } from '../models/promotion';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PromotionService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  /**
   * Route héritée de « Cohorte », le nom d'avant la fusion du 14/08/2026
   * (voir migration V4__supprimer_vestiges_cohorte). Renommer l'URL est un
   * changement de contrat plus risqué que de renommer le code qui l'appelle.
   */
  getAll(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(`${this.api}/cohortes`);
  }

  /** Promotions accessibles à partir des cours attribués au formateur connecté. */
  getMesPromotionsFormateur(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(`${this.api}/formateur/cohortes`);
  }
}
