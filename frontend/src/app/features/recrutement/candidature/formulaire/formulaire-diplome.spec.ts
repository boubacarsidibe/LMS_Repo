import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Auth } from '../../../../core/auth/services/authService';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { NotificationService } from '../../../../shared/components/ui/notification.service';
import { FormulaireComponent } from './formulaire.component';

/**
 * Attestation de diplôme : exigée par l'INPG dans tout dossier, sauf d'un candidat de niveau « 4e secondaire »,
 * qui accède aux CPS sans diplôme (questionnaire de collecte du 2026-09-30).
 */
describe('FormulaireComponent — attestation de diplôme', () => {
  let fixture: ComponentFixture<FormulaireComponent>;
  let composant: FormulaireComponent;
  const el = () => fixture.nativeElement as HTMLElement;
  const texte = () => el().textContent!.replace(/\s+/g, ' ');
  const fichier = () => new File(['%PDF-1.4'], 'diplome.pdf', { type: 'application/pdf' });

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [FormulaireComponent],
      providers: [
        provideRouter([]),
        {
          provide: RecrutementService,
          useValue: {
            getMesCandidatures: () => of([]),
            getCataloguePublic: () => of([]),
            getBrouillon: () => of(null),
            sauvegarderBrouillon: () => of(null),
          },
        },
        { provide: Auth, useValue: { currentUser: () => null, isLoggedIn: () => true } },
        { provide: NotificationService, useValue: { erreur: () => undefined, succes: () => undefined } },
      ],
    });
    TestBed.overrideComponent(FormulaireComponent, {
      remove: { imports: [NavbarComponent, FooterComponent] },
      add: { schemas: [NO_ERRORS_SCHEMA] },
    });
    fixture = TestBed.createComponent(FormulaireComponent);
    composant = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => localStorage.clear());

  function afficherEtapeDocuments(niveau: string): void {
    composant.data.niveauActuel = niveau;
    composant.etape = 3;
    fixture.detectChanges();
  }

  it('est obligatoire pour un candidat de Licence : intitulé « Attestation de diplôme » et badge « Requis »', () => {
    afficherEtapeDocuments('Licence (L3)');

    expect(composant.diplomeObligatoire).toBe(true);
    expect(texte()).toContain('Attestation de diplôme');
    expect(texte()).toContain('Au moins une attestation de diplôme est exigée');
    expect(texte()).not.toContain('Diplômes et certificats obtenus antérieurement');
  });

  it('reste facultatif pour un candidat de niveau 4e secondaire (CPS)', () => {
    afficherEtapeDocuments('4e secondaire');

    expect(composant.diplomeObligatoire).toBe(false);
    expect(composant.diplomeManquant).toBe(false);
    expect(texte()).toContain('Diplômes et certificats obtenus antérieurement');
    expect(texte()).toContain('Facultatif');
    expect(texte()).not.toContain('Au moins une attestation de diplôme est exigée');
  });

  it("manque tant qu'aucune entrée n'a son fichier ET sa description", () => {
    composant.data.niveauActuel = 'Master 2 (M2)';
    expect(composant.diplomeManquant).toBe(true);

    composant.diplomes = [{ file: fichier(), description: '   ' }];
    expect(composant.diplomeManquant).toBe(true); // fichier sans description

    composant.diplomes = [{ file: null, description: 'Master 2, UCAD, 2023' }];
    expect(composant.diplomeManquant).toBe(true); // description sans fichier

    composant.diplomes = [{ file: fichier(), description: 'Master 2, UCAD, 2023' }];
    expect(composant.diplomeManquant).toBe(false);
  });

  it('signale le manque à l\'écran une fois la validation demandée', () => {
    composant.data.niveauActuel = 'Baccalauréat';
    composant.etape = 3;
    composant.showErrors = true;
    fixture.detectChanges();

    expect(el().querySelector('[role="alert"]')?.textContent).toContain('Joignez au moins une attestation de diplôme');
  });

  it('exige le diplôme tant que le niveau n\'est pas choisi', () => {
    composant.data.niveauActuel = '';
    expect(composant.diplomeObligatoire).toBe(true);
  });
});
