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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.util.PageImplRepresentation;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.web.SiglusStockCardSummariesSiglusController;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
public class SiglusOrderServiceTest {
  @Mock
  private OrderController orderController;

  @Mock
  private RequisitionController requisitionController;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private SiglusArchiveProductService siglusArchiveProductService;

  @Mock
  private SiglusStockCardSummariesSiglusController siglusStockCardSummariesSiglusController;

  @InjectMocks
  private SiglusOrderService siglusOrderService;

  private UUID requisitionFacilityId = UUID.randomUUID();
  private UUID approverFacilityId = UUID.randomUUID();
  private UUID orderId = UUID.randomUUID();
  private UUID requisitionId = UUID.randomUUID();
  private UUID approverId = UUID.randomUUID();
  private UUID userId = UUID.randomUUID();
  private UUID userHomeFacilityId = UUID.randomUUID();
  private UUID programId = UUID.randomUUID();
  private UUID orderableId1 = UUID.randomUUID();
  private UUID orderableId2 = UUID.randomUUID();
  private UUID orderableId3 = UUID.randomUUID();

  @Test
  public void shouldGetValidAvailableProductsWithOrder() {
    // given
    when(orderController.getOrder(any(), any())).thenReturn(createOrderDto());
    when(requisitionController.findRequisition(any(), any())).thenReturn(createRequisition());
    when(authenticationHelper.getCurrentUser()).thenReturn(createUser(userId, userHomeFacilityId));
    when(facilityReferenceDataService.findOne(approverFacilityId))
        .thenReturn(createFacilityDto(approverFacilityId));
    when(facilityReferenceDataService.findOne(userHomeFacilityId))
        .thenReturn(createFacilityDto(userHomeFacilityId));
    when(programReferenceDataService.findOne(programId)).thenReturn(createProgramDto());
    when(requisitionService.getApproveProduct(
        createFacilityDto(approverFacilityId), createProgramDto(), null))
        .thenReturn(createApproverAggregator());
    when(requisitionService.getApproveProduct(
        createFacilityDto(userHomeFacilityId), createProgramDto(), null))
        .thenReturn(createUserAggregator());
    when(siglusArchiveProductService.searchArchivedProducts(any()))
        .thenReturn(createArchivedProducts());
    when(siglusStockCardSummariesSiglusController
        .searchStockCardSummaries(any(), any())).thenReturn(createSummaryPage());

    // when
    SiglusOrderDto response = siglusOrderService.searchOrderById(orderId);
    Set<VersionObjectReferenceDto> availableProducts = response.getAvailableProducts();
    VersionObjectReferenceDto filteredProduct = availableProducts.stream().findFirst().orElse(null);

    // then
    assertEquals(1, availableProducts.size());
    assertTrue(filteredProduct.getId().equals(orderableId1));
    assertTrue(filteredProduct.getVersionNumber().equals(new Long(1)));
  }

  private ApproveProductsAggregator createApproverAggregator() {
    ApprovedProductDto productDto = createApprovedProductDto(orderableId1);
    List<ApprovedProductDto> list = new ArrayList<>();
    list.add(productDto);
    ApproveProductsAggregator aggregator = new ApproveProductsAggregator(list, programId);
    return aggregator;
  }

  private ApproveProductsAggregator createUserAggregator() {
    ApprovedProductDto productDto1 = createApprovedProductDto(orderableId1);
    ApprovedProductDto productDto2 = createApprovedProductDto(orderableId2);
    List<ApprovedProductDto> list = new ArrayList<>();
    list.add(productDto1);
    list.add(productDto2);
    ApproveProductsAggregator aggregator = new ApproveProductsAggregator(list, programId);
    return aggregator;
  }

  private ApprovedProductDto createApprovedProductDto(UUID orderableId) {
    MetadataDto meta = new MetadataDto();
    meta.setVersionNumber(new Long(1));
    OrderableDto orderable = new OrderableDto();
    orderable.setId(orderableId);
    orderable.setMeta(meta);
    ApprovedProductDto productDto = new ApprovedProductDto();
    productDto.setId(UUID.randomUUID());
    productDto.setMeta(meta);
    productDto.setOrderable(orderable);
    return productDto;
  }

  private FacilityDto createFacilityDto(UUID id) {
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(id);
    return facilityDto;
  }

  private OrderDto createOrderDto() {
    OrderDto order = new OrderDto();
    order.setId(orderId);
    order.setExternalId(requisitionId);
    order.setCreatedBy(createUser(approverId, approverFacilityId));
    return order;
  }

  private UserDto createUser(UUID userId, UUID facilityId) {
    UserDto user = new UserDto();
    user.setId(userId);
    user.setHomeFacilityId(facilityId);
    return user;
  }

  private Requisition createRequisition() {
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    requisition.setFacilityId(requisitionFacilityId);
    requisition.setProgramId(programId);
    Set<ApprovedProductReference> set = new HashSet<>();
    set.add(new ApprovedProductReference(UUID.randomUUID(), new Long(1), orderableId1,
        new Long(1)));
    set.add(new ApprovedProductReference(UUID.randomUUID(), new Long(1), orderableId2,
            new Long(1)));
    set.add(new ApprovedProductReference(UUID.randomUUID(), new Long(1), orderableId3,
            new Long(1)));
    requisition.setAvailableProducts(set);
    return requisition;
  }

  private ProgramDto createProgramDto() {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    return programDto;
  }

  private Set<String> createArchivedProducts() {
    Set<String> set = new HashSet<>();
    set.add(orderableId2.toString());
    return set;
  }

  private PageImplRepresentation<StockCardSummaryV2Dto> createSummaryPage() {
    PageImplRepresentation<StockCardSummaryV2Dto> page = new PageImplRepresentation<>();
    List<StockCardSummaryV2Dto> list = new ArrayList<>();
    list.add(createMockStockCardSummary());
    page.setContent(list);
    return page;
  }

  private StockCardSummaryV2Dto createMockStockCardSummary() {
    StockCardSummaryV2Dto summaryV2Dto = new StockCardSummaryV2Dto();
    summaryV2Dto.setOrderable(new ObjectReferenceDto("", "", orderableId1));
    CanFulfillForMeEntryDto canFulfillForMeEntryDto = new CanFulfillForMeEntryDto();
    canFulfillForMeEntryDto.setStockOnHand(2);
    Set<CanFulfillForMeEntryDto> set = new HashSet<>();
    set.add(canFulfillForMeEntryDto);
    summaryV2Dto.setCanFulfillForMe(set);
    return summaryV2Dto;
  }
}
