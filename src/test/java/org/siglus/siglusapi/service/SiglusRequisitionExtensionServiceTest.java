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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
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
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;

@RunWith(MockitoJUnitRunner.class)
public class SiglusRequisitionExtensionServiceTest {

  private static final String FACILITY_CODE = "facilityCode";

  @Captor
  private ArgumentCaptor<RequisitionExtension> requisitionExtensionArgumentCaptor;

  @InjectMocks
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Mock
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Mock
  private SiglusGeneratedNumberService siglusGeneratedNumberService;

  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;

  private UUID requisitionId = UUID.randomUUID();

  private UUID facilityId = UUID.randomUUID();

  @Test
  public void shouldSaveRequisitionExtensionWhenCreateRequisitionExtensionIfNotEmergency() {
    // given
    FacilityDto facilityDto = FacilityDto.builder().code(FACILITY_CODE).build();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(siglusGeneratedNumberService.getGeneratedNumber(facilityId)).thenReturn(3);

    // when
    siglusRequisitionExtensionService.createRequisitionExtension(requisitionId, false,
        facilityId);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionArgumentCaptor.capture());
    RequisitionExtension requisitionExtension = requisitionExtensionArgumentCaptor.getValue();
    assertEquals(requisitionId, requisitionExtension.getRequisitionId());
    assertEquals("RNR-NO" + FACILITY_CODE, requisitionExtension.getRequisitionNumberPrefix());
    assertEquals(3, requisitionExtension.getRequisitionNumber().intValue());
  }

  @Test
  public void shouldSaveRequisitionExtensionWhenCreateRequisitionExtensionIfIsEmergency() {
    // given
    FacilityDto facilityDto = FacilityDto.builder().code(FACILITY_CODE).build();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(siglusGeneratedNumberService.getGeneratedNumber(facilityId)).thenReturn(3);

    // when
    siglusRequisitionExtensionService.createRequisitionExtension(requisitionId, true,
        facilityId);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionArgumentCaptor.capture());
    RequisitionExtension requisitionExtension = requisitionExtensionArgumentCaptor.getValue();
    assertEquals(requisitionId, requisitionExtension.getRequisitionId());
    assertEquals("RNR-EM" + FACILITY_CODE, requisitionExtension.getRequisitionNumberPrefix());
    assertEquals(3, requisitionExtension.getRequisitionNumber().intValue());
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

  @Test
  public void shouldNotDeleteRequisitionExtensionIfRequisitionExtensionNotExist() {
    // given
    when(requisitionExtensionRepository.findByRequisitionId(requisitionId))
        .thenReturn(null);

    // when
    siglusRequisitionExtensionService.deleteRequisitionExtension(requisitionId);

    // then
    verify(requisitionExtensionRepository, times(0)).delete(any(RequisitionExtension.class));
  }
}
