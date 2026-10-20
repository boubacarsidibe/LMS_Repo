import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { MonProfil } from './mon-profil';
import { AlumniService } from '../../services/alumni.service';
import { AlumniMetriques, AlumniProfil } from '../../models/alumni.models';

/**
 * Un alumni de la promotion de l'année voyait « Diplômé(e) depuis 0 an » — vrai au sens du calcul, absurde à la lecture,
 * et c'est précisément le cas de tous les alumni d'une plateforme qui vient d'ouvrir.
 */
describe('MonProfil — ancienneté du diplôme', () => {
  function tuileAnciennete(annees: number): string {
    TestBed.configureTestingModule({
      imports: [MonProfil],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: AlumniService,
          useValue: {
            getMonProfil: () => of({ visible: true } as AlumniProfil),
            getMesMetriques: () => of({ anneesDepuisDiplome: annees, promotionLibelle: 'Promotion 2026' } as AlumniMetriques),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(MonProfil);
    fixture.detectChanges();
    const tuile = Array.from(fixture.nativeElement.querySelectorAll('article') as NodeListOf<HTMLElement>)
      .find((a) => a.textContent?.includes('Diplômé(e) depuis'));
    return (tuile?.querySelector('p')?.textContent ?? '').replace(/\s+/g, ' ').trim();
  }

  it("dit « Moins d'un an » plutôt que « 0 an » pour un diplôme de l'année", () => {
    expect(tuileAnciennete(0)).toBe("Moins d'un an");
  });

  it('accorde « an » au singulier et « ans » au pluriel', () => {
    expect(tuileAnciennete(1)).toBe('1 an');
  });

  it('affiche « 3 ans » pour trois ans', () => {
    expect(tuileAnciennete(3)).toBe('3 ans');
  });
});
