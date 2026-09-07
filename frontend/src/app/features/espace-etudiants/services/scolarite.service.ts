import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Camarade,
  CoursEtudiant,
  MaPromotion,
  SeanceEtudiant,
  ReleveDelivre,
  ReleveNotes,
  Reprise,
  SupportEtudiant,
} from '../models/scolarite.models';

/**
 * Scolarité de l'étudiant connecté.
 *
 * <p>Contrairement à {@link FormationService}, qui expose le catalogue complet,
 * ce service ne renvoie que les cours autorisés — ceux au programme de la
 * formation suivie par la promotion de l'étudiant.
 */
@Injectable({ providedIn: 'root' })
export class ScolariteService {
  private api = `${environment.apiUrl}/espace-etudiant/scolarite`;

  constructor(private http: HttpClient) {}

  /** Promotion de l'étudiant et son matricule. */
  getMaPromotion(): Observable<MaPromotion> {
    return this.http.get<MaPromotion>(`${this.api}/ma-promotion`);
  }

  /** Camarades de la promotion de l'étudiant — nom et matricule seulement. */
  getCamarades(): Observable<Camarade[]> {
    return this.http.get<Camarade[]>(`${this.api}/ma-promotion/etudiants`);
  }

  /** Derniers supports ouverts, pour reprendre son travail. */
  getReprendre(): Observable<Reprise[]> {
    return this.http.get<Reprise[]>(`${this.api}/reprendre`);
  }

  /** Relevé de notes : les résultats obtenus par l'étudiant, groupés par cours. */
  /** Relevés officiels délivrés par la direction pédagogique : figés, référencés. */
  getMesReleves(): Observable<ReleveDelivre[]> {
    return this.http.get<ReleveDelivre[]>(`${this.api}/mes-releves`);
  }

  /** PDF officiel d'un relevé délivré (Blob : nom de fichier posé par le serveur). */
  telechargerRelevePdf(id: number): Observable<Blob> {
    return this.http.get(`${this.api}/mes-releves/${id}/pdf`, { responseType: 'blob' });
  }

  getMesNotes(): Observable<ReleveNotes> {
    return this.http.get<ReleveNotes>(`${this.api}/mes-notes`);
  }

  getMesCours(): Observable<CoursEtudiant[]> {
    return this.http.get<CoursEtudiant[]>(`${this.api}/mes-cours`);
  }

  getCours(coursId: number): Observable<CoursEtudiant> {
    return this.http.get<CoursEtudiant>(`${this.api}/mes-cours/${coursId}`);
  }

  getSupports(coursId: number): Observable<SupportEtudiant[]> {
    return this.http.get<SupportEtudiant[]>(`${this.api}/mes-cours/${coursId}/supports`);
  }

  /** Marque un support comme consulté et renvoie la progression recalculée. */
  marquerConsulte(supportId: number): Observable<{ consulte: boolean; progression: number }> {
    return this.http.post<{ consulte: boolean; progression: number }>(
      `${this.api}/supports/${supportId}/consulte`,
      null
    );
  }

  /** Calendrier académique : les séances des cours de l'étudiant. */
  getMonCalendrier(): Observable<SeanceEtudiant[]> {
    return this.http.get<SeanceEtudiant[]>(`${this.api}/mon-calendrier`);
  }
}
