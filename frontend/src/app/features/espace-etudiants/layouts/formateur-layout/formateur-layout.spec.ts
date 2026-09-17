import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { FormateurLayout } from './formateur-layout';

describe('FormateurLayout', () => {
  let component: FormateurLayout;
  let fixture: ComponentFixture<FormateurLayout>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormateurLayout, RouterTestingModule],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FormateurLayout);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
