import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { ResponsableAdmissionPaiementsComponent } from './responsable-admission-paiements.component';
import { CandidatureResponse } from '../../../../core/recrutement/services/recrutement.service';

/**
 * Le bouton « Relancer les impayés » annonçait trois dossiers alors que le serveur n'écrit qu'aux dossiers qui attendent
 * encore le règlement : un dossier déjà entretenu sans avoir payé se régularise, il ne se relance pas.
 */
describe('Paiements — relance des impayés', () => {
  let component: ResponsableAdmissionPaiementsComponent;
  let fixture: ComponentFixture<ResponsableAdmissionPaiementsComponent>;

  const dossier = (id: number, statut: string, paiement: string | null) =>
    ({ id, statut, paiement: paiement ? { statut: paiement } : null }) as unknown as CandidatureResponse;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResponsableAdmissionPaiementsComponent, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(ResponsableAdmissionPaiementsComponent);
    component = fixture.componentInstance;
  });

  it('affiche le libellé du statut, jamais son code', () => {
    expect(component.libelle('PRESELECTIONNE')).toBe('Présélectionné');
    expect(component.libelle('ENTRETIEN_EFFECTUE')).toBe('Entretien effectué');
  });

  it('ne relance que les dossiers qui attendent encore le règlement', () => {
    component.candidatures = [
      dossier(1, 'ENTRETIEN_EFFECTUE', null),          // impayé, mais trop avancé pour une relance
      dossier(2, 'PRESELECTIONNE', null),
      dossier(3, 'PAIEMENT_EN_ATTENTE', 'EN_ATTENTE'),
      dossier(4, 'CONVOQUE_TEST', 'SUCCES'),           // payé
      dossier(5, 'SOUMIS', null),                      // encore à l'instruction : rien à régler
    ];

    expect(component.candidaturesImpayees.map((c) => c.id)).toEqual([1, 2, 3]);
    expect(component.candidaturesARelancer.map((c) => c.id)).toEqual([2, 3]);
  });
});
