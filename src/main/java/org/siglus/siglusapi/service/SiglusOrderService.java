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
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_PERIOD_MATCH;
import static org.siglus.siglusapi.util.SiglusDateHelper.DATE_MONTH_YEAR;
import static org.siglus.siglusapi.util.SiglusDateHelper.getFormatDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.service.OrderService;
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
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.OrderStatusDto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.i18n.MessageKeys;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.repository.dto.OrderSuggestedQuantityDto;
import org.siglus.siglusapi.repository.dto.RequisitionOrderDto;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
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
  private SiglusFacilityRepository siglusFacilityRepository;

  @Autowired
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Autowired
  private StockManagementRepository stockManagementRepository;

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
    if (!basicOrderDtoPage.hasContent()) {
      return new PageImpl<>(Lists.newArrayList(), pageable, basicOrderDtoPage.getTotalElements());
    }

    List<BasicOrderDto> basicOrderDtos = basicOrderDtoPage.getContent();

    Set<UUID> orderIds = basicOrderDtos.stream().map(BasicOrderDto::getId).collect(Collectors.toSet());
    Set<UUID> orderIdsWithSubDraft = podSubDraftRepository.findOrderIdsWithSubDraft(orderIds).stream()
        .map(UUID::fromString).collect(Collectors.toSet());

    List<BasicOrderExtensionResponse> basicOrderExtensionResponseList = Lists.newArrayListWithExpectedSize(
        basicOrderDtos.size());
    for (BasicOrderDto basicOrderDto : basicOrderDtos) {
      BasicOrderExtensionResponse basicOrderExtensionResponse = new BasicOrderExtensionResponse();
      BeanUtils.copyProperties(basicOrderDto, basicOrderExtensionResponse);
      basicOrderExtensionResponse.setHasSubDraft(orderIdsWithSubDraft.contains(basicOrderDto.getId()));
      basicOrderExtensionResponseList.add(basicOrderExtensionResponse);
    }

    return new PageImpl<>(basicOrderExtensionResponseList, pageable, basicOrderDtoPage.getTotalElements());
  }

  public Page<BasicOrderDto> searchOrdersForFulfill(OrderSearchParams params, Pageable pageable) {
    Page<Order> orders = orderService.searchOrdersForFulfillPage(params, pageable);
    List<BasicOrderDto> dtos = basicOrderDtoBuilder.build(orders.getContent());
    return new PageImpl<>(
        dtos,
        pageable, orders.getTotalElements());
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

  void revertOrderToCloseStatus(Order order) {
    draftService.deleteOrderLineItemAndInitialedExtension(order);
    log.info("close order: close order id: {}", order.getId());
    order.setStatus(OrderStatus.CLOSED);
    log.info("save closed order: {}", order);
    orderRepository.save(order);
  }

  public SiglusOrderDto searchOrderById(UUID orderId) {
    OrderDto orderDto = orderController.getOrder(orderId, null);
    return extendOrderDto(orderDto);
  }

  public SiglusOrderDto searchOrderByIdWithoutProducts(UUID orderId) {
    OrderDto orderDto = orderController.getOrder(orderId, null);
    return extendOrderDtoWithoutProducts(orderDto);
  }

  public SiglusOrderDto searchOrderByIdForMultiWareHouseSupply(UUID orderId) {
    Order order = orderRepository.findOne(orderId);
    OrderDto orderDto = orderDtoBuilder.build(order);
    return extendOrderDto(orderDto);
  }

  // manually fill no-id lineitem
  public Set<UUID> updateOrderLineItems(ShipmentDraftDto draftDto) {
    Order order = orderRepository.findOne(draftDto.getOrder().getId());
    List<OrderLineItem> orderLineItems = order.getOrderLineItems();
    List<org.openlmis.fulfillment.web.util.OrderLineItemDto> draftOrderLineItems
        = draftDto.getOrder().getOrderLineItems();

    Set<UUID> addedOrderableIds = new HashSet<>();

    draftOrderLineItems.stream()
        .filter(orderLineItemDto -> orderLineItemDto.getId() == null)
        .map(OrderLineItem::newInstance)
        .forEach(orderLineItem -> {
          orderLineItem.setOrder(order);
          orderLineItems.add(orderLineItem);
          addedOrderableIds.add(orderLineItem.getOrderable().getId());
        });

    if (addedOrderableIds.isEmpty()) {
      return Collections.emptySet();
    }
    log.info("orderId: {}, addedOrderableIds: {}", order.getId(), addedOrderableIds);
    Order saved = orderRepository.save(order);
    Set<UUID> addedLineItemIds = new HashSet<>();
    // orderable-id : lineItem-id
    Map<UUID, UUID> addedLineItemMap = saved.getOrderLineItems()
        .stream().collect(toMap(
            orderLineItem -> orderLineItem.getOrderable().getId(),
            OrderLineItem::getId));

    draftOrderLineItems.forEach(orderLineItemDto -> {
      if (orderLineItemDto.getId() == null) {
        UUID orderableId = orderLineItemDto.getOrderable().getId();
        UUID lineItemId = addedLineItemMap.get(orderableId);
        orderLineItemDto.setId(lineItemId);
        addedLineItemIds.add(lineItemId);
      }
    });

    log.info("orderId: {}, addedOrderLineItemIds: {}", order.getId(), addedLineItemIds);
    return addedLineItemIds;
  }

  public Iterable<BasicOrderDto> createSubOrder(OrderObjectReferenceDto order,
      List<org.openlmis.fulfillment.web.util.OrderLineItemDto>
          orderLineItemDtos) {
    OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
    List<OrderExternal> externals;
    if (external == null) {
      OrderExternal firstExternal = OrderExternal.builder()
          .requisitionId(order.getExternalId()).build();
      OrderExternal secondExternal = OrderExternal.builder()
          .requisitionId(order.getExternalId()).build();
      log.info("save order external : {}", Arrays.asList(firstExternal, secondExternal));
      externals = orderExternalRepository
          .save(Arrays.asList(firstExternal, secondExternal));
      updateExistOrderForSubOrder(order.getId(), externals.get(0).getId(),
          order.getOrderCode().concat("-" + 1), order.getStatus());
      order.setOrderCode(order.getOrderCode().concat("-" + 2));
    } else {
      externals = orderExternalRepository
          .findByRequisitionId(external.getRequisitionId());
      OrderExternal newExternal = OrderExternal.builder()
          .requisitionId(external.getRequisitionId()).build();
      log.info("save new external : {}", newExternal);
      newExternal = orderExternalRepository.save(newExternal);
      externals.add(newExternal);
      order.setOrderCode(replaceLast(order.getOrderCode(), "-" + (externals.size() - 1),
          "-" + externals.size()));
    }
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

  private Map<UUID, BigDecimal> getOrderableIdToSuggestedQuantity(Order order, List<ProcessingPeriod> periods) {
    return ORDER_STATUS_AFTER_START_FULFILL.contains(order.getStatus())
        ? getOrderableIdToSuggestedQuantityFromDb(order)
        : calculateAndSaveOrderableIdToSuggestedQuantity(order, periods);
  }

  private Map<UUID, BigDecimal> getOrderableIdToSuggestedQuantityFromDb(Order order) {
    Set<UUID> lineItemIds = getLineItemIds(order);
    List<OrderSuggestedQuantityDto> dtos = lineItemExtensionRepository.findOrderSuggestedQuantityDtoByOrderLineItemIdIn(
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
      throw new NotFoundException(MessageKeys.ERROR_ORDER_NOT_EXIST);
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
    lineItemIds.forEach(lineItemId -> {
      OrderLineItemExtension extension = OrderLineItemExtension.builder()
          .orderId(order.getId())
          .orderLineItemId(lineItemId)
          .partialFulfilledQuantity(0L)
          .suggestedQuantity(orderableIdToSuggestedQuantity.get(lineItemIdToOrderableId.get(lineItemId)))
          .build();
      extensions.add(extension);
    });
    log.info("save OrderLineItemExtensions, size={}", extensions.size());
    lineItemExtensionRepository.save(extensions);
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
      Integer sumApprovedQuantity = getNoneNullDefaultZero(
          orderableIdToCurrentPeriodSumApprovedQuantity.get(orderableId));
      Integer sumHistoryApprovedQuantity = getNoneNullDefaultZero(
          orderableIdToHistoryPeriodSumApprovedQuantity.get(orderableId));
      Integer soh = getNoneNullDefaultZero(orderableIdToSoh.get(orderableId));

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
            Integer maxApprovedQuantity = getNoneNullDefaultZero(orderableIdToMaxApprovedQuantity.get(orderableId));
            orderableIdToMaxApprovedQuantity.put(orderableId,
                Math.max(maxApprovedQuantity, requisitionLineItem.getApprovedQuantity()));
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
          Integer sumApprovedQuantity = getNoneNullDefaultZero(
              orderableIdToCurrentPeriodSumApprovedQuantity.get(orderableId));
          orderableIdToCurrentPeriodSumApprovedQuantity.put(orderableId,
              sumApprovedQuantity + lineItem.getApprovedQuantity());
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
    return siglusRequisitionRepository.findRequisitionsByOrderInfoAndSupplyingFacilityId(
        clientFacilityIds,
        order.getProgramId(),
        Lists.newArrayList(getCurrentPeriodIdByFulfillDate(periods, LocalDate.now())),
        order.getEmergency(),
        REQUISITION_STATUS_AFTER_FINAL_APPROVED,
        order.getSupplyingFacilityId());
  }

  private List<Requisition> getPreviousThreePeriodAfterApprovedRequisitions(Order order, List<ProcessingPeriod> periods,
      List<Requisition> currentPeriodApprovedRequisitions, List<UUID> clientFacilityIds) {
    Set<UUID> hasApprovedRequisitionFacilityIds = currentPeriodApprovedRequisitions.stream()
        .map(Requisition::getFacilityId).collect(Collectors.toSet());
    List<UUID> noApprovedRequisitionFacilityIds = clientFacilityIds.stream()
        .filter(e -> !hasApprovedRequisitionFacilityIds.contains(e)).collect(Collectors.toList());

    return siglusRequisitionRepository.findRequisitionsByOrderInfoAndSupplyingFacilityId(
        noApprovedRequisitionFacilityIds,
        order.getProgramId(),
        getPreviousThreePeriodIds(periods),
        order.getEmergency(),
        REQUISITION_STATUS_AFTER_FINAL_APPROVED,
        order.getSupplyingFacilityId());
  }

  private UUID getRequisitionId(UUID externalId) {
    OrderExternal orderExternal = orderExternalRepository.findOne(externalId);
    return Objects.isNull(orderExternal) ? externalId : orderExternal.getRequisitionId();
  }

  private Map<UUID, Integer> getOrderableIdToSoh(Set<UUID> orderableIds, UUID programId, UUID facilityId) {
    return stockManagementRepository.getStockOnHandByProduct(facilityId, programId, orderableIds, LocalDate.now());
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
    ProcessingPeriod currentPeriod = siglusProcessingPeriodService.getPeriodDateIn(periods, localDate);
    return currentPeriod.getId();
  }

  private SiglusOrderDto extendOrderDto(OrderDto orderDto) {
    return doExtendOrderDto(orderDto, true);
  }

  private SiglusOrderDto extendOrderDtoWithoutProducts(OrderDto orderDto) {
    return doExtendOrderDto(orderDto, false);
  }

  private SiglusOrderDto doExtendOrderDto(OrderDto orderDto, boolean withAvailableProducts) {
    //totalDispensingUnits not present in lineitem of previous OrderFulfillmentService
    setOrderLineItemExtension(orderDto);
    Requisition requisition = getRequisitionByOrder(orderDto);
    orderDto.setRequisitionNumber(siglusRequisitionExtensionService.formatRequisitionNumber(requisition.getId()));
    orderDto.setActualStartDate(requisition.getActualStartDate());
    orderDto.setActualEndDate(requisition.getActualEndDate());
    SiglusOrderDto order = SiglusOrderDto.builder().order(orderDto).build();
    if (withAvailableProducts) {
      order.setAvailableProducts(getAllUserAvailableProductAggregator(orderDto));
    }
    setIfIsKit(order);
    return order;
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

  private void updateExistOrderForSubOrder(UUID orderId,
      UUID externalId, String orderCode, OrderStatus orderStatus) {
    Order originOrder = orderRepository.findOne(orderId);
    originOrder.setExternalId(externalId);
    originOrder.setOrderCode(orderCode);
    originOrder.setStatus(orderStatus);
    log.info("update exist order for subOrder: {}", originOrder);
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
    Iterable<BasicOrderDto> orderDtos = orderController.batchCreateOrders(Arrays.asList(newOrder),
        (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication());
    if (orderDtos.iterator().hasNext()) {
      UUID newOrderId = orderDtos.iterator().next().getId();
      updateExistOrderForSubOrder(newOrderId, newOrder.getExternalId(),
          newOrder.getOrderCode(), OrderStatus.PARTIALLY_FULFILLED);
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
    log.info("save requisition line item extension: {}", extensions);
    lineItemExtensionRepository.save(extensions);
  }

  private Set<VersionObjectReferenceDto> getAllUserAvailableProductAggregator(OrderDto orderDto) {
    Requisition requisition = getRequisitionByOrder(orderDto);

    UUID approverFacilityId = orderDto.getCreatedBy().getHomeFacilityId();
    UUID userHomeFacilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    // 10+ seconds cost when call following requisitionService.getApproveProduct
    ApproveProductsAggregator approverProductAggregator = requisitionService.getApproveProduct(
        approverFacilityId, requisition.getProgramId(), false);
    ApproveProductsAggregator userProductAggregator;
    if (approverFacilityId.equals(userHomeFacilityId)) {
      userProductAggregator = approverProductAggregator;
    } else {
      userProductAggregator = requisitionService.getApproveProduct(userHomeFacilityId,
          requisition.getProgramId(), false);
    }
    Set<UUID> approverOrderableIds = getOrderableIds(approverProductAggregator);
    Set<UUID> userOrderableIds;
    if (approverFacilityId.equals(userHomeFacilityId)) {
      userOrderableIds = approverOrderableIds;
    } else {
      userOrderableIds = getOrderableIds(userProductAggregator);
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

  private Set<UUID> getOrderableIds(ApproveProductsAggregator aggregator) {
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
        .searchStockCardSummaryV2Dtos(multiValueMap, null, null, new PageRequest(0, Integer.MAX_VALUE));

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

  private String replaceLast(String text, String regex, String replacement) {
    return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
  }

}
