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
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FcConstants.STATUS_ACTIVE;
import static org.siglus.siglusapi.service.fc.FcVariables.LAST_UPDATED_AT;
import static org.siglus.siglusapi.service.fc.FcVariables.START_DATE;

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
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.fc.FcFacilityTypeDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusStockCardLineItemReasons;
import org.siglus.siglusapi.service.client.ValidReasonAssignmentStockManagementService;

@RunWith(MockitoJUnitRunner.class)
public class FcFacilityTypeServiceTest {

  public static final String TEST_1 = "test1";
  public static final String TEST_2 = "test2";
  public static final String TEST_3 = "test3";
  public static final String TEST_4 = "test4";

  @InjectMocks
  private FcFacilityTypeService fcFacilityTypeService;

  @Mock
  private SiglusFacilityTypeReferenceDataService facilityTypeService;

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
    when(siglusStockCardLineItemReasons.findAll()).thenReturn(Collections.singletonList(lineItemReasonDto));
  }

  @Test
  public void shouldReturnFalseGivenEmptyFcResult() {

    // when
    FcIntegrationResultDto result = fcFacilityTypeService.processData(emptyList(), START_DATE, LAST_UPDATED_AT);

    // then
    assertNull(result);
  }

  @Test
  public void shouldFinalSuccessFalseWhenSaveFacilityTypeFailed() {
    // given
    FacilityTypeDto typeDto1 = mockFacilityTypeDto(TEST_1, TEST_1);
    FacilityTypeDto typeDto2 = mockFacilityTypeDto(TEST_2, TEST_2);
    FacilityTypeDto typeDto3 = mockFacilityTypeDto(TEST_3, TEST_3);
    when(facilityTypeService.searchAllFacilityTypes()).thenReturn(newArrayList(typeDto1, typeDto2, typeDto3));
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setId(UUID.randomUUID());
    FcFacilityTypeDto typeDto4 = mockFcFacilityTypeDto(TEST_2, "test23", true);
    FcFacilityTypeDto typeDto5 = mockFcFacilityTypeDto(TEST_4, TEST_4, true);
    when(facilityTypeService.createFacilityType(any(FacilityTypeDto.class))).thenThrow(new RuntimeException());

    // when
    FcIntegrationResultDto result = fcFacilityTypeService.processData(newArrayList(typeDto4, typeDto5),
        START_DATE, LAST_UPDATED_AT);

    // then
    assertNotNull(result);
    assertFalse(result.getFinalSuccess());
  }

  @Test
  public void shouldSaveAndUpdateFacilityType() {
    // given
    FacilityTypeDto typeDto1 = mockFacilityTypeDto(TEST_1, TEST_1);
    FacilityTypeDto typeDto2 = mockFacilityTypeDto(TEST_2, TEST_2);
    FacilityTypeDto typeDto3 = mockFacilityTypeDto(TEST_3, TEST_3);
    when(facilityTypeService.searchAllFacilityTypes()).thenReturn(newArrayList(typeDto1, typeDto2, typeDto3));
    FacilityTypeDto typeDto = new FacilityTypeDto();
    typeDto.setId(UUID.randomUUID());
    when(facilityTypeService.createFacilityType(any())).thenReturn(typeDto);
    FcFacilityTypeDto typeDto4 = mockFcFacilityTypeDto(TEST_2, "test23", true);
    FcFacilityTypeDto typeDto5 = mockFcFacilityTypeDto(TEST_4, TEST_4, true);

    // when
    fcFacilityTypeService.processData(newArrayList(typeDto4, typeDto5), START_DATE, LAST_UPDATED_AT);

    // then
    verify(facilityTypeService).createFacilityType(addFacilityType.capture());
    verify(facilityTypeService).saveFacilityType(updateFacilityType.capture());
    verify(assignmentService).assignReason(reasonArgumentCaptor.capture());
    assertEquals(TEST_4, addFacilityType.getValue().getName());
    assertEquals("test23", updateFacilityType.getValue().getName());
    assertEquals(programId, reasonArgumentCaptor.getValue().getProgramId());
    assertEquals(reasonId, reasonArgumentCaptor.getValue().getReason().getId());
  }

  @Test
  public void shouldUpdateFacilityType() {
    // given
    FacilityTypeDto typeDto2 = mockFacilityTypeDto("test6", "test8");
    FcFacilityTypeDto typeDto4 = mockFcFacilityTypeDto("test6", "test8", false);
    when(facilityTypeService.searchAllFacilityTypes())
        .thenReturn(newArrayList(typeDto2));

    // when
    fcFacilityTypeService.processData(newArrayList(typeDto4), START_DATE, LAST_UPDATED_AT);

    // then
    verify(facilityTypeService).saveFacilityType(updateFacilityType.capture());
    assertFalse(updateFacilityType.getValue().getActive());

  }

  private FacilityTypeDto mockFacilityTypeDto(String code, String name) {
    FacilityTypeDto facilityTypeDto = new FacilityTypeDto();
    facilityTypeDto.setId(UUID.randomUUID());
    facilityTypeDto.setCode(code);
    facilityTypeDto.setName(name);
    facilityTypeDto.setActive(true);
    return facilityTypeDto;
  }

  private FcFacilityTypeDto mockFcFacilityTypeDto(String code, String name, boolean active) {
    FcFacilityTypeDto facilityTypeDto = new FcFacilityTypeDto();
    facilityTypeDto.setCode(code);
    facilityTypeDto.setDescription(name);
    facilityTypeDto.setStatus(active ? STATUS_ACTIVE : "inActivo");
    facilityTypeDto.setLastUpdatedAt(LAST_UPDATED_AT);
    return facilityTypeDto;
  }

}
