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
});
