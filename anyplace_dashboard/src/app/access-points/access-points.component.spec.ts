import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AccessPointsComponent } from './access-points.component';

describe('AccessPointsComponent', () => {
  let component: AccessPointsComponent;
  let fixture: ComponentFixture<AccessPointsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AccessPointsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AccessPointsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
