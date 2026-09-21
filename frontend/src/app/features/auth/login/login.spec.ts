import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { Login } from './login';

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

  it("champEnErreur signale un e-mail invalide dès qu'un caractère est saisi, avant toute tentative", () => {
    component.form.patchValue({ email: 'a' });

    expect(component.tentativeEnvoi).toBe(false);
    expect(component.champEnErreur('email')).toBe(true);
    expect(component.champEnErreur('password')).toBe(false);
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
