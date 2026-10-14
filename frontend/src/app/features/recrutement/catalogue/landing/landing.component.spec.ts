import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { LandingComponent } from './landing.component';
import { Auth } from '../../../../core/auth/services/authService';

/**
 * Audit du 2026-10-14, M-6 : la liste des écoles accréditées est réservée aux comptes connectés (décision du
 * 2026-08-25), mais le bouton de l'accueil y menait un visiteur par la connexion sans explication, puis le
 * déposait sur son tableau de bord. Le bouton demande désormais à la connexion de l'y ramener.
 */
describe('LandingComponent — bouton « Voir les écoles accréditées »', () => {
  let fixture: ComponentFixture<LandingComponent>;
  let auth: Auth;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    auth = TestBed.inject(Auth);
    fixture = TestBed.createComponent(LandingComponent);
  });

  afterEach(() => localStorage.removeItem('token'));

  function lienDuBouton(): URL {
    fixture.detectChanges();
    const liens = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];
    const bouton = liens.find((a) => (a.textContent ?? '').includes('Voir les écoles accréditées'));
    expect(bouton).toBeTruthy();
    return new URL(bouton!.getAttribute('href') ?? '', 'http://exemple.test');
  }

  it('visiteur : passe par la connexion, qui le ramènera à la liste', () => {
    const lien = lienDuBouton();

    expect(lien.pathname).toBe('/auth/login');
    expect(lien.searchParams.get('retour')).toBe('/ecoles-accreditees');
  });

  it('compte connecté : va directement à la liste', () => {
    localStorage.setItem('token', 'jeton-test');
    auth.currentUser.set({ id: 1, username: 'compte_test', email: 'compte@inpg.sn', roles: [] });

    const lien = lienDuBouton();

    expect(lien.pathname).toBe('/ecoles-accreditees');
    expect(lien.search).toBe('');
  });
});
