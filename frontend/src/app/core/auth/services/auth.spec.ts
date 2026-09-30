import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { Auth } from './authService';
import { TypeInscription } from '../models/user.model';
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

  /**
   * Retour de recette : une école ou une entreprise inscrite mais n'ayant rien
   * soumis n'a aucun rôle métier (ils ne sont accordés qu'à la première
   * soumission). À la connexion, elle était traitée comme un candidat et
   * atterrissait sur l'accueil des admissions étudiantes, sans rien qui mène à
   * son espace. Le type d'inscription, désormais renvoyé par le serveur, comble
   * ce vide — sans jamais primer sur un rôle qui existe.
   */
  describe('profil du compte et espace de destination', () => {
    const connecter = (roles: string[], typeInscription?: TypeInscription) =>
      service.currentUser.set({ id: 1, username: 'compte_test', email: 'compte@inpg.sn', roles, typeInscription });

    it('un visiteur n\'a ni profil ni espace', () => {
      expect(service.getProfilCompte()).toBeNull();
      expect(service.getHomeUrl()).toBeNull();
    });

    it('une école sans rôle métier est reconnue par son type et menée à son espace', () => {
      connecter([], 'ECOLE');
      expect(service.getProfilCompte()).toBe('ecole');
      expect(service.getHomeUrl()).toBe('/accreditation');
    });

    it('une entreprise sans rôle métier est reconnue par son type et menée à son espace', () => {
      connecter([], 'ENTREPRISE');
      expect(service.getProfilCompte()).toBe('entreprise');
      expect(service.getHomeUrl()).toBe('/espace-entreprise');
    });

    it('un formateur sans candidature est mené aux offres, qu\'il doit choisir avant de candidater', () => {
      connecter([], 'FORMATEUR');
      expect(service.getProfilCompte()).toBe('formateur');
      expect(service.getHomeUrl()).toBe('/offres-formateurs');
    });

    it('un candidat sans dossier n\'a pas d\'espace propre : il reste sur l\'accueil', () => {
      connecter([], 'CANDIDAT');
      expect(service.getProfilCompte()).toBeNull();
      expect(service.getHomeUrl()).toBeNull();
    });

    it('un compte sans type (ancienne réponse du serveur) reste traité comme un candidat', () => {
      connecter([]);
      expect(service.getProfilCompte()).toBeNull();
      expect(service.getHomeUrl()).toBeNull();
    });

    it('le rôle métier fait foi sur le type : une école dont le type vaut CANDIDAT (comptes de recette)', () => {
      connecter(['ROLE_ECOLE_FORMATION'], 'CANDIDAT');
      expect(service.getProfilCompte()).toBe('ecole');
      expect(service.getHomeUrl()).toBe('/accreditation');
    });

    it('un candidat formateur ayant déjà soumis est mené à sa candidature, pas aux offres', () => {
      connecter(['ROLE_CANDIDAT_FORMATEUR'], 'FORMATEUR');
      expect(service.getProfilCompte()).toBe('formateur');
      expect(service.getHomeUrl()).toBe('/candidature-formateur');
    });

    it('un compte qui porte un autre rôle n\'est jamais reclassé d\'après son type', () => {
      connecter(['ROLE_ETUDIANT'], 'ECOLE');
      expect(service.getProfilCompte()).toBeNull();
      expect(service.getHomeUrl()).toBe('/espace-etudiant/dashboard');

      connecter(['ROLE_RESPONSABLE_ADMISSION'], 'ENTREPRISE');
      expect(service.getProfilCompte()).toBeNull();
      expect(service.getHomeUrl()).toBe('/responsable-admission');
    });
  });
});
