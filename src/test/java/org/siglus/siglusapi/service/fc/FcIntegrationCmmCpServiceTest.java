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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.siglus.siglusapi.domain.CmmDomain;
import org.siglus.siglusapi.domain.CpDomain;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.CmmRepository;
import org.siglus.siglusapi.repository.CpRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SupervisoryNodeRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class FcIntegrationCmmCpServiceTest {

  @InjectMocks
  private FcCmmCpService fcCmmCpService;

  @Mock
  private CmmRepository cmmRepository;

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

  private final String facilityCode = "facilityCode";
  private final String productCode = "productCode";
  private final UUID orderableId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID processingPeriodId = UUID.randomUUID();
  private final UUID supervisoryNodeId = UUID.randomUUID();
  private final UUID lineItemId = UUID.randomUUID();
  private final LocalDate periodEndDate = LocalDate.parse("2020-05-31");


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
    fcCmmCpService.initiateSuggestedQuantityByCmm(
        siglusRequisitionDto.getLineItems(), siglusRequisitionDto.getFacilityId(),
        siglusRequisitionDto.getProcessingPeriodId());

    // then
    assertEquals(16, lineItem.getSuggestedQuantity().intValue());
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
    fcCmmCpService.initiateSuggestedQuantityByCp(
        siglusRequisitionDto.getLineItems(), siglusRequisitionDto.getFacilityId(),
        siglusRequisitionDto.getProcessingPeriodId(), siglusRequisitionDto.getProgramId());

    // then
    assertEquals(3, lineItem.getSuggestedQuantity().intValue());
  }
}
