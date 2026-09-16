import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { ProgramCard } from './program-card';
import { RecrutementModule } from '../../../recrutement.module';

describe('ProgramCard', () => {
  let component: ProgramCard;
  let fixture: ComponentFixture<ProgramCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecrutementModule, RouterTestingModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProgramCard);
    component = fixture.componentInstance;
    component.prog = {
      id: 1, titre: 'Programme test', description: 'Description', imageUrl: '', niveau: 'Licence', duree: '1 an'
    };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
