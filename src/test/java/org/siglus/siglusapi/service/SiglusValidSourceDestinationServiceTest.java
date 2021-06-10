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

package org.siglus.siglusapi.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusValidSourceDestinationServiceTest {

  @InjectMocks
  private SiglusValidSourceDestinationService siglusValidSourceDestinationService;

  @Mock
  private ValidSourceDestinationStockManagementService validSourceDestinationStockManagementService;

  @Mock
  private SupportedProgramsHelper supportedVirtualProgramsHelper;

  private final UUID programId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  @Test
  public void shouldCallGetValidDestinationsWhenFindDestinations() {
    siglusValidSourceDestinationService.findDestinations(programId, facilityId);

    verify(validSourceDestinationStockManagementService)
        .getValidDestinations(programId, facilityId);
  }

  @Test
  public void shouldCallGetValidDestinationsMultipleTimesWhenFindDestinationsForAllProducts() {
    when(supportedVirtualProgramsHelper.findUserSupportedPrograms())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));

    siglusValidSourceDestinationService.findDestinationsForAllProducts(facilityId);

    verify(validSourceDestinationStockManagementService, times(2))
        .getValidDestinations(any(), any());
  }

  @Test
  public void shouldCallGetValidSourcesWhenFindSources() {
    siglusValidSourceDestinationService.findSources(programId, facilityId);

    verify(validSourceDestinationStockManagementService).getValidSources(programId, facilityId);
  }

  @Test
  public void shouldCallGetValidSourcesMultipleTimesWhenFindSourcesForAllProducts() {
    when(supportedVirtualProgramsHelper.findUserSupportedPrograms())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));

    siglusValidSourceDestinationService.findSourcesForAllProducts(facilityId);

    verify(validSourceDestinationStockManagementService, times(2))
        .getValidSources(any(), any());
  }
}
