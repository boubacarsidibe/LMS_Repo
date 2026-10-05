import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { SujetForum } from '../models/sujet';
import { ForumSujet } from '../models/scolarite.models';

@Injectable({ providedIn: 'root' })
export class SujetService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<SujetForum[]> {
    return this.http.get<SujetForum[]>(`${this.api}/sujets`);
  }

  /**
   * Ouvre un sujet dans un cours du formateur, avec un premier message — la
   * « relance collective sur le forum » : contrairement à la relance par e-mail,
   * elle s'adresse à tous les étudiants d'UN cours à la fois (le forum n'a pas de
   * notion de liste arbitraire d'étudiants), dans un nouveau sujet qu'ils peuvent
   * tous rejoindre et où ils peuvent tous répondre, pièce jointe comprise.
   */
  creerSujet(titre: string, coursId: number, premierMessage: string): Observable<ForumSujet> {
    // Réponse de ForumController, pas de EspaceEtudiantController : une forme plus
    // pauvre que SujetForum (pas de messages ni d'avatar), mais c'est tout ce que
    // la confirmation de cet envoi a besoin de lire.
    // pourMaPromotion doit être présent même à false : ForumSujetCreateRequest le
    // déclare en booléen primitif, et son absence fait échouer la désérialisation
    // côté serveur (400 « Failed to read request ») plutôt que de valoir false.
    return this.http.post<ForumSujet>(`${this.api}/forum/sujets`, {
      titre,
      coursId,
      pourMaPromotion: false,
      premierMessage,
    });
  }

  repondre(sujetId: number, contenu: string, marquerCommeResolu: boolean, fichier: File | null): Observable<SujetForum> {
    const payload = new FormData();
    payload.append('contenu', contenu);
    payload.append('marquerCommeResolu', String(marquerCommeResolu));
    if (fichier) {
      payload.append('fichier', fichier);
    }
    return this.http.post<SujetForum>(`${this.api}/sujets/${sujetId}/messages`, payload);
  }

  telechargerPieceJointe(messageId: number): Observable<Blob> {
    return this.http.get(`${this.api}/sujets/messages/${messageId}/piece-jointe`, { responseType: 'blob' });
  }

  toggleUrgence(sujetId: number): Observable<SujetForum> {
    return this.http.patch<SujetForum>(`${this.api}/formateur/sujets/${sujetId}/urgence`, {});
  }

  marquerResolu(sujetId: number): Observable<SujetForum> {
    return this.http.patch<SujetForum>(`${this.api}/formateur/sujets/${sujetId}/resoudre`, {});
  }

  supprimer(sujetId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/formateur/sujets/${sujetId}`);
  }
}
