import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthLayoutComponent } from './auth-layout';

describe('AuthLayoutComponent', () => {
  let component: AuthLayoutComponent;
  let fixture: ComponentFixture<AuthLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthLayoutComponent],
      providers: [provideRouter([])]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AuthLayoutComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it("la marque « INPG LMS » ramène à la page d'accueil", () => {
    fixture.detectChanges();

    const lien = (fixture.nativeElement as HTMLElement).querySelector('a[href="/accueil"]') as HTMLAnchorElement;
    expect(lien).not.toBeNull();
    expect(lien.textContent!.replace(/\s+/g, ' ').trim()).toBe('INPG LMS');
    expect(lien.getAttribute('aria-label')).toContain("retour à l'accueil");
  });
});
