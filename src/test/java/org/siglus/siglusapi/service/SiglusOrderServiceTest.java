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
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.util.SiglusDateHelper.DATE_MONTH_YEAR;
import static org.siglus.siglusapi.util.SiglusDateHelper.getFormatDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.service.OrderService;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.Pagination;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.BasicOrderDtoBuilder;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProcessingScheduleDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApprovedProductReferenceDataService;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.util.PageImplRepresentation;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.powermock.modules.junit4.PowerMockRunner;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.constant.PeriodConstants;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.dto.FulfillOrderDto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.i18n.MessageKeys;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusOrdersRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SiglusShipmentRepository;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.repository.dto.OrderSuggestedQuantityDto;
import org.siglus.siglusapi.repository.dto.RequisitionOrderDto;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.web.response.OrderPickPackResponse;
import org.siglus.siglusapi.web.response.OrderSuggestedQuantityResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.util.ReflectionTestUtils;


@RunWith(PowerMockRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusOrderServiceTest {

  private final String receivingFacilityName = "receiving facility name";
  private final String supplyingFacilityName = "supplying facility name";
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
  private ApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private SiglusArchiveProductService siglusArchiveProductService;

  @Mock
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Mock
  private OrderLineItemExtensionRepository lineItemExtensionRepository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private OrderExternalRepository orderExternalRepository;

  @Mock
  private SiglusOrdersRepository siglusOrdersRepository;

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

  @Mock
  private OrderableRepository orderableRepository;

  @Mock
  private SiglusProcessingPeriodService siglusProcessingPeriodService;

  @Mock
  private SiglusFacilityRepository siglusFacilityRepository;

  @Mock
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Mock
  private StockManagementRepository stockManagementRepository;

  @Mock
  private SiglusShipmentRepository siglusShipmentRepository;

  @Rule
  public ExpectedException exception = ExpectedException.none();

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
  private final UUID lineItemId2 = UUID.randomUUID();
  private final UUID facilityTypeId = UUID.randomUUID();
  private final UUID orderExternalId = UUID.randomUUID();
  private final UUID supplyingFacilityId = UUID.randomUUID();
  private final UUID receivingFacilityId = UUID.randomUUID();
  private final LocalDate now = LocalDate.now();
  private final String orderCode = "ORDER-CODE";
  private final UUID periodId1 = UUID.randomUUID();
  private final LocalDate processPeriodEndDate = LocalDate.of(
          LocalDate.now().getYear(), LocalDate.now().getMonthValue(), 25);

  private final List<UUID> periodIds = Lists.newArrayList(
      UUID.randomUUID(),
      UUID.randomUUID(),
      UUID.randomUUID(),
      UUID.randomUUID(),
      UUID.randomUUID(),
      UUID.randomUUID());

  private final List<String> clientFacilityIds = Lists.newArrayList(
      UUID.randomUUID().toString(),
      UUID.randomUUID().toString(),
      UUID.randomUUID().toString(),
      UUID.randomUUID().toString(),
      UUID.randomUUID().toString(),
      UUID.randomUUID().toString());

  private final List<UUID> requisitionIds = Lists.newArrayList(
      UUID.randomUUID(),
      UUID.randomUUID(),
      UUID.randomUUID(),
      UUID.randomUUID(),
      UUID.randomUUID(),
      UUID.randomUUID());

  @Before
  public void prepare() {
    ReflectionTestUtils.setField(siglusOrderService, "timeZoneId", "UTC");
    ReflectionTestUtils.setField(siglusOrderService, "fcFacilityTypeId",
        facilityTypeId);
    when(siglusShipmentRepository.findAllByOrderIdIn(any())).thenReturn(Collections.emptyList());
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
    when(approvedProductReferenceDataService.getApprovedProducts(approverFacilityId, programId)).thenReturn(
        createApprovedProducts());
    when(approvedProductReferenceDataService.getApprovedProducts(userHomeFacilityId, programId))
        .thenReturn(createUserApprovedProducts());

    when(siglusArchiveProductService.searchArchivedProductsByFacilityId(any())).thenReturn(createArchivedProducts());
    when(siglusStockCardSummariesService.searchStockCardSummaryV2Dtos(any(), any(), any(), any(), any(Boolean.class)))
        .thenReturn(createSummaryPage());
    OrderLineItemExtension extension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .skipped(true)
        .build();
    when(lineItemExtensionRepository.findByOrderLineItemIdIn((newHashSet(lineItemId))))
        .thenReturn(newArrayList(extension));
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId)).thenReturn("requisitionNumber-1");
    List<UUID> orderableIds = newArrayList(orderableId1);
    when(orderableRepository.findLatestByIds(orderableIds)).thenReturn(Collections.emptyList());

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
    when(approvedProductReferenceDataService.getApprovedProducts(approverFacilityId, programId)).thenReturn(list);
    when(approvedProductReferenceDataService.getApprovedProducts(userHomeFacilityId, programId))
        .thenReturn(createUserApprovedProducts());
    when(siglusArchiveProductService.searchArchivedProductsByFacilityId(any()))
        .thenReturn(Collections.emptySet());
    when(siglusStockCardSummariesService
        .searchStockCardSummaryV2Dtos(any(), any(), any(), any(), any(Boolean.class))).thenReturn(createSummaryPage());
    OrderLineItemExtension extension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .skipped(true)
        .build();
    when(lineItemExtensionRepository.findByOrderLineItemIdIn((newHashSet(lineItemId))))
        .thenReturn(newArrayList(extension));
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId))
        .thenReturn("requisitionNumber-2");
    when(siglusRequisitionService.getPreviousEmergencyRequisition(any()))
        .thenReturn(Collections.singletonList(new RequisitionV2Dto()));
    when(filterAddProductForEmergencyService.getInProgressProducts(anyList()))
        .thenReturn(Sets.newHashSet(orderableId2));
    List<UUID> orderableIds = newArrayList(orderableId1);
    when(orderableRepository.findLatestByIds(orderableIds)).thenReturn(Collections.emptyList());

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
    when(approvedProductReferenceDataService.getApprovedProducts(approverFacilityId, programId)).thenReturn(list);
    when(approvedProductReferenceDataService.getApprovedProducts(userHomeFacilityId, programId))
        .thenReturn(createUserApprovedProducts());
    when(siglusArchiveProductService.searchArchivedProductsByFacilityId(any()))
        .thenReturn(Collections.emptySet());
    when(siglusStockCardSummariesService
        .searchStockCardSummaryV2Dtos(any(), any(), any(), any(), any(Boolean.class))).thenReturn(createSummaryPage());
    OrderLineItemExtension extension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .skipped(true)
        .build();
    when(lineItemExtensionRepository.findByOrderLineItemIdIn((newHashSet(lineItemId))))
        .thenReturn(newArrayList(extension));
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId))
        .thenReturn("requisitionNumber-3");
    when(siglusRequisitionService.getPreviousEmergencyRequisition(any()))
        .thenReturn(Collections.singletonList(new RequisitionV2Dto()));
    when(filterAddProductForEmergencyService.getInProgressProducts(anyList()))
        .thenReturn(Sets.newHashSet(orderableId2));
    when(filterAddProductForEmergencyService.getNotFullyShippedProducts(anyList()))
        .thenReturn(Sets.newHashSet(orderableId1));
    List<UUID> orderableIds = newArrayList(orderableId1);
    when(orderableRepository.findLatestByIds(orderableIds)).thenReturn(Collections.emptyList());

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
    OrderObjectReferenceDto orderObjectReferenceDto = new OrderObjectReferenceDto(UUID.randomUUID());
    orderObjectReferenceDto.setExternalId(UUID.randomUUID());
    orderObjectReferenceDto.setOrderCode("order_code/01");
    OrderLineItemDto lineItemDto = new OrderLineItemDto();
    lineItemDto.setOrderedQuantity(50L);
    lineItemDto.setPartialFulfilledQuantity(20L);
    lineItemDto.setOrderable(createOrderableDto(orderableId1));
    orderObjectReferenceDto.setOrderLineItems(Collections.singletonList(lineItemDto));
    OrderExternal firstExternal = OrderExternal.builder()
        .requisitionId(orderObjectReferenceDto.getExternalId()).build();
    firstExternal.setId(UUID.randomUUID());
    OrderExternal secondExternal = OrderExternal.builder()
        .requisitionId(orderObjectReferenceDto.getExternalId()).build();
    secondExternal.setId(UUID.randomUUID());
    when(orderExternalRepository.findOne(orderObjectReferenceDto.getExternalId())).thenReturn(null);
    when(orderExternalRepository.save(anyList())).thenReturn(Arrays.asList(firstExternal, secondExternal));
    Order order = new Order();
    when(orderRepository.findOne(orderObjectReferenceDto.getId())).thenReturn(order);
    BasicOrderDto basicOrderDto = new BasicOrderDto();
    basicOrderDto.setId(UUID.randomUUID());
    basicOrderDto.setExternalId(secondExternal.getId());
    basicOrderDto.setOrderCode("order_code/01");
    when(orderRepository.findOne(basicOrderDto.getId())).thenReturn(order);
    when(orderController.batchCreateOrders(anyList(), any(OAuth2Authentication.class)))
        .thenReturn(Collections.singletonList(basicOrderDto));
    when(orderController.getOrder(basicOrderDto.getId(), null))
        .thenReturn(createOrderDto());

    // when
    siglusOrderService.createSubOrder(orderObjectReferenceDto, Collections.singletonList(lineItemDto));

    // then
    verify(orderController).batchCreateOrders(orderDtoArgumentCaptor.capture(), any());
    OrderDto orderDto = orderDtoArgumentCaptor.getValue().get(0);
    verify(orderRepository, times(2)).save(any(Order.class));
    assertEquals("order_code/02", orderDto.getOrderCode());
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
  public void shouldThrowNotFoundExceptionWhenPeriodNotFound() {
    //then
    exception.expect(NotFoundException.class);
    exception.expectMessage(MessageKeys.ERROR_PERIOD_NOT_FOUND);

    // given
    Page<Order> page = (Page<Order>) mock(Page.class);
    List<Order> list = (List<Order>) mock(List.class);
    when(page.getContent()).thenReturn(list);

    BasicOrderDto basicOrderDto = new BasicOrderDto();
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    basicOrderDto.setProgram(programDto);

    org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto processingPeriodDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto();
    processingPeriodDto.setEndDate(processPeriodEndDate);
    processingPeriodDto.setId(periodId1);
    basicOrderDto.setProcessingPeriod(processingPeriodDto);

    when(processingPeriodExtensionRepository.findByProcessingPeriodId(periodId1)).thenReturn(null);
    when(orderService.searchOrdersForFulfillPage(any(), any())).thenReturn(page);
    List<BasicOrderDto> returnList = Collections.singletonList(basicOrderDto);
    when(basicOrderDtoBuilder.build(list)).thenReturn(returnList);

    OrderSearchParams params = mock(OrderSearchParams.class);
    Pageable pageable = mock(Pageable.class);

    // when
    siglusOrderService.searchOrdersForFulfill(params, pageable);
  }

  @Test
  public void shouldSetExpiredFlagToFulfillOrder() {
    // given
    Page<Order> page = (Page<Order>) mock(Page.class);
    List<Order> list = (List<Order>) mock(List.class);
    when(page.getContent()).thenReturn(list);

    BasicOrderDto basicOrderDto = new BasicOrderDto();
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    basicOrderDto.setProgram(programDto);

    org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto processingPeriodDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto();
    processingPeriodDto.setEndDate(processPeriodEndDate);
    processingPeriodDto.setId(periodId1);
    basicOrderDto.setProcessingPeriod(processingPeriodDto);
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(supplyingFacilityId);
    basicOrderDto.setSupplyingFacility(facilityDto);

    ProcessingPeriodExtension processingPeriodExtension = new ProcessingPeriodExtension();
    processingPeriodExtension.setProcessingPeriodId(periodId1);
    processingPeriodExtension.setSubmitStartDate(LocalDate.now());
    processingPeriodExtension.setSubmitEndDate(LocalDate.now());

    YearMonth yearMonth = YearMonth.of(LocalDate.now().getYear(), LocalDate.now().getMonth());

    when(requisitionService.calculateFulfillOrderYearMonth(processingPeriodExtension))
            .thenReturn(Arrays.asList(yearMonth));
    when(processingPeriodExtensionRepository.findByProcessingPeriodId(periodId1)).thenReturn(processingPeriodExtension);
    when(processingPeriodExtensionRepository.findByProcessingPeriodIdIn(Sets.newHashSet(periodId1)))
            .thenReturn(Arrays.asList(processingPeriodExtension));
    when(orderService.searchOrdersForFulfillPage(any(), any())).thenReturn(page);
    List<BasicOrderDto> returnList = Collections.singletonList(basicOrderDto);
    when(basicOrderDtoBuilder.build(list)).thenReturn(returnList);
    when(siglusOrdersRepository.findBySupplyingFacilityIdAndProgramIdAndStatusIn(supplyingFacilityId, programId,
        Lists.newArrayList(OrderStatus.SHIPPED, OrderStatus.FULFILLING, OrderStatus.PARTIALLY_FULFILLED,
            OrderStatus.RECEIVED))).thenReturn(Collections.emptyList());

    OrderSearchParams params = mock(OrderSearchParams.class);
    Pageable pageable = mock(Pageable.class);

    // when
    Page<FulfillOrderDto> basicOrderDtos = siglusOrderService
        .searchOrdersForFulfill(params, pageable);

    //then
    assertFalse(basicOrderDtos.getContent().get(0).isExpired());
  }

  @Test
  public void shouldNotSetExpiredFlagToFulfillOrderWhenTodayIsNotInSubmissionDay() {
    // given
    Page<Order> page = (Page<Order>) mock(Page.class);
    List<Order> list = (List<Order>) mock(List.class);
    when(page.getContent()).thenReturn(list);

    BasicOrderDto basicOrderDto = new BasicOrderDto();
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    basicOrderDto.setProgram(programDto);

    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(supplyingFacilityId);
    basicOrderDto.setSupplyingFacility(facilityDto);

    org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto processingPeriodDto =
        new org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto();
    processingPeriodDto.setEndDate(LocalDate.now().minusMonths(3));
    processingPeriodDto.setId(periodId1);
    basicOrderDto.setProcessingPeriod(processingPeriodDto);

    ProcessingPeriodExtension processingPeriodExtension = new ProcessingPeriodExtension();
    processingPeriodExtension.setSubmitStartDate(LocalDate.now().minusMonths(1).minusDays(5));
    processingPeriodExtension.setSubmitEndDate(LocalDate.now().minusMonths(1).minusDays(2));

    when(processingPeriodExtensionRepository.findByProcessingPeriodId(periodId1)).thenReturn(processingPeriodExtension);
    when(orderService.searchOrdersForFulfillPage(any(), any())).thenReturn(page);
    List<BasicOrderDto> returnList = Collections.singletonList(basicOrderDto);
    when(basicOrderDtoBuilder.build(list)).thenReturn(returnList);
    when(siglusOrdersRepository.findBySupplyingFacilityIdAndProgramIdAndStatusIn(supplyingFacilityId, programId,
        Lists.newArrayList(OrderStatus.SHIPPED, OrderStatus.FULFILLING, OrderStatus.PARTIALLY_FULFILLED,
            OrderStatus.RECEIVED))).thenReturn(Collections.emptyList());

    OrderSearchParams params = mock(OrderSearchParams.class);
    Pageable pageable = mock(Pageable.class);

    // when
    Page<FulfillOrderDto> basicOrderDtos = siglusOrderService
        .searchOrdersForFulfill(params, pageable);

    //then
    assertTrue(basicOrderDtos.getContent().get(0).isExpired());
  }

  @Test
  public void shouldThrowNotFoundExceptionWhenNotFoundOrder() {
    //then
    exception.expect(NotFoundException.class);
    exception.expectMessage(MessageKeys.ERROR_ORDER_NOT_EXIST);

    //given
    when(siglusOrdersRepository.findOne(orderId)).thenReturn(null);

    //when
    siglusOrderService.closeExpiredOrder(orderId);
  }

  @Test
  public void shouldCloseFulfillOrder() {
    //given
    Order order = new Order();
    Order savedOrder = new Order();
    savedOrder.setStatus(OrderStatus.CLOSED);
    when(siglusOrdersRepository.findOne(orderId)).thenReturn(order);

    //when
    siglusOrderService.closeExpiredOrder(orderId);

    //then
    verify(siglusOrdersRepository).save(savedOrder);
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
    assertTrue(isSuborder);
  }

  @Test
  public void shouldNoSuborderWhenExternalExist() {
    // given
    UUID externalId = UUID.randomUUID();
    when(orderExternalRepository.findOne(externalId)).thenReturn(null);

    // when
    boolean isSuborder = siglusOrderService.isSuborder(externalId);

    //then
    assertFalse(isSuborder);
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
    assertTrue(isAfterNextPeriodEndDate);
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
    assertFalse(isAfterNextPeriodEndDate);
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
    assertFalse(isAfterNextPeriodEndDate);
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
    orderObjectReferenceDto.setOrderCode("order_code/02");
    OrderLineItemDto lineItemDto = new OrderLineItemDto();
    lineItemDto.setOrderedQuantity(50L);
    lineItemDto.setPartialFulfilledQuantity(20L);
    lineItemDto.setOrderable(createOrderableDto(orderableId1));
    orderObjectReferenceDto.setOrderLineItems(Collections.singletonList(lineItemDto));
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
    basicOrderDto.setOrderCode("order_code/02");
    when(orderRepository.findOne(basicOrderDto.getId())).thenReturn(order);
    when(orderController.batchCreateOrders(anyList(), any(OAuth2Authentication.class)))
        .thenReturn(Collections.singletonList(basicOrderDto));
    when(orderController.getOrder(basicOrderDto.getId(), null))
        .thenReturn(createOrderDto());

    // when
    siglusOrderService.createSubOrder(orderObjectReferenceDto, Arrays.asList(lineItemDto));

    // then
    verify(orderController).batchCreateOrders(orderDtoArgumentCaptor.capture(), any());
    OrderDto orderDto = orderDtoArgumentCaptor.getValue().get(0);
    verify(orderRepository, times(1)).save(any(Order.class));
    assertEquals("order_code/03", orderDto.getOrderCode());
    assertEquals(thirdExternal.getId(), orderDto.getExternalId());
  }

  @Test
  public void shouldReturnNotShowSuggestedQuantityWhenPeriodIsPreviousPeriod() {
    // given
    when(orderRepository.findOne(orderId)).thenReturn(buildMockOrderWithPreviousPeriodId());
    when(siglusProcessingPeriodService.getUpToNowMonthlyPeriods()).thenReturn(buildMockPeriods());

    // when
    OrderSuggestedQuantityResponse actualResponse = siglusOrderService.getOrderSuggestedQuantityResponse(orderId);

    // then
    OrderSuggestedQuantityResponse expectResponse = OrderSuggestedQuantityResponse.builder()
        .showSuggestedQuantity(Boolean.FALSE)
        .build();
    assertEquals(expectResponse, actualResponse);
  }

  @Test
  public void shouldReturnShowSuggestedQuantityAndEmptyMapWhenPeriodIsCurrentPeriodAndOrderIsEmergency() {
    // given
    when(orderRepository.findOne(orderId)).thenReturn(buildMockEmergencyOrderWithCurrentPeriodId());
    when(siglusProcessingPeriodService.getUpToNowMonthlyPeriods()).thenReturn(buildMockPeriods());

    // when
    OrderSuggestedQuantityResponse actualResponse = siglusOrderService.getOrderSuggestedQuantityResponse(orderId);

    // then
    OrderSuggestedQuantityResponse expectResponse = OrderSuggestedQuantityResponse.builder()
        .showSuggestedQuantity(Boolean.TRUE)
        .build();
    assertEquals(expectResponse, actualResponse);
  }

  @Test
  public void shouldReturnShowSuggestedQuantityAndEmptyMapWhenPeriodIsCurrentPeriodAndOrderIsSubOrder() {
    // given
    when(orderRepository.findOne(orderId)).thenReturn(buildMockSubOrderWithCurrentPeriodId());
    when(siglusProcessingPeriodService.getUpToNowMonthlyPeriods()).thenReturn(buildMockPeriods());
    when(orderExternalRepository.findOne(orderExternalId)).thenReturn(buildMockOrderExternal());

    // when
    OrderSuggestedQuantityResponse actualResponse = siglusOrderService.getOrderSuggestedQuantityResponse(orderId);

    // then
    OrderSuggestedQuantityResponse expectResponse = OrderSuggestedQuantityResponse.builder()
        .showSuggestedQuantity(Boolean.TRUE)
        .build();
    assertEquals(expectResponse, actualResponse);
  }

  @Test
  public void shouldCalculateAndSaveSuggestedQuantityWhenStartFulfillOrderWithSohIsEnough() {
    // given
    mockCalculateSuggestedQuantityCommonConditions(buildMockOrderWithCurrentPeriodIdAndStatusNotFulfilling(),
        buildCurrentPeriodRequisitions());

    Map<UUID, Integer> orderableIdToSoh = Maps.newHashMap();
    orderableIdToSoh.put(orderableId1, 100);
    orderableIdToSoh.put(orderableId2, 200);

    when(stockManagementRepository.getAvailableStockOnHandByProduct(any(), any(), anyList(), anyObject())).thenReturn(
        orderableIdToSoh);

    // when
    OrderSuggestedQuantityResponse actualResponse = siglusOrderService.getOrderSuggestedQuantityResponse(orderId);

    // 6 facility,
    // orderable1: 10 / (10 + 10 + 0 + 0 + 30 + 0 + 0) * 10 = 20
    // orderable2: 20 / (20 + 20 + 0 + 0 + 0 + 40 + 0) * 20 = 50

    // then
    Map<UUID, BigDecimal> orderbaleIdToSuggestedQuantity = Maps.newHashMap();
    orderbaleIdToSuggestedQuantity.put(orderableId1, BigDecimal.valueOf(10).setScale(2, RoundingMode.DOWN));
    orderbaleIdToSuggestedQuantity.put(orderableId2, BigDecimal.valueOf(20).setScale(2, RoundingMode.DOWN));

    OrderSuggestedQuantityResponse expectResponse = OrderSuggestedQuantityResponse.builder()
        .showSuggestedQuantity(Boolean.TRUE)
        .orderableIdToSuggestedQuantity(orderbaleIdToSuggestedQuantity)
        .build();
    assertEquals(expectResponse, actualResponse);
    verify(lineItemExtensionRepository).save(anyList());
  }

  @Test
  public void shouldCalculateAndSaveSuggestedQuantityWhenStartFulfillOrderWithSohIsNotEnough() {
    // given
    mockCalculateSuggestedQuantityCommonConditions(buildMockOrderWithCurrentPeriodIdAndStatusNotFulfilling(),
        buildCurrentPeriodRequisitions());

    Map<UUID, Integer> orderableIdToSoh = Maps.newHashMap();
    orderableIdToSoh.put(orderableId1, 10);
    orderableIdToSoh.put(orderableId2, 20);

    when(stockManagementRepository.getAvailableStockOnHandByProduct(any(), any(), anyList(), anyObject())).thenReturn(
        orderableIdToSoh);

    // when
    OrderSuggestedQuantityResponse actualResponse = siglusOrderService.getOrderSuggestedQuantityResponse(orderId);

    // then
    // 6 facility,
    // orderable1: 10 / (10 + 10 + 0 + 0 + 30 + 0 + 0) * 10 = 2
    // orderable2: 20 / (20 + 20 + 0 + 0 + 0 + 40 + 0) * 20 = 5
    Map<UUID, BigDecimal> orderbaleIdToSuggestedQuantity = Maps.newHashMap();
    orderbaleIdToSuggestedQuantity.put(orderableId1, BigDecimal.valueOf(2).setScale(2, RoundingMode.DOWN));
    orderbaleIdToSuggestedQuantity.put(orderableId2, BigDecimal.valueOf(5).setScale(2, RoundingMode.DOWN));

    OrderSuggestedQuantityResponse expectResponse = OrderSuggestedQuantityResponse.builder()
        .showSuggestedQuantity(Boolean.TRUE)
        .orderableIdToSuggestedQuantity(orderbaleIdToSuggestedQuantity)
        .build();
    assertEquals(expectResponse, actualResponse);
    verify(lineItemExtensionRepository).save(anyList());
  }

  @Test
  public void shouldCalculateAndSaveSuggestedQuantityWhenStartFulfillOrderWithSohIsNotEnoughAndApprovedQuantity0() {
    // given
    mockCalculateSuggestedQuantityCommonConditions(buildMockOrderWithCurrentPeriodIdAndStatusNotFulfilling(),
        buildCurrentPeriodRequisitionsWithCurrentRequisitionApproveQuantity0());

    Map<UUID, Integer> orderableIdToSoh = Maps.newHashMap();
    orderableIdToSoh.put(orderableId1, 10);
    orderableIdToSoh.put(orderableId2, 20);

    when(stockManagementRepository.getAvailableStockOnHandByProduct(any(), any(), anyList(), anyObject())).thenReturn(
        orderableIdToSoh);

    // when
    OrderSuggestedQuantityResponse actualResponse = siglusOrderService.getOrderSuggestedQuantityResponse(orderId);

    // then
    // 6 facility,
    // orderable1: approved quantity is 0, so suggested quantity is 0
    // orderable2: 20 / (20 + 20 + 0 + 0 + 0 + 40 + 0) * 20 = 5
    Map<UUID, BigDecimal> orderbaleIdToSuggestedQuantity = Maps.newHashMap();
    orderbaleIdToSuggestedQuantity.put(orderableId1, BigDecimal.valueOf(0).setScale(2, RoundingMode.DOWN));
    orderbaleIdToSuggestedQuantity.put(orderableId2, BigDecimal.valueOf(5).setScale(2, RoundingMode.DOWN));

    OrderSuggestedQuantityResponse expectResponse = OrderSuggestedQuantityResponse.builder()
        .showSuggestedQuantity(Boolean.TRUE)
        .orderableIdToSuggestedQuantity(orderbaleIdToSuggestedQuantity)
        .build();
    assertEquals(expectResponse, actualResponse);
    verify(lineItemExtensionRepository).save(anyList());
  }

  @Test
  public void shouldQuerySuggestedQuantityFromDbWhenContinueFulfillOrder() {
    // given
    mockCalculateSuggestedQuantityCommonConditions(buildMockOrderWithCurrentPeriodIdAndStatusFulfilling(),
        buildCurrentPeriodRequisitions());
    when(lineItemExtensionRepository.findOrderSuggestedQuantityDtoByOrderLineItemIdIn(
        anyList())).thenReturn(buildMockOrderSuggestedQuantityDtos());

    // when
    OrderSuggestedQuantityResponse actualResponse = siglusOrderService.getOrderSuggestedQuantityResponse(orderId);

    // then
    Map<UUID, BigDecimal> orderbaleIdToSuggestedQuantity = Maps.newHashMap();
    orderbaleIdToSuggestedQuantity.put(orderableId1, BigDecimal.valueOf(2).setScale(2, RoundingMode.DOWN));
    orderbaleIdToSuggestedQuantity.put(orderableId2, BigDecimal.valueOf(5).setScale(2, RoundingMode.DOWN));

    OrderSuggestedQuantityResponse expectResponse = OrderSuggestedQuantityResponse.builder()
        .showSuggestedQuantity(Boolean.TRUE)
        .orderableIdToSuggestedQuantity(orderbaleIdToSuggestedQuantity)
        .build();
    assertEquals(expectResponse, actualResponse);
    verify(lineItemExtensionRepository).findOrderSuggestedQuantityDtoByOrderLineItemIdIn(anyList());
  }

  @Test
  public void shuildReturnOrderPickPackResponseWhenGetOrderPickPackResponse() {
    // given
    when(orderRepository.findOne(orderId)).thenReturn(buildMockOrderWithCurrentPeriodIdAndStatusFulfilling());
    when(siglusFacilityRepository.findAll(Lists.newArrayList(receivingFacilityId, supplyingFacilityId))).thenReturn(
        buildMockFacilities());

    // when
    OrderPickPackResponse actualResponse = siglusOrderService.getOrderPickPackResponse(orderId);

    // then
    OrderPickPackResponse expectResponse = OrderPickPackResponse.builder()
        .generatedDate(getFormatDate(LocalDate.now(), DATE_MONTH_YEAR))
        .orderCode(orderCode)
        .clientFacility(receivingFacilityName)
        .supplierFacility(supplyingFacilityName)
        .build();
    assertEquals(expectResponse, actualResponse);
  }

  private List<Facility> buildMockFacilities() {
    Facility receivingFacility = new Facility();
    receivingFacility.setId(receivingFacilityId);
    receivingFacility.setName(receivingFacilityName);

    Facility supplyingFacility = new Facility();
    supplyingFacility.setId(supplyingFacilityId);
    supplyingFacility.setName(supplyingFacilityName);

    return Lists.newArrayList(receivingFacility, supplyingFacility);
  }

  private List<OrderSuggestedQuantityDto> buildMockOrderSuggestedQuantityDtos() {
    OrderSuggestedQuantityDto dto1 = OrderSuggestedQuantityDto.builder()
        .orderableId(orderableId1)
        .suggestedQuantity(2d)
        .build();

    OrderSuggestedQuantityDto dto2 = OrderSuggestedQuantityDto.builder()
        .orderableId(orderableId2)
        .suggestedQuantity(5d)
        .build();

    return Lists.newArrayList(dto1, dto2);
  }

  private void mockCalculateSuggestedQuantityCommonConditions(Order order,
      List<Requisition> currentPeriodRequisitions) {

    when(orderRepository.findOne(orderId)).thenReturn(order);
    when(siglusProcessingPeriodService.getUpToNowMonthlyPeriods()).thenReturn(buildMockPeriods());
    when(siglusFacilityRepository.findAllClientFacilityIds(supplyingFacilityId, programId)).thenReturn(
        clientFacilityIds);

    when(siglusRequisitionRepository.findRequisitionsByOrderInfo(anyList(), any(), anyList(),
        anyBoolean(), anyList()))
        .thenReturn(currentPeriodRequisitions)
        .thenReturn(buildPreviousPeriodRequisitions());

    List<Requisition> releasedRequisitions = currentPeriodRequisitions.stream()
        .filter(requisition -> RequisitionStatus.RELEASED == requisition.getStatus())
        .collect(toList());
    Set<UUID> releasedRequisitionIds = releasedRequisitions.stream().map(Requisition::getId)
        .collect(Collectors.toSet());
    when(siglusRequisitionRepository.findRequisitionOrderDtoByRequisitionIds(releasedRequisitionIds)).thenReturn(
        buildMockRequisitionOrderDtos());
    when(lineItemExtensionRepository.findByOrderLineItemIdIn(anyList())).thenReturn(buildMockOrderLineItemExtensions());
  }

  private List<OrderLineItemExtension> buildMockOrderLineItemExtensions() {
    OrderLineItemExtension lineItemExtension = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId)
        .build();
    lineItemExtension.setId(UUID.randomUUID());

    OrderLineItemExtension lineItemExtension2 = OrderLineItemExtension.builder()
        .orderLineItemId(lineItemId2)
        .build();
    lineItemExtension2.setId(UUID.randomUUID());

    return Lists.newArrayList(lineItemExtension, lineItemExtension2);
  }

  private List<RequisitionOrderDto> buildMockRequisitionOrderDtos() {
    // convert to order(released) and not start fulfill
    RequisitionOrderDto dto1 = RequisitionOrderDto.builder()
        .requisitionId(requisitionIds.get(1))
        .orderStatus(OrderStatus.ORDERED.name())
        .build();

    // convert to order(released) and finished fulfillment
    RequisitionOrderDto dto2 = RequisitionOrderDto.builder()
        .requisitionId(requisitionIds.get(2))
        .orderStatus(OrderStatus.SHIPPED.name())
        .build();
    return Lists.newArrayList(dto1, dto2);
  }

  private List<Requisition> buildCurrentPeriodRequisitions() {
    List<UUID> clientFacilityIds = getClientFacilityIds();

    // final approved requisition
    Requisition requisition0 = new Requisition();
    requisition0.setId(requisitionIds.get(0));
    requisition0.setFacilityId(clientFacilityIds.get(0));
    requisition0.setStatus(RequisitionStatus.APPROVED);
    requisition0.setRequisitionLineItems(
        Lists.newArrayList(buildMockLineItemWithOrderableId1ApprovedQuantity10(),
            buildMockLineItemWithOrderableId2ApprovedQuantity20()));

    // convert to order(released) and not start fulfill
    Requisition requisition1 = new Requisition();
    requisition1.setId(requisitionIds.get(1));
    requisition1.setFacilityId(clientFacilityIds.get(1));
    requisition1.setStatus(RequisitionStatus.RELEASED);
    requisition1.setRequisitionLineItems(
        Lists.newArrayList(buildMockLineItemWithOrderableId1ApprovedQuantity10(),
            buildMockLineItemWithOrderableId2ApprovedQuantity20()));

    // convert to order(released) and finished fulfillment
    Requisition requisition2 = new Requisition();
    requisition2.setId(requisitionIds.get(2));
    requisition2.setFacilityId(clientFacilityIds.get(2));
    requisition2.setStatus(RequisitionStatus.RELEASED);

    // released without order
    Requisition requisition3 = new Requisition();
    requisition3.setId(requisitionIds.get(3));
    requisition3.setFacilityId(clientFacilityIds.get(3));
    requisition3.setStatus(RequisitionStatus.RELEASED_WITHOUT_ORDER);

    return Lists.newArrayList(requisition0, requisition1, requisition2, requisition3);
  }

  private List<Requisition> buildCurrentPeriodRequisitionsWithCurrentRequisitionApproveQuantity0() {
    List<UUID> clientFacilityIds = getClientFacilityIds();

    // final approved requisition
    Requisition requisition0 = new Requisition();
    requisition0.setId(requisitionIds.get(0));
    requisition0.setFacilityId(clientFacilityIds.get(0));
    requisition0.setStatus(RequisitionStatus.APPROVED);
    requisition0.setRequisitionLineItems(
        Lists.newArrayList(buildMockLineItemWithOrderableId1ApprovedQuantity0(),
            buildMockLineItemWithOrderableId2ApprovedQuantity20()));

    // convert to order(released) and not start fulfill
    Requisition requisition1 = new Requisition();
    requisition1.setId(requisitionIds.get(1));
    requisition1.setFacilityId(clientFacilityIds.get(1));
    requisition1.setStatus(RequisitionStatus.RELEASED);
    requisition1.setRequisitionLineItems(
        Lists.newArrayList(buildMockLineItemWithOrderableId1ApprovedQuantity10(),
            buildMockLineItemWithOrderableId2ApprovedQuantity20()));

    // convert to order(released) and finished fulfillment
    Requisition requisition2 = new Requisition();
    requisition2.setId(requisitionIds.get(2));
    requisition2.setFacilityId(clientFacilityIds.get(2));
    requisition2.setStatus(RequisitionStatus.RELEASED);

    // released without order
    Requisition requisition3 = new Requisition();
    requisition3.setId(requisitionIds.get(3));
    requisition3.setFacilityId(clientFacilityIds.get(3));
    requisition3.setStatus(RequisitionStatus.RELEASED_WITHOUT_ORDER);

    return Lists.newArrayList(requisition0, requisition1, requisition2, requisition3);
  }

  private List<Requisition> buildPreviousPeriodRequisitions() {
    List<UUID> clientFacilityIds = getClientFacilityIds();

    // facility4: final approved requisition
    Requisition requisition4One = new Requisition();
    requisition4One.setId(requisitionIds.get(4));
    requisition4One.setFacilityId(clientFacilityIds.get(4));
    requisition4One.setStatus(RequisitionStatus.APPROVED);
    requisition4One.setRequisitionLineItems(Lists.newArrayList(buildMockLineItemWithOrderableId1ApprovedQuantity10()));

    // facility4: convert to order(released) and not start fulfill
    Requisition requisition4Two = new Requisition();
    requisition4Two.setId(requisitionIds.get(4));
    requisition4Two.setFacilityId(clientFacilityIds.get(4));
    requisition4Two.setStatus(RequisitionStatus.RELEASED);
    requisition4Two.setRequisitionLineItems(Lists.newArrayList(buildMockLineItemWithOrderableId1ApprovedQuantity30()));

    // facility5: convert to order(released) and finished fulfillment
    Requisition requisition5One = new Requisition();
    requisition5One.setId(requisitionIds.get(5));
    requisition5One.setFacilityId(clientFacilityIds.get(5));
    requisition5One.setStatus(RequisitionStatus.RELEASED);
    requisition5One.setRequisitionLineItems(Lists.newArrayList(buildMockLineItemWithOrderableId2ApprovedQuantity20()));

    // facility5: released without order
    Requisition requisition5Two = new Requisition();
    requisition5Two.setId(requisitionIds.get(5));
    requisition5Two.setFacilityId(clientFacilityIds.get(5));
    requisition5Two.setStatus(RequisitionStatus.RELEASED_WITHOUT_ORDER);
    requisition5Two.setRequisitionLineItems(Lists.newArrayList(buildMockLineItemWithOrderableId2ApprovedQuantity40()));

    return Lists.newArrayList(requisition4One, requisition4Two, requisition5One, requisition5Two);
  }

  private RequisitionLineItem buildMockLineItemWithOrderableId1ApprovedQuantity10() {
    RequisitionLineItem lineItemOrderableId1 = new RequisitionLineItem();
    lineItemOrderableId1.setOrderable(mockRequisitionOrderable(orderableId1));
    lineItemOrderableId1.setApprovedQuantity(10);
    return lineItemOrderableId1;
  }

  private RequisitionLineItem buildMockLineItemWithOrderableId1ApprovedQuantity30() {
    RequisitionLineItem lineItemOrderableId1 = new RequisitionLineItem();
    lineItemOrderableId1.setOrderable(mockRequisitionOrderable(orderableId1));
    lineItemOrderableId1.setApprovedQuantity(30);
    return lineItemOrderableId1;
  }

  private RequisitionLineItem buildMockLineItemWithOrderableId1ApprovedQuantity0() {
    RequisitionLineItem lineItemOrderableId1 = new RequisitionLineItem();
    lineItemOrderableId1.setOrderable(mockRequisitionOrderable(orderableId1));
    lineItemOrderableId1.setApprovedQuantity(0);
    return lineItemOrderableId1;
  }

  private RequisitionLineItem buildMockLineItemWithOrderableId2ApprovedQuantity20() {
    RequisitionLineItem lineItemOrderableId1 = new RequisitionLineItem();
    lineItemOrderableId1.setOrderable(mockRequisitionOrderable(orderableId2));
    lineItemOrderableId1.setApprovedQuantity(20);
    return lineItemOrderableId1;
  }

  private RequisitionLineItem buildMockLineItemWithOrderableId2ApprovedQuantity40() {
    RequisitionLineItem lineItemOrderableId1 = new RequisitionLineItem();
    lineItemOrderableId1.setOrderable(mockRequisitionOrderable(orderableId2));
    lineItemOrderableId1.setApprovedQuantity(40);
    return lineItemOrderableId1;
  }

  private org.openlmis.requisition.domain.requisition.VersionEntityReference mockRequisitionOrderable(
      UUID orderableId) {
    org.openlmis.requisition.domain.requisition.VersionEntityReference orderable =
        new org.openlmis.requisition.domain.requisition.VersionEntityReference();
    orderable.setId(orderableId);
    return orderable;
  }

  private List<UUID> getClientFacilityIds() {
    return clientFacilityIds.stream().map(UUID::fromString).collect(Collectors.toList());
  }

  private OrderExternal buildMockOrderExternal() {
    return OrderExternal.builder().build();
  }

  private Order buildMockOrderWithCurrentPeriodIdAndStatusNotFulfilling() {
    OrderLineItem lineItem1 = new OrderLineItem();
    lineItem1.setId(lineItemId);
    lineItem1.setOrderable(new VersionEntityReference(orderableId1, 1L));

    OrderLineItem lineItem2 = new OrderLineItem();
    lineItem2.setId(lineItemId2);
    lineItem2.setOrderable(new VersionEntityReference(orderableId2, 1L));

    Order order = new Order();
    order.setOrderLineItems(Lists.newArrayList(lineItem1, lineItem2));
    order.setId(orderId);
    order.setProcessingPeriodId(periodIds.get(1));
    order.setEmergency(Boolean.FALSE);
    order.setExternalId(requisitionIds.get(0));

    order.setSupplyingFacilityId(supplyingFacilityId);
    order.setProgramId(programId);
    order.setStatus(OrderStatus.ORDERED);

    return order;
  }

  private Order buildMockOrderWithCurrentPeriodIdAndStatusFulfilling() {
    OrderLineItem lineItem1 = new OrderLineItem();
    lineItem1.setId(lineItemId);
    lineItem1.setOrderable(new VersionEntityReference(orderableId1, 1L));

    OrderLineItem lineItem2 = new OrderLineItem();
    lineItem2.setId(lineItemId2);
    lineItem2.setOrderable(new VersionEntityReference(orderableId2, 1L));

    Order order = new Order();
    order.setOrderLineItems(Lists.newArrayList(lineItem1, lineItem2));
    order.setId(orderId);
    order.setProcessingPeriodId(periodIds.get(1));
    order.setEmergency(Boolean.FALSE);
    order.setExternalId(requisitionIds.get(0));

    order.setReceivingFacilityId(receivingFacilityId);
    order.setSupplyingFacilityId(supplyingFacilityId);
    order.setProgramId(programId);
    order.setStatus(OrderStatus.FULFILLING);

    order.setOrderCode(orderCode);

    return order;
  }

  private Order buildMockEmergencyOrderWithCurrentPeriodId() {
    OrderLineItem lineItem1 = new OrderLineItem();
    lineItem1.setId(lineItemId);
    lineItem1.setOrderable(new VersionEntityReference(orderableId1, 1L));

    OrderLineItem lineItem2 = new OrderLineItem();
    lineItem2.setId(lineItemId2);
    lineItem2.setOrderable(new VersionEntityReference(orderableId2, 1L));

    Order order = new Order();
    order.setOrderLineItems(Lists.newArrayList(lineItem1, lineItem2));
    order.setId(orderId);
    order.setProcessingPeriodId(periodIds.get(1));
    order.setEmergency(Boolean.TRUE);

    return order;
  }

  private Order buildMockSubOrderWithCurrentPeriodId() {
    OrderLineItem lineItem1 = new OrderLineItem();
    lineItem1.setId(lineItemId);
    lineItem1.setOrderable(new VersionEntityReference(orderableId1, 1L));

    OrderLineItem lineItem2 = new OrderLineItem();
    lineItem2.setId(lineItemId2);
    lineItem2.setOrderable(new VersionEntityReference(orderableId2, 1L));

    Order order = new Order();
    order.setOrderLineItems(Lists.newArrayList(lineItem1, lineItem2));
    order.setId(orderId);
    order.setProcessingPeriodId(periodIds.get(1));
    order.setExternalId(orderExternalId);

    return order;
  }

  private Order buildMockOrderWithPreviousPeriodId() {
    OrderLineItem lineItem1 = new OrderLineItem();
    lineItem1.setId(lineItemId);
    lineItem1.setOrderable(new VersionEntityReference(orderableId1, 1L));

    OrderLineItem lineItem2 = new OrderLineItem();
    lineItem2.setId(lineItemId2);
    lineItem2.setOrderable(new VersionEntityReference(orderableId2, 1L));

    Order order = new Order();
    order.setOrderLineItems(Lists.newArrayList(lineItem1, lineItem2));
    order.setId(orderId);
    order.setProcessingPeriodId(periodIds.get(2));

    return order;
  }

  private List<ProcessingPeriod> buildMockPeriods() {
    // up to now 12 periods
    ProcessingSchedule m1Schedule = new ProcessingSchedule();
    m1Schedule.setCode(PeriodConstants.MONTH_SCHEDULE_CODE);
    List<ProcessingPeriod> periods = Lists.newArrayList();
    LocalDate currentPeriodStartDate = getCurrentPeriodStartDate();
    for (int i = 1; i < 6; i++) {
      LocalDate startDate = currentPeriodStartDate.minusMonths(i - 1);
      ProcessingPeriod m1ProcessingPeriod = ProcessingPeriod.newPeriod("number_" + i, m1Schedule,
          startDate, startDate.plusMonths(1).minusDays(1));
      m1ProcessingPeriod.setId(periodIds.get(i));
      periods.add(m1ProcessingPeriod);
    }
    return periods;
  }

  private LocalDate getCurrentPeriodStartDate() {
    int day = now.getDayOfMonth();
    // fulfill period is n.26~n+1.25
    if (day >= 26) {
      LocalDate oneMothAgo = now.minusMonths(1);
      return LocalDate.of(oneMothAgo.getYear(), oneMothAgo.getMonth(), 21);
    }
    LocalDate twoMothAgo = now.minusMonths(2);
    return LocalDate.of(twoMothAgo.getYear(), twoMothAgo.getMonth(), 21);
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

  private List<ApprovedProductDto> createApprovedProducts() {
    ApprovedProductDto productDto = createApprovedProductDto(orderableId1);
    List<ApprovedProductDto> list = new ArrayList<>();
    list.add(productDto);
    return list;
  }

  private List<ApprovedProductDto> createUserApprovedProducts() {
    ApprovedProductDto productDto1 = createApprovedProductDto(orderableId1);
    ApprovedProductDto productDto2 = createApprovedProductDto(orderableId2);
    List<ApprovedProductDto> list = new ArrayList<>();
    list.add(productDto1);
    list.add(productDto2);
    return list;
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
