import { ResponsableUsersComponent } from './responsable-users.component';

/** Seuls candidat, étudiant et formateur s'attribuent ; le serveur refuse les autres (RoleManagementService). */
describe('ResponsableUsersComponent — rôles attribuables', () => {
  const c = new ResponsableUsersComponent({} as never, {} as never);

  it('ne propose que les trois rôles que le serveur accepte', () => {
    expect(c.rolesAttribuables.map((r) => r.code)).toEqual(['ROLE_CANDIDAT', 'ROLE_ETUDIANT', 'ROLE_FORMATEUR']);
  });

  it('un compte du personnel est affiché en lecture seule', () => {
    expect(c.estPersonnel({ roles: ['ROLE_RESPONSABLE_PEDAGOGIQUE'] } as never)).toBe(true);
    expect(c.estPersonnel({ roles: ['ROLE_EVALUATEUR_ACCREDITATION'] } as never)).toBe(true);
    expect(c.estPersonnel({ roles: ['ROLE_ETUDIANT'] } as never)).toBe(false);
  });

  it('nomme les rôles, y compris ceux qui ne s\'attribuent pas ici', () => {
    expect(c.libelleRole('ROLE_ALUMNI')).toBe('Alumni');
    expect(c.libelleRole('')).toBe('Aucun rôle');
    expect(c.estAttribuable('ROLE_ALUMNI')).toBe(false);
  });
});
