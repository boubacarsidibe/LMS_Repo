import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { DashboardFormateur } from './dashboard-formateur';

describe('DashboardFormateur', () => {
  let component: DashboardFormateur;
  let fixture: ComponentFixture<DashboardFormateur>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardFormateur, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DashboardFormateur);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
