import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Subject } from 'rxjs';

import { FormationDetailComponent } from './formation-detail.component';
import { FormationDetailPublique, RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

/**
 * Tant que la réponse de l'API n'était pas arrivée, `formation` valait null et le gabarit (`*ngIf="formation; else
 * notFoundTpl"`) affichait « Formation non trouvée » : le visiteur qui cliquait sur une carte du catalogue voyait un
 * message d'erreur pendant le chargement, avant que la fiche ne s'affiche. Les drapeaux `chargement` et `notFound`
 * existaient dans le composant, mais le gabarit ne les lisait pas.
 */
describe('FormationDetailComponent — états de la page', () => {
  let reponse: Subject<FormationDetailPublique>;

  beforeEach(() => {
    reponse = new Subject<FormationDetailPublique>();
  });

  function monter(id: string) {
    TestBed.configureTestingModule({
      imports: [FormationDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id }) } } },
        { provide: RecrutementService, useValue: { getFormationPublique: () => reponse } },
      ],
    });
    const fixture = TestBed.createComponent(FormationDetailComponent);
    fixture.detectChanges();
    return fixture;
  }

  const texte = (f: { nativeElement: HTMLElement }) => f.nativeElement.textContent ?? '';

  it("n'annonce pas « Formation non trouvée » pendant le chargement", () => {
    const fixture = monter('8');

    expect(texte(fixture)).not.toContain('Formation non trouvée');
    expect(texte(fixture)).toContain('Chargement de la formation');
  });

  it('affiche la fiche une fois la réponse reçue, sans message de chargement ni d’erreur', () => {
    const fixture = monter('8');

    reponse.next({
      id: 8, code: 'HSE', intitule: 'Certifications HSE', description: 'Survie, incendie, H2S', niveau: 'Certificat',
      domaine: 'Sécurité', duree: '3 jours', imageUrl: null, ficheDetaillee: null,
    } as FormationDetailPublique);
    fixture.detectChanges();

    expect(texte(fixture)).toContain('Certifications HSE');
    expect(texte(fixture)).not.toContain('Formation non trouvée');
    expect(texte(fixture)).not.toContain('Chargement de la formation');
  });

  it('annonce « Formation non trouvée » quand l’API répond par une erreur', () => {
    const fixture = monter('999');

    reponse.error({ status: 404 });
    fixture.detectChanges();

    expect(texte(fixture)).toContain('Formation non trouvée');
    expect(texte(fixture)).not.toContain('Chargement de la formation');
  });

  it('annonce « Formation non trouvée » d’emblée quand l’identifiant de l’adresse n’est pas un nombre', () => {
    const fixture = monter('abc');

    expect(texte(fixture)).toContain('Formation non trouvée');
  });
});
