import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { CertificatService } from '../../services/certificat.service';
import { ScolariteService } from '../../services/scolarite.service';
import { ReleveDelivre, ReleveNotes } from '../../models/scolarite.models';
import { MesResultats } from './mes-resultats';

/**
 * Les notes de module se saisissent sur 100 (le formateur, côté serveur : « entre 0 et 100 ») et la page
 * affichait déjà chaque note « n/100 (n/5 sur 20) ». Mais la moyenne générale, la moyenne par cours et les relevés
 * délivrés — calculés sur ces mêmes notes — étaient étiquetés « /20 » : un étudiant à 47,5 sur 100 lisait
 * « 47,5 / 20 », un chiffre impossible, juste au-dessus de ses notes affichées sur 100.
 */
describe('MesResultats — barème des notes', () => {
  let fixture: ComponentFixture<MesResultats>;

  const releve: ReleveNotes = {
    moyenneGenerale: 47.5,
    nombreNotes: 2,
    parCours: [
      {
        coursId: 1,
        coursCode: 'DS-101',
        coursTitre: 'Introduction au Machine Learning',
        moyenne: 47.5,
        coefficient: 1,
        notes: [
          { id: 1, module: 'Fondamentaux', note: 60, commentaire: null, dateEvaluation: '2026-10-01', coursId: 1, coursCode: 'DS-101', coursTitre: 'ML', formateurNom: 'M. Ndiaye' },
          { id: 2, module: 'Supervisé', note: 35, commentaire: null, dateEvaluation: '2026-10-05', coursId: 1, coursCode: 'DS-101', coursTitre: 'ML', formateurNom: 'M. Ndiaye' },
        ],
      },
    ],
  };

  const releveDelivre: ReleveDelivre = {
    id: 7,
    reference: 'REL-2026-0007',
    etudiantNom: 'Bineta Ndour',
    matricule: 'ETU-TEST-001',
    promotionLibelle: 'M1-DS-2026',
    periode: 'Semestre 1 — 2026/2027',
    dateEmission: '2026-10-10',
    delivrePar: 'Direction pédagogique',
    moyenneGenerale: 50,
    nombreNotes: 2,
    lignes: [],
  };

  const texte = () => ((fixture.nativeElement as HTMLElement).textContent ?? '').replace(/\s+/g, ' ');

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MesResultats],
      providers: [
        provideRouter([]),
        { provide: ScolariteService, useValue: { getMesNotes: () => of(releve), getMesReleves: () => of([releveDelivre]) } },
        { provide: CertificatService, useValue: { getAll: () => of([]) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MesResultats);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('annonce la moyenne générale sur 100, avec son équivalent sur 20', () => {
    expect(texte()).toContain('47.5 / 100');
    expect(texte()).toContain('(9.5 / 20)');
    expect(texte()).not.toContain('47.5 / 20');
  });

  it('annonce la moyenne de chaque cours sur 100', () => {
    expect(texte()).toMatch(/Moyenne\s*47\.5\s*\/100/);
  });

  it('annonce la moyenne d\'un relevé délivré sur 100, avec son équivalent sur 20', () => {
    expect(texte()).toContain('50.0/100 (10.0/20)');
  });

  it('garde l\'affichage de chaque note sur 100 avec son équivalent sur 20', () => {
    expect(texte()).toContain('60/100 (12/20)');
    expect(texte()).toContain('35/100 (7/20)');
  });
});
