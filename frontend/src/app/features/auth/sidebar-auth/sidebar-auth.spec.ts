import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SidebarAuth } from './sidebar-auth';

describe('SidebarAuth', () => {
  let component: SidebarAuth;
  let fixture: ComponentFixture<SidebarAuth>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SidebarAuth]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SidebarAuth);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('porte le logo blanc de l\'INPG en en-tête : le bleu nuit du panneau ne laisse pas voir la version couleur', () => {
    fixture.detectChanges();
    const logo = (fixture.nativeElement as HTMLElement).querySelector('app-logo-inpg img') as HTMLImageElement;

    expect(logo).not.toBeNull();
    expect(logo.getAttribute('src')).toBe('/images/logo-inpg-blanc.png');
  });
});
