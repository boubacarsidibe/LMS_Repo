import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { MesCours } from './mes-cours';

describe('MesCours', () => {
  let component: MesCours;
  let fixture: ComponentFixture<MesCours>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MesCours, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MesCours);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('compte les étudiants distincts, pas la somme des effectifs par cours', () => {
    // Deux cours de 19 étudiants chacun : la somme donnerait 38 pour 19 personnes.
    component.cours = [{ nbEtudiants: 19 }, { nbEtudiants: 19 }] as never;
    component.mesEtudiants = Array.from({ length: 19 }, (_, i) => ({ id: i + 1 })) as never;
    expect(component.totalEtudiantsInscrits).toBe(19);
  });

  it('n’affiche aucun chiffre tant que la liste des étudiants n’est pas connue', () => {
    component.cours = [{ nbEtudiants: 19 }] as never;
    component.mesEtudiants = null;
    expect(component.totalEtudiantsInscrits).toBeNull();
  });
});
