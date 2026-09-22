import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { Auth } from '../services/authService';
import { environment } from '../../../../environments/environment';

describe('authInterceptor', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  let token: string | null;
  const clearSession = vi.fn();
  const navigate = vi.fn();
  beforeEach(() => {
    token = 'token-initial';
    clearSession.mockReset(); navigate.mockReset();
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(),
      { provide: Auth, useValue: { getToken: () => token, clearSession } },
      { provide: Router, useValue: { url: '/profil', navigate } },
    ] });
    http = TestBed.inject(HttpClient); requests = TestBed.inject(HttpTestingController);
  });
  afterEach(() => requests.verify());

  it('joint le token uniquement aux routes de notre API', () => {
    for (const url of [environment.apiUrl + '/auth/current-user', 'https://tiers.example/api', environment.apiUrl + '-autre']) {
      http.get(url).subscribe();
      const req = requests.expectOne(url);
      expect(req.request.headers.has('Authorization')).toBe(url === environment.apiUrl + '/auth/current-user');
      req.flush({});
    }
  });

  it('ne déconnecte pas sur un 401 externe', () => {
    http.get('https://tiers.example/api').subscribe({ error: () => {} });
    requests.expectOne('https://tiers.example/api').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(clearSession).not.toHaveBeenCalled();
  });

  it('déconnecte sur un 401 de la session courante mais pas sur un 403', () => {
    for (const status of [403, 401]) {
      http.get(environment.apiUrl + '/profil').subscribe({ error: () => {} });
      requests.expectOne(environment.apiUrl + '/profil').flush({}, { status, statusText: 'Denied' });
      expect(clearSession).toHaveBeenCalledTimes(status === 401 ? 1 : 0);
    }
    expect(navigate).toHaveBeenCalledWith(['/auth/login'], { queryParams: { expired: 1 } });
  });

  it('un ancien 401 ne détruit pas une nouvelle session', () => {
    http.get(environment.apiUrl + '/profil').subscribe({ error: () => {} });
    const req = requests.expectOne(environment.apiUrl + '/profil');
    token = 'nouvelle-session';
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(clearSession).not.toHaveBeenCalled();
  });
});
