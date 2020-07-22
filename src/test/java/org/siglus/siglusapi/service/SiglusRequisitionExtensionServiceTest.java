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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;

@RunWith(MockitoJUnitRunner.class)
public class SiglusRequisitionExtensionServiceTest {

  @Captor
  private ArgumentCaptor<RequisitionExtension> requisitionExtensionArgumentCaptor;

  @InjectMocks
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;

  private UUID requisitionId = UUID.randomUUID();

  private String facilityCode = "facilityCode";

  @Test
  public void shouldSaveRequisitionExtensionWhenCreateRequisitionExtensionIfNotEmergency() {
    // when
    siglusRequisitionExtensionService.createRequisitionExtension(requisitionId, false,
        facilityCode);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionArgumentCaptor.capture());
    RequisitionExtension requisitionExtension = requisitionExtensionArgumentCaptor.getValue();
    assertEquals(requisitionId, requisitionExtension.getRequisitionId());
    assertEquals("NO" + "facilityCode", requisitionExtension.getRequisitionNumberPrefix());
  }

  @Test
  public void shouldSaveRequisitionExtensionWhenCreateRequisitionExtensionIfIsEmergency() {
    // when
    siglusRequisitionExtensionService.createRequisitionExtension(requisitionId, true,
        facilityCode);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionArgumentCaptor.capture());
    RequisitionExtension requisitionExtension = requisitionExtensionArgumentCaptor.getValue();
    assertEquals(requisitionId, requisitionExtension.getRequisitionId());
    assertEquals("EM" + "facilityCode", requisitionExtension.getRequisitionNumberPrefix());
  }

  @Test
  public void shouldReturnFormatRequisitionNumber() {
    // given
    RequisitionExtension requisitionExtension = RequisitionExtension.builder()
        .requisitionNumberPrefix("NO01051002")
        .requisitionNumber(4)
        .build();
    when(requisitionExtensionRepository.findByRequisitionId(requisitionId))
        .thenReturn(requisitionExtension);

    // when
    String formatRequisitionNumber = siglusRequisitionExtensionService
        .formatRequisitionNumber(requisitionId);

    // then
    assertEquals("NO010510020000004", formatRequisitionNumber);
  }

  @Test
  public void shouldReturnNullIfRequisitionExtensionNotExist() {
    // given
    when(requisitionExtensionRepository.findByRequisitionId(requisitionId))
        .thenReturn(null);

    // when
    String formatRequisitionNumber = siglusRequisitionExtensionService
        .formatRequisitionNumber(requisitionId);

    // then
    assertNull(formatRequisitionNumber);
  }

  @Test
  public void shouldDeleteRequisitionExtension() {
    // given
    RequisitionExtension requisitionExtension = new RequisitionExtension();
    when(requisitionExtensionRepository.findByRequisitionId(requisitionId))
        .thenReturn(requisitionExtension);

    // when
    siglusRequisitionExtensionService.deleteRequisitionExtension(requisitionId);

    // then
    verify(requisitionExtensionRepository).delete(requisitionExtension);
  }
}
