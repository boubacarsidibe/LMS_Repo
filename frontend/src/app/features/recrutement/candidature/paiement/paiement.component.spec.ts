import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { PaiementComponent } from './paiement.component';
import { PaiementResponse } from '../../../../core/recrutement/services/recrutement.service';

/**
 * Le candidat devait « déclarer ce règlement » en lisant « Montant communiqué à la validation » : le montant
 * n'apparaissait qu'une fois la tentative enregistrée. Il est désormais annoncé avant, par le serveur.
 */
describe('PaiementComponent — montant affiché', () => {
  let component: PaiementComponent;
  let fixture: ComponentFixture<PaiementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaiementComponent, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(PaiementComponent);
    component = fixture.componentInstance;
  });

  it('annonce les frais prévus avant tout règlement', () => {
    component.paiement = null;
    component.fraisPrevus = { montant: 25000, devise: 'XOF' };
    expect(component.montantFormate).toMatch(/^25\s000 FCFA$/);
  });

  it('affiche le montant enregistré dès qu’une tentative existe, même s’il diffère des frais annoncés', () => {
    component.fraisPrevus = { montant: 25000, devise: 'XOF' };
    component.paiement = { montant: 30000, devise: 'XOF' } as PaiementResponse;
    expect(component.montantFormate).toMatch(/^30\s000 FCFA$/);
  });

  it('ne promet aucun montant quand le serveur n’en a donné aucun', () => {
    component.paiement = null;
    component.fraisPrevus = null;
    expect(component.montantFormate).toBe('Montant indisponible');
  });
});
