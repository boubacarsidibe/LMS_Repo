import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProfilEtudiant } from './profil-etudiant';

describe('ProfilEtudiant', () => {
  let component: ProfilEtudiant;
  let fixture: ComponentFixture<ProfilEtudiant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfilEtudiant]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProfilEtudiant);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
