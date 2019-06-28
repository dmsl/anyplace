import { TestBed } from '@angular/core/testing';

import { AnyplaceService } from './anyplace.service';

describe('AnyplaceService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: AnyplaceService = TestBed.get(AnyplaceService);
    expect(service).toBeTruthy();
  });
});
