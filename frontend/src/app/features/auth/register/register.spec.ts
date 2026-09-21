import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { Register } from './register';

describe('Register', () => {
  let component: Register;
  let fixture: ComponentFixture<Register>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Register, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Register);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /**
   * Avant cette story, confirmPassword n'avait que Validators.required : un
   * mot de passe de confirmation différent du premier passait la validation
   * sans avertissement (relevé lors d'un audit externe).
   */
  it('signale des mots de passe différents au niveau du groupe', () => {
    component.registerForm.patchValue({ password: 'motdepasse1234', confirmPassword: 'autrepassword' });

    expect(component.registerForm.hasError('motsDePasseDifferents')).toBe(true);
  });

  it("n'affiche pas d'erreur de correspondance quand les deux mots de passe sont identiques", () => {
    component.registerForm.patchValue({ password: 'motdepasse1234', confirmPassword: 'motdepasse1234' });

    expect(component.registerForm.hasError('motsDePasseDifferents')).toBe(false);
  });

  it("champEnErreur ignore un champ invalide même touché, tant qu'aucun envoi n'a été tenté", () => {
    expect(component.registerForm.get('email')?.invalid).toBe(true);
    component.registerForm.get('email')?.markAsTouched();

    expect(component.champEnErreur('email')).toBe(false);
  });

  it("champEnErreur ne signale un champ invalide qu'après une tentative d'envoi (pas au blur seul)", () => {
    component.tentativeEnvoi = true;

    expect(component.champEnErreur('email')).toBe(true);
  });

  it('onSubmit sans avoir touché les champs déclenche quand même leur affichage (tentativeEnvoi)', () => {
    component.onSubmit();

    expect(component.tentativeEnvoi).toBe(true);
    expect(component.champEnErreur('prenom')).toBe(true);
  });

  it('rejette un mot de passe de moins de 12 caractères', () => {
    component.registerForm.patchValue({ password: 'court1234' });

    expect(component.registerForm.get('password')?.hasError('minlength')).toBe(true);
  });

  it("bascule prenom/nom vers nomOrganisation quand le type d'inscription devient ECOLE", () => {
    component.choisirTypeInscription('ECOLE');

    expect(component.registerForm.get('prenom')?.validator).toBe(null);
    expect(component.registerForm.get('nomOrganisation')?.hasError('required')).toBe(true);
  });
});
