import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';
import { authGuard } from './auth-guard';
import { Auth } from '../services/authService';

describe('authGuard', () => {
  it('autorise une session valide et annule une navigation hors ligne sans redirection', async () => {
    let valid = true;
    TestBed.configureTestingModule({ providers: [
      { provide: Auth, useValue: { checkToken: () => of(valid), getToken: () => 'session' } },
      { provide: Router, useValue: {} },
    ] });
    const run = () => TestBed.runInInjectionContext(() => authGuard({} as any, {} as any)) as Observable<boolean>;
    expect(await firstValueFrom(run())).toBe(true);
    valid = false;
    expect(await firstValueFrom(run())).toBe(false);
  });
  it('redirige vers la connexion lorsque le jeton a été rejeté', async () => {
    const login = {};
    TestBed.configureTestingModule({ providers: [
      { provide: Auth, useValue: { checkToken: () => of(false), getToken: () => null } },
      { provide: Router, useValue: { createUrlTree: () => login } },
    ] });
    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any)) as Observable<unknown>;
    expect(await firstValueFrom(result)).toBe(login);
  });
});
