import { ValidatorFn } from '@angular/forms';

/** Même règle que PasswordPolicy côté API : 8 caractères Unicode, 72 octets BCrypt. */
export function validPassword(value: string): boolean {
  return !!value.trim() && Array.from(value).length >= 8 && new TextEncoder().encode(value).length <= 72;
}

export const passwordPolicy: ValidatorFn = control => validPassword(control.value ?? '')
  ? null : { passwordPolicy: true };
