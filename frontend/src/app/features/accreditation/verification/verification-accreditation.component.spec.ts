import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { VerificationAccreditationComponent } from './verification-accreditation.component';
import { AttestationVerificationService } from '../services/attestation-verification.service';
import { AttestationAccreditationVerification } from '../models/attestation-verification';

/**
 * Page publique vers laquelle pointe le QR code de l'attestation d'accréditation. Sert aussi de fiche
 * détaillée depuis la liste des écoles accréditées : doit montrer ce qui varie d'une accréditation à
 * l'autre (programme, niveau, décision, échéance) sans jamais exposer l'adresse ni le plan d'actions.
 */
describe('VerificationAccreditationComponent', () => {
  let fixture: ComponentFixture<VerificationAccreditationComponent>;

  const attestation: AttestationAccreditationVerification = {
    reference: 'INPG-ACC-2026-38B149EF',
    nomEcole: 'Institut Supérieur du Pétrole',
    intituleProgramme: 'Technicien en forage',
    niveauProgramme: 'TECHNICIEN',
    dateDecision: '2026-09-27T10:00:00',
    dateExpiration: '2029-09-27',
    sousReserve: false,
    verifiable: true,
    valide: true,
  };

  const fiche = (): Record<string, string> =>
    Object.fromEntries(
      Array.from(fixture.nativeElement.querySelectorAll('dt') as NodeListOf<HTMLElement>).map((dt) => [
        dt.textContent!.trim(),
        dt.nextElementSibling!.textContent!.trim(),
      ]),
    );

  async function afficher(reponse: AttestationAccreditationVerification): Promise<string> {
    await TestBed.configureTestingModule({
      imports: [VerificationAccreditationComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ reference: reponse.reference }) } } },
        { provide: AttestationVerificationService, useValue: { verifier: () => of(reponse) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(VerificationAccreditationComponent);
    fixture.detectChanges();
    return (fixture.nativeElement.querySelector('main') as HTMLElement).textContent!.replace(/\s+/g, ' ');
  }

  it("montre l'école, le programme et le niveau, avec le libellé français", async () => {
    const texte = await afficher(attestation);

    expect(fiche()['École accréditée']).toBe('Institut Supérieur du Pétrole');
    expect(fiche()['Programme']).toBe('Technicien en forage');
    expect(fiche()['Niveau']).toBe('Technicien');
    expect(texte).toContain('Attestation authentique et non modifiée.');
  });

  it('signale une accréditation sous réserve', async () => {
    const texte = await afficher({ ...attestation, sousReserve: true });

    expect(texte).toContain('Accréditation conditionnelle (sous réserve');
  });

  it('signale une référence inconnue', async () => {
    await TestBed.configureTestingModule({
      imports: [VerificationAccreditationComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ reference: 'INCONNUE' }) } } },
        { provide: AttestationVerificationService, useValue: { verifier: () => throwError(() => ({ status: 404 })) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(VerificationAccreditationComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Référence inconnue');
  });
});
