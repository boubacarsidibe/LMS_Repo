import { DatePipe, DecimalPipe } from '@angular/common';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { appConfig } from './app.config';

/**
 * L'application est entièrement en français, mais le DatePipe d'Angular utilise la locale « en-US » tant qu'aucune
 * autre n'est fournie : une convocation affichait « Saturday 24 October », la liste d'accréditation « Déposée le
 * 19 Oct 2026 » et les moyennes « 47.5 » — une centaine d'endroits (98 usages du pipe `date`).
 */
describe('appConfig — locale', () => {
  it('fournit la locale française à toute l’application', () => {
    const fournisseur = (appConfig.providers as { provide?: unknown; useValue?: unknown }[]).find(
      (p) => p?.provide === LOCALE_ID,
    );
    expect(fournisseur?.useValue).toBe('fr');
  });

  it('formate dates et nombres en français', () => {
    // Importer app.config enregistre les données de la locale ; ici on s'assure qu'elles servent bien aux pipes.
    TestBed.configureTestingModule({ providers: [{ provide: LOCALE_ID, useValue: 'fr' }] });
    const locale = TestBed.inject(LOCALE_ID);
    const date = new DatePipe(locale);
    const nombre = new DecimalPipe(locale);

    expect(date.transform('2026-10-24T10:00:00Z', 'EEEE d MMMM', 'UTC')).toBe('samedi 24 octobre');
    expect(date.transform('2026-10-19T10:00:00Z', 'd MMM y', 'UTC')).toBe('19 oct. 2026');
    expect(nombre.transform(47.5, '1.1-1')).toBe('47,5');
  });
});
