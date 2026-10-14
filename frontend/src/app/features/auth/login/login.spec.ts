import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { Login } from './login';
import { Auth } from '../../../core/auth/services/authService';
import { AuthResponse } from '../../../core/auth/models/auth-response';

describe('Login', () => {
  let component: Login;
  let fixture: ComponentFixture<Login>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /**
   * Avant cette story, le bouton "Se connecter" était désactivé tant que le
   * formulaire était invalide — impossible donc de cliquer pour déclencher
   * l'affichage des erreurs, qui n'apparaissaient de toute façon qu'au blur
   * d'un champ (avant toute tentative de connexion), pas après un clic.
   */
  it("champEnErreur ignore un champ invalide, même quitté, tant qu'aucune connexion n'a été tentée", () => {
    expect(component.form.get('email')?.invalid).toBe(true);
    component.form.get('email')?.markAsTouched();

    expect(component.champEnErreur('email')).toBe(false);
  });

  it("champEnErreur signale un champ invalide après une tentative de connexion (clic sur Se connecter)", () => {
    component.onSubmit();

    expect(component.tentativeEnvoi).toBe(true);
    expect(component.champEnErreur('email')).toBe(true);
    expect(component.champEnErreur('password')).toBe(true);
  });

  it("n'appelle pas le service d'authentification tant que le formulaire est invalide", () => {
    const spy = vi.spyOn((component as any).authService, 'login');

    component.onSubmit();

    expect(spy).not.toHaveBeenCalled();
  });
});

/**
 * Audit du 2026-10-14, M-6 : le bouton « Voir les écoles accréditées » de l'accueil menait un visiteur à la
 * connexion sans un mot d'explication, puis, une fois connecté, à son tableau de bord — la liste demandée était
 * perdue. `?retour=` y remédie ; `retour` vient de l'URL, donc de n'importe qui : seule une page de la liste
 * fermée est acceptée.
 */
describe('Login — retour vers une page réservée aux comptes', () => {
  function creer(retour?: string) {
    TestBed.configureTestingModule({
      imports: [Login, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(retour === undefined ? {} : { retour }) } },
        },
      ],
    });
    const auth = TestBed.inject(Auth);
    const router = TestBed.inject(Router);
    const login = TestBed.createComponent(Login).componentInstance;

    // Connexion réussie, sans serveur : on ne regarde que l'endroit où l'on arrive.
    vi.spyOn(auth, 'login').mockReturnValue(of({} as AuthResponse));
    vi.spyOn(auth, 'setSession').mockImplementation(() => {});
    const versLaPage = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const versLeRole = vi.spyOn(auth, 'redirectByRole').mockImplementation(() => {});
    login.form.setValue({ email: 'visiteur@inpg.sn', password: 'Test@2026' });
    return { login, versLaPage, versLeRole };
  }

  it("explique pourquoi on passe par la connexion, sans en faire une erreur", () => {
    const { login } = creer('/ecoles-accreditees');

    expect(login.infoMessage).toContain('écoles accréditées');
    expect(login.infoMessage).toContain('réservée aux comptes connectés');
    expect(login.errorMessage).toBe('');
  });

  it("ramène à la liste après la connexion, pas à l'accueil du rôle", () => {
    const { login, versLaPage, versLeRole } = creer('/ecoles-accreditees');

    login.onSubmit();

    expect(versLaPage).toHaveBeenCalledWith('/ecoles-accreditees');
    expect(versLeRole).not.toHaveBeenCalled();
  });

  it.each([
    ['une adresse extérieure', 'https://evil.example/'],
    ['une adresse sans protocole', '//evil.example'],
    ['une page de l’application hors liste', '/pedagogie/cours'],
    ['la page d’inscription', '/auth/register'],
    ['un nom d’objet JavaScript', 'constructor'],
    ['une valeur vide', ''],
  ])("ignore %s : ni message, ni redirection libre", (_nom, retour) => {
    const { login, versLaPage, versLeRole } = creer(retour);

    expect(login.infoMessage).toBe('');

    login.onSubmit();

    expect(versLaPage).not.toHaveBeenCalled();
    expect(versLeRole).toHaveBeenCalledTimes(1);
  });

  it("sans paramètre : l'accueil du rôle, comme avant", () => {
    const { login, versLaPage, versLeRole } = creer();

    expect(login.infoMessage).toBe('');

    login.onSubmit();

    expect(versLaPage).not.toHaveBeenCalled();
    expect(versLeRole).toHaveBeenCalledTimes(1);
  });
});
