import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AttestationAccreditationVerification } from '../models/attestation-verification';

/**
 * Vérification publique d'une attestation d'accréditation par sa référence.
 * Aucune authentification requise : c'est l'endpoint vers lequel pointe le QR
 * code imprimé sur l'attestation (voir AttestationAccreditationPublicController
 * côté backend), sur le même principe que CertificatVerificationService.
 */
@Injectable({ providedIn: 'root' })
export class AttestationVerificationService {
  private api = `${environment.apiUrl}/attestations-accreditation/public`;

  constructor(private http: HttpClient) {}

  verifier(reference: string): Observable<AttestationAccreditationVerification> {
    return this.http.get<AttestationAccreditationVerification>(`${this.api}/verifier/${encodeURIComponent(reference)}`);
  }
}
