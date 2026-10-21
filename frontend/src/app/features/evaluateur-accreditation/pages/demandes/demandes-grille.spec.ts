import { TestBed } from '@angular/core/testing';
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
 * Ouvrir un dossier « en cours d'évaluation » figeait le navigateur : la grille (31 champs `ngModel`) bouclait dans un
 * `*ngFor` alimenté par un accesseur qui reconstruisait ses tableaux à chaque lecture — chaque passage de détection de
 * changements recréait les champs, dont l'écriture asynchrone relançait un passage, sans fin.
 *
 * Le test vérifie la cause (identité stable des sections entre deux lectures) plutôt que d'attendre un gel, qui ne se
 * signale pas : une boucle de micro-tâches empêche jusqu'au délai d'expiration du test de se déclencher.
 */
describe('EvaluateurDemandesComponent — grille d’évaluation', () => {
  function enCours(): DemandeAccreditationEvaluateur {
    return {
      ...demande(7),
      statut: 'EN_COURS_EVALUATION',
      criteresActifs: Array.from({ length: 31 }, (_, i) => ({
        id: i + 1, code: `${1 + (i % 4)}.${i}`, libelle: `Condition ${i + 1}`, description: null,
        section: `${1 + (i % 4)}. Section`, actif: true, ordre: i,
      })) as never,
    };
  }

  function monter(d: DemandeAccreditationEvaluateur) {
    TestBed.configureTestingModule({
      imports: [EvaluateurDemandesComponent, RouterTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } }],
    });
    const fixture = TestBed.createComponent(EvaluateurDemandesComponent);
    const service = TestBed.inject(EvaluateurAccreditationService);
    vi.spyOn(service, 'getDemandes').mockReturnValue(of([d]));
    vi.spyOn(service, 'getDemande').mockReturnValue(of(d));
    fixture.detectChanges();
    fixture.componentInstance.selectionner(d);
    return fixture;
  }

  it('garde les mêmes sections d’une lecture à l’autre (sinon le gabarit se recrée sans fin)', () => {
    const composant = monter(enCours()).componentInstance;
    expect(composant.sectionsGrille).toBe(composant.sectionsGrille);
    expect(composant.sectionsGrille.length).toBe(4);
    expect(composant.sectionsGrille.reduce((s, x) => s + x.total, 0)).toBe(31);
  });
});
