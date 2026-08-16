import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { RetourEvaluation } from '../models/retour-evaluation';

interface RetourEvaluationDto {
  id: number;
  note: number;
  commentaire: string;
  date: string;
  coursId: number;
  cours: string;
  statut: 'VALIDE' | 'ACTION_REQUISE';
  epingle: boolean;
}

function versRetour(dto: RetourEvaluationDto): RetourEvaluation {
  return {
    id: dto.id,
    note: dto.note,
    commentaire: dto.commentaire,
    date: new Date(dto.date).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }),
    // La date brute est conservée : une fois localisée, elle n'est plus triable.
    dateIso: dto.date,
    coursId: dto.coursId,
    cours: dto.cours,
    statut: dto.statut === 'ACTION_REQUISE' ? 'action-requise' : 'valide',
    epingle: dto.epingle,
  };
}

@Injectable({ providedIn: 'root' })
export class RetourService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<RetourEvaluation[]> {
    return this.http.get<RetourEvaluationDto[]>(`${this.api}/formateur/retours`).pipe(
      map((list) => list.map(versRetour))
    );
  }

  togglePin(id: number): Observable<RetourEvaluation> {
    return this.http.patch<RetourEvaluationDto>(`${this.api}/formateur/retours/${id}/epingle`, {}).pipe(
      map(versRetour)
    );
  }
}
