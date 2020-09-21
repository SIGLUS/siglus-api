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

package org.siglus.siglusapi.service.fc;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FcConstants.STATUS_ACTIVE;

import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.dto.StockCardLineItemReasonDto;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.siglus.common.dto.referencedata.FacilityTypeDto;
import org.siglus.siglusapi.dto.fc.FcFacilityTypeDto;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeService;
import org.siglus.siglusapi.service.client.SiglusStockCardLineItemReasons;
import org.siglus.siglusapi.service.client.ValidReasonAssignmentStockManagementService;

@RunWith(MockitoJUnitRunner.class)
public class FcFacilityTypeServiceTest {

  @InjectMocks
  private FcFacilityTypeService fcFacilityTypeService;

  @Mock
  private SiglusFacilityTypeService facilityTypeService;

  @Mock
  private SiglusStockCardLineItemReasons siglusStockCardLineItemReasons;

  @Mock
  private ProgramReferenceDataService programRefDataService;

  @Mock
  private ValidReasonAssignmentStockManagementService assignmentService;

  @Captor
  private ArgumentCaptor<FacilityTypeDto> updateFacilityType;

  @Captor
  private ArgumentCaptor<FacilityTypeDto> addFacilityType;

  @Captor
  private ArgumentCaptor<ValidReasonAssignmentDto> reasonArgumentCaptor;

  private final UUID programId = UUID.randomUUID();
  private final UUID reasonId = UUID.randomUUID();

  @Before
  public void setup() {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    StockCardLineItemReasonDto lineItemReasonDto = new StockCardLineItemReasonDto();
    lineItemReasonDto.setId(reasonId);
    when(programRefDataService.findAll()).thenReturn(Collections.singletonList(programDto));
    when(siglusStockCardLineItemReasons.findAll()).thenReturn(
        Collections.singletonList(lineItemReasonDto));
  }

  @Test
  public void shouldReturnFalseGivenEmptyFcResult() {

    // when
    boolean result = fcFacilityTypeService.processFacilityTypes(Collections.emptyList());

    // then
    assertFalse(result);
  }

  @Test
  public void shouldSaveAndUpdateFacilityType() {
    // given
    FacilityTypeDto typeDto1 = mockFacilityTypeDto("test1", "test1", true);
    FacilityTypeDto typeDto2 = mockFacilityTypeDto("test2", "test2", true);
    FacilityTypeDto typeDto3 = mockFacilityTypeDto("test3", "test3", true);
    when(facilityTypeService.searchAllFacilityTypes())
        .thenReturn(newArrayList(typeDto1, typeDto2, typeDto3));
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setId(UUID.randomUUID());
    when(facilityTypeService.createFacilityType(any())).thenReturn(typeDto);
    FcFacilityTypeDto typeDto4 = mockFcFacilityTypeDto("test2", "test23", true);
    FcFacilityTypeDto typeDto5 = mockFcFacilityTypeDto("test4", "test4", true);

    // when
    fcFacilityTypeService.processFacilityTypes(newArrayList(typeDto4, typeDto5));

    // then
    verify(facilityTypeService).createFacilityType(addFacilityType.capture());
    verify(facilityTypeService).saveFacilityType(updateFacilityType.capture());
    verify(assignmentService).assignReason(reasonArgumentCaptor.capture());
    assertEquals("test4", addFacilityType.getValue().getName());
    assertEquals("test23", updateFacilityType.getValue().getName());
    assertEquals(programId, reasonArgumentCaptor.getValue().getProgramId());
    assertEquals(reasonId, reasonArgumentCaptor.getValue().getReason().getId());
  }

  @Test
  public void shouldUpdateFacilityType() {
    // given
    FacilityTypeDto typeDto2 = mockFacilityTypeDto("test6", "test8", true);
    FcFacilityTypeDto typeDto4 = mockFcFacilityTypeDto("test6", "test8", false);
    when(facilityTypeService.searchAllFacilityTypes())
        .thenReturn(newArrayList(typeDto2));

    // when
    fcFacilityTypeService.processFacilityTypes(
        newArrayList(typeDto4));

    // then
    verify(facilityTypeService).saveFacilityType(updateFacilityType.capture());
    assertEquals(false, updateFacilityType.getValue().getActive());

  }

  private FacilityTypeDto mockFacilityTypeDto(String code, String name, boolean active) {
    FacilityTypeDto facilityTypeDto = new FacilityTypeDto();
    facilityTypeDto.setId(UUID.randomUUID());
    facilityTypeDto.setCode(code);
    facilityTypeDto.setName(name);
    facilityTypeDto.setActive(active);
    return facilityTypeDto;
  }

  private FcFacilityTypeDto mockFcFacilityTypeDto(String code, String name, boolean active) {
    FcFacilityTypeDto facilityTypeDto = new FcFacilityTypeDto();
    facilityTypeDto.setCode(code);
    facilityTypeDto.setDescription(name);
    facilityTypeDto.setStatus(active ? STATUS_ACTIVE : "inActivo");
    return facilityTypeDto;
  }

}
