import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Creneau, DemandeRendezVousSaisie, PersonnelProfil, RendezVous } from '../models/rendez-vous.models';

/**
 * Gestion des rendez-vous (TDR §J). Commun à tout utilisateur connecté — un
 * seul service, une seule modélisation, pas de duplication par rôle.
 */
@Injectable({ providedIn: 'root' })
export class RendezVousService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/rendez-vous`;

  /**
   * Personnel consultable pour une prise de rendez-vous. {@code scope='EXTERNE'}
   * restreint aux responsables pédagogique et admission — les deux seuls
   * interlocuteurs des écoles partenaires et des entreprises, jamais un formateur.
   */
  getPersonnel(scope?: 'EXTERNE'): Observable<PersonnelProfil[]> {
    const params = scope ? { params: { scope } } : {};
    return this.http.get<PersonnelProfil[]>(`${this.api}/personnel`, params);
  }

  getCreneauxLibres(personnelId: number): Observable<Creneau[]> {
    return this.http.get<Creneau[]>(`${this.api}/personnel/${personnelId}/creneaux`);
  }

  /** Agenda du membre du personnel connecté — créneaux libres et réservés. */
  getMesCreneaux(): Observable<Creneau[]> {
    return this.http.get<Creneau[]>(`${this.api}/mes-creneaux`);
  }

  creerCreneau(dateHeureDebut: string, dateHeureFin: string): Observable<Creneau> {
    return this.http.post<Creneau>(`${this.api}/creneaux`, { dateHeureDebut, dateHeureFin });
  }

  supprimerCreneau(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/creneaux/${id}`);
  }

  demanderRendezVous(saisie: DemandeRendezVousSaisie): Observable<RendezVous> {
    return this.http.post<RendezVous>(`${this.api}/demandes`, saisie);
  }

  getMesDemandes(): Observable<RendezVous[]> {
    return this.http.get<RendezVous[]>(`${this.api}/mes-demandes`);
  }

  getDemandesRecues(): Observable<RendezVous[]> {
    return this.http.get<RendezVous[]>(`${this.api}/demandes-recues`);
  }

  annulerDemande(id: number): Observable<RendezVous> {
    return this.http.post<RendezVous>(`${this.api}/${id}/annuler`, {});
  }

  accepter(id: number, commentaire?: string): Observable<RendezVous> {
    return this.http.post<RendezVous>(`${this.api}/${id}/reponse`, { action: 'ACCEPTER', commentaire });
  }

  refuser(id: number, commentaire?: string): Observable<RendezVous> {
    return this.http.post<RendezVous>(`${this.api}/${id}/reponse`, { action: 'REFUSER', commentaire });
  }

  proposerAutreDate(
    id: number,
    nouvelleDateHeureDebut: string,
    nouvelleDateHeureFin: string,
    commentaire?: string
  ): Observable<RendezVous> {
    return this.http.post<RendezVous>(`${this.api}/${id}/reponse`, {
      action: 'PROPOSER',
      nouvelleDateHeureDebut,
      nouvelleDateHeureFin,
      commentaire,
    });
  }

  repondreProposition(id: number, accepter: boolean): Observable<RendezVous> {
    return this.http.post<RendezVous>(`${this.api}/${id}/reponse-proposition`, { accepter });
  }
}
