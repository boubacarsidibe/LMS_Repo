import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';

import { noRoleGuard } from './no-role-guard';
import { Auth } from '../services/authService';
import { TypeInscription } from '../models/user.model';

/**
 * /accreditation et /espace-entreprise restent ouverts au compte tout juste créé
 * (sans rôle métier), mais plus à un compte qui a déjà un rôle interne établi : il
 * obtenait ROLE_ECOLE_FORMATION / ROLE_ENTREPRISE en plus du sien à la première
 * soumission.
 */
describe('noRoleGuard', () => {
  const ROLES_INTERNES = ['ROLE_FORMATEUR', 'ROLE_ETUDIANT', 'ROLE_RESPONSABLE_ADMISSION'];

  let auth: Auth;
  let router: Router;
  let navigate: ReturnType<typeof vi.spyOn>;

  const connecter = (roles: string[], typeInscription?: TypeInscription) =>
    auth.currentUser.set({ id: 1, username: 'compte_test', email: 'compte@inpg.sn', roles, typeInscription });

  /** Exécute le garde comme le routeur, pour l'URL demandée. */
  async function visiter(url: string, ...exclus: string[]): Promise<boolean> {
    const garde = noRoleGuard(...exclus);
    const resultat = TestBed.runInInjectionContext(() =>
      garde({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot)) as Observable<boolean>;
    return firstValueFrom(resultat);
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(Auth);
    router = TestBed.inject(Router);
    navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('laisse passer un compte tout juste créé, sans rôle métier', async () => {
    connecter([], 'ECOLE');

    expect(await visiter('/accreditation', ...ROLES_INTERNES)).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('laisse passer un candidat, qui peut encore choisir son parcours', async () => {
    connecter(['ROLE_CANDIDAT'], 'CANDIDAT');

    expect(await visiter('/espace-entreprise', ...ROLES_INTERNES)).toBe(true);
  });

  it('renvoie un formateur vers son propre espace', async () => {
    connecter(['ROLE_FORMATEUR']);

    expect(await visiter('/accreditation', ...ROLES_INTERNES)).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/espace-etudiant/formateur/dashboard-formateur']);
  });

  it('renvoie un étudiant vers son propre espace', async () => {
    connecter(['ROLE_ETUDIANT']);

    expect(await visiter('/espace-entreprise', ...ROLES_INTERNES)).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/espace-etudiant/dashboard']);
  });

  it("renvoie une entreprise qui tente l'espace école vers SON espace", async () => {
    connecter(['ROLE_ENTREPRISE']);

    expect(await visiter('/accreditation', ...ROLES_INTERNES, 'ROLE_ENTREPRISE')).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/espace-entreprise']);
  });

  it('renvoie un visiteur sans session vers la connexion', async () => {
    expect(await visiter('/accreditation', ...ROLES_INTERNES)).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  /**
   * Avant d'exister, ce garde ne posait aucune limite : un compte a pu cumuler
   * les deux rôles. Son espace (l'école, prioritaire) est exclu par la route de
   * l'entreprise ET l'inverse — le renvoyer chez lui le faisait rebondir sur
   * la même URL, sans fin.
   */
  it('ne renvoie pas en boucle un compte école + entreprise sur son propre espace', async () => {
    connecter(['ROLE_ECOLE_FORMATION', 'ROLE_ENTREPRISE']);

    expect(await visiter('/accreditation', ...ROLES_INTERNES, 'ROLE_ENTREPRISE')).toBe(true);
    expect(await visiter('/accreditation/rendez-vous?x=1', ...ROLES_INTERNES, 'ROLE_ENTREPRISE')).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });

  it("renvoie ce même compte double de l'espace entreprise vers son espace école", async () => {
    connecter(['ROLE_ECOLE_FORMATION', 'ROLE_ENTREPRISE']);

    expect(await visiter('/espace-entreprise', ...ROLES_INTERNES, 'ROLE_ECOLE_FORMATION')).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/accreditation']);
  });
});
