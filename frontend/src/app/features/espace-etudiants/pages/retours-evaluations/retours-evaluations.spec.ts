import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RetoursEvaluations } from './retours-evaluations';

describe('RetoursEvaluations', () => {
  let component: RetoursEvaluations;
  let fixture: ComponentFixture<RetoursEvaluations>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RetoursEvaluations]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RetoursEvaluations);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
