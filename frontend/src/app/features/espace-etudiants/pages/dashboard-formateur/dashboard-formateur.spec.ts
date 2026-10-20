import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { DashboardFormateur } from './dashboard-formateur';
import { Etudiant } from '../../models/etudiant';
import { RetourEvaluation } from '../../models/retour-evaluation';

describe('DashboardFormateur', () => {
  let component: DashboardFormateur;
  let fixture: ComponentFixture<DashboardFormateur>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardFormateur, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DashboardFormateur);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  const etudiant = (progression: number, statut: 'Actif' | 'À risque'): Etudiant => ({
    id: 1, nom: 'X', promotion: 'P', derniereActivite: 'Aucune activité', progression,
    noteMoyenne: null, statut, initiales: 'X', couleurAvatar: '',
  });
  const retour = (note: number, statut: 'valide' | 'action-requise'): RetourEvaluation => ({
    id: 1, note, commentaire: '', date: '', dateIso: '', coursId: 1, cours: 'C', statut, epingle: false,
  });

  it('calcule la progression moyenne des étudiants, et non un zéro codé en dur', () => {
    component.mesEtudiants = [etudiant(100, 'Actif'), etudiant(50, 'Actif'), etudiant(0, 'À risque')];
    expect(component.progressionMoyenne).toBe(50);
    expect(component.etudiantsAAccompagner).toBe(1);
  });

  it('affiche 0 % sans étudiant, sans diviser par zéro', () => {
    component.mesEtudiants = [];
    expect(component.progressionMoyenne).toBe(0);
  });

  it('calcule la note moyenne des retours au dixième, null tant qu’il n’y en a aucun', () => {
    component.retours = [];
    expect(component.noteMoyenneRetours).toBeNull();
    component.retours = [retour(5, 'valide'), retour(4, 'valide'), retour(2, 'action-requise')];
    expect(component.noteMoyenneRetours).toBe(3.7);
    expect(component.retoursAAgir).toBe(1);
  });

  it('n’annonce plus « Bientôt disponible » pour des fonctions qui existent', () => {
    component.mesEtudiants = [etudiant(80, 'Actif')];
    component.retours = [retour(4, 'valide')];
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Bientôt disponible');
  });
});
