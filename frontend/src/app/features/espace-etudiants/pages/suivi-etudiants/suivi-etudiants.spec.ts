import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SuiviEtudiants } from './suivi-etudiants';

describe('SuiviEtudiants', () => {
  let component: SuiviEtudiants;
  let fixture: ComponentFixture<SuiviEtudiants>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SuiviEtudiants]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SuiviEtudiants);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
