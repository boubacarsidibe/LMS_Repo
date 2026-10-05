import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { VerificationCertificatComponent } from './verification-certificat.component';
import { CertificatVerificationService } from '../services/certificat-verification.service';
import { CertificatVerification } from '../models/certificat-verification';

/**
 * Page publique vers laquelle pointe le QR code du certificat. Elle doit montrer ce qui varie d'un certificat à
 * l'autre — la période et le code imprimés — pour qu'un faux dont on aurait retouché les dates ne passe pas pour
 * authentique ; la date de naissance, elle, n'y figure jamais.
 */
describe('VerificationCertificatComponent', () => {
  let fixture: ComponentFixture<VerificationCertificatComponent>;

  const certificat: CertificatVerification = {
    reference: 'INPG-CERT-2026-38B149EF',
    etudiantNomComplet: 'Cherif Tidiane Aidara',
    intituleFormation: 'Comptage du gaz',
    periode: 'Du 02 au 06 février 2026',
    dateEmission: '2026-09-25',
    formateurNom: 'INPG — Direction pédagogique',
    codeCertificat: 'CG1001260206/03',
    verifiable: true,
    valide: true,
  };

  /** Les lignes « libellé → valeur » de la fiche (chaque <dt> et le <dd> qui le suit). */
  const fiche = (): Record<string, string> =>
    Object.fromEntries(
      Array.from(fixture.nativeElement.querySelectorAll('dt') as NodeListOf<HTMLElement>).map((dt) => [
        dt.textContent!.trim(),
        dt.nextElementSibling!.textContent!.trim(),
      ]),
    );

  async function afficher(reponse: CertificatVerification): Promise<string> {
    await TestBed.configureTestingModule({
      imports: [VerificationCertificatComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ reference: reponse.reference }) } } },
        { provide: CertificatVerificationService, useValue: { verifier: () => of(reponse) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(VerificationCertificatComponent);
    fixture.detectChanges();
    return (fixture.nativeElement.querySelector('main') as HTMLElement).textContent!.replace(/\s+/g, ' ');
  }

  it('montre la période et le code du certificat, à côté de la formation', async () => {
    const texte = await afficher(certificat);

    expect(fiche()['Formation']).toBe('Comptage du gaz');
    expect(fiche()['Période']).toBe('Du 02 au 06 février 2026');
    expect(fiche()['Code du certificat']).toBe('CG1001260206/03');
    expect(texte).toContain('Certificat authentique et non modifié.');
  });

  it("n'affiche ni période ni code pour un certificat délivré avant le modèle INPG", async () => {
    await afficher({ ...certificat, periode: null, codeCertificat: null });

    expect(fiche()['Formation']).toBe('Comptage du gaz');
    expect(Object.keys(fiche())).not.toContain('Période');
    expect(Object.keys(fiche())).not.toContain('Code du certificat');
  });
});
