import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { CertificatVerification } from '../models/certificat-verification';

/**
 * Vérification publique d'un certificat par sa référence. Aucune authentification
 * requise : c'est l'endpoint vers lequel pointe le QR code imprimé sur le
 * certificat (voir CertificatPublicController côté backend).
 */
@Injectable({ providedIn: 'root' })
export class CertificatVerificationService {
  private api = `${environment.apiUrl}/certificats/public`;

  constructor(private http: HttpClient) {}

  verifier(reference: string): Observable<CertificatVerification> {
    return this.http.get<CertificatVerification>(`${this.api}/verifier/${encodeURIComponent(reference)}`);
  }
}
