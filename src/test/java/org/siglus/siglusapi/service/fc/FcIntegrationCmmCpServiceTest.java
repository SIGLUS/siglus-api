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
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.siglus.common.domain.referencedata.SupervisoryNode;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.domain.CmmDomain;
import org.siglus.siglusapi.domain.CpDomain;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.fc.CmmDto;
import org.siglus.siglusapi.dto.fc.CpDto;
import org.siglus.siglusapi.repository.CmmRepository;
import org.siglus.siglusapi.repository.CpRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SupervisoryNodeRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class FcIntegrationCmmCpServiceTest {

  @InjectMocks
  private FcIntegrationCmmCpService fcIntegrationCmmCpService;

  @Captor
  private ArgumentCaptor<CmmDomain> cmmArgumentCaptor;

  @Mock
  private CmmRepository cmmRepository;

  @Captor
  private ArgumentCaptor<CpDomain> cpArgumentCaptor;

  @Mock
  private CpRepository cpRepository;

  @Mock
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private SiglusProcessingPeriodReferenceDataService processingPeriodReferenceDataService;

  @Mock
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Mock
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Mock
  private SiglusRequisitionLineItemExtensionRepository lineItemExtensionRepository;

  private String facilityCode = "facilityCode";

  private String productCode = "productCode";

  private String period = "M5";

  private int year = 2020;

  private String queryDate = "05-2020";

  private UUID orderableId = UUID.randomUUID();

  private UUID facilityId = UUID.randomUUID();

  private UUID processingPeriodId = UUID.randomUUID();

  private UUID supervisoryNodeId = UUID.randomUUID();

  private UUID lineItemId = UUID.randomUUID();

  private UUID cmmId = UUID.randomUUID();

  private UUID cpId = UUID.randomUUID();

  private LocalDate periodEndDate = LocalDate.parse("2020-05-31");

  @Test
  public void shouldAddCpData() {
    // given
    CpDto dto = CpDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();

    // when
    fcIntegrationCmmCpService.processCpData(newArrayList(dto), queryDate);

    // then
    verify(cpRepository).findCpByFacilityCodeAndProductCodeAndQueryDate(facilityCode, productCode,
        queryDate);
    verify(cpRepository).save(any(CpDomain.class));
  }

  @Test
  public void shouldUpdateCpData() {
    // given
    CpDto dto = CpDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    CpDomain existCp = new CpDomain();
    existCp.setId(cpId);
    when(cpRepository.findCpByFacilityCodeAndProductCodeAndQueryDate(facilityCode, productCode,
        queryDate)).thenReturn(existCp);

    // when
    fcIntegrationCmmCpService.processCpData(newArrayList(dto), queryDate);

    // then
    verify(cpRepository).save(cpArgumentCaptor.capture());
    assertEquals(cpId, cpArgumentCaptor.getValue().getId());
  }

  @Test
  public void shouldReturnFalseIfCatchExceptionWhenDealCpData() {
    // given
    CpDto dto = CpDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    when(cpRepository.save(any(CpDomain.class))).thenThrow(new RuntimeException());

    // when
    boolean result = fcIntegrationCmmCpService.processCpData(newArrayList(dto), queryDate);

    // then
    assertFalse(result);
  }

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
    fcIntegrationCmmCpService.processCmmData(newArrayList(dto), queryDate);

    // then
    verify(cmmRepository)
        .findCmmByFacilityCodeAndProductCodeAndQueryDate(facilityCode, productCode, queryDate);
    verify(cmmRepository).save(any(CmmDomain.class));
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
    CmmDomain existCmm = new CmmDomain();
    existCmm.setId(cmmId);
    when(cmmRepository.findCmmByFacilityCodeAndProductCodeAndQueryDate(facilityCode,
        productCode, queryDate)).thenReturn(existCmm);

    // when
    fcIntegrationCmmCpService.processCmmData(newArrayList(dto), queryDate);

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
    when(cmmRepository.save(any(CmmDomain.class))).thenThrow(new RuntimeException());

    // when
    boolean result = fcIntegrationCmmCpService.processCmmData(newArrayList(dto), queryDate);

    // then
    assertFalse(result);
  }

  @Test
  public void shouldInitiateSuggestedQuantityByCmm() {
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
    CmmDomain cmm = CmmDomain.builder().productCode(productCode).cmm(10).max(3).build();
    when(cmmRepository.findAllByFacilityCodeAndProductCodeInAndQueryDate(any(), any(), any()))
        .thenReturn(newArrayList(cmm));

    // when
    fcIntegrationCmmCpService.initiateSuggestedQuantityByCmm(
        siglusRequisitionDto.getLineItems(), siglusRequisitionDto.getFacilityId(),
        siglusRequisitionDto.getProcessingPeriodId());

    // then
    assertEquals(16, lineItem.getSuggestedQuantity().intValue());
    verify(lineItemExtensionRepository).save(any(List.class));
  }

  @Test
  public void shouldInitiateSuggestedQuantityByCp() {
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
    CpDomain cp = CpDomain.builder().productCode(productCode).cp(10).max(3).build();
    when(cpRepository.findAllByFacilityCodeAndProductCodeInAndQueryDate(any(), any(), any()))
        .thenReturn(newArrayList(cp));
    SupervisoryNode supervisoryNode = new SupervisoryNode();
    supervisoryNode.setId(supervisoryNodeId);
    when(supervisoryNodeRepository.findAllByFacilityId(facilityId))
        .thenReturn(newHashSet(supervisoryNode));
    VersionEntityReference orderableEntity = new VersionEntityReference();
    orderableEntity.setId(orderableId);
    RequisitionLineItem requisitionLineItem1 = new RequisitionLineItem();
    requisitionLineItem1.setOrderable(orderableEntity);
    requisitionLineItem1.setStockOnHand(8);
    Requisition requisition1 = new Requisition();
    requisition1.setRequisitionLineItems(newArrayList(requisitionLineItem1));
    RequisitionLineItem requisitionLineItem2 = new RequisitionLineItem();
    requisitionLineItem2.setOrderable(orderableEntity);
    requisitionLineItem2.setStockOnHand(5);
    Requisition requisition2 = new Requisition();
    requisition2.setRequisitionLineItems(newArrayList(requisitionLineItem2));
    when(siglusRequisitionRepository.searchForSuggestedQuantity(any(), any(), any(), any()))
        .thenReturn(newArrayList(requisition1, requisition2));

    // when
    fcIntegrationCmmCpService.initiateSuggestedQuantityByCp(
        siglusRequisitionDto.getLineItems(), siglusRequisitionDto.getFacilityId(),
        siglusRequisitionDto.getProcessingPeriodId(), siglusRequisitionDto.getProgramId());

    // then
    assertEquals(3, lineItem.getSuggestedQuantity().intValue());
    verify(lineItemExtensionRepository).save(any(List.class));
  }
}
