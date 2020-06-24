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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.referencedata.FulfillmentOrderableReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.util.FulfillmentOrderDtoBuilder;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.MetadataDto;
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
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.dto.SiglusOrderLineItemDto;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.web.SiglusStockCardSummariesSiglusController;
import org.springframework.beans.BeanUtils;

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

  @Mock
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Mock
  private FulfillmentOrderableReferenceDataService fulfillmentOrderableReferenceDataService;

  @Mock
  private OrderLineItemExtensionRepository lineItemExtensionRepository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private FulfillmentOrderDtoBuilder fulfillmentOrderDtoBuilder;

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
  private UUID lotId = UUID.randomUUID();
  private UUID lineItemId = UUID.randomUUID();

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
    when(siglusStockCardSummariesService
        .searchStockCardSummaryV2Dtos(any(), any())).thenReturn(createSummaryPage());
    OrderLineItemExtension extension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .skipped(true)
        .build();
    when(lineItemExtensionRepository.findByOrderLineItemIdIn((newHashSet(lineItemId))))
        .thenReturn(newArrayList(extension));

    // when
    SiglusOrderDto response = siglusOrderService.searchOrderById(orderId);
    Set<VersionObjectReferenceDto> availableProducts = response.getAvailableProducts();
    VersionObjectReferenceDto filteredProduct = availableProducts.stream().findFirst().orElse(null);

    // then
    assertEquals(1, availableProducts.size());
    assertTrue(filteredProduct.getId().equals(orderableId1));
    assertTrue(filteredProduct.getVersionNumber().equals(1L));
    response.getOrder().getOrderLineItems().forEach(lineItem -> assertTrue(lineItem.isSkipped()));
  }

  @Test
  public void shouldCreateOrderLineItem() {
    // given
    when(authenticationHelper.getCurrentUser()).thenReturn(createUser(userId, userHomeFacilityId));
    when(fulfillmentOrderableReferenceDataService.findByIds(Lists.newArrayList(orderableId1)))
        .thenReturn(Lists.newArrayList(createOrderableDto(orderableId1)));
    when(siglusStockCardSummariesService
        .searchStockCardSummaryV2Dtos(any(), any())).thenReturn(createSummaryPage());

    // when
    List<SiglusOrderLineItemDto> response =
        siglusOrderService.createOrderLineItem(Lists.newArrayList(orderableId1));
    SiglusOrderLineItemDto lineItemDto = response.get(0);

    // then
    assertEquals(1, response.size());
    assertEquals(orderableId1, lineItemDto.getOrderLineItem().getOrderable().getId());
    assertEquals(lotId, lineItemDto.getLots().get(0).getId());
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
    meta.setVersionNumber(1L);
    OrderableDto orderable = new OrderableDto();
    orderable.setId(orderableId);
    orderable.setMeta(convertMetadataDto(meta));
    ApprovedProductDto productDto = new ApprovedProductDto();
    productDto.setId(UUID.randomUUID());
    productDto.setMeta(meta);
    productDto.setOrderable(convertOrderableDto(orderable));
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
    OrderLineItemDto orderLineItemDto = new OrderLineItemDto();
    orderLineItemDto.setId(lineItemId);
    orderLineItemDto.setOrderable(createOrderableDto(orderableId1));
    orderLineItemDto.setOrderedQuantity(1L);
    order.setOrderLineItems(newArrayList(orderLineItemDto));
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
    set.add(new ApprovedProductReference(UUID.randomUUID(), 1L, orderableId1,
        1L));
    set.add(new ApprovedProductReference(UUID.randomUUID(), 1L, orderableId2,
            1L));
    set.add(new ApprovedProductReference(UUID.randomUUID(), 1L, orderableId3,
            1L));
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
    canFulfillForMeEntryDto.setLot(createLot());
    Set<CanFulfillForMeEntryDto> set = new HashSet<>();
    set.add(canFulfillForMeEntryDto);
    summaryV2Dto.setCanFulfillForMe(set);
    return summaryV2Dto;
  }

  private OrderableDto createOrderableDto(UUID orderableId) {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setPrograms(Collections.emptySet());
    return orderableDto;
  }

  private ObjectReferenceDto createLot() {
    return new ObjectReferenceDto("", "lots", lotId);
  }

  private org.openlmis.requisition.dto.OrderableDto convertOrderableDto(OrderableDto sourceDto) {
    org.openlmis.requisition.dto.OrderableDto orderableDto = new
        org.openlmis.requisition.dto.OrderableDto();
    BeanUtils.copyProperties(sourceDto, orderableDto);
    return orderableDto;
  }

  private org.openlmis.fulfillment.web.util.MetadataDto convertMetadataDto(MetadataDto sourceMeta) {
    org.openlmis.fulfillment.web.util.MetadataDto meta = new
        org.openlmis.fulfillment.web.util.MetadataDto();
    BeanUtils.copyProperties(sourceMeta, meta);
    return meta;
  }

  private Order createOrder() {
    Order order = new Order();
    List<OrderLineItem> list = new ArrayList<>();
    order.setOrderLineItems(list);
    return order;
  }

  private Order createSavedOrder() {
    Order order = createOrder();
    OrderLineItemDto orderLineItemDto = new OrderLineItemDto();
    orderLineItemDto.setOrderable(createOrderableDto(orderableId1));
    orderLineItemDto.setOrderedQuantity(1L);
    order.getOrderLineItems().add(OrderLineItem.newInstance(orderLineItemDto));
    return order;
  }
}
