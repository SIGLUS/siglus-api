/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.siglus.siglusapi.web;

import static org.mockito.Mockito.verify;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.service.SiglusValidSourceDestinationService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusValidSourceDestinationControllerTest {

  @InjectMocks
  private SiglusValidSourceDestinationController siglusValidSourceDestinationController;

  @Mock
  private SiglusValidSourceDestinationService validSourceDestinationService;

  private UUID programId = UUID.randomUUID();

  private UUID facilityId = UUID.randomUUID();

  @Test
  public void shouldCallFindDestForAllProductsWhenSearchValidDestIfProgramIsAllProducts() {
    siglusValidSourceDestinationController
        .searchValidDestinations(ALL_PRODUCTS_PROGRAM_ID, facilityId);

    verify(validSourceDestinationService).findDestinationsForAllProducts(facilityId);
  }

  @Test
  public void shouldCallFindDestinationsWhenSearchValidDestinationsIfProgramIsNotAllProducts() {
    siglusValidSourceDestinationController.searchValidDestinations(programId, facilityId);

    verify(validSourceDestinationService).findDestinations(programId, facilityId);
  }

  @Test
  public void shouldCallFindSourcesForAllProductsWhenSearchValidSourcesIfProgramIsAllProducts() {
    siglusValidSourceDestinationController.searchValidSources(ALL_PRODUCTS_PROGRAM_ID, facilityId);

    verify(validSourceDestinationService).findSourcesForAllProducts(facilityId);
  }

  @Test
  public void shouldCallFindSourcesWhenSearchValidSourcesIfProgramIsNotAllProducts() {
    siglusValidSourceDestinationController.searchValidSources(programId, facilityId);

    verify(validSourceDestinationService).findSources(programId, facilityId);
  }
}
