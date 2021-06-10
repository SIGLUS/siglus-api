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
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.service.OrderService;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FulfillmentOrderableReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.Pagination;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.BasicOrderDtoBuilder;
import org.openlmis.fulfillment.web.util.FulfillmentOrderDtoBuilder;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProcessingScheduleDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.util.PageImplRepresentation;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.web.SiglusStockCardSummariesController;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
public class SiglusOrderServiceTest {

  @Mock
  private OrderController orderController;

  @Mock
  private OrderService orderService;

  @Mock
  private BasicOrderDtoBuilder basicOrderDtoBuilder;

  @Mock
  private RequisitionController requisitionController;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private SiglusArchiveProductService siglusArchiveProductService;

  @Mock
  private SiglusStockCardSummariesController siglusStockCardSummariesController;

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

  @Mock
  private OrderExternalRepository orderExternalRepository;

  @Captor
  private ArgumentCaptor<List<OrderDto>> orderDtoArgumentCaptor;

  @Mock
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Mock
  private SiglusProcessingPeriodReferenceDataService periodService;

  @Mock
  private SiglusShipmentDraftService draftService;

  @Mock
  private ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  @Mock
  private SiglusFilterAddProductForEmergencyService filterAddProductForEmergencyService;

  @Mock
  private SiglusRequisitionRequisitionService siglusRequisitionService;

  @InjectMocks
  private SiglusOrderService siglusOrderService;

  private final UUID requisitionFacilityId = UUID.randomUUID();
  private final UUID approverFacilityId = UUID.randomUUID();
  private final UUID orderId = UUID.randomUUID();
  private final UUID requisitionId = UUID.randomUUID();
  private final UUID approverId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID userHomeFacilityId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID orderableId1 = UUID.randomUUID();
  private final UUID orderableId2 = UUID.randomUUID();
  private final UUID orderableId3 = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();
  private final UUID lineItemId = UUID.randomUUID();
  private final UUID facilityTypeId = UUID.randomUUID();


  @Before
  public void prepare() {
    ReflectionTestUtils.setField(siglusOrderService, "timeZoneId", "UTC");
    ReflectionTestUtils.setField(siglusOrderService, "fcFacilityTypeId",
        facilityTypeId);
  }

