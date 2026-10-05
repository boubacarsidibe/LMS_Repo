import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { Auth } from './authService';
import { environment } from '../../../../environments/environment';

describe('Auth', () => {
  let service: Auth;
  let requests: HttpTestingController;
  const navigate = vi.fn();
  const api = environment.apiUrl + '/auth';
  beforeEach(() => {
    localStorage.clear(); navigate.mockReset();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(),
      { provide: Router, useValue: { navigate } }] });
    service = TestBed.inject(Auth); requests = TestBed.inject(HttpTestingController);
    service.setToken('session');
  });
  afterEach(() => { requests.verify(); localStorage.clear(); });

  it('nettoie la session même lorsque la déconnexion échoue', () => {
    service.logout().subscribe({ error: () => {} });
    requests.expectOne(api + '/logout').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(service.getToken()).toBeNull();
  });
  it('nettoie la session avant de notifier la déconnexion réussie', () => {
    service.logout().subscribe(() => expect(service.getToken()).toBeNull());
    requests.expectOne(api + '/logout').flush('Déconnecté');
  });
  it('préserve la session lors d’une panne réseau de vérification', () => {
    service.checkToken().subscribe(value => expect(value).toBe(false));
    requests.expectOne(api + '/verify').error(new ProgressEvent('error'));
    expect(service.getToken()).toBe('session');
  });
  it('efface un jeton réellement rejeté', () => {
    service.checkToken().subscribe();
    requests.expectOne(api + '/verify').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(service.getToken()).toBeNull();
  });
  it('redirige vers la connexion après un changement de mot de passe', () => {
    service.ChangePassword('ancien', 'mot-de-passe-long').subscribe();
    requests.expectOne(api + '/change-password').flush({ message: 'Modifié' });
    expect(service.getToken()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/auth/login'], { queryParams: { passwordChanged: 1 } });
  });
  it('ne restaure pas un profil dont la session est terminée', () => {
    service.getCurrentUser().subscribe(user => expect(user).toBeNull());
    service.clearSession();
    requests.expectOne(api + '/current-user').flush({ id: 1, username: 'alice', email: 'alice@example.test', roles: [] });
    expect(service.currentUser()).toBeNull();
  });
});
