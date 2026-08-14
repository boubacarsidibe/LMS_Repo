import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Cohorte } from '../models/cohorte';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CohorteService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Cohorte[]> {
    return this.http.get<Cohorte[]>(`${this.api}/cohortes`);
  }
}
