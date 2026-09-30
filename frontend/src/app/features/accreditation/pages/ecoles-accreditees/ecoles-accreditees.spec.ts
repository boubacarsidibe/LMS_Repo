import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { EcolesAccrediteesComponent } from './ecoles-accreditees';
import { Auth } from '../../../../core/auth/services/authService';
import { User } from '../../../../core/auth/models/user.model';

/**
 * Cette liste est ouverte à tous les comptes connectés et n'a pas de layout de
 * rôle : son bandeau ramène l'utilisateur à SON espace. Après un rechargement de
 * la page, le compte n'est pas encore chargé (authGuard ne vérifie que le jeton) :
 * le lien menait alors toujours à l'accueil, quel que soit le compte.
 */
describe('EcolesAccrediteesComponent — bandeau de retour', () => {
  let fixture: ComponentFixture<EcolesAccrediteesComponent>;
  let auth: Auth;

  const ecole: User = { id: 1, username: 'ecole_test', email: 'ecole@inpg.sn', roles: [], typeInscription: 'ECOLE' };

  const lienRetour = (): string | null =>
    (fixture.nativeElement as HTMLElement).querySelector('header a[href]:last-of-type')?.getAttribute('href') ?? null;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EcolesAccrediteesComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    auth = TestBed.inject(Auth);
    fixture = TestBed.createComponent(EcolesAccrediteesComponent);
  });

  it("charge le compte s'il n'est pas encore connu, puis renvoie vers son espace", () => {
    const chargement = vi.spyOn(auth, 'getCurrentUser').mockImplementation(() => {
      auth.currentUser.set(ecole);
      return of(ecole);
    });

    fixture.detectChanges();

    expect(chargement).toHaveBeenCalledTimes(1);
    expect(lienRetour()).toBe('/accreditation');
  });

  it('ne recharge pas un compte déjà connu', () => {
    auth.currentUser.set(ecole);
    const chargement = vi.spyOn(auth, 'getCurrentUser');

    fixture.detectChanges();

    expect(chargement).not.toHaveBeenCalled();
    expect(lienRetour()).toBe('/accreditation');
  });
});
