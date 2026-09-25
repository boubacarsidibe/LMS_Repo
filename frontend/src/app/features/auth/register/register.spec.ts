import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
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

  it('ne crée pas de compte lorsque les mots de passe diffèrent', () => {
    component.registerForm.patchValue({ prenom: 'Alice', nom: 'Test', email: 'alice@example.test',
      password: 'mot-de-passe-long', confirmPassword: 'autre-mot-de-passe' });
    component.onSubmit();
    expect(component.errorMessage).toContain('ne correspondent pas');
    TestBed.inject(HttpTestingController).expectNone(() => true);
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

  it("champEnErreur ignore un champ vide, même quitté, tant qu'aucun envoi n'a été tenté", () => {
    expect(component.registerForm.get('email')?.invalid).toBe(true);
    component.registerForm.get('email')?.markAsTouched();

    expect(component.champEnErreur('email')).toBe(false);
  });

  it("champEnErreur signale un champ invalide dès qu'un caractère est saisi, sans attendre le clic", () => {
    component.registerForm.patchValue({ email: 'a' });

    expect(component.tentativeEnvoi).toBe(false);
    expect(component.champEnErreur('email')).toBe(true);
  });

  it("l'erreur disparaît quand l'utilisateur vide le champ avant tout clic", () => {
    component.registerForm.patchValue({ email: 'a' });
    component.registerForm.patchValue({ email: '' });

    expect(component.champEnErreur('email')).toBe(false);
  });

  it('un mot de passe trop court est signalé dès le premier caractère', () => {
    component.registerForm.patchValue({ password: 'a' });

    expect(component.champEnErreur('password')).toBe(true);
    expect(component.registerForm.get('password')?.hasError('minlength')).toBe(true);
  });

  /**
   * Le clic ne créait aucun compte et ne montrait rien d'utile — signalé en
   * recette comme un bouton « qui ne fait rien ». Il affiche désormais l'erreur
   * de tous les champs invalides (même vides) ET nomme par quoi commencer.
   */
  it('un clic sur formulaire vierge signale tous les champs bloquants', () => {
    component.onSubmit();

    expect(component.tentativeEnvoi).toBe(true);
    expect(component.champEnErreur('prenom')).toBe(true);
    expect(component.champEnErreur('nom')).toBe(true);
    expect(component.champEnErreur('email')).toBe(true);
    expect(component.champEnErreur('password')).toBe(true);
    expect(component.champEnErreur('confirmPassword')).toBe(true);
  });

  it('le clic nomme le champ bloquant, pour que le bouton ne paraisse pas inerte', () => {
    component.onSubmit();

    expect(component.errorMessage).toContain('Prénom');
  });

  it('une fois le champ bloquant renseigné, le clic suivant désigne le suivant', () => {
    component.registerForm.patchValue({ prenom: 'Awa' });

    component.onSubmit();

    expect(component.champEnErreur('prenom')).toBe(false);
    expect(component.champEnErreur('nom')).toBe(true);
    expect(component.errorMessage).toContain('Nom');
  });

  it("affiche « les mots de passe ne correspondent pas » dès qu'un caractère de confirmation est saisi", () => {
    component.registerForm.patchValue({ password: 'motdepasse1234', confirmPassword: 'm' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Les mots de passe ne correspondent pas');
  });

  it("n'affiche pas cette erreur tant que la confirmation est vide", () => {
    component.registerForm.patchValue({ password: 'motdepasse1234', confirmPassword: '' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Les mots de passe ne correspondent pas');
  });

  it('désigne la confirmation quand seuls les deux mots de passe diffèrent', () => {
    component.registerForm.patchValue({
      prenom: 'Awa',
      nom: 'Diop',
      email: 'awa.diop@test.inpg.sn',
      password: 'MotDePasse2026!',
      confirmPassword: 'Different2026!'
    });

    component.onSubmit();

    expect(component.errorMessage).toContain('Confirmation');
    expect(component.registerForm.get('confirmPassword')?.touched).toBe(true);
  });

  it('rejette un mot de passe de moins de 8 caractères', () => {
    component.registerForm.patchValue({ password: 'court12' });

    expect(component.registerForm.get('password')?.hasError('minlength')).toBe(true);
  });

  it('accepte un mot de passe de 8 caractères exactement', () => {
    component.registerForm.patchValue({ password: 'huit1234' });

    expect(component.registerForm.get('password')?.hasError('minlength')).toBe(false);
  });

  it("bascule prenom/nom vers nomOrganisation quand le type d'inscription devient ECOLE", () => {
    component.choisirTypeInscription('ECOLE');

    expect(component.registerForm.get('prenom')?.validator).toBe(null);
    expect(component.registerForm.get('nomOrganisation')?.hasError('required')).toBe(true);
  });
});
