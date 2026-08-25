import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { Evenement, EvenementCreation } from '../models/evenement';

interface EvenementDto {
  id: number;
  titre: string;
  date: string;
  heure: string;
  type: 'cours' | 'echeance' | 'tutorat';
  lienVisio?: string;
}

@Injectable({ providedIn: 'root' })
export class EvenementService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Evenement[]> {
    return this.http.get<EvenementDto[]>(`${this.api}/evenements`).pipe(
      map((list) => list.map((dto) => ({ ...dto, date: new Date(dto.date) })))
    );
  }

  create(data: EvenementCreation): Observable<Evenement> {
    return this.http.post<EvenementDto>(`${this.api}/formateur/evenements`, data).pipe(
      map((dto) => ({ ...dto, date: new Date(dto.date) }))
    );
  }

  update(id: number, data: EvenementCreation): Observable<Evenement> {
    return this.http.patch<EvenementDto>(`${this.api}/formateur/evenements/${id}`, data).pipe(
      map((dto) => ({ ...dto, date: new Date(dto.date) }))
    );
  }
}
