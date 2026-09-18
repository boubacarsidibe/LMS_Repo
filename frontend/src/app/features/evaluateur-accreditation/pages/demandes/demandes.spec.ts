import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { EvaluateurDemandesComponent } from './demandes';
import { EvaluateurAccreditationService } from '../../services/evaluateur-accreditation.service';
import { DemandeAccreditationEvaluateur } from '../../../accreditation/models/accreditation.models';

function demande(id: number): DemandeAccreditationEvaluateur {
  return {
    id,
    statut: 'SOUMISE',
    dateSoumission: '2026-08-01',
    dateDecision: null,
    dateExpiration: null,
    motifRejet: null,
    commentaireEvaluation: null,
    planActions: null,
    ecoleId: 1,
    nomEcole: `École ${id}`,
    intituleProgramme: null,
    niveauProgramme: null,
    typeStructure: null,
    effectifCohorte: null,
    dateDebutCohorte: null,
    pieces: [],
    adresse: '',
    telephone: '',
    siteWeb: null,
    emailEcole: null,
    avis: [],
    criteresActifs: [],
    maGrille: [],
  };
}

/**
 * Avant cette story, cet écran n'était atteignable que via sa liste — rien
 * ne permettait de renvoyer directement vers un dossier précis depuis une
 * autre page (le nouveau tableau de bord de l'espace évaluateur en a besoin
 * pour ses liens « en attente de votre avis »).
 */
describe('EvaluateurDemandesComponent — lien direct par ?id=', () => {
  let fixture: ComponentFixture<EvaluateurDemandesComponent>;
  let component: EvaluateurDemandesComponent;
  let service: EvaluateurAccreditationService;

  function configurer(idQueryParam: string | null) {
    TestBed.configureTestingModule({
      imports: [EvaluateurDemandesComponent, RouterTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(idQueryParam ? { id: idQueryParam } : {}) } },
        },
      ],
    });
    fixture = TestBed.createComponent(EvaluateurDemandesComponent);
    component = fixture.componentInstance;
    service = TestBed.inject(EvaluateurAccreditationService);
  }

  it('sélectionne automatiquement le dossier désigné par ?id=', () => {
    configurer('2');
    const demandes = [demande(1), demande(2), demande(3)];
    vi.spyOn(service, 'getDemandes').mockReturnValue(of(demandes));
    vi.spyOn(service, 'getDemande').mockReturnValue(of(demande(2)));

    fixture.detectChanges();

    expect(component.selection?.id).toBe(2);
  });

  it('ne sélectionne rien sans ?id= (comportement inchangé)', () => {
    configurer(null);
    vi.spyOn(service, 'getDemandes').mockReturnValue(of([demande(1)]));

    fixture.detectChanges();

    expect(component.selection).toBeNull();
  });

  it('ignore un ?id= qui ne correspond à aucun dossier', () => {
    configurer('999');
    vi.spyOn(service, 'getDemandes').mockReturnValue(of([demande(1)]));

    fixture.detectChanges();

    expect(component.selection).toBeNull();
  });
});
