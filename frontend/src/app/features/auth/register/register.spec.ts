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
});
