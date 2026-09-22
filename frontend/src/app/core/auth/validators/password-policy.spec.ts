import { validPassword } from './password-policy';

describe('password policy', () => {
  it('compte les caractères Unicode et limite les octets BCrypt', () => {
    expect(validPassword('😀'.repeat(11))).toBe(false);
    expect(validPassword('😀'.repeat(12))).toBe(true);
    expect(validPassword('é'.repeat(36))).toBe(true);
    expect(validPassword('é'.repeat(37))).toBe(false);
    expect(validPassword(' '.repeat(12))).toBe(false);
  });
});
