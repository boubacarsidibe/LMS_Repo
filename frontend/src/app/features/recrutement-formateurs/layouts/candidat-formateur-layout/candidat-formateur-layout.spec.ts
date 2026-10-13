import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ContexteNavbarService } from '../../../../shared/components/navbar/contexte-navbar.service';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { CandidatFormateurLayout } from './candidat-formateur-layout';

/**
 * L'espace Candidat formateur utilise le menu public partagé, comme tout le parcours du candidat : plus de barre
 * latérale propre à cet espace.
 */
describe('CandidatFormateurLayout', () => {
  let fixture: ComponentFixture<CandidatFormateurLayout>;
  const el = () => fixture.nativeElement as HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [CandidatFormateurLayout], providers: [provideRouter([])] });
    TestBed.overrideComponent(CandidatFormateurLayout, {
      remove: { imports: [NavbarComponent, FooterComponent] },
      add: { schemas: [NO_ERRORS_SCHEMA] },
    });
    fixture = TestBed.createComponent(CandidatFormateurLayout);
    fixture.detectChanges();
  });

  it('pose le menu public partagé en haut, le contenu au milieu et le pied de page en bas', () => {
    const enfants = Array.from(el().querySelector('div')!.children).map((e) => e.tagName.toLowerCase());

    expect(enfants).toEqual(['app-navbar', 'main', 'app-footer']);
    expect(el().querySelector('main router-outlet')).not.toBeNull();
  });

  it("n'a plus de barre latérale à lui ni de texte « INPG LMS »", () => {
    expect(el().querySelector('aside')).toBeNull();
    expect(el().textContent).not.toContain('INPG LMS');
  });

  it('place le menu en contexte « formateur » : son onglet propre est « Offres de recrutement »', () => {
    expect(TestBed.inject(ContexteNavbarService).contexte()).toBe('formateur');
  });
});
