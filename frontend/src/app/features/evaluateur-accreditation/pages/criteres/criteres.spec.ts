import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { EvaluateurCriteresComponent } from './criteres';
import { EvaluateurAccreditationService } from '../../services/evaluateur-accreditation.service';
import { CritereAccreditation } from '../../../accreditation/models/accreditation.models';

function critere(id: number, section: string, actif = true): CritereAccreditation {
  return { id, code: `${id}`, section, libelle: `Condition ${id}`, description: null, ordre: id, actif };
}

/**
 * Avant cette story, le référentiel affichait ses 31 conditions à plat, sans
 * regroupement dépliable par section — un utilisateur externe suggérait un
 * accordéon par grande ligne (1. Management de la qualité, 2. Suivi et
 * évaluation des compétences, etc.) pour parcourir les quatre sections sans
 * dérouler la totalité des critères.
 */
describe('EvaluateurCriteresComponent', () => {
  let component: EvaluateurCriteresComponent;
  let fixture: ComponentFixture<EvaluateurCriteresComponent>;
  let service: EvaluateurAccreditationService;

  const criteresDemo = [critere(1, '1. Management de la qualité'), critere(2, '2. Suivi et évaluation des compétences')];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EvaluateurCriteresComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EvaluateurCriteresComponent);
    component = fixture.componentInstance;
    service = TestBed.inject(EvaluateurAccreditationService);
    vi.spyOn(service, 'getCriteres').mockReturnValue(of(criteresDemo));
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('toutes les sections sont repliées après le chargement', () => {
    expect(component.sections.length).toBe(2);
    expect(component.sections.every((s) => !component.sectionEstOuverte(s.nom))).toBe(true);
    expect(component.toutEstOuvert).toBe(false);
  });

  it('basculerSection ouvre puis referme une seule section', () => {
    const [premiere] = component.sections;

    component.basculerSection(premiere.nom);
    expect(component.sectionEstOuverte(premiere.nom)).toBe(true);

    component.basculerSection(premiere.nom);
    expect(component.sectionEstOuverte(premiere.nom)).toBe(false);
  });

  it('basculerTout ouvre toutes les sections et les lignes directrices, puis les referme', () => {
    component.basculerTout();

    expect(component.toutEstOuvert).toBe(true);
    expect(component.sections.every((s) => component.sectionEstOuverte(s.nom))).toBe(true);
    expect(component.detailsOuverts).toBe(true);

    component.basculerTout();

    expect(component.toutEstOuvert).toBe(false);
    expect(component.sections.every((s) => !component.sectionEstOuverte(s.nom))).toBe(true);
    expect(component.detailsOuverts).toBe(false);
  });

  it("l'ajout d'une condition ouvre automatiquement sa section", () => {
    const nouvelle = critere(3, '3. Structure et contenu des programmes');
    vi.spyOn(service, 'creerCritere').mockReturnValue(of(nouvelle));
    component.nouveauLibelle = 'Nouvelle condition';
    component.nouvelleSection = nouvelle.section!;

    component.creer();

    expect(component.sectionEstOuverte(nouvelle.section!)).toBe(true);
  });
});
