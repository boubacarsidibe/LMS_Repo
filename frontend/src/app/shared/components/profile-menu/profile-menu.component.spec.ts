import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ProfileMenuComponent } from './profile-menu.component';

describe('ProfileMenuComponent', () => {
  let fixture: ComponentFixture<ProfileMenuComponent>;
  let composant: ProfileMenuComponent;
  const el = () => fixture.nativeElement as HTMLElement;
  const bouton = () => el().querySelector('button[aria-haspopup="menu"]') as HTMLButtonElement;
  /** Aucun chevron à côté du cercle de profil (retiré à la demande du 2026-10-12). */
  const chevrons = () => Array.from(el().querySelectorAll('.material-symbols-outlined')).map(i => i.textContent!.trim()).filter(t => t.startsWith('expand_'));

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ProfileMenuComponent], providers: [provideRouter([])] }).compileComponents();
    fixture = TestBed.createComponent(ProfileMenuComponent);
    composant = fixture.componentInstance;
    composant.initiales = 'TE';
    composant.nom = 'test etudiant';
    composant.email = 'test.etudiant@inpg.sn';
    composant.lienProfil = '/espace-etudiant/mon-profil';
    fixture.detectChanges();
  });

  it('est fermé au départ, et n\'affiche aucun chevron', () => {
    expect(el().querySelector('[role="menu"]')).toBeNull();
    expect(chevrons()).toEqual([]);
  });

  it('un clic sur le cercle ouvre la liste, toujours sans chevron', () => {
    bouton().click();
    fixture.detectChanges();
    expect(el().querySelector('[role="menu"]')).not.toBeNull();
    expect(chevrons()).toEqual([]);
    const items = Array.from(el().querySelectorAll('[role="menuitem"]')).map(i => i.textContent!.trim());
    expect(items.some(t => t.includes('Mon profil'))).toBe(true);
    expect(items.some(t => t.includes('Déconnexion'))).toBe(true);
  });

  it('la déconnexion émet l\'événement et referme le menu', () => {
    let emis = 0;
    composant.deconnexion.subscribe(() => emis++);
    bouton().click();
    fixture.detectChanges();
    (Array.from(el().querySelectorAll('button[role="menuitem"]'))[0] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(emis).toBe(1);
    expect(el().querySelector('[role="menu"]')).toBeNull();
  });

  it('un clic à l\'extérieur ou la touche Échap referment le menu', () => {
    bouton().click();
    fixture.detectChanges();
    document.body.click();
    fixture.detectChanges();
    expect(el().querySelector('[role="menu"]')).toBeNull();

    bouton().click();
    fixture.detectChanges();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(el().querySelector('[role="menu"]')).toBeNull();
  });
});
