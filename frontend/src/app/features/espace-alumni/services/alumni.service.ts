import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  AlumniProfil,
  AlumniProfilUpdate,
  DocumentPartage,
  EnqueteSuivi,
  FilForum,
  ForumMessage,
  ForumSujet,
  Mentionnable,
  ReleveDelivre,
  ReponseEnqueteSuiviSaisie,
  SituationProfessionnelle,
  SituationProfessionnelleSaisie,
} from '../models/alumni.models';

/**
 * Espace Alumni de l'alumni connecté : sa fiche, son historique professionnel,
 * ses relevés délivrés, le forum alumni et le partage de documents (TDR §C).
 */
@Injectable({ providedIn: 'root' })
export class AlumniService {
  private api = `${environment.apiUrl}/espace-alumni`;

  constructor(private http: HttpClient) {}

  getMonProfil(): Observable<AlumniProfil> {
    return this.http.get<AlumniProfil>(`${this.api}/mon-profil`);
  }

  mettreAJourProfil(donnees: AlumniProfilUpdate): Observable<AlumniProfil> {
    return this.http.put<AlumniProfil>(`${this.api}/mon-profil`, donnees);
  }

  getMesSituations(): Observable<SituationProfessionnelle[]> {
    return this.http.get<SituationProfessionnelle[]>(`${this.api}/mes-situations`);
  }

  ajouterSituation(donnees: SituationProfessionnelleSaisie): Observable<SituationProfessionnelle> {
    return this.http.post<SituationProfessionnelle>(`${this.api}/mes-situations`, donnees);
  }

  modifierSituation(id: number, donnees: SituationProfessionnelleSaisie): Observable<SituationProfessionnelle> {
    return this.http.put<SituationProfessionnelle>(`${this.api}/mes-situations/${id}`, donnees);
  }

  supprimerSituation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/mes-situations/${id}`);
  }

  getMesReleves(): Observable<ReleveDelivre[]> {
    return this.http.get<ReleveDelivre[]>(`${this.api}/mes-releves`);
  }

  // ---------- Enquêtes de suivi ----------

  getEnquetes(): Observable<EnqueteSuivi[]> {
    return this.http.get<EnqueteSuivi[]>(`${this.api}/enquetes`);
  }

  repondreEnquete(id: number, donnees: ReponseEnqueteSuiviSaisie): Observable<unknown> {
    return this.http.post(`${this.api}/enquetes/${id}/reponse`, donnees);
  }

  // ---------- Forum ----------

  getSujets(): Observable<ForumSujet[]> {
    return this.http.get<ForumSujet[]>(`${this.api}/forum/sujets`);
  }

  getFil(sujetId: number): Observable<FilForum> {
    return this.http.get<FilForum>(`${this.api}/forum/sujets/${sujetId}`);
  }

  ouvrirSujet(titre: string, premierMessage: string): Observable<ForumSujet> {
    return this.http.post<ForumSujet>(`${this.api}/forum/sujets`, {
      titre,
      coursId: null,
      premierMessage,
    });
  }

  citables(sujetId: number): Observable<Mentionnable[]> {
    return this.http.get<Mentionnable[]>(`${this.api}/forum/sujets/${sujetId}/citables`);
  }

  repondre(sujetId: number, contenu: string, citeIds: number[] = []): Observable<ForumMessage> {
    return this.http.post<ForumMessage>(`${this.api}/forum/sujets/${sujetId}/messages`, {
      contenu,
      citeIds,
    });
  }

  supprimerMessage(messageId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/forum/messages/${messageId}`);
  }

  // ---------- Documents partagés ----------

  getDocuments(): Observable<DocumentPartage[]> {
    return this.http.get<DocumentPartage[]>(`${this.api}/documents`);
  }

  ajouterDocument(titre: string, description: string | null, fichier: File): Observable<DocumentPartage> {
    const donnees = new FormData();
    donnees.append('titre', titre);
    if (description) {
      donnees.append('description', description);
    }
    donnees.append('fichier', fichier);
    return this.http.post<DocumentPartage>(`${this.api}/documents`, donnees);
  }

  supprimerDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/documents/${id}`);
  }

  /** URL relative du fichier, à passer à {@code MediaProtegeService.ouvrir(...)}. */
  urlFichierDocument(id: number): string {
    return `/api/espace-alumni/documents/${id}/fichier`;
  }
}
