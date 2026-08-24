import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { CertificatVerification } from '../models/certificat-verification';

/**
 * Consomme l'endpoint public de vérification d'un certificat — pas d'en-tête
 * d'authentification à joindre, l'appelant n'a en général aucun compte sur la
 * plateforme (cf. VerificationCertificatController côté back-end).
 */
@Injectable({ providedIn: 'root' })
export class VerificationCertificatService {
  private api = `${environment.apiUrl}/verification-certificat`;

  constructor(private http: HttpClient) {}

  verifier(reference: string): Observable<CertificatVerification> {
    return this.http.get<CertificatVerification>(`${this.api}/${encodeURIComponent(reference)}`);
  }
}
