import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AlbumPhoto, ContenuAlbum, Photo } from '../models/scolarite.models';

/**
 * Photothèque, côté formateur.
 *
 * <p>Ce service visait `GET /photos` et `POST /formateur/photos`, deux endpoints
 * qui n'existent plus : la photothèque est passée à un modèle en albums, où un
 * cliché appartient toujours à un album. La page ne pouvait donc ni afficher ni
 * envoyer quoi que ce soit — d'où le message « L'envoi de la photo a échoué ».
 */
@Injectable({ providedIn: 'root' })
export class PhotoService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/espace-etudiant`;

  /** Albums visibles par l'utilisateur connecté. */
  getAlbums(): Observable<AlbumPhoto[]> {
    return this.http.get<AlbumPhoto[]>(`${this.api}/photheque/albums`);
  }

  /** Un album et les clichés qu'il contient : { album, photos }. */
  getAlbum(albumId: number): Observable<ContenuAlbum> {
    return this.http.get<ContenuAlbum>(`${this.api}/photheque/albums/${albumId}`);
  }

  /**
   * Crée un album.
   *
   * @param promotionId promotion visée, ou `null` pour un album institutionnel
   *                    visible de tous les étudiants
   */
  creerAlbum(
    titre: string,
    description: string | null,
    promotionId: number | null,
    dateEvenement: string | null
  ): Observable<AlbumPhoto> {
    return this.http.post<AlbumPhoto>(`${this.api}/formateur/albums`, {
      titre,
      description,
      promotionId,
      dateEvenement,
    });
  }

  /** Ajoute un cliché à un album existant. */
  ajouterPhoto(albumId: number, fichier: File, legende: string | null): Observable<Photo> {
    const donnees = new FormData();
    donnees.append('fichier', fichier);
    if (legende) {
      donnees.append('legende', legende);
    }
    return this.http.post<Photo>(`${this.api}/formateur/albums/${albumId}/photos`, donnees);
  }
}
