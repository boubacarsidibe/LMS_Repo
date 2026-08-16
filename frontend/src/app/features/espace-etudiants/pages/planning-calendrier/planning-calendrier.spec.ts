import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlanningCalendrier } from './planning-calendrier';

describe('PlanningCalendrier', () => {
  let component: PlanningCalendrier;
  let fixture: ComponentFixture<PlanningCalendrier>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlanningCalendrier]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PlanningCalendrier);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
