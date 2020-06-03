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

package org.openlmis.fulfillment.service;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.javers.common.collections.Sets.asSet;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.fulfillment.service.PermissionService.ORDERS_EDIT;
import static org.openlmis.fulfillment.service.PermissionService.ORDERS_VIEW;
import static org.openlmis.fulfillment.service.PermissionService.PODS_MANAGE;
import static org.openlmis.fulfillment.service.PermissionService.PODS_VIEW;
import static org.openlmis.fulfillment.service.PermissionService.SHIPMENTS_EDIT;
import static org.openlmis.fulfillment.service.PermissionService.SHIPMENTS_VIEW;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.OrderLineItemDataBuilder;
import org.openlmis.fulfillment.StatusChangeDataBuilder;
import org.openlmis.fulfillment.domain.Base36EncodedOrderNumberGenerator;
import org.openlmis.fulfillment.domain.ExternalStatus;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderNumberConfiguration;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.StatusChange;
import org.openlmis.fulfillment.domain.TransferType;
import org.openlmis.fulfillment.extension.ExtensionManager;
import org.openlmis.fulfillment.extension.point.OrderNumberGenerator;
import org.openlmis.fulfillment.repository.OrderNumberConfigurationRepository;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.repository.TransferPropertiesRepository;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.OrderableReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.PeriodReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.PermissionStrings;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.ProgramReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.openlmis.fulfillment.testutils.FacilityDataBuilder;
import org.openlmis.fulfillment.testutils.OrderableDataBuilder;
import org.openlmis.fulfillment.testutils.ProcessingPeriodDataBuilder;
import org.openlmis.fulfillment.testutils.ProgramDataBuilder;
import org.openlmis.fulfillment.testutils.UserDataBuilder;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.DateHelper;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@SuppressWarnings({"PMD.TooManyMethods"})
@RunWith(MockitoJUnitRunner.class)
public class OrderServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private OrderNumberConfigurationRepository orderNumberConfigurationRepository;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @Mock
  private OrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @Mock
  private TransferPropertiesRepository transferPropertiesRepository;

  @Mock
  private FulfillmentNotificationService notificationService;

  @Mock
  private OrderStorage orderStorage;

  @Mock
  private OrderSender orderSender;

  @Mock
  private DateHelper dateHelper;

  @Mock
  private ExtensionManager extensionManager;

  @Mock
  private PermissionService permissionService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @InjectMocks
  private ExporterBuilder exporter;

  @InjectMocks
  private OrderService orderService;

  @Captor
  private ArgumentCaptor<Order> orderCaptor;

  private ProgramDto program;
  private FacilityDto facility;
  private ProcessingPeriodDto period1;
  private ProcessingPeriodDto period2;
  private OrderableDto orderable;
  private OrderNumberConfiguration orderNumberConfiguration;
  private Order order;
  private UserDto userDto;
  private FtpTransferProperties properties;
  private LocalDate startDate;
  private LocalDate endDate;

  @Before
  public void setUp() {
    generateTestData();
    mockResponses();
  }

  @Test
  public void shouldCreateRegularOrder() throws Exception {
    OrderDto dto = OrderDto.newInstance(order, exporter);
    dto.setId(null);

    Order created = orderService.createOrder(dto, userDto.getId());

    // then
    validateCreatedOrder(created, order);

    verify(orderRepository).save(orderCaptor.capture());
    verify(orderStorage).store(any(Order.class));
    verify(orderSender).send(any(Order.class));
    verify(orderStorage).delete(any(Order.class));

    assertEquals(OrderStatus.IN_ROUTE, orderCaptor.getValue().getStatus());

    verify(notificationService).sendOrderCreatedNotification(eq(created));
  }

  @Test
  public void shouldCreateRegularOrderIfFacilityNotSupportProgram() throws Exception {
    facility.setSupportedPrograms(emptyList());

    OrderDto dto = OrderDto.newInstance(order, exporter);
    dto.setId(null);

    order.setStatus(OrderStatus.ORDERED);

    Order created = orderService.createOrder(dto, userDto.getId());

    // then
    validateCreatedOrder(created, order);

    verify(orderRepository).save(orderCaptor.capture());
    verify(orderStorage).store(any(Order.class));
    verify(orderSender).send(any(Order.class));
    verify(orderStorage).delete(any(Order.class));

    assertEquals(OrderStatus.IN_ROUTE, orderCaptor.getValue().getStatus());

    verify(notificationService).sendOrderCreatedNotification(eq(created));
  }

  @Test
  public void shouldCreateOrderForFulfill() throws Exception {
    program.setSupportLocallyFulfilled(true);

    OrderDto dto = OrderDto.newInstance(order, exporter);
    dto.setId(null);

    order.setStatus(OrderStatus.ORDERED);

    Order created = orderService.createOrder(dto, userDto.getId());

    // then
    validateCreatedOrder(created, order);

    verify(orderRepository).save(orderCaptor.capture());
    verify(orderStorage).store(any(Order.class));
    verify(orderSender).send(any(Order.class));
    verify(orderStorage).delete(any(Order.class));

    assertEquals(OrderStatus.ORDERED, orderCaptor.getValue().getStatus());

    verify(notificationService).sendOrderCreatedNotification(eq(created));
  }

  @Test
  public void shouldSaveOrder() throws Exception {
    Order created = orderService.save(order);

    // then
    validateCreatedOrder(created, order);
    assertEquals(OrderStatus.IN_ROUTE, created.getStatus());

    InOrder inOrder = inOrder(orderRepository, orderStorage, orderSender);
    inOrder.verify(orderRepository).save(order);
    inOrder.verify(orderStorage).store(order);
    inOrder.verify(orderSender).send(order);
    inOrder.verify(orderStorage).delete(order);

    verify(notificationService).sendOrderCreatedNotification(eq(created));
  }

  @Test
  public void shouldSaveOrderAndNotDeleteFileIfFtpSendFailure() throws Exception {
    StatusChange statusChange = new StatusChange();
    statusChange.setStatus(ExternalStatus.APPROVED);
    statusChange.setCreatedDate(ZonedDateTime.now());
    statusChange.setAuthorId(randomUUID());
    order.setStatusChanges(Lists.newArrayList(statusChange));

    when(orderSender.send(order)).thenReturn(false);
    Order created = orderService.save(order);

    // then
    validateCreatedOrder(created, order);
    assertEquals(OrderStatus.TRANSFER_FAILED, created.getStatus());

    InOrder inOrder = inOrder(orderRepository, orderStorage, orderSender);
    inOrder.verify(orderRepository).save(order);
    inOrder.verify(orderStorage).store(order);
    inOrder.verify(orderSender).send(order);
    inOrder.verify(orderStorage, never()).delete(order);
  }

  @Test
  public void shouldFindOrderIfMatchedSupplyingAndRequestingFacilitiesAndProgram() {
    Order order = generateOrder();
    Pageable pageable = new PageRequest(0, 10);
    UserDto user = new UserDataBuilder().build();
    PermissionStrings.Handler handler = mock(PermissionStrings.Handler.class);
    when(handler.getFacilityIds(ORDERS_EDIT, ORDERS_VIEW, SHIPMENTS_EDIT, SHIPMENTS_VIEW))
        .thenReturn(newHashSet(order.getSupplyingFacilityId()));
    when(handler.getFacilityIds(PODS_MANAGE, PODS_VIEW))
        .thenReturn(newHashSet(order.getRequestingFacilityId()));

    when(permissionService.getPermissionStrings(user.getId())).thenReturn(handler);

    OrderSearchParams params = new OrderSearchParams(
        order.getSupplyingFacilityId(), order.getRequestingFacilityId(), order.getProgramId(),
        order.getProcessingPeriodId(), Sets.newHashSet(order.getStatus().toString()), null, null
    );
    when(orderRepository.searchOrders(
        params, asSet(order.getProcessingPeriodId()),
        pageable, newHashSet(order.getSupplyingFacilityId()),
        newHashSet(order.getRequestingFacilityId())))
        .thenReturn(new PageImpl<>(Collections.singletonList(order), pageable, 1));

    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    Page<Order> receivedOrders = orderService.searchOrders(params, pageable);

    assertEquals(1, receivedOrders.getContent().size());
    assertEquals(receivedOrders.getContent().get(0).getSupplyingFacilityId(),
        order.getSupplyingFacilityId());
    assertEquals(receivedOrders.getContent().get(0).getRequestingFacilityId(),
        order.getRequestingFacilityId());
    assertEquals(receivedOrders.getContent().get(0).getProgramId(), order.getProgramId());

    verify(orderRepository, atLeastOnce())
        .searchOrders(anyObject(), anyObject(), anyObject(), anySet(), anySet());
  }

  @Test
  public void shouldNotCheckPermissionWhenCrossServiceRequest() {
    Order order = generateOrder();
    Pageable pageable = new PageRequest(0, 10);

    OrderSearchParams params = new OrderSearchParams(
        order.getSupplyingFacilityId(), order.getRequestingFacilityId(), order.getProgramId(),
        order.getProcessingPeriodId(), Sets.newHashSet(order.getStatus().toString()), null, null);
    when(orderRepository.searchOrders(
        params, asSet(order.getProcessingPeriodId()), pageable))
        .thenReturn(new PageImpl<>(Collections.singletonList(order), pageable, 1));

    when(authenticationHelper.getCurrentUser()).thenReturn(null);

    Page<Order> receivedOrders = orderService.searchOrders(params, pageable);

    assertEquals(receivedOrders.getContent().get(0).getSupplyingFacilityId(),
        order.getSupplyingFacilityId());
    assertEquals(receivedOrders.getContent().get(0).getRequestingFacilityId(),
        order.getRequestingFacilityId());
    assertEquals(receivedOrders.getContent().get(0).getProgramId(), order.getProgramId());

    verify(orderRepository, atLeastOnce())
        .searchOrders(anyObject(), anyObject(), anyObject());

    verify(permissionService, never()).getPermissionStrings(anyObject());
  }

  @Test
  public void shouldSearchByStartDateAndEndDate() {
    Order order = generateOrder();
    Pageable pageable = new PageRequest(0, 10);

    OrderSearchParams params = new OrderSearchParams(
        order.getSupplyingFacilityId(), order.getRequestingFacilityId(), order.getProgramId(),
        null, Sets.newHashSet(order.getStatus().toString()), startDate, endDate);
    when(orderRepository.searchOrders(
        params, asSet(period1.getId(), period2.getId()), pageable))
        .thenReturn(new PageImpl<>(Collections.singletonList(order), pageable, 1));

    when(authenticationHelper.getCurrentUser()).thenReturn(null);

    Page<Order> receivedOrders = orderService.searchOrders(params, pageable);

    assertEquals(1, receivedOrders.getContent().size());
    assertEquals(order, receivedOrders.getContent().get(0));

    verify(orderRepository, atLeastOnce()).searchOrders(anyObject(), anyObject(), anyObject());
  }

  @Test
  public void shouldReturnEmptyPageIfFilteredPeriodsAndGivenPeriodIdDoesNotMatch() {
    Order order = generateOrder();
    Pageable pageable = new PageRequest(0, 10);

    when(authenticationHelper.getCurrentUser()).thenReturn(null);

    OrderSearchParams params = new OrderSearchParams(
        order.getSupplyingFacilityId(), order.getRequestingFacilityId(), order.getProgramId(),
        order.getProcessingPeriodId(), Sets.newHashSet(order.getStatus().toString()),
        startDate, endDate);
    Page<Order> receivedOrders = orderService.searchOrders(params, pageable);

    assertEquals(0, receivedOrders.getContent().size());
    verify(orderRepository, never())
        .searchOrders(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
  }

  @Test
  public void shouldReturnEmptyPageIfPeriodsSearchReturnsEmptyList() {
    Order order = generateOrder();
    Pageable pageable = new PageRequest(0, 10);

    when(authenticationHelper.getCurrentUser()).thenReturn(null);

    when(periodReferenceDataService.search(startDate, endDate)).thenReturn(emptyList());

    OrderSearchParams params = new OrderSearchParams(
        order.getSupplyingFacilityId(), order.getRequestingFacilityId(), order.getProgramId(),
        null, Sets.newHashSet(order.getStatus().toString()),
        startDate, endDate);
    Page<Order> receivedOrders = orderService.searchOrders(params, pageable);

    assertEquals(0, receivedOrders.getContent().size());
    verify(orderRepository, never())
        .searchOrders(anyObject(), anyObject(), anyObject(), anyObject(), anyObject());
  }

  @Test
  public void shouldSearchByStartDateAndEndDateAndPeriodId() {
    Order order = generateOrder();
    Pageable pageable = new PageRequest(0, 10);

    OrderSearchParams params = new OrderSearchParams(
        order.getSupplyingFacilityId(), order.getRequestingFacilityId(), order.getProgramId(),
        period1.getId(), Sets.newHashSet(order.getStatus().toString()), startDate, endDate);
    when(orderRepository.searchOrders(
        params, asSet(period1.getId()), pageable))
        .thenReturn(new PageImpl<>(Collections.singletonList(order), pageable, 1));

    when(authenticationHelper.getCurrentUser()).thenReturn(null);

    Page<Order> receivedOrders = orderService.searchOrders(params, pageable);

    assertEquals(1, receivedOrders.getContent().size());
    assertEquals(order, receivedOrders.getContent().get(0));

    verify(orderRepository, atLeastOnce()).searchOrders(anyObject(), anyObject(), anyObject());
  }

  private Order generateOrder() {
    return new OrderDataBuilder().withOrderedStatus().build();
  }

  private void validateCreatedOrder(Order actual, Order expected) {
    assertEquals(actual.getExternalId(), expected.getExternalId());
    assertEquals(actual.getReceivingFacilityId(), expected.getReceivingFacilityId());
    assertEquals(actual.getRequestingFacilityId(), expected.getRequestingFacilityId());
    assertEquals(actual.getProgramId(), expected.getProgramId());
    assertEquals(actual.getSupplyingFacilityId(), expected.getSupplyingFacilityId());
    assertEquals(1, actual.getOrderLineItems().size());
    assertEquals(1, expected.getOrderLineItems().size());

    OrderLineItem actualLineItem = actual.getOrderLineItems().iterator().next();
    OrderLineItem expectedLineItem = expected.getOrderLineItems().iterator().next();

    assertEquals(expectedLineItem.getOrderedQuantity(), actualLineItem.getOrderedQuantity());
    assertEquals(expectedLineItem.getOrderable(), actualLineItem.getOrderable());

    StatusChange actualStatusChange = actual.getStatusChanges().iterator().next();
    StatusChange expectedStatusChange = expected.getStatusChanges().iterator().next();

    assertEquals(expectedStatusChange.getStatus(), actualStatusChange.getStatus());
    assertEquals(expectedStatusChange.getCreatedDate(), actualStatusChange.getCreatedDate());
    assertEquals(expectedStatusChange.getAuthorId(), actualStatusChange.getAuthorId());
  }

  private void generateTestData() {
    program = new ProgramDataBuilder().build();
    facility = new FacilityDataBuilder()
        .withSupportedPrograms(Collections.singletonList(program))
        .build();
    period1 = new ProcessingPeriodDataBuilder().build();
    period2 = new ProcessingPeriodDataBuilder().build();

    orderNumberConfiguration = new OrderNumberConfiguration("prefix", true, true, true);

    userDto = new UserDataBuilder().build();

    orderable = new OrderableDataBuilder().build();
    OrderLineItem orderLineItem = new OrderLineItemDataBuilder()
        .withOrderedQuantity(100L)
        .withOrderable(orderable.getId(), orderable.getVersionNumber())
        .build();
    StatusChange statusChange = new StatusChangeDataBuilder().build();
    order = new OrderDataBuilder()
        .withQuotedCost(BigDecimal.ZERO)
        .withProgramId(program.getId())
        .withCreatedById(userDto.getId())
        .withEmergencyFlag()
        .withStatus(OrderStatus.IN_ROUTE)
        .withStatusChanges(statusChange)
        .withSupplyingFacilityId(facility.getId())
        .withProcessingPeriodId(period1.getId())
        .withLineItems(orderLineItem)
        .build();

    userDto = new UserDataBuilder().build();

    properties = new FtpTransferProperties();
    properties.setTransferType(TransferType.ORDER);

    startDate = LocalDate.now();
    endDate = startDate.plusMonths(1);
  }

  private void mockResponses() {
    when(programReferenceDataService.findOne(program.getId())).thenReturn(program);
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);
    when(periodReferenceDataService.findOne(period1.getId())).thenReturn(period1);
    when(orderableReferenceDataService.findByIds(any()))
        .thenReturn(Collections.singletonList(orderable));

    when(userReferenceDataService.findOne(any())).thenReturn(userDto);

    when(orderNumberConfigurationRepository.findAll())
        .thenReturn(Collections.singletonList(orderNumberConfiguration));

    when(extensionManager.getExtension(OrderNumberGenerator.POINT_ID, OrderNumberGenerator.class))
        .thenReturn(new Base36EncodedOrderNumberGenerator());

    when(transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(any(),any()))
        .thenReturn(properties);

    when(orderRepository.save(any(Order.class))).thenReturn(order);
    when(orderSender.send(order)).thenReturn(true);

    when(dateHelper.getCurrentDateTimeWithSystemZone()).thenReturn(ZonedDateTime.now());

    when(periodReferenceDataService.search(startDate, endDate))
        .thenReturn(asList(period1, period2));
  }
}
