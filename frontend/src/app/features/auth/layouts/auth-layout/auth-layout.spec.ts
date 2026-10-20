import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthLayoutComponent } from './auth-layout';

describe('AuthLayoutComponent', () => {
  let component: AuthLayoutComponent;
  let fixture: ComponentFixture<AuthLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthLayoutComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AuthLayoutComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('n\'écrit plus « INPG LMS » au-dessus du formulaire : le logo officiel le remplace', () => {
    fixture.detectChanges();
    const texte = ((fixture.nativeElement as HTMLElement).textContent ?? '').replace(/\s+/g, ' ');

    expect(texte).toContain('Excellence'); // garde-fou : le gabarit est bien rendu, la vérification ci-dessous n'est pas vacueuse

    expect(texte).not.toMatch(/INPG\s*LMS/);
  });

  it('garde le logo en couleur sur mobile seulement, là où le panneau sombre (et son logo blanc) est masqué', () => {
    fixture.detectChanges();
    const logos = (fixture.nativeElement as HTMLElement).querySelectorAll('app-logo-inpg');
    const surMobile = (fixture.nativeElement as HTMLElement).querySelector('app-logo-inpg.md\\:hidden img') as HTMLImageElement;

    expect(logos.length).toBe(2); // blanc (panneau, md+) + couleur (formulaire, mobile)
    expect(surMobile.getAttribute('src')).toBe('/images/logo-inpg.png');
  });
});
