import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { EvaluateurTableauDeBordComponent } from './tableau-de-bord';
import { EvaluateurAccreditationService } from '../../services/evaluateur-accreditation.service';
import { Auth } from '../../../../core/auth/services/authService';
import { DemandeAccreditationEvaluateur, CritereAccreditation, EvaluationCritere } from '../../../accreditation/models/accreditation.models';

function critere(id: number, section: string): CritereAccreditation {
  return { id, code: `${id}`, section, libelle: `Critère ${id}`, description: null, ordre: id, actif: true };
}

function evaluation(critereId: number, appreciation: EvaluationCritere['appreciation']): EvaluationCritere {
  return { critereId, critereLibelle: `Critère ${critereId}`, critereDescription: null, appreciation, commentaire: null, dateEvaluation: '2026-09-01' };
}

function demande(
  id: number,
  statut: DemandeAccreditationEvaluateur['statut'],
  avis: DemandeAccreditationEvaluateur['avis'],
  criteresActifs: CritereAccreditation[],
  maGrille: EvaluationCritere[]
): DemandeAccreditationEvaluateur {
  return {
    id,
    statut,
    dateSoumission: '2026-08-01',
    dateDecision: null,
    dateExpiration: null,
    motifRejet: null,
    commentaireEvaluation: null,
    planActions: null,
    ecoleId: 1,
    nomEcole: `École ${id}`,
    intituleProgramme: 'Programme test',
    niveauProgramme: null,
    typeStructure: null,
    effectifCohorte: null,
    dateDebutCohorte: null,
    pieces: [],
    adresse: '',
    telephone: '',
    siteWeb: null,
    emailEcole: null,
    avis,
    criteresActifs,
    maGrille,
  };
}

/**
 * Avant cette story, l'espace évaluateur n'avait aucune page d'accueil (la
 * route racine redirigeait directement vers « Demandes ») : rien ne
 * signalait combien de dossiers attendaient un avis, ni où se concentraient
 * les non-conformités d'une section à l'autre du référentiel.
 */
describe('EvaluateurTableauDeBordComponent', () => {
  let component: EvaluateurTableauDeBordComponent;
  let fixture: ComponentFixture<EvaluateurTableauDeBordComponent>;
  let service: EvaluateurAccreditationService;
  let auth: Auth;

  const critereQualite = critere(1, '1. Management de la qualité');
  const critereSuivi = critere(2, '2. Suivi et évaluation des compétences');

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EvaluateurTableauDeBordComponent, RouterTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(EvaluateurTableauDeBordComponent);
    component = fixture.componentInstance;
    service = TestBed.inject(EvaluateurAccreditationService);
    auth = TestBed.inject(Auth);
    auth.currentUser.set({ id: 42, username: 'evaluateur', email: 'e@inpg.sn', roles: ['ROLE_EVALUATEUR_ACCREDITATION'] });
  });

  it('should create', () => {
    vi.spyOn(service, 'getDemandes').mockReturnValue(of([]));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it("compte les dossiers actifs et ceux qui attendent l'avis de l'évaluateur connecté", () => {
    const demandes = [
      demande(1, 'SOUMISE', [], [], []),
      demande(2, 'EN_COURS_EVALUATION', [{ id: 9, evaluateurId: 42, evaluateurNom: 'Moi', avis: 'FAVORABLE', commentaire: null, dateAvis: '2026-09-01' }], [], []),
      demande(3, 'ACCREDITE', [], [], []),
    ];
    vi.spyOn(service, 'getDemandes').mockReturnValue(of(demandes));

    fixture.detectChanges();

    expect(component.dossiersActifs.length).toBe(2);
    // Le dossier 2 porte déjà un avis de l'évaluateur connecté (id 42) : il ne doit pas ressortir.
    expect(component.dossiersEnAttenteDeMonAvis.map((d) => d.id)).toEqual([1]);
  });

  it('agrège la conformité par section à partir de maGrille, en ignorant les autres évaluateurs', () => {
    const demandes = [
      demande(1, 'EN_COURS_EVALUATION', [], [critereQualite, critereSuivi], [
        evaluation(1, 'CONFORME'),
        evaluation(2, 'NON_CONFORME'),
      ]),
      demande(2, 'EN_COURS_EVALUATION', [], [critereQualite], [evaluation(1, 'CONFORME')]),
    ];
    vi.spyOn(service, 'getDemandes').mockReturnValue(of(demandes));

    fixture.detectChanges();

    const sections = component.conformiteParSection;
    const qualite = sections.find((s) => s.nom === critereQualite.section);
    const suivi = sections.find((s) => s.nom === critereSuivi.section);
    expect(qualite?.conformes).toBe(2);
    expect(qualite?.tauxConformite).toBe(100);
    expect(suivi?.nonConformes).toBe(1);
    expect(suivi?.tauxConformite).toBe(0);
  });

  it('nombreDossiersJuges ne compte que les dossiers où maGrille contient au moins un jugement', () => {
    const demandes = [
      demande(1, 'EN_COURS_EVALUATION', [], [critereQualite], [evaluation(1, 'CONFORME')]),
      demande(2, 'SOUMISE', [], [critereQualite], []),
    ];
    vi.spyOn(service, 'getDemandes').mockReturnValue(of(demandes));

    fixture.detectChanges();

    expect(component.nombreDossiersJuges).toBe(1);
  });
});
