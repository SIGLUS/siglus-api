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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.RequisitionStatusProcessor;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.utils.Pagination;
import org.openlmis.requisition.web.OrderDtoBuilder;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.service.client.SiglusUserReferenceDataService;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.dto.fc.ProductDto;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.client.ValidSourceDestinationStockManagementService;
import org.siglus.siglusapi.service.fc.FcIssueVoucherService;
import org.siglus.siglusapi.util.SimulateUserAuthenticationHelper;
import org.siglus.siglusapi.validator.FcValidate;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class FcIssueVoucherServiceTest {

  @InjectMocks
  private FcIssueVoucherService service;

  @Mock
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Mock
  private SiglusUserReferenceDataService userReferenceDataService;

  @Mock
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;

  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductService;

  @Spy
  private FcValidate fcDataValidate;

  @Mock
  private SiglusStockEventsService stockEventsService;

  @Mock
  private ValidSourceDestinationStockManagementService sourceDestinationService;

  @Mock
  private PodExtensionRepository podExtensionRepository;

  @Mock
  private OrderExternalRepository orderExternalRepository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private SiglusOrderService siglusOrderService;

  @Mock
  private SiglusShipmentDraftService shipmentDraftService;

  @Mock
  private SiglusStockCardSummariesService stockCardSummariesService;

  @Mock
  private SiglusLotReferenceDataService lotReferenceDataService;

  @Mock
  private SiglusShipmentService siglusShipmentService;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private SimulateUserAuthenticationHelper simulateUser;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private RequisitionStatusProcessor requisitionStatusProcessor;

  @Mock
  private OrderDtoBuilder orderDtoBuilder;

  @Mock
  private OrderFulfillmentService orderFulfillmentService;

  @Captor
  private ArgumentCaptor<ShipmentDto> shipmentCaptor;

  @Captor
  private ArgumentCaptor<Requisition> requisitionCaptor;

  @Captor
  private ArgumentCaptor<List<OrderDto>> orderListCaptor;

  private UserDto userDto;
  private FacilityDto facilityDto;
  private UUID programId = UUID.randomUUID();
  private IssueVoucherDto issueVoucherDto = getIssueVoucherDto();

  @Before
  public void prepare() {
    when(requisitionService.convertToOrder(any(), any())).thenReturn(Collections.emptyList());
    ReflectionTestUtils.setField(service, "timeZoneId", "UTC");
    ReflectionTestUtils.setField(service, "receiveReason",
        "44814bc4-df64-11e9-9e7e-4c32759554d9");
    when(stockEventsService.createStockEvent(any())).thenReturn(UUID.randomUUID());
    facilityDto = new FacilityDto();
    facilityDto.setId(UUID.randomUUID());
    when(siglusFacilityReferenceDataService
        .getFacilityByCode(getIssueVoucherDto().getWarehouseCode()))
        .thenReturn(Pagination
            .getPage(Arrays.asList(facilityDto), new PageRequest(0, 10), 0));
    userDto = new UserDto();
    userDto.setId(UUID.randomUUID());
    userDto.setHomeFacilityId(facilityDto.getId());
    when(userReferenceDataService
        .getUserInfo(facilityDto.getId()))
        .thenReturn(Pagination
            .getPage(Arrays.asList(userDto), new PageRequest(0, 10), 0));
    IssueVoucherDto issueVoucherDto = getIssueVoucherDto();
    when(podExtensionRepository
        .findByClientCodeAndIssueVoucherNumber(issueVoucherDto.getClientCode(),
            issueVoucherDto.getIssueVoucherNumber())).thenReturn(null);
    ValidSourceDestinationDto sourceDestinationDto = new ValidSourceDestinationDto();
    sourceDestinationDto.setId(UUID.randomUUID());
    sourceDestinationDto.setName("FC Integration");
    Node node = new Node();
    node.setId(UUID.randomUUID());
    sourceDestinationDto.setNode(node);
    when(sourceDestinationService.getValidSources(programId,
        facilityDto.getId())).thenReturn(Arrays.asList(sourceDestinationDto));
  }

  @Test
  public void shouldNotCreateIssueVoucherWhenPodHaveExist() {
    // given
    IssueVoucherDto issueVoucherDto = getIssueVoucherDto();
    PodExtension extension = new PodExtension();
    when(podExtensionRepository
        .findByClientCodeAndIssueVoucherNumber(issueVoucherDto.getClientCode(),
            issueVoucherDto.getIssueVoucherNumber())).thenReturn(extension);

    // when
    boolean isSuccess = service.createIssueVouchers(Arrays.asList(issueVoucherDto));

    // then
    assertEquals(true, isSuccess);
    verify(requisitionExtensionRepository, times(0))
        .findByRequisitionNumber(issueVoucherDto.getRequisitionNumber());
  }

  @Test
  public void shouldReturnFailIfRequisitionStatusNotApproved() {
    // given
    RequisitionExtension requisitionExtension = getRequisitionExtension();
    fcDataValidate.validateEmptyFacilityCode(issueVoucherDto.getWarehouseCode());
    requisitionExtension.setRequisitionId(UUID.randomUUID());
    when(requisitionExtensionRepository
        .findByRequisitionNumber(issueVoucherDto.getRequisitionNumber()))
        .thenReturn(requisitionExtension);
    RequisitionV2Dto requisitionV2Dto = getRequisitionV2Dto(requisitionExtension);
    requisitionV2Dto.setStatus(RequisitionStatus.AUTHORIZED);
    when(siglusRequisitionRequisitionService
        .searchRequisition(requisitionExtension.getRequisitionId()))
        .thenReturn(requisitionV2Dto);

    // when
    boolean isSuccess = service.createIssueVouchers(Arrays.asList(issueVoucherDto));

    // then
    assertEquals(false, isSuccess);
    assertEquals(1, service.getStatusErrorIssueVoucherNumber().size());
  }

  @Test
  public void shouldReturnSuccessWhenUpdateSubOrderAndCanFulfillEqualNull() {
    // given
    RequisitionExtension requisitionExtension = getRequisitionExtension();
    fcDataValidate.validateEmptyFacilityCode(issueVoucherDto.getWarehouseCode());
    requisitionExtension.setRequisitionId(UUID.randomUUID());
    when(requisitionExtensionRepository
        .findByRequisitionNumber(issueVoucherDto.getRequisitionNumber()))
        .thenReturn(requisitionExtension);
    RequisitionV2Dto requisitionV2Dto = getRequisitionV2Dto(requisitionExtension);
    when(siglusRequisitionRequisitionService
        .searchRequisition(requisitionExtension.getRequisitionId()))
        .thenReturn(requisitionV2Dto);
    OrderableDto orderableDto = getOrderableDto();
    ApprovedProductDto approvedProductDto = getApprovedProductDto(orderableDto);
    when(approvedProductService.getApprovedProducts(userDto.getHomeFacilityId(),
        requisitionV2Dto.getProgramId(), null, false))
        .thenReturn(Arrays.asList(approvedProductDto));
    OrderExternal orderExternal = new OrderExternal();
    orderExternal.setId(UUID.randomUUID());
    when(orderExternalRepository.findByRequisitionId(requisitionV2Dto.getId())).thenReturn(
        Collections.emptyList());
    Order canFulfillOrder = new Order();
    canFulfillOrder.setId(UUID.randomUUID());
    canFulfillOrder.setOrderLineItems(new ArrayList<>());
    when(orderRepository.findCanFulfillOrderAndInExternalId(Arrays.asList(
        orderExternal.getId()))).thenReturn(null);
    Order order = new Order();
    order.setId(UUID.randomUUID());
    when(orderRepository.findByExternalId(requisitionV2Dto.getId()))
        .thenReturn(order, order);
    org.openlmis.fulfillment.web.util.OrderDto orderDto = new
        org.openlmis.fulfillment.web.util.OrderDto();
    orderDto.setId(order.getId());
    SiglusOrderDto dto = new SiglusOrderDto();
    dto.setOrder(orderDto);
    when(siglusOrderService.searchOrderById(order.getId())).thenReturn(dto);
    BasicOrderDto basicOrderDto = new BasicOrderDto();
    basicOrderDto.setId(order.getId());
    when(siglusOrderService.createSubOrder(any(), any())).thenReturn(
        Arrays.asList(basicOrderDto));
    StockCardSummaryV2Dto summaryV2Dto = new StockCardSummaryV2Dto();
    summaryV2Dto.setOrderable(
        new org.openlmis.stockmanagement.dto.ObjectReferenceDto("", "",
            orderableDto.getId()));
    CanFulfillForMeEntryDto fulfillForMeEntryDto = new CanFulfillForMeEntryDto();
    UUID lotId = UUID.randomUUID();
    fulfillForMeEntryDto.setStockOnHand(50);
    fulfillForMeEntryDto.setLot(
        new org.openlmis.stockmanagement.dto.ObjectReferenceDto("", "", lotId));
    Set<CanFulfillForMeEntryDto> fulfillForMeEntryDtos = new HashSet();
    fulfillForMeEntryDtos.add(fulfillForMeEntryDto);
    summaryV2Dto.setCanFulfillForMe(fulfillForMeEntryDtos);
    when(stockCardSummariesService.findSiglusStockCard(any(), any()))
        .thenReturn(Pagination.getPage(Arrays.asList(summaryV2Dto)));
    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
    org.openlmis.fulfillment.service.referencedata.OrderableDto orderableDto1 =
        new org.openlmis.fulfillment.service.referencedata.OrderableDto();
    orderableDto1.setId(orderableDto.getId());
    shipmentLineItemDto.setOrderable(orderableDto1);
    shipmentLineItemDto.setLotId(lotId);
    ShipmentDraftDto shipmentDraftDto = new ShipmentDraftDto();
    shipmentDraftDto.setLineItems(Arrays.asList(shipmentLineItemDto));
    when(shipmentDraftService.createShipmentDraft(any())).thenReturn(shipmentDraftDto);
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setId(UUID.randomUUID());
    PodExtension podExtension = new PodExtension();
    when(podExtensionRepository.save(any(PodExtension.class))).thenReturn(podExtension);
    when(siglusShipmentService.createSubOrderAndShipment(shipmentCaptor.capture()))
        .thenReturn(shipmentDto);
    LotDto lotDto = getLotDto(lotId);
    when(lotReferenceDataService.getLots(any())).thenReturn(Arrays.asList(lotDto));

    // when
    boolean isSuccess = service.createIssueVouchers(Arrays.asList(issueVoucherDto));

    // then
    ShipmentDto shipmentDto1 = shipmentCaptor.getValue();
    assertEquals(Long.valueOf(2), shipmentDto1.getLineItems().get(0).getQuantityShipped());
    assertEquals(true, isSuccess);
  }

  @Test
  public void shouldReturnSuccessIfDataAndCallApiNormalWhenUpdateSubOrder() {
    // given
    RequisitionExtension requisitionExtension = getRequisitionExtension();
    fcDataValidate.validateEmptyFacilityCode(issueVoucherDto.getWarehouseCode());
    requisitionExtension.setRequisitionId(UUID.randomUUID());
    when(requisitionExtensionRepository
        .findByRequisitionNumber(issueVoucherDto.getRequisitionNumber()))
        .thenReturn(requisitionExtension);
    RequisitionV2Dto requisitionV2Dto = getRequisitionV2Dto(requisitionExtension);
    when(siglusRequisitionRequisitionService
        .searchRequisition(requisitionExtension.getRequisitionId()))
        .thenReturn(requisitionV2Dto);
    OrderableDto orderableDto = getOrderableDto();
    ApprovedProductDto approvedProductDto = getApprovedProductDto(orderableDto);
    when(approvedProductService.getApprovedProducts(userDto.getHomeFacilityId(),
        requisitionV2Dto.getProgramId(), null, false))
        .thenReturn(Arrays.asList(approvedProductDto));
    OrderExternal orderExternal = new OrderExternal();
    orderExternal.setId(UUID.randomUUID());
    when(orderExternalRepository.findByRequisitionId(requisitionV2Dto.getId())).thenReturn(
        Arrays.asList(orderExternal));
    Order canFulfillOrder = new Order();
    canFulfillOrder.setId(UUID.randomUUID());
    canFulfillOrder.setOrderLineItems(new ArrayList<>());
    when(orderRepository.findCanFulfillOrderAndInExternalId(Arrays.asList(
        orderExternal.getId()))).thenReturn(canFulfillOrder);
    Order order = new Order();
    order.setId(UUID.randomUUID());
    when(orderRepository.findByExternalId(requisitionV2Dto.getId()))
        .thenReturn(null, order);
    org.openlmis.fulfillment.web.util.OrderDto orderDto = new
        org.openlmis.fulfillment.web.util.OrderDto();
    orderDto.setId(order.getId());
    SiglusOrderDto dto = new SiglusOrderDto();
    dto.setOrder(orderDto);
    when(siglusOrderService.searchOrderById(canFulfillOrder.getId())).thenReturn(dto);
    StockCardSummaryV2Dto summaryV2Dto = new StockCardSummaryV2Dto();
    summaryV2Dto.setOrderable(
        new org.openlmis.stockmanagement.dto.ObjectReferenceDto("", "",
            orderableDto.getId()));
    CanFulfillForMeEntryDto fulfillForMeEntryDto = new CanFulfillForMeEntryDto();
    UUID lotId = UUID.randomUUID();
    fulfillForMeEntryDto.setStockOnHand(50);
    fulfillForMeEntryDto.setLot(
        new org.openlmis.stockmanagement.dto.ObjectReferenceDto("", "", lotId));
    Set<CanFulfillForMeEntryDto> fulfillForMeEntryDtos = new HashSet();
    fulfillForMeEntryDtos.add(fulfillForMeEntryDto);
    summaryV2Dto.setCanFulfillForMe(fulfillForMeEntryDtos);
    when(stockCardSummariesService.findSiglusStockCard(any(), any()))
        .thenReturn(Pagination.getPage(Arrays.asList(summaryV2Dto)));
    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
    org.openlmis.fulfillment.service.referencedata.OrderableDto orderableDto1 =
        new org.openlmis.fulfillment.service.referencedata.OrderableDto();
    orderableDto1.setId(orderableDto.getId());
    shipmentLineItemDto.setOrderable(orderableDto1);
    shipmentLineItemDto.setLotId(lotId);
    ShipmentDraftDto shipmentDraftDto = new ShipmentDraftDto();
    shipmentDraftDto.setLineItems(Arrays.asList(shipmentLineItemDto));
    when(shipmentDraftService.createShipmentDraft(any())).thenReturn(shipmentDraftDto);
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setId(UUID.randomUUID());
    PodExtension podExtension = new PodExtension();
    when(podExtensionRepository.save(any(PodExtension.class))).thenReturn(podExtension);
    when(siglusShipmentService.createSubOrderAndShipment(shipmentCaptor.capture()))
        .thenReturn(shipmentDto);
    LotDto lotDto = getLotDto(lotId);
    when(lotReferenceDataService.getLots(any())).thenReturn(Arrays.asList(lotDto));

    // when
    boolean isSuccess = service.createIssueVouchers(Arrays.asList(issueVoucherDto));

    // then
    ShipmentDto shipmentDto1 = shipmentCaptor.getValue();
    assertEquals(Long.valueOf(2), shipmentDto1.getLineItems().get(0).getQuantityShipped());
    assertEquals(true, isSuccess);
  }

  @Test
  public void shouldReturnSuccessIfDataAndCallApiNormalWhenConvertOrder() {
    // given
    RequisitionExtension requisitionExtension = getRequisitionExtension();
    fcDataValidate.validateEmptyFacilityCode(issueVoucherDto.getWarehouseCode());
    requisitionExtension.setRequisitionId(UUID.randomUUID());
    when(requisitionExtensionRepository
        .findByRequisitionNumber(issueVoucherDto.getRequisitionNumber()))
        .thenReturn(requisitionExtension);
    RequisitionV2Dto requisitionV2Dto = getRequisitionV2Dto(requisitionExtension);
    when(siglusRequisitionRequisitionService
        .searchRequisition(requisitionExtension.getRequisitionId()))
        .thenReturn(requisitionV2Dto);
    OrderableDto orderableDto = getOrderableDto();
    ApprovedProductDto approvedProductDto = getApprovedProductDto(orderableDto);
    when(approvedProductService.getApprovedProducts(userDto.getHomeFacilityId(),
        requisitionV2Dto.getProgramId(), null, false))
        .thenReturn(Arrays.asList(approvedProductDto));
    when(orderExternalRepository.findByRequisitionId(requisitionV2Dto.getId())).thenReturn(
        Collections.emptyList());
    Order order = new Order();
    order.setId(UUID.randomUUID());
    when(orderRepository.findByExternalId(requisitionV2Dto.getId()))
        .thenReturn(null, order);
    org.openlmis.fulfillment.web.util.OrderDto orderDto = new
        org.openlmis.fulfillment.web.util.OrderDto();
    orderDto.setId(order.getId());
    SiglusOrderDto dto = new SiglusOrderDto();
    dto.setOrder(orderDto);
    when(siglusOrderService.searchOrderById(order.getId())).thenReturn(dto);
    StockCardSummaryV2Dto summaryV2Dto = new StockCardSummaryV2Dto();
    summaryV2Dto.setOrderable(
        new org.openlmis.stockmanagement.dto.ObjectReferenceDto("", "",
            orderableDto.getId()));
    CanFulfillForMeEntryDto fulfillForMeEntryDto = new CanFulfillForMeEntryDto();
    UUID lotId = UUID.randomUUID();
    fulfillForMeEntryDto.setStockOnHand(50);
    fulfillForMeEntryDto.setLot(
        new org.openlmis.stockmanagement.dto.ObjectReferenceDto("", "", lotId));
    Set<CanFulfillForMeEntryDto> fulfillForMeEntryDtos = new HashSet();
    fulfillForMeEntryDtos.add(fulfillForMeEntryDto);
    summaryV2Dto.setCanFulfillForMe(fulfillForMeEntryDtos);
    when(stockCardSummariesService.findSiglusStockCard(any(), any()))
        .thenReturn(Pagination.getPage(Arrays.asList(summaryV2Dto)));
    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDto();
    org.openlmis.fulfillment.service.referencedata.OrderableDto orderableDto1 =
        new org.openlmis.fulfillment.service.referencedata.OrderableDto();
    orderableDto1.setId(orderableDto.getId());
    shipmentLineItemDto.setOrderable(orderableDto1);
    shipmentLineItemDto.setLotId(lotId);
    ShipmentDraftDto shipmentDraftDto = new ShipmentDraftDto();
    shipmentDraftDto.setLineItems(Arrays.asList(shipmentLineItemDto));
    when(shipmentDraftService.createShipmentDraft(any())).thenReturn(shipmentDraftDto);
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setId(UUID.randomUUID());
    PodExtension podExtension = new PodExtension();
    when(podExtensionRepository.save(any(PodExtension.class))).thenReturn(podExtension);
    when(siglusShipmentService.createSubOrderAndShipment(shipmentCaptor.capture()))
        .thenReturn(shipmentDto);
    when(lotReferenceDataService.getLots(any())).thenReturn(Arrays.asList(getLotDto(lotId)));
    Requisition requisition = new Requisition();
    when(requisitionRepository.findOne(requisitionV2Dto.getId())).thenReturn(requisition);
    org.openlmis.requisition.dto.OrderDto orderRequisitionDto =
        new org.openlmis.requisition.dto.OrderDto();
    when(orderDtoBuilder.build(any(), any())).thenReturn(orderRequisitionDto);

    // when
    boolean isSuccess = service.createIssueVouchers(Arrays.asList(issueVoucherDto));

    // then
    assertEquals(true, isSuccess);
    verify(simulateUser).simulateUserAuth(any());
    verify(orderFulfillmentService).create(orderListCaptor.capture());
    verify(requisitionStatusProcessor).statusChange(requisitionCaptor.capture(), any());
    OrderDto saveOrderDto = orderListCaptor.getValue().get(0);
    assertEquals(Long.valueOf(4), saveOrderDto.getOrderLineItems().get(0).getOrderedQuantity());
    ShipmentDto shipmentDto1 = shipmentCaptor.getValue();
    assertEquals(Long.valueOf(2), shipmentDto1.getLineItems().get(0).getQuantityShipped());
  }

  @Test
  public void shouldReturnSuccessIfDataError() {
    // given
    IssueVoucherDto issueVoucherDto = getIssueVoucherDto();
    issueVoucherDto.setRequisitionNumber("");

    // when
    boolean isSuccess = service.createIssueVouchers(Arrays.asList(issueVoucherDto));

    // then
    assertEquals(true, isSuccess);

  }

  private ApprovedProductDto getApprovedProductDto(OrderableDto orderableDto) {
    ApprovedProductDto approvedProductDto = new ApprovedProductDto();
    approvedProductDto.setOrderable(orderableDto);
    approvedProductDto.setId(UUID.randomUUID());
    return approvedProductDto;
  }

  private RequisitionV2Dto getRequisitionV2Dto(RequisitionExtension requisitionExtension) {
    RequisitionV2Dto requisitionV2Dto = new RequisitionV2Dto();
    ObjectReferenceDto program = new ObjectReferenceDto();
    program.setId(programId);
    requisitionV2Dto.setProgram(program);
    requisitionV2Dto.setStatus(RequisitionStatus.APPROVED);
    requisitionV2Dto.setId(requisitionExtension.getRequisitionId());
    return requisitionV2Dto;
  }

  private OrderableDto getOrderableDto() {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(UUID.randomUUID());
    orderableDto.setProductCode("02E01");
    orderableDto.setNetContent((long) 25);
    return orderableDto;
  }

  private LotDto getLotDto(UUID lotId) {
    LotDto lotDto = new LotDto();
    lotDto.setId(lotId);
    lotDto.setLotCode("M18361");
    return lotDto;
  }

  private RequisitionExtension getRequisitionExtension() {
    RequisitionExtension requisitionExtension = new RequisitionExtension();
    requisitionExtension.setRequisitionNumber(Integer.valueOf("192"));
    return requisitionExtension;
  }

  private IssueVoucherDto getIssueVoucherDto() {
    IssueVoucherDto issueVoucherDto = new IssueVoucherDto();
    issueVoucherDto.setWarehouseCode("04030101");
    issueVoucherDto.setClientCode("04030101");
    issueVoucherDto.setRequisitionNumber("RNR-NO010906120000192");
    issueVoucherDto.setShippingDate(ZonedDateTime.now());
    ProductDto productDto = new ProductDto();
    productDto.setApprovedQuantity(100);
    productDto.setShippedQuantity(50);
    productDto.setBatch("M18361");
    productDto.setFnmCode("02E01");
    productDto.setExpiryDate(new Date());
    issueVoucherDto.setProducts(Arrays.asList(productDto));
    return issueVoucherDto;
  }
}
