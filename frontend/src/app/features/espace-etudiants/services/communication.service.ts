import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Annonce,
  ContenuAlbum,
  EvaluationCours,
  FilForum,
  ForumMessage,
  ForumSujet,
  AlbumPhoto,
  SyntheseEvaluation,
} from '../models/scolarite.models';

/**
 * Communications de l'espace étudiant : annonces de l'INPG, forum, photothèque
 * et évaluations de satisfaction.
 */
@Injectable({ providedIn: 'root' })
export class CommunicationService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  // ---------- Annonces, messages et mémos ----------

  getAnnonces(): Observable<Annonce[]> {
    return this.http.get<Annonce[]>(`${this.api}/annonces`);
  }

  countAnnoncesNonLues(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.api}/annonces/count-non-lues`);
  }

  marquerAnnonceLue(id: number): Observable<{ lu: boolean }> {
    return this.http.post<{ lu: boolean }>(`${this.api}/annonces/${id}/lu`, null);
  }

  marquerToutesAnnoncesLues(): Observable<{ marquees: number }> {
    return this.http.post<{ marquees: number }>(`${this.api}/annonces/tout-lu`, null);
  }

  // ---------- Forum ----------

  getSujets(): Observable<ForumSujet[]> {
    return this.http.get<ForumSujet[]>(`${this.api}/forum/sujets`);
  }

  getFil(sujetId: number): Observable<FilForum> {
    return this.http.get<FilForum>(`${this.api}/forum/sujets/${sujetId}`);
  }

  ouvrirSujet(payload: {
    titre: string;
    coursId: number | null;
    premierMessage: string;
  }): Observable<ForumSujet> {
    return this.http.post<ForumSujet>(`${this.api}/forum/sujets`, payload);
  }

  repondre(sujetId: number, contenu: string): Observable<ForumMessage> {
    return this.http.post<ForumMessage>(`${this.api}/forum/sujets/${sujetId}/messages`, {
      contenu,
    });
  }

  supprimerMessage(messageId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/forum/messages/${messageId}`);
  }

  // ---------- Photothèque ----------

  getAlbums(): Observable<AlbumPhoto[]> {
    return this.http.get<AlbumPhoto[]>(`${this.api}/photheque/albums`);
  }

  getAlbum(id: number): Observable<ContenuAlbum> {
    return this.http.get<ContenuAlbum>(`${this.api}/photheque/albums/${id}`);
  }

  // ---------- Évaluations de satisfaction ----------

  getMesEvaluations(): Observable<EvaluationCours[]> {
    return this.http.get<EvaluationCours[]>(`${this.api}/evaluations/mes-evaluations`);
  }

  evaluerCours(
    coursId: number,
    note: number,
    commentaire: string
  ): Observable<EvaluationCours> {
    return this.http.post<EvaluationCours>(`${this.api}/evaluations/cours/${coursId}`, {
      note,
      commentaire,
    });
  }

  getSyntheseCours(coursId: number): Observable<SyntheseEvaluation> {
    return this.http.get<SyntheseEvaluation>(`${this.api}/evaluations/cours/${coursId}/synthese`);
  }
}
