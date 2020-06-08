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
import org.siglus.siglusapi.service.client.ValidReasonAssignmentStockManagementService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusValidReasonAssignmentServiceTest {

  @InjectMocks
  private SiglusValidReasonAssignmentService siglusValidReasonAssignmentService;

  @Mock
  private ValidReasonAssignmentStockManagementService validReasonAssignmentStockManagementService;

  @Mock
  private ProgramExtensionService programExtensionService;

  @Test
  public void shouldCallGetValidReasonsWhenGetValidReasons() {
    siglusValidReasonAssignmentService.getValidReasons(null, null, null, null);

    verify(validReasonAssignmentStockManagementService).getValidReasons(null, null, null, null);
  }

  @Test
  public void shouldCallGetValidReasonsMultipleTimesWhenGetValidReasonsForAllProducts() {
    when(programExtensionService.findSupportedVirtualPrograms())
        .thenReturn(Sets.newHashSet(UUID.randomUUID(), UUID.randomUUID()));

    siglusValidReasonAssignmentService.getValidReasonsForAllProducts(null, null, null);

    verify(validReasonAssignmentStockManagementService, times(2))
        .getValidReasons(any(), any(), any(), any());
  }
}
