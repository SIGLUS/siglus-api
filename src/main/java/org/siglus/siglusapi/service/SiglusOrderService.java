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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.openlmis.requisition.web.ResourceNames.ORDERABLES;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_CARDS_VIEW;
import static org.siglus.common.constant.KitConstants.APE_KITS;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.NON_EMPTY_ONLY;
import static org.siglus.siglusapi.constant.FieldConstants.PROGRAM_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.PaginationConstants.DEFAULT_PAGE_NUMBER;
import static org.siglus.siglusapi.constant.PaginationConstants.NO_PAGINATION;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_PERIOD_MATCH;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ORDER_NOT_EXIST;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERIOD_NOT_FOUND;
import static org.siglus.siglusapi.util.SiglusDateHelper.DATE_MONTH_YEAR;
import static org.siglus.siglusapi.util.SiglusDateHelper.getFormatDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.service.OrderService;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.BasicOrderDtoBuilder;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderDtoBuilder;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.ApprovedProductReferenceDataService;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.domain.LocalIssueVoucher;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.dto.FulfillOrderDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.OrderStatusDto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusLocalIssueVoucherRepository;
import org.siglus.siglusapi.repository.SiglusOrdersRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SiglusShipmentRepository;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.repository.dto.OrderSuggestedQuantityDto;
import org.siglus.siglusapi.repository.dto.RequisitionOrderDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.scheduledtask.SiglusOrderCloseSchedulerService;
import org.siglus.siglusapi.util.PeriodUtil;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.response.BasicOrderExtensionResponse;
import org.siglus.siglusapi.web.response.OrderPickPackResponse;
import org.siglus.siglusapi.web.response.OrderSuggestedQuantityResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusOrderService {

  @Autowired
  private RequisitionController requisitionController;

  @Autowired
  private RequisitionService requisitionService;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private SiglusAuthenticationHelper siglusAuthenticationHelper;

  @Autowired
  private SiglusArchiveProductService siglusArchiveProductService;

  @Autowired
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Autowired
  private OrderLineItemExtensionRepository lineItemExtensionRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Value("${service.url}")
  private String serviceUrl;

  @Autowired
  private OrderExternalRepository orderExternalRepository;

  @Autowired
  private OrderController orderController;

  @Autowired
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Autowired
  private SiglusProcessingPeriodReferenceDataService periodService;

  @Autowired
  private SiglusShipmentDraftService draftService;

  @Autowired
  private SiglusProgramService programService;

  @Autowired
  private ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  @Autowired
  private OrderService orderService;

  @Autowired
  private BasicOrderDtoBuilder basicOrderDtoBuilder;

  @Autowired
  private SiglusRequisitionRequisitionService siglusRequisitionService;

  @Autowired
  private SiglusFilterAddProductForEmergencyService filterProductService;

  @Autowired
  private OrderDtoBuilder orderDtoBuilder;

  @Value("${time.zoneId}")
  private String timeZoneId;

  @Value("${fc.facilityTypeId}")
  private UUID fcFacilityTypeId;

  @Autowired
  private PodSubDraftRepository podSubDraftRepository;

  @Autowired
  private OrderableRepository orderableRepository;

  @Autowired
  private SiglusProcessingPeriodService siglusProcessingPeriodService;

  @Autowired
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private ApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Autowired
  private SiglusFacilityRepository siglusFacilityRepository;

  @Autowired
  private SiglusLocalIssueVoucherRepository localReceiptVoucherRepository;

  @Autowired
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Autowired
  private StockManagementRepository stockManagementRepository;

  @Autowired
  private SiglusOrdersRepository siglusOrdersRepository;

  @Autowired
  private SiglusOrderCloseSchedulerService siglusOrderCloseSchedulerService;

  @Autowired
  private SiglusShipmentRepository siglusShipmentRepository;

  private static final String SLASH = "/";

  private static final List<String> REQUISITION_STATUS_AFTER_FINAL_APPROVED = Lists.newArrayList(
      RequisitionStatus.APPROVED.name(),
      RequisitionStatus.RELEASED.name(),
      RequisitionStatus.RELEASED_WITHOUT_ORDER.name());

  private static final List<String> ORDER_STATUS_NOT_FINISHED = Lists.newArrayList(
      OrderStatus.ORDERED.name(),
      OrderStatus.FULFILLING.name());

  private static final List<OrderStatus> ORDER_STATUS_AFTER_START_FULFILL = Lists.newArrayList(
      OrderStatus.FULFILLING,
      OrderStatus.SHIPPED,
      OrderStatus.RECEIVED
  );

  public Page<BasicOrderDto> searchOrders(OrderSearchParams params, Pageable pageable) {
    return orderController.searchOrders(params, pageable);
  }

  public Page<BasicOrderExtensionResponse> searchOrdersWithSubDraftStatus(OrderSearchParams params, Pageable pageable) {
    Page<BasicOrderDto> basicOrderDtoPage = orderController.searchOrders(params, pageable);
    List<LocalIssueVoucher> localIssueVouchers = localReceiptVoucherRepository
        .findByProgramIdAndRequestingFacilityId(params.getProgramId(), params.getRequestingFacilityId());
    if (!basicOrderDtoPage.hasContent() && localIssueVouchers.isEmpty()) {
      return new PageImpl<>(Lists.newArrayList(), pageable, basicOrderDtoPage.getTotalElements());
    }

    List<BasicOrderExtensionResponse> localBasicOrderExtensionResponses = getLocalBasicOrderExtensionResponses(
        localIssueVouchers);

    List<BasicOrderDto> basicOrderDtos = basicOrderDtoPage.getContent();
    List<BasicOrderExtensionResponse> electronicBasicOrderExtensionResponses =
        getElectronicBasicOrderExtensionResponses(basicOrderDtos);

    List<BasicOrderExtensionResponse> basicOrderExtensionResponses = Stream
        .concat(localBasicOrderExtensionResponses.stream(), electronicBasicOrderExtensionResponses.stream())
        .collect(toList());

    Set<UUID> orderIds = basicOrderExtensionResponses.stream().map(BasicOrderExtensionResponse::getId)
        .collect(Collectors.toSet());
    Set<UUID> orderIdsWithSubDraft = podSubDraftRepository.findOrderIdsWithSubDraft(orderIds).stream()
        .map(UUID::fromString).collect(Collectors.toSet());

    boolean isConsistent = false;
    if (params.getRequestingFacilityId() != null) {
      isConsistent = params.getRequestingFacilityId()
          .equals(basicOrderExtensionResponses.get(0).getRequestingFacility().getId());
    }
    boolean canDeleteLocalIssueVoucher = siglusAuthenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts();

    for (BasicOrderExtensionResponse response : basicOrderExtensionResponses) {
      response.setHasSubDraft(orderIdsWithSubDraft.contains(response.getId()));
      response.setCanCreateLocalIssueVoucher(isConsistent);
      response.setCanDeleteLocalIssueVoucher(canDeleteLocalIssueVoucher);
    }
    Sort orders = new Sort("local", "status", "createdDate");
    PageRequest pageRequest = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), orders);
    return new PageImpl<>(basicOrderExtensionResponses, pageRequest, basicOrderDtoPage.getTotalElements());
  }

  private List<BasicOrderExtensionResponse> getElectronicBasicOrderExtensionResponses(
      List<BasicOrderDto> basicOrderDtos) {
    return basicOrderDtos.stream()
        .map(basicOrderDto -> {
          BasicOrderExtensionResponse basicOrderExtensionResponse = new BasicOrderExtensionResponse();
          BeanUtils.copyProperties(basicOrderDto, basicOrderExtensionResponse);
          basicOrderExtensionResponse.setLocal(false);
          return basicOrderExtensionResponse;
        }).collect(toList());
  }

  private List<BasicOrderExtensionResponse> getLocalBasicOrderExtensionResponses(
      List<LocalIssueVoucher> localIssueVouchers) {
    return localIssueVouchers.stream()
        .map(localReceiptVoucher -> {
          BasicOrderExtensionResponse response = new BasicOrderExtensionResponse();
          response.setLocal(true);
          response.setOrderCode(localReceiptVoucher.getOrderCode());
          response.setStatus(localReceiptVoucher.getStatus());
          response.setId(localReceiptVoucher.getId());
          response.setCreatedBy(authenticationHelper.getCurrentUser());
          FacilityDto requestingFacilityDto = new FacilityDto();
          org.siglus.siglusapi.dto.FacilityDto siglusRequestingFacilityDto = facilityReferenceDataService
              .findOne(localReceiptVoucher.getRequestingFacilityId());
          BeanUtils.copyProperties(siglusRequestingFacilityDto, requestingFacilityDto);
          response.setRequestingFacility(requestingFacilityDto);
          FacilityDto supplyingFacilityDto = new FacilityDto();
          org.siglus.siglusapi.dto.FacilityDto siglusSupplyingFacilityDto = facilityReferenceDataService
              .findOne(localReceiptVoucher.getSupplyingFacilityId());
          BeanUtils.copyProperties(siglusSupplyingFacilityDto, supplyingFacilityDto);
          response.setSupplyingFacility(supplyingFacilityDto);
          ProgramDto requisitionProgramDto = programService.getProgram(localReceiptVoucher.getProgramId());
          org.openlmis.fulfillment.service.referencedata.ProgramDto fulfillmentProgramDto =
              new org.openlmis.fulfillment.service.referencedata.ProgramDto();
          BeanUtils.copyProperties(requisitionProgramDto, fulfillmentProgramDto);
          response.setProgram(fulfillmentProgramDto);
          return response;
        }).collect(toList());
  }

  public Page<FulfillOrderDto> searchOrdersForFulfill(OrderSearchParams params, Pageable pageable) {
    Page<Order> orders = orderService.searchOrdersForFulfillPage(params, pageable);
    List<BasicOrderDto> dtos = basicOrderDtoBuilder.build(orders.getContent());
    List<FulfillOrderDto> fulfillOrderDtos = dtos.stream()
        .map(basicOrderDto -> FulfillOrderDto.builder().basicOrder(basicOrderDto).build())
        .collect(toList());
    List<FulfillOrderDto> processedFulfillOrderDtos =
        fulfillOrderDtos.isEmpty() ? fulfillOrderDtos : processExpiredFulfillOrder(fulfillOrderDtos);
    return new PageImpl<>(processedFulfillOrderDtos, pageable, orders.getTotalElements());
  }

  public List<FulfillOrderDto> processExpiredFulfillOrder(List<FulfillOrderDto> fulfillOrderDtos) {
    Map<UUID, List<FulfillOrderDto>> programIdToMap = fulfillOrderDtos.stream()
        .collect(Collectors.groupingBy(fulfillOrderDto -> fulfillOrderDto.getBasicOrder().getProgram().getId()));
    UUID processingPeriodId = fulfillOrderDtos.get(0).getBasicOrder().getProcessingPeriod().getId();
    ProcessingPeriodExtension processingPeriodExtension = processingPeriodExtensionRepository
        .findByProcessingPeriodId(processingPeriodId);
    if (processingPeriodExtension == null) {
      throw new NotFoundException(ERROR_PERIOD_NOT_FOUND);
    }
    processExpiredFulfillOrders(programIdToMap);
    return fulfillOrderDtos;
  }

  private void processExpiredFulfillOrders(Map<UUID, List<FulfillOrderDto>> programIdToMap) {
    programIdToMap.keySet().forEach(key -> {
      List<FulfillOrderDto> fulfillOrderDtos = programIdToMap.get(key);
      FulfillOrderDto latestFulfillOrderDto = fulfillOrderDtos.get(0);
      UUID supplyingFacilityId = latestFulfillOrderDto.getBasicOrder().getSupplyingFacility().getId();
      UUID programId = latestFulfillOrderDto.getBasicOrder().getProgram().getId();

      List<Order> orders = siglusOrdersRepository
          .findBySupplyingFacilityIdAndProgramIdAndStatusIn(supplyingFacilityId, programId, Lists
              .newArrayList(OrderStatus.SHIPPED, OrderStatus.FULFILLING, OrderStatus.PARTIALLY_FULFILLED,
                  OrderStatus.RECEIVED));

      Set<UUID> periodIds = fulfillOrderDtos.stream()
              .map(orderDto -> orderDto.getBasicOrder().getProcessingPeriod().getId())
              .collect(toSet());
      Map<UUID, ProcessingPeriodExtension> processingPeriodIdToExtension = processingPeriodExtensionRepository
              .findByProcessingPeriodIdIn(periodIds).stream()
              .collect(Collectors.toMap(ProcessingPeriodExtension::getProcessingPeriodId, Function.identity()));

      List<FulfillOrderDto> filteredFulfillOrderDtos = fulfillOrderDtos.stream()
          .filter(fulfillOrderDto -> {
            ProcessingPeriodDto period = fulfillOrderDto.getBasicOrder().getProcessingPeriod();
            UUID processingPeriodId = period.getId();
            ProcessingPeriodExtension processingPeriodExtension = processingPeriodIdToExtension.get(processingPeriodId);

            List<YearMonth> calculatedFulfillOrderMonth = requisitionService
                    .calculateFulfillOrderYearMonth(processingPeriodExtension);
            YearMonth orderYearMonth = YearMonth.of(period.getEndDate().getYear(), period.getEndDate().getMonth());
            return calculatedFulfillOrderMonth.contains(orderYearMonth);
          })
          .collect(toList());

      if (orders.isEmpty()) {
        filteredFulfillOrderDtos.forEach(dto -> dto.setExpired(false));
      } else {
        setWarningPopupFlag(orders, filteredFulfillOrderDtos);
      }
    });
  }

  private void setWarningPopupFlag(List<Order> orders, List<FulfillOrderDto> filteredFulfillOrderDtos) {
    Set<UUID> processingPeriodIds = orders.stream().map(Order::getProcessingPeriodId).collect(Collectors.toSet());
    List<org.openlmis.requisition.dto.ProcessingPeriodDto> processingPeriodDtos = periodService
        .findByIds(processingPeriodIds);
    processingPeriodDtos.sort((o1, o2) -> o2.getEndDate().compareTo(o1.getEndDate()));
    LocalDate latestOrderPeriodEndDate = processingPeriodDtos.get(0).getEndDate();
    filteredFulfillOrderDtos.forEach(dto -> {
      YearMonth yearMonth = YearMonth.of(dto.getBasicOrder().getProcessingPeriod().getEndDate().getYear(),
              dto.getBasicOrder().getProcessingPeriod().getEndDate().getMonth());
      YearMonth latestOrderYearMonth = YearMonth.of(latestOrderPeriodEndDate.getYear(),
              latestOrderPeriodEndDate.getMonth());
      if (yearMonth.isAfter(latestOrderYearMonth) || yearMonth.equals(latestOrderYearMonth)) {
        dto.setExpired(false);
        LocalDate requisitionPeriodEndDate = dto.getBasicOrder().getProcessingPeriod().getEndDate();
        if (requisitionPeriodEndDate.isAfter(latestOrderPeriodEndDate)) {
          dto.setShowWarningPopup(true);
        }
      }
    });
  }

  public List<Month> calculateFulfillOrderMonth(ProcessingPeriodExtension processingPeriodExtension) {
    LocalDate submitStartDate = processingPeriodExtension.getSubmitStartDate();
    LocalDate submitEndDate = processingPeriodExtension.getSubmitEndDate();
    LocalDate currentDate = LocalDate.now();
    if (currentDate.getDayOfMonth() >= submitStartDate.getDayOfMonth() && currentDate.getDayOfMonth() <= submitEndDate
        .getDayOfMonth()) {
      return Lists.newArrayList(currentDate.getMonth().minus(1), currentDate.getMonth());
    } else if (currentDate.getDayOfMonth() < submitStartDate.getDayOfMonth()) {
      return Lists.newArrayList(currentDate.getMonth().minus(1));
    } else {
      return Lists.newArrayList(currentDate.getMonth());
    }
  }

  public void closeExpiredOrder(UUID fulfillOrderId) {
    Order order = siglusOrdersRepository.findOne(fulfillOrderId);
    if (order == null) {
      throw new NotFoundException(ERROR_ORDER_NOT_EXIST);
    }
    log.info("manually close the fulfill order with order id: {}", fulfillOrderId);
    order.setStatus(OrderStatus.CLOSED);
    siglusOrdersRepository.save(order);
  }

  public void batchCloseExpiredOrders() {
    OrderSearchParams params = new OrderSearchParams();
    params.setStatus(Sets.newHashSet(OrderStatus.FULFILLING.name(),
        OrderStatus.ORDERED.name(), OrderStatus.PARTIALLY_FULFILLED.name()));
    Pageable pageable = new PageRequest(DEFAULT_PAGE_NUMBER, NO_PAGINATION);
    Page<Order> orderPage = orderService.searchOrdersForFulfillPage(params, pageable);
    siglusOrderCloseSchedulerService.batchProcessExpiredOrders(orderPage.getContent());
  }

  public OrderStatusDto searchOrderStatusById(UUID orderId) {
    OrderDto orderDto = orderController.getOrder(orderId, null);
    OrderStatusDto orderStatusDto = new OrderStatusDto();
    orderStatusDto.setClosed(needCloseOrder(orderDto));
    orderStatusDto.setSuborder(isSuborder(orderDto.getExternalId()));
    if (orderStatusDto.isClosed()
        && orderStatusDto.isSuborder()
        && orderDto.getStatus() != OrderStatus.CLOSED) {
      Order order = orderRepository.findOne(orderId);
      revertOrderToCloseStatus(order);
    }
    return orderStatusDto;
  }

  public boolean isSuborder(UUID externalId) {
    OrderExternal external = orderExternalRepository.findOne(externalId);
    return external != null;
  }

  public boolean needCloseOrder(OrderDto orderDto) {
    if (!orderDto.getFacility().getType().getId().equals(fcFacilityTypeId)) {
      return currentDateIsAfterNextPeriodEndDate(orderDto);
    }
    return false;
  }

  public void revertOrderToCloseStatus(Order order) {
    draftService.deleteOrderLineItemAndInitialedExtension(order);
    log.info("close order: close order id: {}", order.getId());
    order.setStatus(OrderStatus.CLOSED);
    log.info("save closed order: {}", order);
    orderRepository.save(order);
  }

  public SiglusOrderDto searchOrderById(UUID orderId) {
    OrderDto orderDto = orderController.getOrder(orderId, null);
    return extendOrderDto(orderDto, false);
  }

  public SiglusOrderDto searchOrderByIdWithoutProducts(UUID orderId) {
    OrderDto orderDto = orderController.getOrder(orderId, null);
    return extendOrderDtoWithoutProducts(orderDto);
  }

  public SiglusOrderDto searchOrderByIdForMultiWareHouseSupply(UUID orderId) {
    Order order = orderRepository.findOne(orderId);
    OrderDto orderDto = orderDtoBuilder.build(order);
    return extendOrderDto(orderDto, true);
  }

  // manually fill no-id lineItem
  public Set<UUID> updateOrderLineItems(ShipmentDraftDto draftDto) {
    Order order = orderRepository.findOne(draftDto.getOrder().getId());
    List<OrderLineItem> orderLineItems = order.getOrderLineItems();
    List<OrderLineItemDto> draftOrderLineItems = draftDto.getOrder().getOrderLineItems();
    Collection<UUID> draftOrderableIds = draftOrderLineItems.stream()
        .map(draftOrderLineItem -> draftOrderLineItem.getOrderable().getId())
        .collect(Collectors.toList());
    // remove order line item if it was removed in draft
    orderLineItems.removeIf(orderLineItem -> !draftOrderableIds.contains(orderLineItem.getOrderable().getId()));
    draftOrderLineItems.stream()
        .filter(orderLineItemDto -> orderLineItemDto.getId() == null)
        .map(OrderLineItem::newInstance)
        .forEach(orderLineItem -> {
          orderLineItem.setOrder(order);
          orderLineItems.add(orderLineItem);
        });
    log.info("save order line items with added products, orderId: {}", order.getId());
    Order saved = orderRepository.save(order);

    Set<UUID> addedLineItemIds = new HashSet<>();
    Map<UUID, UUID> orderableIdToLineItemId = saved.getOrderLineItems().stream()
        .collect(toMap(orderLineItem -> orderLineItem.getOrderable().getId(), OrderLineItem::getId));
    draftOrderLineItems.forEach(orderLineItemDto -> {
      if (orderLineItemDto.getId() == null) {
        UUID orderableId = orderLineItemDto.getOrderable().getId();
        UUID lineItemId = orderableIdToLineItemId.get(orderableId);
        orderLineItemDto.setId(lineItemId);
        addedLineItemIds.add(lineItemId);
      }
    });

    log.info("orderId: {}, addedOrderLineItemIds: {}", order.getId(), addedLineItemIds);
    return addedLineItemIds;
  }

  public Iterable<BasicOrderDto> createSubOrder(OrderObjectReferenceDto order,
      List<org.openlmis.fulfillment.web.util.OrderLineItemDto> orderLineItemDtos) {
    OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
    List<OrderExternal> externals;
    if (external == null) {
      OrderExternal firstExternal = OrderExternal.builder().requisitionId(order.getExternalId()).build();
      OrderExternal secondExternal = OrderExternal.builder().requisitionId(order.getExternalId()).build();
      log.info("save order external : {}", Arrays.asList(firstExternal, secondExternal));
      externals = orderExternalRepository.save(Arrays.asList(firstExternal, secondExternal));
      updateExternalIdAndStatusForSubOrder(order.getId(), externals.get(0).getId(), order.getStatus());
    } else {
      externals = orderExternalRepository.findByRequisitionId(external.getRequisitionId());
      OrderExternal newExternal = OrderExternal.builder().requisitionId(external.getRequisitionId()).build();
      log.info("save new external : {}", newExternal);
      newExternal = orderExternalRepository.save(newExternal);
      externals.add(newExternal);
    }
    order.setOrderCode(increaseOrderNumber(order.getOrderCode()));
    return createNewOrder(order, orderLineItemDtos, externals);
  }

  public OrderObjectReferenceDto getExtensionOrder(OrderObjectReferenceDto orderDto) {
    setOrderLineItemExtension(orderDto.getOrderLineItems());
    return orderDto;
  }

  public Requisition getRequisitionByOrder(OrderDto orderDto) {
    OrderExternal external = orderExternalRepository.findOne(orderDto.getExternalId());
    UUID requisitionId = external == null ? orderDto.getExternalId() : external.getRequisitionId();
    return requisitionController.findRequisition(requisitionId, requisitionController.getProfiler("GET_ORDER"));
  }

  public OrderSuggestedQuantityResponse getOrderSuggestedQuantityResponse(UUID orderId) {
    Order order = getOrder(orderId);
    List<ProcessingPeriod> periods = getUpToNowMonthlyPeriods();

    if (!isCurrentProcessingPeriod(order, periods)) {
      return OrderSuggestedQuantityResponse.builder().showSuggestedQuantity(Boolean.FALSE).build();
    }

    if (shouldNotCalculateSuggestedQuantity(order)) {
      return OrderSuggestedQuantityResponse.builder().showSuggestedQuantity(Boolean.TRUE).build();
    }

    return OrderSuggestedQuantityResponse.builder()
        .showSuggestedQuantity(Boolean.TRUE)
        .orderableIdToSuggestedQuantity(getOrderableIdToSuggestedQuantity(order, periods))
        .build();
  }

  public OrderPickPackResponse getOrderPickPackResponse(UUID orderId) {
    Order order = getOrder(orderId);

    List<Facility> facilities = siglusFacilityRepository.findAll(
        Lists.newArrayList(order.getReceivingFacilityId(), order.getSupplyingFacilityId()));
    Map<UUID, String> facilityIdToName = facilities.stream()
        .collect(Collectors.toMap(Facility::getId, Facility::getName));

    return OrderPickPackResponse.builder()
        .generatedDate(getFormatDate(LocalDate.now(), DATE_MONTH_YEAR))
        .orderCode(order.getOrderCode())
        .clientFacility(facilityIdToName.get(order.getReceivingFacilityId()))
        .supplierFacility(facilityIdToName.get(order.getSupplyingFacilityId()))
        .build();
  }

  public String increaseOrderNumber(String orderNumber) {
    String[] split = orderNumber.split(SLASH);
    return String.format("%s%02d", split[0] + SLASH, (Integer.parseInt(split[1]) + 1));
  }

  private Map<UUID, BigDecimal> getOrderableIdToSuggestedQuantity(Order order, List<ProcessingPeriod> periods) {
    return ORDER_STATUS_AFTER_START_FULFILL.contains(order.getStatus())
        ? getOrderableIdToSuggestedQuantityFromDb(order)
        : calculateAndSaveOrderableIdToSuggestedQuantity(order, periods);
  }

  private Map<UUID, BigDecimal> getOrderableIdToSuggestedQuantityFromDb(Order order) {
    Set<UUID> lineItemIds = getLineItemIds(order);
    List<OrderSuggestedQuantityDto> dtos = lineItemExtensionRepository
        .findOrderSuggestedQuantityDtoByOrderLineItemIdIn(
            lineItemIds);
    Map<UUID, BigDecimal> orderableIdToSuggestedQuantity = Maps.newHashMapWithExpectedSize(dtos.size());
    dtos.forEach(dto -> orderableIdToSuggestedQuantity.put(dto.getOrderableId(),
        Objects.isNull(dto.getSuggestedQuantity()) ? null : toBigDecimalRoundDown(dto.getSuggestedQuantity())));
    return orderableIdToSuggestedQuantity;
  }

  private Set<UUID> getLineItemIds(Order order) {
    return order.getOrderLineItems().stream().map(OrderLineItem::getId).collect(Collectors.toSet());
  }

  private boolean isCurrentProcessingPeriod(Order order, List<ProcessingPeriod> periods) {
    return order.getProcessingPeriodId().equals(getCurrentPeriodIdByFulfillDate(periods, LocalDate.now()));
  }

  private Order getOrder(UUID orderId) {
    Order order = orderRepository.findOne(orderId);
    if (Objects.isNull(order)) {
      throw new NotFoundException(ERROR_ORDER_NOT_EXIST);
    }
    return order;
  }

  private Map<UUID, BigDecimal> calculateAndSaveOrderableIdToSuggestedQuantity(Order order,
      List<ProcessingPeriod> periods) {
    List<UUID> clientFacilityIds = getAllClientFacilityIds(order.getSupplyingFacilityId(), order.getProgramId());
    List<Requisition> currentPeriodAfterApprovedRequisitions = getCurrentPeriodAfterApprovedRequisitions(order, periods,
        clientFacilityIds);
    List<Requisition> currentPeriodNotFinishedRequisitions = getCurrentPeriodNotFinishedRequisitions(
        currentPeriodAfterApprovedRequisitions);
    List<Requisition> historyPeriodApprovedRequisitions = getPreviousThreePeriodAfterApprovedRequisitions(order,
        periods, currentPeriodAfterApprovedRequisitions, clientFacilityIds);

    Set<UUID> orderableIds = getOrderOrderableIds(order);

    Map<UUID, Integer> orderableIdToCurrentPeriodSumApprovedQuantity = getOrderableIdToCurrentPeriodSumApprovedQuantity(
        currentPeriodNotFinishedRequisitions);
    Map<UUID, Integer> orderableIdToHistoryPeriodSumApprovedQuantity = getOrderableIdToHistoryPeriodSumApprovedQuantity(
        historyPeriodApprovedRequisitions, orderableIds);
    Map<UUID, Integer> orderableIdToSoh = getOrderableIdToSoh(orderableIds, order.getProgramId(),
        order.getSupplyingFacilityId());
    Map<UUID, Integer> currentRequisitionOrderableToApprovedQuantity =
        getCurrentRequisitionOrderableToFinalApprovedQuantity(order, currentPeriodNotFinishedRequisitions);

    Map<UUID, BigDecimal> orderableIdToSuggestedQuantity = calculateOrderableToSuggestedQuantityMap(
        currentRequisitionOrderableToApprovedQuantity, orderableIdToCurrentPeriodSumApprovedQuantity,
        orderableIdToHistoryPeriodSumApprovedQuantity, orderableIds, orderableIdToSoh);

    saveSuggestedQuantity(order, orderableIdToSuggestedQuantity);

    return orderableIdToSuggestedQuantity;
  }

  private List<Requisition> getCurrentPeriodNotFinishedRequisitions(
      List<Requisition> currentPeriodAfterApprovedRequisitions) {
    List<Requisition> releasedRequisitions = currentPeriodAfterApprovedRequisitions.stream()
        .filter(requisition -> RequisitionStatus.RELEASED == requisition.getStatus())
        .collect(toList());

    // released requisition maybe already fulfilled
    Set<UUID> requisitionIds = releasedRequisitions.stream().map(Requisition::getId)
        .collect(Collectors.toSet());
    Map<UUID, String> requisitionIdToOrderStatus = Maps.newHashMapWithExpectedSize(requisitionIds.size());
    if (CollectionUtils.isNotEmpty(requisitionIds)) {
      List<RequisitionOrderDto> requisitionOrderDtos = siglusRequisitionRepository
          .findRequisitionOrderDtoByRequisitionIds(requisitionIds);
      requisitionOrderDtos.forEach(dto -> requisitionIdToOrderStatus.put(dto.getRequisitionId(), dto.getOrderStatus()));
    }

    // not finished = not convert to order + convert to order but not finish fulfill
    List<Requisition> notFinishedRequisitions = currentPeriodAfterApprovedRequisitions.stream()
        .filter(requisition -> RequisitionStatus.APPROVED == requisition.getStatus())
        .collect(toList());
    notFinishedRequisitions.addAll(releasedRequisitions.stream()
        .filter(requisition -> ORDER_STATUS_NOT_FINISHED.contains(
            requisitionIdToOrderStatus.get(requisition.getId())))
        .collect(toList()));
    return notFinishedRequisitions;
  }

  private void saveSuggestedQuantity(Order order, Map<UUID, BigDecimal> orderableIdToSuggestedQuantity) {
    Set<UUID> lineItemIds = getLineItemIds(order);
    Map<UUID, UUID> lineItemIdToOrderableId = order.getOrderLineItems().stream()
        .collect(Collectors.toMap(OrderLineItem::getId, e -> e.getOrderable().getId()));
    List<OrderLineItemExtension> extensions = Lists.newArrayListWithExpectedSize(lineItemIds.size());
    Map<UUID, OrderLineItemExtension> existedLineItemExtensionIdToExtension = lineItemExtensionRepository
        .findByOrderLineItemIdIn(lineItemIds).stream()
        .collect(Collectors.toMap(OrderLineItemExtension::getOrderLineItemId, e -> e));
    lineItemIds.forEach(lineItemId -> {
      OrderLineItemExtension existedExtension = existedLineItemExtensionIdToExtension.get(lineItemId);
      OrderLineItemExtension extension = Objects.isNull(existedExtension)
          ? createInitialExtension(order.getId(), lineItemId)
          : existedExtension;
      extension.setSuggestedQuantity(orderableIdToSuggestedQuantity.get(lineItemIdToOrderableId.get(lineItemId)));
      extensions.add(extension);
    });
    log.info("save OrderLineItemExtensions, orderId:{}, size:{}", order.getId(), extensions.size());
    lineItemExtensionRepository.save(extensions);
  }

  private OrderLineItemExtension createInitialExtension(UUID orderId, UUID lineItemId) {
    return OrderLineItemExtension.builder()
        .orderId(orderId)
        .orderLineItemId(lineItemId)
        .partialFulfilledQuantity(0L)
        .build();
  }

  private Set<UUID> getOrderOrderableIds(Order order) {
    return order.getOrderLineItems().stream().map(lineItem -> lineItem.getOrderable().getId())
        .collect(Collectors.toSet());
  }

  private Map<UUID, BigDecimal> calculateOrderableToSuggestedQuantityMap(
      Map<UUID, Integer> currentRequisitionOrderableToApprovedQuantity,
      Map<UUID, Integer> orderableIdToCurrentPeriodSumApprovedQuantity,
      Map<UUID, Integer> orderableIdToHistoryPeriodSumApprovedQuantity,
      Set<UUID> orderableIds,
      Map<UUID, Integer> orderableIdToSoh) {

    Map<UUID, BigDecimal> orderableIdToSuggestedQuantity = Maps.newHashMapWithExpectedSize(orderableIds.size());
    orderableIds.forEach(orderableId -> {
      Integer sumApprovedQuantity = orderableIdToCurrentPeriodSumApprovedQuantity.getOrDefault(orderableId, 0);
      Integer sumHistoryApprovedQuantity = orderableIdToHistoryPeriodSumApprovedQuantity.getOrDefault(orderableId, 0);
      Integer soh = orderableIdToSoh.getOrDefault(orderableId, 0);

      BigDecimal suggestedQuantity = calculateSuggestedQuantity(sumApprovedQuantity, sumHistoryApprovedQuantity, soh,
          currentRequisitionOrderableToApprovedQuantity.get(orderableId));
      orderableIdToSuggestedQuantity.put(orderableId, suggestedQuantity);
    });
    return orderableIdToSuggestedQuantity;
  }

  private BigDecimal calculateSuggestedQuantity(Integer sumApprovedQuantity, Integer sumHistoryApprovedQuantity,
      Integer soh, Integer currentRequisitionApprovedQuantity) {
    if (currentRequisitionApprovedQuantity == 0) {
      return toBigDecimalRoundDown(0);
    }
    if (sumApprovedQuantity + sumHistoryApprovedQuantity <= soh) {
      return toBigDecimalRoundDown(currentRequisitionApprovedQuantity);
    } else {
      return toBigDecimalRoundDown(
          soh * 1d / (sumApprovedQuantity + sumHistoryApprovedQuantity) * currentRequisitionApprovedQuantity);
    }
  }

  private BigDecimal toBigDecimalRoundDown(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.DOWN);
  }

  private Map<UUID, Map<UUID, Integer>> getFacilityIdToOrderableIdToMaxApprovedQuantity(
      List<Requisition> historyPeriodApprovedRequisitions) {

    Map<UUID, List<Requisition>> facilityIdToRequisitions = historyPeriodApprovedRequisitions.stream()
        .collect(Collectors.groupingBy(Requisition::getFacilityId));

    Map<UUID, Map<UUID, Integer>> facilityIdToOrderableIdToMaxApprovedQuantity = Maps.newHashMap();
    facilityIdToRequisitions.forEach((facilityId, requisitions) -> {
      Map<UUID, Integer> orderableIdToMaxApprovedQuantity = Maps.newHashMap();
      requisitions.forEach(requisition ->
          requisition.getRequisitionLineItems().forEach(requisitionLineItem -> {
            UUID orderableId = requisitionLineItem.getOrderable().getId();
            Integer maxApprovedQuantity = orderableIdToMaxApprovedQuantity.getOrDefault(orderableId, 0);
            orderableIdToMaxApprovedQuantity.put(orderableId,
                Math.max(maxApprovedQuantity, getNoneNullDefaultZero(requisitionLineItem.getApprovedQuantity())));
          })
      );
      facilityIdToOrderableIdToMaxApprovedQuantity.put(facilityId, orderableIdToMaxApprovedQuantity);
    });
    return facilityIdToOrderableIdToMaxApprovedQuantity;
  }

  private Map<UUID, Integer> getOrderableIdToCurrentPeriodSumApprovedQuantity(
      List<Requisition> currentPeriodNotFinishedRequisitions) {
    Map<UUID, Integer> orderableIdToCurrentPeriodSumApprovedQuantity = Maps.newHashMap();
    currentPeriodNotFinishedRequisitions.forEach(requisition ->
        requisition.getRequisitionLineItems().forEach(lineItem -> {
          UUID orderableId = lineItem.getOrderable().getId();
          Integer sumApprovedQuantity = orderableIdToCurrentPeriodSumApprovedQuantity.getOrDefault(orderableId, 0);
          orderableIdToCurrentPeriodSumApprovedQuantity.put(orderableId,
              sumApprovedQuantity + getNoneNullDefaultZero(lineItem.getApprovedQuantity()));
        })
    );
    return orderableIdToCurrentPeriodSumApprovedQuantity;
  }

  private Map<UUID, Integer> getOrderableIdToHistoryPeriodSumApprovedQuantity(
      List<Requisition> historyPeriodApprovedRequisitions, Set<UUID> orderableIds) {

    Map<UUID, Map<UUID, Integer>> facilityIdToOrderableIdToMaxApprovedQuantity =
        getFacilityIdToOrderableIdToMaxApprovedQuantity(historyPeriodApprovedRequisitions);

    Map<UUID, Integer> orderableToSumApprovedQuantity = Maps.newHashMap();
    orderableIds.forEach(orderableId -> {
      Integer sumHistoryApprovedQuantity = 0;
      for (Entry<UUID, Map<UUID, Integer>> entry : facilityIdToOrderableIdToMaxApprovedQuantity.entrySet()) {
        sumHistoryApprovedQuantity += getNoneNullDefaultZero(entry.getValue().get(orderableId));
      }
      orderableToSumApprovedQuantity.put(orderableId, sumHistoryApprovedQuantity);
    });
    return orderableToSumApprovedQuantity;
  }

  private Map<UUID, Integer> getCurrentRequisitionOrderableToFinalApprovedQuantity(Order order,
      List<Requisition> currentPeriodRequisitions) {
    UUID requisitionId = getRequisitionId(order.getExternalId());
    Requisition currentRequisition = getCurrentRequisition(requisitionId, currentPeriodRequisitions);

    Map<UUID, Integer> orderableToFinalApprovedQuantity = Maps.newHashMap();
    currentRequisition.getRequisitionLineItems().forEach(
        lineItem -> orderableToFinalApprovedQuantity.put(lineItem.getOrderable().getId(),
            lineItem.getApprovedQuantity()));
    return orderableToFinalApprovedQuantity;
  }

  private Requisition getCurrentRequisition(UUID requisitionId, List<Requisition> currentPeriodRequisitions) {
    return currentPeriodRequisitions.stream()
        .filter(requisition -> requisition.getId().equals(requisitionId)).findFirst()
        .orElseThrow(() -> new NotFoundException("requisition not found"));
  }

  private List<Requisition> getCurrentPeriodAfterApprovedRequisitions(Order order, List<ProcessingPeriod> periods,
      List<UUID> clientFacilityIds) {
    return siglusRequisitionRepository.findRequisitionsByOrderInfo(
        clientFacilityIds,
        order.getProgramId(),
        Lists.newArrayList(getCurrentPeriodIdByFulfillDate(periods, LocalDate.now())),
        order.getEmergency(),
        REQUISITION_STATUS_AFTER_FINAL_APPROVED);
  }

  private List<Requisition> getPreviousThreePeriodAfterApprovedRequisitions(Order order, List<ProcessingPeriod> periods,
      List<Requisition> currentPeriodApprovedRequisitions, List<UUID> clientFacilityIds) {
    Set<UUID> hasApprovedRequisitionFacilityIds = currentPeriodApprovedRequisitions.stream()
        .map(Requisition::getFacilityId).collect(Collectors.toSet());
    List<UUID> noApprovedRequisitionFacilityIds = clientFacilityIds.stream()
        .filter(e -> !hasApprovedRequisitionFacilityIds.contains(e)).collect(Collectors.toList());
    List<UUID> periodIds = getPreviousThreePeriodIds(periods);
    if (CollectionUtils.isEmpty(noApprovedRequisitionFacilityIds)
        || CollectionUtils.isEmpty(periodIds)) {
      return Collections.emptyList();
    }
    return siglusRequisitionRepository.findRequisitionsByOrderInfo(
        noApprovedRequisitionFacilityIds,
        order.getProgramId(),
        periodIds,
        order.getEmergency(),
        REQUISITION_STATUS_AFTER_FINAL_APPROVED);
  }

  private UUID getRequisitionId(UUID externalId) {
    OrderExternal orderExternal = orderExternalRepository.findOne(externalId);
    return Objects.isNull(orderExternal) ? externalId : orderExternal.getRequisitionId();
  }

  private Map<UUID, Integer> getOrderableIdToSoh(Set<UUID> orderableIds, UUID programId, UUID facilityId) {
    return stockManagementRepository.getAvailableStockOnHandByProduct(facilityId, programId, orderableIds,
        LocalDate.now());
  }

  private Integer getNoneNullDefaultZero(Integer quantity) {
    return Objects.isNull(quantity) ? 0 : quantity;
  }

  private List<UUID> getPreviousThreePeriodIds(List<ProcessingPeriod> periods) {
    List<UUID> previousThreePeriodIds = Lists.newArrayList();
    ProcessingPeriod currentPeriod = getCurrentPeriodByFulfillDate(periods, LocalDate.now());
    for (int i = 1; i < 4; i++) {
      previousThreePeriodIds.add(getPeriodIdDateIn(periods, currentPeriod.getStartDate().minusMonths(i)));
    }
    return previousThreePeriodIds;
  }

  private List<UUID> getAllClientFacilityIds(UUID facilityId, UUID programId) {
    return siglusFacilityRepository.findAllClientFacilityIds(facilityId, programId).stream()
        .map(UUID::fromString).collect(Collectors.toList());
  }

  private List<ProcessingPeriod> getUpToNowMonthlyPeriods() {
    List<ProcessingPeriod> upToNowMonthlyPeriods = siglusProcessingPeriodService.getUpToNowMonthlyPeriods();
    upToNowMonthlyPeriods.sort(Comparator.comparing(ProcessingPeriod::getStartDate));
    return upToNowMonthlyPeriods;
  }

  private boolean shouldNotCalculateSuggestedQuantity(Order order) {
    return Boolean.TRUE.equals(order.getEmergency()) || isSuborder(order.getExternalId());
  }

  private UUID getCurrentPeriodIdByFulfillDate(List<ProcessingPeriod> periods, LocalDate date) {
    return getCurrentPeriodByFulfillDate(periods, date).getId();
  }

  private ProcessingPeriod getCurrentPeriodByFulfillDate(List<ProcessingPeriod> periods, LocalDate date) {
    return periods.stream().filter(period -> isFulfillDateMatchProcessingPeriod(period, date)).findFirst()
        .orElseThrow(() -> new BusinessDataException(new Message(ERROR_NO_PERIOD_MATCH)));
  }

  private boolean isFulfillDateMatchProcessingPeriod(ProcessingPeriod period, LocalDate date) {
    LocalDate fulfillStartDate = period.getEndDate().plusDays(6);
    LocalDate fulfillEndDate = period.getEndDate().plusMonths(1).plusDays(5);
    return isDateIn(fulfillStartDate, fulfillEndDate, date);
  }

  private boolean isDateIn(LocalDate startDate, LocalDate endDate, LocalDate date) {
    return !isDateNotIn(startDate, endDate, date);
  }

  private boolean isDateNotIn(LocalDate startDate, LocalDate endDate, LocalDate date) {
    return date.isBefore(startDate) || date.isAfter(endDate);
  }

  private UUID getPeriodIdDateIn(List<ProcessingPeriod> periods, LocalDate localDate) {
    ProcessingPeriod currentPeriod = PeriodUtil.getPeriodDateIn(periods, localDate);
    return currentPeriod.getId();
  }

  private SiglusOrderDto extendOrderDto(OrderDto orderDto, boolean isFcRequest) {
    return doExtendOrderDto(orderDto, true, isFcRequest);
  }

  private SiglusOrderDto extendOrderDtoWithoutProducts(OrderDto orderDto) {
    return doExtendOrderDto(orderDto, false, false);
  }

  private SiglusOrderDto doExtendOrderDto(OrderDto orderDto, boolean withAvailableProducts, boolean isFcRequest) {
    //totalDispensingUnits not present in lineitem of previous OrderFulfillmentService
    setOrderLineItemExtension(orderDto);
    fillPartialFulfillQuantity(orderDto);
    Requisition requisition = getRequisitionByOrder(orderDto);
    orderDto.setRequisitionNumber(siglusRequisitionExtensionService.formatRequisitionNumber(requisition.getId()));
    orderDto.setActualStartDate(requisition.getActualStartDate());
    orderDto.setActualEndDate(requisition.getActualEndDate());
    SiglusOrderDto order = SiglusOrderDto.builder().order(orderDto).build();
    if (withAvailableProducts) {
      order.setAvailableProducts(getAllUserAvailableProductAggregator(orderDto, isFcRequest));
    }
    setIfIsKit(order);
    return order;
  }

  private void fillPartialFulfillQuantity(OrderDto orderDto) {
    UUID externalId = orderDto.getExternalId();
    OrderExternal external = orderExternalRepository.findOne(externalId);
    UUID requisitionId = external == null ? externalId : external.getRequisitionId();
    List<OrderExternal> externals = orderExternalRepository.findByRequisitionId(requisitionId);
    Set<UUID> externalIds = externals.stream().map(OrderExternal::getId).collect(toSet());
    List<Order> orders = siglusOrdersRepository.findAllByExternalIdIn(externalIds);
    Set<UUID> orderIds = orders.stream().map(Order::getId).collect(toSet());
    List<Shipment> shipments = siglusShipmentRepository.findAllByOrderIdIn(orderIds);
    if (CollectionUtils.isEmpty(shipments)) {
      return;
    }
    Map<UUID, Long> orderableIdToPartialFulfillQuantity = new HashMap<>();
    Map<UUID, List<ShipmentLineItem>> orderableIdToItems = shipments
        .stream().map(Shipment::getLineItems)
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(ShipmentLineItem::getOrderableId));
    orderableIdToItems.entrySet().stream().forEach(entry -> {
      Long sum = entry.getValue().stream()
          .map(ShipmentLineItem::getQuantityShipped).reduce(Long::sum).orElse(0L);
      orderableIdToPartialFulfillQuantity.put(entry.getKey(), sum);
    });
    orderDto.orderLineItems().forEach(lineItem -> {
      UUID orderableId = lineItem.getOrderableIdentity().getId();
      if (orderableIdToPartialFulfillQuantity.containsKey(orderableId)) {
        lineItem.setPartialFulfilledQuantity(orderableIdToPartialFulfillQuantity.get(orderableId));
      }
    });
  }

  private void setIfIsKit(SiglusOrderDto siglusOrderDto) {
    List<UUID> orderableIds = siglusOrderDto.getOrder().orderLineItems().stream()
        .map(orderableDto -> orderableDto.getOrderable().getId()).collect(Collectors.toList());
    List<Orderable> orderables = orderableRepository.findLatestByIds(orderableIds);
    Map<UUID, Boolean> orderableToIsKitMap = new HashMap<>();
    orderables.forEach(orderable ->
        orderableToIsKitMap.put(orderable.getId(), CollectionUtils.isNotEmpty(orderable.getChildren())
            || APE_KITS.contains(orderable.getProductCode().toString())));
    siglusOrderDto.getOrder().orderLineItems().forEach(orderLineItemDto -> {
      OrderableDto orderable = orderLineItemDto.getOrderable();
      orderable.setIsKit(orderableToIsKitMap.get(orderable.getId()));
    });
  }

  private boolean currentDateIsAfterNextPeriodEndDate(OrderDto orderDto) {
    LocalDate currentDate = LocalDate.now(ZoneId.of(timeZoneId));
    ProcessingPeriodDto period = orderDto.getProcessingPeriod();
    List<org.openlmis.requisition.dto.ProcessingPeriodDto> periodDtos =
        getNextProcessingPeriodDto(period);
    if (!CollectionUtils.isEmpty(periodDtos)) {
      org.openlmis.requisition.dto.ProcessingPeriodDto nextPeriod = periodDtos.get(0);
      ProcessingPeriodExtension extension =
          processingPeriodExtensionRepository.findByProcessingPeriodId(nextPeriod.getId());
      if (extension != null) {
        return extension.getSubmitEndDate().isBefore(currentDate);
      }
    }
    return false;
  }

  private void updateExternalIdAndStatusForSubOrder(UUID orderId, UUID externalId, OrderStatus orderStatus) {
    Order originOrder = orderRepository.findOne(orderId);
    originOrder.setExternalId(externalId);
    originOrder.setStatus(orderStatus);
    log.info("update externalId and Status for subOrder: {}", originOrder);
    orderRepository.save(originOrder);
  }

  private List<org.openlmis.requisition.dto.ProcessingPeriodDto> getNextProcessingPeriodDto(
      ProcessingPeriodDto period) {
    Pageable pageable = new PageRequest(0, 1);
    return periodService
        .searchProcessingPeriods(period.getProcessingSchedule().getId(),
            null, null, period.getEndDate().plusDays(1),
            null,
            null, pageable)
        .getContent();
  }

  private Iterable<BasicOrderDto> createNewOrder(OrderObjectReferenceDto order,
      List<org.openlmis.fulfillment.web.util.OrderLineItemDto> orderLineItemDtos,
      List<OrderExternal> externals) {
    OrderDto newOrder = new OrderDto();
    BeanUtils.copyProperties(order, newOrder);
    newOrder.setId(null);
    newOrder.setExternalId(externals.get(externals.size() - 1).getId());
    newOrder.setOrderCode(order.getOrderCode());
    newOrder.setOrderLineItems(orderLineItemDtos);
    newOrder.setStatus(OrderStatus.ORDERED);
    newOrder.setStatusMessages(Collections.emptyList());
    newOrder.setCreatedDate(order.getCreatedDate());
    Iterable<BasicOrderDto> orderDtos = orderController.batchCreateOrders(Collections.singletonList(newOrder),
        (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication());
    if (order.getCreatedDate() != null) {
      orderDtos.forEach(savedDto -> {
        Order orderEntity = orderRepository.findOne(savedDto.getId());
        orderEntity.setCreatedDate(order.getCreatedDate());
        orderRepository.save(orderEntity);
        log.info("[FC] update order: {}, createdDate: {}", orderEntity.getId(), orderEntity.getCreatedDate());
      });
    }
    if (orderDtos.iterator().hasNext()) {
      UUID newOrderId = orderDtos.iterator().next().getId();
      updateExternalIdAndStatusForSubOrder(newOrderId, newOrder.getExternalId(), OrderStatus.PARTIALLY_FULFILLED);
      updateOrderExtension(orderLineItemDtos, orderDtos);
    }
    return orderDtos;
  }

  private void updateOrderExtension(
      List<org.openlmis.fulfillment.web.util.OrderLineItemDto> orderLineItemDtos,
      Iterable<BasicOrderDto> orderDtos) {
    OrderDto orderDto = orderController
        .getOrder(orderDtos.iterator().next().getId(), null);
    Map<UUID, org.openlmis.fulfillment.web.util.OrderLineItemDto> orderLineItemDtoMap =
        orderLineItemDtos.stream()
            .collect(toMap(orderLineItemDto -> orderLineItemDto.getOrderableIdentity().getId(),
                orderLineItemDto -> orderLineItemDto));

    List<OrderLineItemExtension> extensions = new ArrayList<>();
    Map<UUID, OrderLineItemExtension> existedLineItemIdToExtension = lineItemExtensionRepository.findByOrderId(
        orderDto.getId()).stream().collect(Collectors.toMap(OrderLineItemExtension::getOrderLineItemId, e -> e));
    for (OrderLineItem.Importer importer : orderDto.getOrderLineItems()) {
      OrderLineItemExtension extension = Objects.isNull(existedLineItemIdToExtension.get(importer.getId()))
          ? new OrderLineItemExtension()
          : existedLineItemIdToExtension.get(importer.getId());
      extension.setOrderId(orderDto.getId());
      extension.setOrderLineItemId(importer.getId());
      extension.setPartialFulfilledQuantity(
          orderLineItemDtoMap.get(importer.getOrderableIdentity().getId())
              .getPartialFulfilledQuantity());
      extensions.add(extension);
    }
    log.info("save order line item extension, orderId:{}, size:{}", orderDto.getId(), extensions);
    lineItemExtensionRepository.save(extensions);
  }

  private Set<VersionObjectReferenceDto> getAllUserAvailableProductAggregator(OrderDto orderDto, boolean isFcRequest) {
    Requisition requisition = getRequisitionByOrder(orderDto);

    UUID approverFacilityId = orderDto.getCreatedBy().getHomeFacilityId();
    UUID userHomeFacilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    UUID programId = requisition.getProgramId();
    List<ApprovedProductDto> approverProducts;
    if (isFcRequest) {
      approverProducts = requisitionService.getAllApprovedProducts(
          approverFacilityId, programId);
    } else {
      approverProducts = approvedProductReferenceDataService.getApprovedProducts(
          approverFacilityId, programId);
    }
    List<ApprovedProductDto> userProducts;
    if (approverFacilityId.equals(userHomeFacilityId)) {
      userProducts = approverProducts;
    } else if (isFcRequest) {
      userProducts = requisitionService.getAllApprovedProducts(userHomeFacilityId, programId);
    } else {
      userProducts = approvedProductReferenceDataService.getApprovedProducts(userHomeFacilityId, programId);
    }
    Set<UUID> approverOrderableIds = getOrderableIds(approverProducts, programId);
    Set<UUID> userOrderableIds;
    if (approverFacilityId.equals(userHomeFacilityId)) {
      userOrderableIds = approverOrderableIds;
    } else {
      userOrderableIds = getOrderableIds(userProducts, programId);
    }

    Set<UUID> archivedOrderableIds = getArchivedOrderableIds(Sets.newHashSet(
        requisition.getFacilityId(), approverFacilityId, userHomeFacilityId));

    Map<UUID, StockCardSummaryV2Dto> orderableSohMap = getOrderableIdSohMap(userHomeFacilityId);

    Set<UUID> emergencyFilteredProducts = getEmergencyFilteredProducts(requisition);

    return Optional
        .ofNullable(requisition.getAvailableProducts())
        .orElse(Collections.emptySet())
        .stream()
        .map(ApprovedProductReference::getOrderable)
        .filter(orderable -> {
          UUID orderableId = orderable.getId();
          if (!archivedOrderableIds.contains(orderableId)
              && approverOrderableIds.contains(orderableId)
              && userOrderableIds.contains(orderableId)
              && orderableSohMap.get(orderableId) != null) {
            Integer soh = orderableSohMap.get(orderableId).getStockOnHand();
            return soh != null && soh > 0;
          }
          return false;
        })
        .filter(orderable -> !emergencyFilteredProducts.contains(orderable.getId()))
        .map(orderable -> new VersionObjectReferenceDto(
            orderable.getId(), serviceUrl, ORDERABLES, orderable.getVersionNumber())
        ).collect(Collectors.toSet());
  }

  private Set<UUID> getEmergencyFilteredProducts(Requisition requisition) {
    Set<UUID> emergencyOrderableIds = new HashSet<>();
    if (Boolean.TRUE.equals(requisition.getEmergency())) {
      List<RequisitionV2Dto> previousRequisitions =
          siglusRequisitionService.getPreviousEmergencyRequisition(requisition);
      if (CollectionUtils.isNotEmpty(previousRequisitions)) {
        emergencyOrderableIds.addAll(filterProductService.getInProgressProducts(previousRequisitions));
        emergencyOrderableIds.addAll(filterProductService.getNotFullyShippedProducts(previousRequisitions));
      }
    }
    return emergencyOrderableIds;
  }

  private Set<UUID> getOrderableIds(List<ApprovedProductDto> productDtos, UUID programId) {
    ApproveProductsAggregator aggregator = new ApproveProductsAggregator(productDtos, programId);
    return Optional
        .ofNullable(aggregator.getApprovedProductReferences())
        .orElse(Collections.emptySet())
        .stream()
        .map(ApprovedProductReference::getOrderable)
        .map(VersionEntityReference::getId)
        .collect(toSet());
  }

  private Set<UUID> getArchivedOrderableIds(Set<UUID> facilityIds) {
    return siglusArchiveProductService.searchArchivedProductsByFacilityIds(facilityIds).stream()
        .map(UUID::fromString)
        .collect(Collectors.toSet());
  }

  private Map<UUID, StockCardSummaryV2Dto> getOrderableIdSohMap(UUID userFacilityId) {
    MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
    multiValueMap.set(FACILITY_ID, userFacilityId.toString());
    multiValueMap.set(PROGRAM_ID, ALL_PRODUCTS_PROGRAM_ID.toString());
    multiValueMap.set(RIGHT_NAME, STOCK_CARDS_VIEW);
    multiValueMap.set(NON_EMPTY_ONLY, Boolean.TRUE.toString());
    Page<StockCardSummaryV2Dto> stockCardSummary = siglusStockCardSummariesService
        .searchStockCardSummaryV2Dtos(multiValueMap, null, null, new PageRequest(0, Integer.MAX_VALUE), false);

    // to map stockCardSummaryV2Dto.getStockOnHand() return null cause NPE
    return stockCardSummary.getContent().stream().collect(Collectors.toMap(
        stockCardSummaryV2Dto -> stockCardSummaryV2Dto.getOrderable().getId(),
        stockCardSummaryV2Dto -> stockCardSummaryV2Dto,
        (s1, s2) -> s1
    ));
  }

  private void setOrderLineItemExtension(OrderDto orderDto) {
    setOrderLineItemExtension(orderDto.orderLineItems());
  }

  private void setOrderLineItemExtension(List<OrderLineItemDto> lineItems) {
    Set<UUID> lineItemIds = lineItems.stream().map(OrderLineItem.Importer::getId)
        .collect(Collectors.toSet());
    Map<UUID, OrderLineItemExtension> lineItemExtensionMap = lineItemExtensionRepository
        .findByOrderLineItemIdIn(lineItemIds).stream()
        .collect(toMap(OrderLineItemExtension::getOrderLineItemId, extension -> extension));
    lineItems.forEach(lineItem -> {
      OrderLineItemExtension extension = lineItemExtensionMap.get(lineItem.getId());
      lineItem.setPartialFulfilledQuantity(null != extension
          ? extension.getPartialFulfilledQuantity() : Long.valueOf(0));
      if (null != extension) {
        lineItem.setSkipped(extension.isSkipped());
        lineItem.setAdded(extension.isAdded());
      }
    });
  }
}
