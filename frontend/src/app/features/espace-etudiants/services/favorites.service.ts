import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FavoritesService {
  private api = `${environment.apiUrl}/espace-etudiant`;

  constructor(private http: HttpClient) {}

  getFavorites(): Observable<Array<number>> {
    return this.http.get<Array<number>>(`${this.api}/favorites`);
  }

  toggleFavorite(id: number | string | undefined): Observable<{ favorite: boolean }> {
    if (id === undefined) {
      return throwError(() => new Error('Identifiant de formation manquant.'));
    }
    return this.http.post<{ favorite: boolean }>(`${this.api}/favorites/${id}`, null);
  }
}
