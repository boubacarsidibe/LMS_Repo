import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { ProfilEtudiant } from './profil-etudiant';

describe('ProfilEtudiant', () => {
  let component: ProfilEtudiant;
  let fixture: ComponentFixture<ProfilEtudiant>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfilEtudiant, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
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
