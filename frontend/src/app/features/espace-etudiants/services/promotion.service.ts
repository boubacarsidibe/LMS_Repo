import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Promotion } from '../models/promotion';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PromotionService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  /** Promotions accessibles à partir des cours attribués au formateur connecté. */
  getMesPromotionsFormateur(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(`${this.api}/formateur/cohortes`);
  }
}
