import { validPassword } from './password-policy';

describe('password policy', () => {
  it('exige 8 caractères Unicode minimum', () => {
    expect(validPassword('😀'.repeat(7))).toBe(false);
    expect(validPassword('😀'.repeat(8))).toBe(true);
    expect(validPassword('Abcd123')).toBe(false);
    expect(validPassword('Abcd123!')).toBe(true);
  });

  it("n'impose aucune longueur maximale", () => {
    expect(validPassword('é'.repeat(200))).toBe(true);
  });

  it('refuse un mot de passe fait uniquement d\'espaces', () => {
    expect(validPassword(' '.repeat(12))).toBe(false);
  });
});
