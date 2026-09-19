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

  it("champEnErreur ignore un champ invalide tant que l'utilisateur ne l'a pas quitté", () => {
    expect(component.registerForm.get('email')?.invalid).toBe(true);
    expect(component.champEnErreur('email')).toBe(false);
  });

  it('champEnErreur signale un champ invalide une fois touché', () => {
    component.registerForm.get('email')?.markAsTouched();

    expect(component.champEnErreur('email')).toBe(true);
  });

  /**
   * Un indicateur « tentative d'envoi » forçait auparavant l'affichage de TOUS
   * les champs invalides au premier clic : un formulaire vierge se couvrait de
   * cinq messages rouges d'un coup, pour des champs où l'utilisateur n'avait
   * rien saisi et donc commis aucune erreur. Signalé en recette comme un clic
   * « qui ne fait rien » — il ne créait pas de compte et n'affichait qu'un mur
   * de rouge, sans désigner par quoi commencer.
   */
  it('un clic sur formulaire vierge ne signale que le premier champ bloquant', () => {
    component.onSubmit();

    expect(component.champEnErreur('prenom')).toBe(true);
    expect(component.champEnErreur('nom')).toBe(false);
    expect(component.champEnErreur('email')).toBe(false);
    expect(component.champEnErreur('password')).toBe(false);
    expect(component.champEnErreur('confirmPassword')).toBe(false);
  });

  it('le clic nomme le champ bloquant, pour que le bouton ne paraisse pas inerte', () => {
    component.onSubmit();

    expect(component.errorMessage).toContain('Prénom');
  });

  it('une fois le champ bloquant renseigné, le clic suivant désigne le suivant', () => {
    component.registerForm.patchValue({ prenom: 'Awa' });

    component.onSubmit();

    expect(component.champEnErreur('nom')).toBe(true);
    expect(component.errorMessage).toContain('Nom');
    expect(component.champEnErreur('email')).toBe(false);
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