  @Test
  public void shouldGetValidAvailableProductsAndRequisitionNumberWithOrderWhenReqIsNormal() {
    // given
    OrderDto orderDto = createOrderDto();
    when(orderController.getOrder(orderId, null)).thenReturn(orderDto);
    when(orderExternalRepository.findOne(orderDto.getExternalId())).thenReturn(null);
    Requisition requisition = createRequisition();
    requisition.setEmergency(false);
    when(requisitionController.findRequisition(any(), any())).thenReturn(requisition);
    when(authenticationHelper.getCurrentUser()).thenReturn(createUser(userId, userHomeFacilityId));
    when(requisitionService.getApproveProduct(approverFacilityId, programId, false))
        .thenReturn(createApproverAggregator());
    when(requisitionService.getApproveProduct(userHomeFacilityId, programId, false))
        .thenReturn(createUserAggregator());
    when(siglusArchiveProductService.searchArchivedProductsByFacilityId(any()))
        .thenReturn(createArchivedProducts());
    when(siglusStockCardSummariesService
        .searchStockCardSummaryV2Dtos(any(), any())).thenReturn(createSummaryPage());
    OrderLineItemExtension extension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .skipped(true)
        .build();
    when(lineItemExtensionRepository.findByOrderLineItemIdIn((newHashSet(lineItemId))))
        .thenReturn(newArrayList(extension));
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId))
        .thenReturn("requisitionNumber-1");

    // when
    SiglusOrderDto response = siglusOrderService.searchOrderById(orderId);
    Set<VersionObjectReferenceDto> availableProducts = response.getAvailableProducts();
    VersionObjectReferenceDto filteredProduct = availableProducts.stream().findFirst().orElse(null);

    // then
    assertEquals(1, availableProducts.size());
    assertEquals(filteredProduct.getId(), orderableId1);
    assertEquals(1L, (long) filteredProduct.getVersionNumber());
    response.getOrder().getOrderLineItems().forEach(lineItem -> assertTrue(lineItem.isSkipped()));
    assertEquals("requisitionNumber-1", response.getOrder().getRequisitionNumber());
  }

  @Test
  public void shouldFilterInProgressProductWhenReqIsEmergency() {
    // given
    OrderDto orderDto = createOrderDto();
    when(orderController.getOrder(orderId, null)).thenReturn(orderDto);
    when(orderExternalRepository.findOne(orderDto.getExternalId())).thenReturn(null);
    Requisition requisition = createRequisition();
    requisition.setEmergency(true);
    when(requisitionController.findRequisition(any(), any())).thenReturn(requisition);
    when(authenticationHelper.getCurrentUser()).thenReturn(createUser(userId, userHomeFacilityId));
    ApprovedProductDto productDto1 = createApprovedProductDto(orderableId1);
    ApprovedProductDto productDto2 = createApprovedProductDto(orderableId2);
    List<ApprovedProductDto> list = Arrays.asList(productDto1, productDto2);
    when(requisitionService.getApproveProduct(approverFacilityId, programId, false))
        .thenReturn(new ApproveProductsAggregator(list, programId));
    when(requisitionService.getApproveProduct(userHomeFacilityId, programId, false))
        .thenReturn(createUserAggregator());
    when(siglusArchiveProductService.searchArchivedProductsByFacilityId(any()))
        .thenReturn(Collections.emptySet());
    when(siglusStockCardSummariesService
        .searchStockCardSummaryV2Dtos(any(), any())).thenReturn(createSummaryPage());
    OrderLineItemExtension extension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .skipped(true)
        .build();
    when(lineItemExtensionRepository.findByOrderLineItemIdIn((newHashSet(lineItemId))))
        .thenReturn(newArrayList(extension));
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId))
        .thenReturn("requisitionNumber-2");
    when(siglusRequisitionService.getPreviousEmergencyRequisition(any(), any(), any()))
        .thenReturn(Arrays.asList(new RequisitionV2Dto()));
    when(filterAddProductForEmergencyService.getInProgressProducts(anyList()))
        .thenReturn(Sets.newHashSet(orderableId2));

    // when
    SiglusOrderDto response = siglusOrderService.searchOrderById(orderId);
    Set<VersionObjectReferenceDto> availableProducts = response.getAvailableProducts();
    VersionObjectReferenceDto filteredProduct = availableProducts.stream().findFirst().orElse(null);

    // then
    assertEquals(1, availableProducts.size());
    assertEquals(filteredProduct.getId(), orderableId1);
    assertEquals(1L, (long) filteredProduct.getVersionNumber());
    response.getOrder().getOrderLineItems().forEach(lineItem -> assertTrue(lineItem.isSkipped()));
    assertEquals("requisitionNumber-2", response.getOrder().getRequisitionNumber());
  }


  @Test
  public void shouldFilterNotFullyShippedWhenReqIsEmergency() {
    // given
    OrderDto orderDto = createOrderDto();
    when(orderController.getOrder(orderId, null)).thenReturn(orderDto);
    when(orderExternalRepository.findOne(orderDto.getExternalId())).thenReturn(null);
    Requisition requisition = createRequisition();
    requisition.setEmergency(true);
    when(requisitionController.findRequisition(any(), any())).thenReturn(requisition);
    when(authenticationHelper.getCurrentUser()).thenReturn(createUser(userId, userHomeFacilityId));
    ApprovedProductDto productDto1 = createApprovedProductDto(orderableId1);
    ApprovedProductDto productDto2 = createApprovedProductDto(orderableId2);
    List<ApprovedProductDto> list = Arrays.asList(productDto1, productDto2);
    when(requisitionService.getApproveProduct(approverFacilityId, programId, false))
        .thenReturn(new ApproveProductsAggregator(list, programId));
    when(requisitionService.getApproveProduct(userHomeFacilityId, programId, false))
        .thenReturn(createUserAggregator());
    when(siglusArchiveProductService.searchArchivedProductsByFacilityId(any()))
        .thenReturn(Collections.emptySet());
    when(siglusStockCardSummariesService
        .searchStockCardSummaryV2Dtos(any(), any())).thenReturn(createSummaryPage());
    OrderLineItemExtension extension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .skipped(true)
        .build();
    when(lineItemExtensionRepository.findByOrderLineItemIdIn((newHashSet(lineItemId))))
        .thenReturn(newArrayList(extension));
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId))
        .thenReturn("requisitionNumber-3");
    when(siglusRequisitionService.getPreviousEmergencyRequisition(any(), any(), any()))
        .thenReturn(Arrays.asList(new RequisitionV2Dto()));
    when(filterAddProductForEmergencyService.getInProgressProducts(anyList()))
        .thenReturn(Sets.newHashSet(orderableId2));
    when(filterAddProductForEmergencyService.getNotFullyShippedProducts(anyList()))
        .thenReturn(Sets.newHashSet(orderableId1));

    // when
    SiglusOrderDto response = siglusOrderService.searchOrderById(orderId);
    Set<VersionObjectReferenceDto> availableProducts = response.getAvailableProducts();

    // then
    assertEquals(0, availableProducts.size());
    response.getOrder().getOrderLineItems().forEach(lineItem -> assertTrue(lineItem.isSkipped()));
    assertEquals("requisitionNumber-3", response.getOrder().getRequisitionNumber());
  }

  @Test
  public void shouldUpdateOrderLineItemsAndFillLineItemId() {
    // given
    ShipmentDraftDto draftDto = createShipmentDraftDto();
    when(orderRepository.findOne(orderId)).thenReturn(createOrder());
    when(orderRepository.save(any(Order.class))).thenReturn(createSavedOrder());

    // when
    Set<UUID> response = siglusOrderService.updateOrderLineItems(draftDto);

    // then
    assertEquals(1, response.size());
    assertTrue(response.contains(lineItemId));
    assertEquals(draftDto.getOrder().getOrderLineItems().get(0).getId(), lineItemId);
  }

  @Test
  public void shouldCreateNewOrderAndUpdateExistOrderWhenFistPartial() {
    // given
    OrderObjectReferenceDto orderObjectReferenceDto = new OrderObjectReferenceDto(
        UUID.randomUUID());
    orderObjectReferenceDto.setExternalId(UUID.randomUUID());
    orderObjectReferenceDto.setOrderCode("order_code");
    OrderLineItemDto lineItemDto = new OrderLineItemDto();
    lineItemDto.setOrderedQuantity(Long.valueOf(50));
    lineItemDto.setPartialFulfilledQuantity(Long.valueOf(20));
    lineItemDto.setOrderable(createOrderableDto(orderableId1));
    orderObjectReferenceDto.setOrderLineItems(Arrays.asList(lineItemDto));
    OrderExternal firstExternal = OrderExternal.builder()
        .requisitionId(orderObjectReferenceDto.getExternalId()).build();
    firstExternal.setId(UUID.randomUUID());
    OrderExternal secondExternal = OrderExternal.builder()
        .requisitionId(orderObjectReferenceDto.getExternalId()).build();
    secondExternal.setId(UUID.randomUUID());
    when(orderExternalRepository.findOne(orderObjectReferenceDto.getExternalId())).thenReturn(null);
    when(orderExternalRepository.save(anyList()))
        .thenReturn(Arrays.asList(firstExternal, secondExternal));
    Order order = new Order();
    when(orderRepository.findOne(orderObjectReferenceDto.getId())).thenReturn(order);
    BasicOrderDto basicOrderDto = new BasicOrderDto();
    basicOrderDto.setId(UUID.randomUUID());
    basicOrderDto.setExternalId(secondExternal.getId());
    basicOrderDto.setOrderCode("order_code");
    when(orderRepository.findOne(basicOrderDto.getId())).thenReturn(order);
    when(orderController.batchCreateOrders(anyList(), any(OAuth2Authentication.class)))
        .thenReturn(Arrays.asList(basicOrderDto));
    when(orderController.getOrder(basicOrderDto.getId(), null))
        .thenReturn(createOrderDto());

    // when
    siglusOrderService.createSubOrder(orderObjectReferenceDto, Arrays.asList(lineItemDto));

    // then
    verify(orderController).batchCreateOrders(orderDtoArgumentCaptor.capture(), any());
    OrderDto orderDto = orderDtoArgumentCaptor.getValue().get(0);
    verify(orderRepository, times(2)).save(any(Order.class));
    assertEquals("order_code-2", orderDto.getOrderCode());
    assertEquals(secondExternal.getId(), orderDto.getExternalId());
  }

  @Test
  public void shouldCallControllerWhenSearchOrders() {
    // when
    siglusOrderService.searchOrders(null, null);

    //then
    verify(orderController).searchOrders(null, null);
  }

  @Test
  public void shouldCallControllerWhenSearchOrdersForFulfill() {
    // given
    @SuppressWarnings("unchecked")
    Page<Order> page = (Page<Order>) mock(Page.class);
    @SuppressWarnings("unchecked")
    List<Order> list = (List<Order>) mock(List.class);
    when(page.getContent()).thenReturn(list);
    when(orderService.searchOrdersForFulfillPage(any(), any())).thenReturn(page);
    List<BasicOrderDto> returnList = Collections.singletonList(mock(BasicOrderDto.class));
    when(basicOrderDtoBuilder.build(list)).thenReturn(returnList);
    OrderSearchParams params = mock(OrderSearchParams.class);
    Pageable pageable = mock(Pageable.class);

    // when
    Page<BasicOrderDto> basicOrderDtos = siglusOrderService
        .searchOrdersForFulfill(params, pageable);

    //then
    verify(orderService).searchOrdersForFulfillPage(params, pageable);
    verify(basicOrderDtoBuilder).build(list);
    assertEquals(basicOrderDtos.getContent(), returnList);
  }

  @Test
  public void shouldIsSuborderWhenExternalExist() {
    // given
    UUID externalId = UUID.randomUUID();
    OrderExternal external = new OrderExternal();
    when(orderExternalRepository.findOne(externalId)).thenReturn(external);

    // when
    boolean isSuborder = siglusOrderService.isSuborder(externalId);

    //then
    assertEquals(true, isSuborder);
  }

  @Test
  public void shouldNoSuborderWhenExternalExist() {
    // given
    UUID externalId = UUID.randomUUID();
    when(orderExternalRepository.findOne(externalId)).thenReturn(null);

    // when
    boolean isSuborder = siglusOrderService.isSuborder(externalId);

    //then
    assertEquals(false, isSuborder);
  }

  @Test
  public void shouldTrueWhenCurrentDateIsAfterNextPeriodSubmitEndDate() {
    // given
    LocalDate current = LocalDate.now();
    ProcessingPeriodDto dto = new ProcessingPeriodDto();
    dto.setId(UUID.randomUUID());
    ProcessingScheduleDto scheduleDto = new ProcessingScheduleDto();
    scheduleDto.setId(UUID.randomUUID());
    dto.setStartDate(current.minusMonths(1));
    dto.setEndDate(current.minusDays(1));
    dto.setProcessingSchedule(scheduleDto);
    Page<ProcessingPeriodDto> periodDtos = Pagination
        .getPage(Collections.singletonList(dto));
    when(periodService
        .searchProcessingPeriods(any(UUID.class), any(), any(), any(), any(), any(), any()))
        .thenReturn(periodDtos);
    org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto currentDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto();
    currentDto.setStartDate(current.minusMonths(2));
    currentDto.setEndDate(current.minusMonths(1));
    org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto scheduleDto2 =
        new org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto();
    scheduleDto2.setId(UUID.randomUUID());
    currentDto.setProcessingSchedule(scheduleDto2);
    OrderDto orderDto = new OrderDto();
    orderDto.setProcessingPeriod(currentDto);
    orderDto.setFacility(getFacilityDto());
    ProcessingPeriodExtension extension = new ProcessingPeriodExtension();
    extension.setSubmitStartDate(current.minusDays(10));
    extension.setSubmitEndDate(current.minusDays(2));
    when(processingPeriodExtensionRepository.findByProcessingPeriodId(dto.getId()))
        .thenReturn(extension);

    // when
    boolean isAfterNextPeriodEndDate =
        siglusOrderService.needCloseOrder(orderDto);

    //then
    assertEquals(true, isAfterNextPeriodEndDate);
  }

  @Test
  public void shouldFalseWhenCurrentDateIsAfterNextPeriodEndDate() {
    // given
    LocalDate current = LocalDate.now();
    ProcessingPeriodDto dto = new ProcessingPeriodDto();
    dto.setId(UUID.randomUUID());
    ProcessingScheduleDto scheduleDto = new ProcessingScheduleDto();
    scheduleDto.setId(UUID.randomUUID());
    dto.setStartDate(current.minusMonths(1));
    dto.setEndDate(current.minusDays(1));
    dto.setProcessingSchedule(scheduleDto);

    Page<ProcessingPeriodDto> periodDtos = Pagination
        .getPage(Arrays.asList(dto));
    when(periodService
        .searchProcessingPeriods(any(UUID.class), any(), any(), any(), any(), any(), any()))
        .thenReturn(periodDtos);
    org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto currentDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto();
    currentDto.setStartDate(current.minusMonths(2));
    currentDto.setEndDate(current.minusMonths(1));
    org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto scheduleDto2 =
        new org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto();
    scheduleDto2.setId(UUID.randomUUID());
    currentDto.setProcessingSchedule(scheduleDto2);
    ProcessingPeriodExtension extension = new ProcessingPeriodExtension();
    extension.setSubmitStartDate(current.minusDays(10));
    extension.setSubmitEndDate(current.plusDays(2));
    when(processingPeriodExtensionRepository.findByProcessingPeriodId(dto.getId()))
        .thenReturn(extension);

    OrderDto orderDto = new OrderDto();
    orderDto.setProcessingPeriod(currentDto);
    orderDto.setFacility(getFacilityDto());

    // when
    boolean isAfterNextPeriodEndDate =
        siglusOrderService.needCloseOrder(orderDto);

    //then
    assertEquals(false, isAfterNextPeriodEndDate);
  }

  @Test
  public void shouldTrueWhenCurrentDateIsAfterNextPeriodEndDateButTypeIsFc() {
    // given
    LocalDate current = LocalDate.now();
    ProcessingPeriodDto dto = new ProcessingPeriodDto();
    dto.setStartDate(current.minusMonths(1));
    dto.setEndDate(current.minusDays(1));
    Page<ProcessingPeriodDto> periodDtos = Pagination
        .getPage(Arrays.asList(dto));
    when(periodService
        .searchProcessingPeriods(any(UUID.class), any(), any(), any(), any(), any(), any()))
        .thenReturn(periodDtos);
    org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto currentDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto();
    currentDto.setStartDate(current.minusMonths(2));
    currentDto.setEndDate(current.minusMonths(1));
    OrderDto orderDto = new OrderDto();
    orderDto.setProcessingPeriod(currentDto);
    orderDto.setFacility(getBelongFcFacilityDto());

    // when
    boolean isAfterNextPeriodEndDate =
        siglusOrderService.needCloseOrder(orderDto);

    //then
    assertEquals(false, isAfterNextPeriodEndDate);
  }

  @Test
  public void shouldRevertOrderWhenCurrentDateIsAfterNextPeriodSubmitEndDateAndIsSubOrder() {
    // given
    OrderDto orderDto = new OrderDto();
    UUID externalId = UUID.randomUUID();
    orderDto.setExternalId(externalId);
    orderDto.setFacility(getFacilityDto());
    orderDto.setStatus(OrderStatus.FULFILLING);
    LocalDate current = LocalDate.now();
    org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto currentDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto();
    currentDto.setStartDate(current.minusMonths(2));
    currentDto.setEndDate(current.minusMonths(1));
    org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto processingScheduleDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    currentDto.setProcessingSchedule(processingScheduleDto);
    orderDto.setProcessingPeriod(currentDto);
    ProcessingPeriodDto dto = new ProcessingPeriodDto();
    dto.setStartDate(current.minusMonths(1));
    dto.setEndDate(current.plusDays(1));
    dto.setId(UUID.randomUUID());
    ProcessingPeriodExtension extension = new ProcessingPeriodExtension();
    extension.setSubmitStartDate(current.minusDays(10));
    extension.setSubmitEndDate(current.minusDays(2));
    when(processingPeriodExtensionRepository.findByProcessingPeriodId(dto.getId()))
        .thenReturn(extension);
    Page<ProcessingPeriodDto> periodDtos = Pagination
        .getPage(Collections.singletonList(dto));
    when(periodService
        .searchProcessingPeriods(any(UUID.class), any(), any(), any(), any(), any(), any()))
        .thenReturn(periodDtos);
    when(orderController.getOrder(orderId, null)).thenReturn(orderDto);
    when(orderExternalRepository.findOne(externalId)).thenReturn(new OrderExternal());
    Order order = new Order();
    order.setOrderLineItems(Collections.emptyList());
    when(orderRepository.findOne(orderId)).thenReturn(order);

    // when
    siglusOrderService.searchOrderStatusById(orderId);

    // then
    verify(draftService).deleteOrderLineItemAndInitialedExtension(any(Order.class));
    verify(orderRepository).save(any(Order.class));
  }

  @Test
  public void shouldDontRevertOrderWhenOrderStatusClosed() {
    // given
    OrderDto orderDto = new OrderDto();
    UUID externalId = UUID.randomUUID();
    orderDto.setExternalId(externalId);
    orderDto.setFacility(getFacilityDto());
    orderDto.setStatus(OrderStatus.CLOSED);
    LocalDate current = LocalDate.now();
    org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto currentDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto();
    currentDto.setStartDate(current.minusMonths(2));
    currentDto.setEndDate(current.minusMonths(1));
    org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto processingScheduleDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    currentDto.setProcessingSchedule(processingScheduleDto);
    orderDto.setProcessingPeriod(currentDto);
    ProcessingPeriodDto dto = new ProcessingPeriodDto();
    dto.setStartDate(current.minusMonths(1));
    dto.setEndDate(current.minusDays(1));
    dto.setId(UUID.randomUUID());
    ProcessingPeriodExtension extension = new ProcessingPeriodExtension();
    extension.setSubmitStartDate(current.minusDays(10));
    extension.setSubmitEndDate(current.minusDays(1));
    when(processingPeriodExtensionRepository.findByProcessingPeriodId(dto.getId()))
        .thenReturn(extension);
    Page<ProcessingPeriodDto> periodDtos = Pagination
        .getPage(Collections.singletonList(dto));
    when(periodService
        .searchProcessingPeriods(any(UUID.class), any(), any(), any(), any(), any(), any()))
        .thenReturn(periodDtos);
    when(orderController.getOrder(orderId, null)).thenReturn(orderDto);
    when(orderExternalRepository.findOne(externalId)).thenReturn(new OrderExternal());
    Order order = new Order();
    order.setOrderLineItems(Collections.emptyList());
    when(orderRepository.findOne(orderId)).thenReturn(order);

    // when
    siglusOrderService.searchOrderStatusById(orderId);

    // then
    verify(draftService, times(0))
        .deleteOrderLineItemAndInitialedExtension(any(Order.class));
    verify(orderRepository, times(0)).save(any(Order.class));
  }

  @Test
  public void shouldDontRevertOrderWhenOrderNoSubOrder() {
    // given
    OrderDto orderDto = new OrderDto();
    UUID externalId = UUID.randomUUID();
    orderDto.setExternalId(externalId);
    orderDto.setStatus(OrderStatus.CLOSED);
    orderDto.setFacility(getFacilityDto());
    LocalDate current = LocalDate.now();
    org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto currentDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto();
    currentDto.setStartDate(current.minusMonths(2));
    currentDto.setEndDate(current.minusMonths(1));
    org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto processingScheduleDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    currentDto.setProcessingSchedule(processingScheduleDto);
    orderDto.setProcessingPeriod(currentDto);
    ProcessingPeriodDto dto = new ProcessingPeriodDto();
    dto.setStartDate(current.minusMonths(1));
    dto.setEndDate(current.plusDays(10));
    dto.setId(UUID.randomUUID());
    ProcessingPeriodExtension extension = new ProcessingPeriodExtension();
    extension.setSubmitStartDate(current.minusDays(10));
    extension.setSubmitEndDate(current.minusDays(1));
    when(processingPeriodExtensionRepository.findByProcessingPeriodId(dto.getId()))
        .thenReturn(extension);
    Page<ProcessingPeriodDto> periodDtos = Pagination
        .getPage(Collections.singletonList(dto));
    when(periodService
        .searchProcessingPeriods(any(UUID.class), any(), any(), any(), any(), any(), any()))
        .thenReturn(periodDtos);
    when(orderController.getOrder(orderId, null)).thenReturn(orderDto);
    when(orderExternalRepository.findOne(externalId)).thenReturn(null);
    Order order = new Order();
    order.setOrderLineItems(Collections.emptyList());
    when(orderRepository.findOne(orderId)).thenReturn(order);

    // when
    siglusOrderService.searchOrderStatusById(orderId);

    // then
    verify(draftService, times(0))
        .deleteOrderLineItemAndInitialedExtension(any(Order.class));
    verify(orderRepository, times(0)).save(any(Order.class));
  }

  @Test
  public void shouldDontRevertOrderWhenCurrentDateIsBeforeNextPeriodSubmitEndDate() {
    // given
    OrderDto orderDto = new OrderDto();
    UUID externalId = UUID.randomUUID();
    orderDto.setFacility(getFacilityDto());
    orderDto.setExternalId(externalId);
    orderDto.setStatus(OrderStatus.CLOSED);
    LocalDate current = LocalDate.now();
    org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto currentDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto();
    currentDto.setStartDate(current.minusMonths(2));
    currentDto.setEndDate(current.minusMonths(1));
    org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto processingScheduleDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    currentDto.setProcessingSchedule(processingScheduleDto);
    orderDto.setProcessingPeriod(currentDto);
    ProcessingPeriodDto dto = new ProcessingPeriodDto();
    dto.setStartDate(current.minusMonths(1));
    dto.setEndDate(current.minusDays(1));
    dto.setId(UUID.randomUUID());
    ProcessingPeriodExtension extension = new ProcessingPeriodExtension();
    extension.setSubmitStartDate(current.minusDays(10));
    extension.setSubmitEndDate(current.plusDays(10));
    when(processingPeriodExtensionRepository.findByProcessingPeriodId(dto.getId()))
        .thenReturn(extension);
    Page<ProcessingPeriodDto> periodDtos = Pagination
        .getPage(Collections.singletonList(dto));
    when(periodService
        .searchProcessingPeriods(any(UUID.class), any(), any(), any(), any(), any(), any()))
        .thenReturn(periodDtos);
    when(orderController.getOrder(orderId, null)).thenReturn(orderDto);
    when(orderExternalRepository.findOne(externalId)).thenReturn(null);
    Order order = new Order();
    order.setOrderLineItems(Collections.emptyList());
    when(orderRepository.findOne(orderId)).thenReturn(order);

    // when
    siglusOrderService.searchOrderStatusById(orderId);

    // then
    verify(draftService, times(0))
        .deleteOrderLineItemAndInitialedExtension(any(Order.class));
    verify(orderRepository, times(0)).save(any(Order.class));
  }

  @Test
  public void shouldCreateNewOrderWhenSecondPartial() {
    // given
    UUID requisitionId = UUID.randomUUID();
    OrderObjectReferenceDto orderObjectReferenceDto = new OrderObjectReferenceDto(
        UUID.randomUUID());
    OrderExternal secondExternal = OrderExternal.builder().requisitionId(requisitionId).build();
    secondExternal.setId(UUID.randomUUID());
    orderObjectReferenceDto.setExternalId(secondExternal.getId());
    orderObjectReferenceDto.setOrderCode("order_code-2");
    OrderLineItemDto lineItemDto = new OrderLineItemDto();
    lineItemDto.setOrderedQuantity(Long.valueOf(50));
    lineItemDto.setPartialFulfilledQuantity(Long.valueOf(20));
    lineItemDto.setOrderable(createOrderableDto(orderableId1));
    orderObjectReferenceDto.setOrderLineItems(Arrays.asList(lineItemDto));
    OrderExternal firstExternal = OrderExternal.builder().requisitionId(requisitionId).build();
    firstExternal.setId(UUID.randomUUID());
    OrderExternal thirdExternal = OrderExternal.builder().requisitionId(requisitionId).build();
    thirdExternal.setId(UUID.randomUUID());
    when(orderExternalRepository.findOne(orderObjectReferenceDto.getExternalId()))
        .thenReturn(secondExternal);
    List<OrderExternal> existOrderExternals = new ArrayList<>();
    existOrderExternals.add(firstExternal);
    existOrderExternals.add(secondExternal);
    when(orderExternalRepository.findByRequisitionId(secondExternal.getRequisitionId()))
        .thenReturn(existOrderExternals);
    when(orderExternalRepository.save(any(OrderExternal.class)))
        .thenReturn(thirdExternal);
    Order order = new Order();
    when(orderRepository.findOne(orderObjectReferenceDto.getId())).thenReturn(order);
    BasicOrderDto basicOrderDto = new BasicOrderDto();
    basicOrderDto.setId(UUID.randomUUID());
    basicOrderDto.setExternalId(thirdExternal.getId());
    basicOrderDto.setOrderCode("order_code-2");
    when(orderRepository.findOne(basicOrderDto.getId())).thenReturn(order);
    when(orderController.batchCreateOrders(anyList(), any(OAuth2Authentication.class)))
        .thenReturn(Arrays.asList(basicOrderDto));
    when(orderController.getOrder(basicOrderDto.getId(), null))
        .thenReturn(createOrderDto());

    // when
    siglusOrderService.createSubOrder(orderObjectReferenceDto, Arrays.asList(lineItemDto));

    // then
    verify(orderController).batchCreateOrders(orderDtoArgumentCaptor.capture(), any());
    OrderDto orderDto = orderDtoArgumentCaptor.getValue().get(0);
    verify(orderRepository, times(1)).save(any(Order.class));
    assertEquals("order_code-3", orderDto.getOrderCode());
    assertEquals(thirdExternal.getId(), orderDto.getExternalId());
  }

  private ShipmentDraftDto createShipmentDraftDto() {
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setOrder(createOrderObjectReferenceDto());

    return draftDto;
  }

  private OrderObjectReferenceDto createOrderObjectReferenceDto() {
    OrderObjectReferenceDto dto = new OrderObjectReferenceDto(orderId);
    OrderLineItemDto orderLineItemDto = new OrderLineItemDto();
    orderLineItemDto.setOrderable(createOrderableDto(orderableId1));
    orderLineItemDto.setOrderedQuantity(1L);
    dto.setOrderLineItems(newArrayList(orderLineItemDto));
    return dto;
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

  private OrderDto createOrderDto() {
    OrderDto order = new OrderDto();
    order.setFacility(getFacilityDto());
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

  private FacilityDto getFacilityDto() {
    FacilityDto facilityDto = new FacilityDto();
    org.openlmis.fulfillment.service.referencedata.FacilityTypeDto typeDto =
        new org.openlmis.fulfillment.service.referencedata.FacilityTypeDto();
    typeDto.setId(UUID.randomUUID());
    facilityDto.setType(typeDto);
    return facilityDto;
  }

  private FacilityDto getBelongFcFacilityDto() {
    FacilityDto facilityDto = new FacilityDto();
    org.openlmis.fulfillment.service.referencedata.FacilityTypeDto typeDto =
        new org.openlmis.fulfillment.service.referencedata.FacilityTypeDto();
    typeDto.setId(facilityTypeId);
    facilityDto.setType(typeDto);
    return facilityDto;
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
    summaryV2Dto.setOrderable(
        new org.openlmis.stockmanagement.dto.referencedata.VersionObjectReferenceDto(orderableId1,
            "", "", 1L));
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
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setFullSupply(true);
    orderableDto.setPrograms(Sets.newHashSet(programOrderableDto));
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
    order.setId(orderId);
    return order;
  }

  private Order createSavedOrder() {
    OrderLineItemDto orderLineItemDto = new OrderLineItemDto();
    orderLineItemDto.setOrderable(createOrderableDto(orderableId1));
    orderLineItemDto.setOrderedQuantity(1L);
    orderLineItemDto.setId(lineItemId);
    Order order = createOrder();
    order.getOrderLineItems().add(OrderLineItem.newInstance(orderLineItemDto));
    return order;
  }
}
