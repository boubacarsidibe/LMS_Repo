import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LogoInpgComponent } from './logo-inpg.component';

/**
 * Le logotype de l'INPG est l'image du logo officiel, plus l'écriture « INPG LMS » : ce composant en est l'unique
 * source, posée dans le navbar, les barres latérales et les en-têtes de tous les espaces.
 */
describe('LogoInpgComponent', () => {
  let fixture: ComponentFixture<LogoInpgComponent>;

  const image = () => fixture.nativeElement.querySelector('img') as HTMLImageElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [LogoInpgComponent] }).compileComponents();
    fixture = TestBed.createComponent(LogoInpgComponent);
  });

  it("affiche l'image du logo officiel, avec un texte alternatif", () => {
    fixture.detectChanges();

    expect(image().getAttribute('src')).toBe('/images/logo-inpg.png');
    expect(image().alt).toContain('Institut National du Pétrole et du Gaz');
  });

  it("n'écrit rien : le logotype est l'image, plus du texte « INPG LMS »", () => {
    fixture.detectChanges();

    expect((fixture.nativeElement.textContent as string).trim()).toBe('');
  });

  it('la hauteur se règle par une classe Tailwind et la largeur suit les proportions du logo', () => {
    fixture.componentRef.setInput('hauteur', 'h-12');
    fixture.detectChanges();

    expect(image().classList).toContain('h-12');
    expect(image().classList).toContain('w-auto');
    expect(image().classList).not.toContain('h-10');
  });

  it('a une hauteur de 2,5 rem par défaut', () => {
    fixture.detectChanges();

    expect(image().classList).toContain('h-10');
  });
});
