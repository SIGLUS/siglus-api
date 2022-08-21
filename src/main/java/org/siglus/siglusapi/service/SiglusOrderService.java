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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
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
import org.siglus.siglusapi.dto.OrderStatusDto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.web.response.BasicOrderExtensionResponse;
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

  public Page<BasicOrderDto> searchOrders(OrderSearchParams params, Pageable pageable) {
    return orderController.searchOrders(params, pageable);
  }

  public Page<BasicOrderDto> searchOrdersWithSubDraftStatus(OrderSearchParams params, Pageable pageable) {
    Page<BasicOrderDto> basicOrderDtoPage = orderController.searchOrders(params, pageable);
    if (!basicOrderDtoPage.hasContent()) {
      return basicOrderDtoPage;
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

    return new PageImpl(basicOrderExtensionResponseList, pageable, basicOrderDtoPage.getTotalElements());
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
    for (OrderLineItem.Importer importer : orderDto.getOrderLineItems()) {
      OrderLineItemExtension extension = new OrderLineItemExtension();
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
