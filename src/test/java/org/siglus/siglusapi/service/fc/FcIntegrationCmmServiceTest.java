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
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.domain.Cmm;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.fc.CmmDto;
import org.siglus.siglusapi.repository.CmmRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class FcIntegrationCmmServiceTest {

  @InjectMocks
  private FcIntegrationCmmService fcIntegrationCmmService;

  @Captor
  private ArgumentCaptor<Cmm> cmmArgumentCaptor;

  @Mock
  private CmmRepository cmmRepository;

  @Mock
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private SiglusProcessingPeriodReferenceDataService processingPeriodReferenceDataService;

  @Mock
  private SiglusRequisitionLineItemExtensionRepository lineItemExtensionRepository;

  private String facilityCode = "facilityCode";

  private String productCode = "productCode";

  private String period = "M5";

  private int year = 2020;

  private UUID orderableId = UUID.randomUUID();

  private UUID facilityId = UUID.randomUUID();

  private UUID processingPeriodId = UUID.randomUUID();

  private UUID lineItemId = UUID.randomUUID();

  private UUID cmmId = UUID.randomUUID();

  private LocalDate periodEndDate = LocalDate.parse("2020-05-31");

  @Test
  public void shouldAddCmmData() {
    // given
    CmmDto dto = CmmDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();

    // when
    fcIntegrationCmmService.dealCmmData(newArrayList(dto));

    // then
    verify(cmmRepository)
        .findCmmByFacilityCodeAndProductCodeAndPeriodAndYear(facilityCode, productCode,
            period, year);
    verify(cmmRepository).save(any(Cmm.class));
  }

  @Test
  public void shouldUpdateCmmData() {
    // given
    CmmDto dto = CmmDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    Cmm existCmm = new Cmm();
    existCmm.setId(cmmId);
    when(cmmRepository.findCmmByFacilityCodeAndProductCodeAndPeriodAndYear(facilityCode,
        productCode, period, year)).thenReturn(existCmm);

    // when
    fcIntegrationCmmService.dealCmmData(newArrayList(dto));

    // then
    verify(cmmRepository).save(cmmArgumentCaptor.capture());
    assertEquals(cmmId, cmmArgumentCaptor.getValue().getId());
  }

  @Test
  public void shouldReturnFalseIfCatchExceptionWhenDealCmmData() {
    // given
    CmmDto dto = CmmDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    when(cmmRepository.save(any(Cmm.class))).thenThrow(new RuntimeException());

    // when
    boolean result = fcIntegrationCmmService.dealCmmData(newArrayList(dto));

    // then
    assertFalse(result);
  }

  @Test
  public void shouldInitiateSuggestedQuantity() {
    // given
    VersionObjectReferenceDto orderableReference = new VersionObjectReferenceDto();
    orderableReference.setId(orderableId);
    RequisitionLineItemV2Dto lineItem = new RequisitionLineItemV2Dto();
    lineItem.setOrderable(orderableReference);
    lineItem.setStockOnHand(14);
    lineItem.setId(lineItemId);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setRequisitionLineItems(newArrayList(lineItem));
    ObjectReferenceDto facility = new ObjectReferenceDto();
    facility.setId(facilityId);
    siglusRequisitionDto.setFacility(facility);
    ObjectReferenceDto processingPeriod = new ObjectReferenceDto();
    processingPeriod.setId(processingPeriodId);
    siglusRequisitionDto.setProcessingPeriod(processingPeriod);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setProductCode(productCode);
    when(orderableReferenceDataService.findByIds(newHashSet(orderableId)))
        .thenReturn(newArrayList(orderableDto));
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setCode(facilityCode);
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setEndDate(periodEndDate);
    when(processingPeriodReferenceDataService.findOne(processingPeriodId))
        .thenReturn(processingPeriodDto);
    Cmm cmm = Cmm.builder().productCode(productCode).cmm(10).max(3).build();
    when(cmmRepository.findAllByFacilityCodeAndProductCodeInAndPeriodAndYear(any(), any(), any(),
        any())).thenReturn(newArrayList(cmm));

    // when
    fcIntegrationCmmService.initiateSuggestedQuantity(
        siglusRequisitionDto.getLineItems(), siglusRequisitionDto.getFacilityId(),
        siglusRequisitionDto.getProcessingPeriodId());

    // then
    assertEquals(16, lineItem.getSuggestedQuantity().intValue());
    verify(lineItemExtensionRepository).save(any(List.class));
  }
}
