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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;

@RunWith(MockitoJUnitRunner.class)
public class SiglusProcessingPeriodExtensionServiceTest {
  @InjectMocks
  private SiglusProcessingPeriodExtensionService service;

  @Mock
  private ProcessingPeriodExtensionRepository mockRepository;

  @Test
  public void shouldReturnAllProcessingPeriodExtensionsWhenFindAll() {
    // Mock data
    List<ProcessingPeriodExtension> expectedExtensions =
        Arrays.asList(new ProcessingPeriodExtension(), new ProcessingPeriodExtension());
    Mockito.when(mockRepository.findAll()).thenReturn(expectedExtensions);

    // Call the service method
    List<ProcessingPeriodExtension> actualExtensions = service.findAll();

    // Assertions
    assertEquals(expectedExtensions, actualExtensions);
    Mockito.verify(mockRepository).findAll();
  }

  @Test
  public void shouldReturnEmptyListWhenFindAll() {
    // Mock data
    Mockito.when(mockRepository.findAll()).thenReturn(Collections.emptyList());

    // Call the service method
    List<ProcessingPeriodExtension> actualExtensions = service.findAll();

    // Assertions
    assertTrue(actualExtensions.isEmpty());
    Mockito.verify(mockRepository).findAll();
  }
}
