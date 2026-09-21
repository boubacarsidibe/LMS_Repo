import { FormControl, Validators } from '@angular/forms';

import { aSaisiQuelqueChose, erreurVisible } from './erreur-champ';

describe('erreurVisible', () => {
  const email = (valeur: string) => new FormControl(valeur, [Validators.required, Validators.email]);

  it("n'affiche rien pour un champ vide, même quitté, tant qu'aucun envoi n'a été tenté", () => {
    const champ = email('');
    champ.markAsTouched();

    expect(champ.invalid).toBe(true);
    expect(erreurVisible(champ, false)).toBe(false);
  });

  it("affiche l'erreur dès qu'un caractère est saisi et que la valeur est invalide", () => {
    expect(erreurVisible(email('a'), false)).toBe(true);
    expect(erreurVisible(email('awa@'), false)).toBe(true);
  });

  it("n'affiche rien pour une valeur valide", () => {
    expect(erreurVisible(email('awa@inpg.sn'), false)).toBe(false);
    expect(erreurVisible(email('awa@inpg.sn'), true)).toBe(false);
  });

  it("après une tentative d'envoi, affiche aussi l'erreur d'un champ vide", () => {
    expect(erreurVisible(email(''), true)).toBe(true);
  });

  it("s'efface quand l'utilisateur vide le champ (avant tout envoi)", () => {
    const champ = email('a');
    expect(erreurVisible(champ, false)).toBe(true);

    champ.setValue('');

    expect(erreurVisible(champ, false)).toBe(false);
  });

  it("ignore un contrôle absent", () => {
    expect(erreurVisible(null, true)).toBe(false);
    expect(erreurVisible(undefined, true)).toBe(false);
  });

  it("considère une valeur non textuelle choisie comme saisie, pas null ni undefined", () => {
    expect(aSaisiQuelqueChose(0)).toBe(true);
    expect(aSaisiQuelqueChose(false)).toBe(true);
    expect(aSaisiQuelqueChose(null)).toBe(false);
    expect(aSaisiQuelqueChose(undefined)).toBe(false);
    expect(aSaisiQuelqueChose('')).toBe(false);
    expect(aSaisiQuelqueChose(' ')).toBe(true);
  });
});
