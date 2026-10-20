import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { Subject } from 'rxjs';

import { ProfilEtudiant } from './profil-etudiant';
import { EtudiantService } from '../../services/etudiant.service';
import { Etudiant } from '../../models/etudiant';

describe('ProfilEtudiant', () => {
  let component: ProfilEtudiant;
  let fixture: ComponentFixture<ProfilEtudiant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfilEtudiant, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProfilEtudiant);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

/**
 * Tant que la fiche n'était pas arrivée, `etudiant` valait undefined et le gabarit (`*ngIf="etudiant; else
 * introuvable"`) affichait « Étudiant introuvable. » : le formateur qui ouvrait un profil voyait un message d'erreur
 * pendant le chargement, avant que la fiche ne s'affiche.
 */
describe('ProfilEtudiant — états de la page', () => {
  let reponse: Subject<Etudiant>;

  function monter() {
    reponse = new Subject<Etudiant>();
    TestBed.configureTestingModule({
      imports: [ProfilEtudiant, RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } } },
        { provide: EtudiantService, useValue: { getById: () => reponse } },
      ],
    });
    const fixture = TestBed.createComponent(ProfilEtudiant);
    fixture.detectChanges();
    return fixture;
  }

  const texte = (f: { nativeElement: HTMLElement }) => f.nativeElement.textContent ?? '';

  it("n'annonce pas « Étudiant introuvable » pendant le chargement", () => {
    const fixture = monter();

    expect(texte(fixture)).not.toContain('Étudiant introuvable');
    expect(texte(fixture)).toContain('Chargement du profil');
  });

  it('annonce « Étudiant introuvable » quand l’API répond par une erreur', () => {
    const fixture = monter();

    reponse.error({ status: 404 });
    fixture.detectChanges();

    expect(texte(fixture)).toContain('Étudiant introuvable');
    expect(texte(fixture)).not.toContain('Chargement du profil');
  });
});
