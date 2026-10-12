import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TypeRecrutement, TypeRecrutementComponent } from './type-recrutement.component';

describe('TypeRecrutementComponent', () => {
  let fixture: ComponentFixture<TypeRecrutementComponent>;
  const el = () => fixture.nativeElement as HTMLElement;
  const liens = () => Array.from(el().querySelectorAll('a')) as HTMLAnchorElement[];
  const actifs = () => liens().filter((a) => a.getAttribute('aria-current') === 'page');

  function afficher(type: TypeRecrutement): void {
    fixture.componentRef.setInput('type', type);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TypeRecrutementComponent],
      providers: [provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(TypeRecrutementComponent);
  });

  it('propose les deux recrutements, chacun vers sa propre page', () => {
    afficher('ETUDIANT');
    expect(liens().map((a) => a.textContent!.replace('school', '').replace('co_present', '').trim())).toEqual([
      'Étudiants',
      'Formateurs',
    ]);
    expect(liens().map((a) => a.getAttribute('href'))).toEqual([
      '/responsable-admission/sessions',
      '/responsable-admission/recrutement-formateurs/offres',
    ]);
  });

  it("marque comme page courante le seul segment du type affiché", () => {
    afficher('ETUDIANT');
    expect(actifs().map((a) => a.getAttribute('href'))).toEqual(['/responsable-admission/sessions']);

    afficher('FORMATEUR');
    expect(actifs().map((a) => a.getAttribute('href'))).toEqual([
      '/responsable-admission/recrutement-formateurs/offres',
    ]);
  });

  it("n'habille en actif que le segment courant (pas de style sur l'autre)", () => {
    afficher('FORMATEUR');
    const [etudiants, formateurs] = liens();
    expect(formateurs.classList.contains('bg-primary-container')).toBe(true);
    expect(etudiants.classList.contains('bg-primary-container')).toBe(false);
  });
});
