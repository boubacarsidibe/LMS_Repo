import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Cloche de notifications persistées, adossée à l'utilisateur connecté.
 *
 * <p>À ne pas confondre avec {@code NotificationService} (shared/components/ui) :
 * celui-là affiche des toasts éphémères côté navigateur, celui-ci lit les
 * notifications réellement enregistrées en base par le serveur.
 *
 * <p>L'URL reste sous {@code /api/espace-etudiant} pour des raisons historiques —
 * la cloche y est née — mais l'endpoint est commun à l'étudiant, au formateur, au
 * responsable pédagogique et au responsable admission.
 */
@Injectable({ providedIn: 'root' })
export class ClocheNotificationsService {
  private http = inject(HttpClient);
  private baseUrl = '/api/espace-etudiant/notifications';

  lister(): Observable<NotificationCloche[]> {
    return this.http.get<NotificationCloche[]>(this.baseUrl);
  }

  compterNonLues(): Observable<{ nonLues: number }> {
    return this.http.get<{ nonLues: number }>(`${this.baseUrl}/count-non-lues`);
  }

  marquerLue(id: number): Observable<NotificationCloche> {
    return this.http.post<NotificationCloche>(`${this.baseUrl}/${id}/lu`, {});
  }

  toutMarquerLu(): Observable<{ marquees: number }> {
    return this.http.post<{ marquees: number }>(`${this.baseUrl}/tout-lu`, {});
  }
}

export interface NotificationCloche {
  id: number;
  message: string;
  type: string;
  lu: boolean;
  dateCreation: string;
  lien: string;
}
