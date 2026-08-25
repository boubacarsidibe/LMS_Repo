import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  DemandeEntreprise,
  DemandeEntrepriseSaisie,
  FormationCatalogue,
  MessageEntreprise,
} from '../models/entreprise.models';

/**
 * Espace Entreprise (TDR §H) : catalogue, soumission de besoins/devis, suivi
 * de ses demandes, messagerie privée avec la pédagogie.
 */
@Injectable({ providedIn: 'root' })
export class EntrepriseService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/entreprise`;

  getCatalogue(): Observable<FormationCatalogue[]> {
    return this.http.get<FormationCatalogue[]>(`${this.api}/catalogue`);
  }

  soumettreDemande(donnees: DemandeEntrepriseSaisie): Observable<DemandeEntreprise> {
    return this.http.post<DemandeEntreprise>(`${this.api}/demandes`, donnees);
  }

  getMesDemandes(): Observable<DemandeEntreprise[]> {
    return this.http.get<DemandeEntreprise[]>(`${this.api}/demandes`);
  }

  getMesMessages(): Observable<MessageEntreprise[]> {
    return this.http.get<MessageEntreprise[]>(`${this.api}/messages`);
  }

  envoyerMessage(contenu: string): Observable<MessageEntreprise> {
    return this.http.post<MessageEntreprise>(`${this.api}/messages`, { contenu });
  }
}
