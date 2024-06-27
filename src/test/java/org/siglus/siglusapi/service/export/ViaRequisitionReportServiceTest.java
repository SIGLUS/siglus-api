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

package org.siglus.siglusapi.service.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.alibaba.excel.ExcelWriter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionPeriodDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.dto.FacilitySearchResultDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.service.SiglusAdministrationsService;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class ViaRequisitionReportServiceTest {

  @InjectMocks
  private ViaRequisitionReportService service;

  @Mock
  private SiglusAdministrationsService administrationsService;

  @Mock
  private SiglusProcessingPeriodService periodService;

  @Mock
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  private UUID orderableId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();
  private UUID periodId = UUID.randomUUID();

  @Test
  public void shouldGetSupportProgramCodeIsVia() {
    ViaRequisitionReportService service = new ViaRequisitionReportService();
    Set<String> result = service.supportedProgramCodes();
    assertEquals(2, result.size());
    assertTrue(result.contains(ProgramConstants.VIA_PROGRAM_CODE));
    assertTrue(result.contains(ProgramConstants.MMC_PROGRAM_CODE));
  }

  @Test
  public void shouldGenerateReportSuccess() {
    ExcelWriter excelWriter = Mockito.mock(ExcelWriter.class);
    when(excelWriter.fill(any(), any())).thenReturn(null);
    when(excelWriter.fill(any(), any(), any())).thenReturn(null);
    when(administrationsService.getFacility(facilityId)).thenReturn(buildFacility());
    SiglusRequisitionDto requisitionDto = buildRequisition();
    when(periodService.getProcessingPeriodDto(periodId)).thenReturn(buildPeriod());
    when(orderableReferenceDataService.findByIds(Collections.singletonList(orderableId)))
        .thenReturn(Collections.singletonList(buildOrderable()));

    service.generateReport(requisitionDto, excelWriter);
  }

  private SiglusRequisitionDto buildRequisition() {
    SiglusRequisitionDto requisitionDto = new SiglusRequisitionDto();
    requisitionDto.setEmergency(false);
    requisitionDto.setFacility(new ObjectReferenceDto(facilityId));
    requisitionDto.setProcessingPeriod(new ObjectReferenceDto(periodId));
    requisitionDto.setRequisitionLineItems(Collections.singletonList(buildLineItem()));
    requisitionDto.setExtraData(new HashMap<>());
    return requisitionDto;
  }

  private RequisitionLineItemV2Dto buildLineItem() {
    RequisitionLineItemV2Dto lineItemV2Dto = new RequisitionLineItemV2Dto();
    lineItemV2Dto.setOrderable(new VersionObjectReferenceDto(orderableId, null, "orderables", 1L));
    lineItemV2Dto.setBeginningBalance(1);
    lineItemV2Dto.setTotalReceivedQuantity(2);
    lineItemV2Dto.setTotalConsumedQuantity(2);
    lineItemV2Dto.setStockOnHand(1);
    return lineItemV2Dto;
  }

  private OrderableDto buildOrderable() {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setProductCode("productCode");
    orderableDto.setFullProductName("full product name");
    return orderableDto;
  }

  private FacilitySearchResultDto buildFacility() {
    FacilitySearchResultDto facility = new FacilitySearchResultDto();
    facility.setId(facilityId);
    facility.setName("facilityName");
    GeographicZoneDto zoneDto = new GeographicZoneDto();
    zoneDto.setName("District");
    GeographicZoneDto parentZoneDto = new GeographicZoneDto();
    zoneDto.setName("province");
    zoneDto.setParent(parentZoneDto);
    facility.setGeographicZone(zoneDto);
    return facility;
  }

  private ProcessingPeriodDto buildPeriod() {
    ProcessingPeriodDto periodDto = new RequisitionPeriodDto();
    periodDto.setId(periodId);
    periodDto.setStartDate(LocalDate.now());
    periodDto.setEndDate(LocalDate.now());
    return periodDto;
  }
}
