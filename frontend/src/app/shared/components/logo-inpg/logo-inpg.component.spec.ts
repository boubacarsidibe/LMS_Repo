import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { LogoInpgComponent } from './logo-inpg.component';

/**
 * Le logotype de l'INPG est l'image du logo officiel, plus l'écriture « INPG LMS » : ce composant en est l'unique
 * source, posée dans le navbar, les barres latérales et les en-têtes de tous les espaces.
 */
describe('LogoInpgComponent', () => {
  let fixture: ComponentFixture<LogoInpgComponent>;

  const image = () => fixture.nativeElement.querySelector('img') as HTMLImageElement;
  const lien = () => fixture.nativeElement.querySelector('a') as HTMLAnchorElement | null;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [LogoInpgComponent], providers: [provideRouter([])] }).compileComponents();
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

  it("est cliquable et ramène à la page d'accueil", () => {
    fixture.detectChanges();

    expect(lien()).not.toBeNull();
    expect(lien()!.getAttribute('href')).toBe('/accueil');
    expect(lien()!.contains(image())).toBe(true);
  });

  it("le lien se nomme pour un lecteur d'écran et annonce sa destination", () => {
    fixture.detectChanges();

    expect(lien()!.getAttribute('aria-label')).toContain("retour à l'accueil");
    expect(lien()!.getAttribute('aria-label')).toContain('Institut National du Pétrole et du Gaz');
    expect(lien()!.title).toBe("Retour à l'accueil");
  });

  it("n'écrit toujours rien : le lien n'ajoute aucun texte visible", () => {
    fixture.detectChanges();

    expect((fixture.nativeElement.textContent as string).trim()).toBe('');
  });

  it("[cliquable]=false pose l'image seule, sans lien (pour un logo déjà dans un lien)", () => {
    fixture.componentRef.setInput('cliquable', false);
    fixture.detectChanges();

    expect(lien()).toBeNull();
    expect(image().getAttribute('src')).toBe('/images/logo-inpg.png');
  });

  it("la zone cliquable épouse le logo : un bloc qui s'étire ferait naviguer en cliquant à côté", () => {
    fixture.detectChanges();

    expect(lien()!.classList).toContain('flex');
    expect(lien()!.classList).toContain('w-fit');
  });
});
