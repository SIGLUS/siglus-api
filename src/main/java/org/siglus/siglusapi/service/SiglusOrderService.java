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
import static org.openlmis.stockmanagement.service.StockmanagementPermissionService.STOCK_CARDS_VIEW;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.PROGRAM_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.service.ResourceNames;
import org.openlmis.fulfillment.service.referencedata.FulfillmentOrderableReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.util.Pagination;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.dto.OrderLineItemDto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.dto.SiglusOrderLineItemDto;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusOrderFulfillmentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
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
  private FulfillmentOrderableReferenceDataService fulfillmentOrderableReferenceDataService;

  @Autowired
  private OrderLineItemExtensionRepository lineItemExtensionRepository;

  @Autowired
  private SiglusOrderFulfillmentService siglusOrderFulfillmentService;

  @Autowired
  private OrderRepository orderRepository;

  @Value("${service.url}")
  private String serviceUrl;

  @Autowired
  private OrderExternalRepository orderExternalRepository;

  @Autowired
  private OrderController orderController;

  public Page<BasicOrderDto> searchOrders(OrderSearchParams params, Pageable pageable) {
    List<BasicOrderDto> orderDtos = siglusOrderFulfillmentService
        .searchOrders(params, pageable).getContent();
    if (orderDtos.isEmpty()) {
      return Pagination.getPage(orderDtos, pageable, orderDtos.size());
    }
    List<UUID> orderedExternalIds = orderDtos.stream()
        .filter(order -> order.getStatus().equals(OrderStatus.ORDERED))
        .map(BasicOrderDto::getExternalId).collect(Collectors.toList());
    List<UUID> exsitExternalIds = orderExternalRepository
        .findAll(orderedExternalIds).stream()
        .map(OrderExternal::getId).collect(Collectors.toList());
    if (!exsitExternalIds.isEmpty()) {
      orderDtos.forEach(basicOrderDto -> {
        if (basicOrderDto.getStatus().equals(OrderStatus.ORDERED)
            && exsitExternalIds.contains(basicOrderDto.getExternalId())) {
          basicOrderDto.setStatus(OrderStatus.PARTIALLY_FULFILLED);
        }
      });
    }
    return Pagination.getPage(orderDtos, pageable, orderDtos.size());
  }

  public SiglusOrderDto searchOrderById(UUID orderId) {
    //totalDispensingUnits not present in lineitem of previous OrderFulfillmentService
    OrderDto orderDto = siglusOrderFulfillmentService.findOne(orderId);
    setOrderLineItemExtension(orderDto);
    return SiglusOrderDto.builder()
        .order(orderDto)
        .availableProducts(getAllUserAvailableProductAggregator(orderDto)).build();
  }

  public List<SiglusOrderLineItemDto> createOrderLineItem(List<UUID> orderableIds) {
    Map<UUID, StockCardSummaryV2Dto> summaryMap = getOrderableIdSohMap(
        authenticationHelper.getCurrentUser().getHomeFacilityId());

    Map<UUID, OrderableDto> orderableMap = fulfillmentOrderableReferenceDataService
        .findByIds(orderableIds).stream()
        .collect(toMap(OrderableDto::getId, orderableDto -> orderableDto));
    return orderableIds.stream()
        .filter(orderableMap::containsKey)
        .map(orderableId -> buildOrderLineItem(summaryMap, orderableMap, orderableId))
        .collect(toList());
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
      externals = orderExternalRepository
          .save(Arrays.asList(firstExternal, secondExternal));
      updateExistOrderForSubOrder(order.getId(), externals.get(0).getId(),
          order.getOrderCode().concat("-" + 1));
      order.setOrderCode(order.getOrderCode().concat("-" + 2));
    } else {
      externals = orderExternalRepository
          .findByRequisitionId(external.getRequisitionId());
      OrderExternal newExternal = OrderExternal.builder()
          .requisitionId(external.getRequisitionId()).build();
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

  private String replaceLast(String text, String regex, String replacement) {
    return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
  }

  private void updateExistOrderForSubOrder(UUID orderId,
      UUID externalId, String orderCode) {
    Order originOrder = orderRepository.findOne(orderId);
    originOrder.setExternalId(externalId);
    originOrder.setOrderCode(orderCode);
    log.info("update exist order for subOrder: {}", originOrder);
    orderRepository.save(originOrder);
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
    Iterable<BasicOrderDto> orderDtos = orderController.batchCreateOrders(Arrays.asList(newOrder),
        (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication());
    if (orderDtos.iterator().hasNext()) {
      UUID newOrderId = orderDtos.iterator().next().getId();
      updateExistOrderForSubOrder(newOrderId, newOrder.getExternalId(),
          newOrder.getOrderCode());
      updateOrderExtension(orderLineItemDtos, orderDtos);
    }
    return orderDtos;
  }

  private void updateOrderExtension(
      List<org.openlmis.fulfillment.web.util.OrderLineItemDto> orderLineItemDtos,
      Iterable<BasicOrderDto> orderDtos) {
    OrderDto orderDto = siglusOrderFulfillmentService
        .findOne(orderDtos.iterator().next().getId());
    Map<UUID, org.openlmis.fulfillment.web.util.OrderLineItemDto> orderLineItemDtoMap =
        orderLineItemDtos.stream()
            .collect(toMap(orderLineItemDto -> orderLineItemDto.getOrderableIdentity().getId(),
                orderLineItemDto -> orderLineItemDto));

    List<OrderLineItemExtension> extensions = new ArrayList<>();
    for (OrderLineItem.Importer importer : orderDto.getOrderLineItems()) {
      OrderLineItemExtension extension = new OrderLineItemExtension();
      extension.setOrderLineItemId(importer.getId());
      extension.setPartialFulfilledQuantity(
          orderLineItemDtoMap.get(importer.getOrderableIdentity().getId())
              .getPartialFulfilledQuantity());
      extensions.add(extension);
    }
    log.info("save requisition line item extension: {}", extensions);
    lineItemExtensionRepository.save(extensions);
  }

  private SiglusOrderLineItemDto buildOrderLineItem(Map<UUID, StockCardSummaryV2Dto> summaryMap,
      Map<UUID, OrderableDto> orderableMap, UUID orderableId) {
    OrderLineItemDto orderLineItemDto = new OrderLineItemDto();
    OrderableDto orderableDto = orderableMap.get(orderableId);
    orderLineItemDto.setOrderable(new VersionObjectReferenceDto(orderableDto.getId(), serviceUrl,
        ResourceNames.ORDERABLES, orderableDto.getVersionNumber()));
    // no order quantity/packsToOrder, manually input by user
    orderLineItemDto.setOrderedQuantity(0L);

    List<ObjectReferenceDto> lotList = summaryMap.get(orderableId).getCanFulfillForMe()
        .stream()
        .filter(canFulfillForMeEntryDto -> canFulfillForMeEntryDto.getStockOnHand() > 0)
        .map(CanFulfillForMeEntryDto::getLot)
        .collect(Collectors.toList());
    return SiglusOrderLineItemDto
        .builder()
        .orderLineItem(orderLineItemDto)
        .lots(lotList)
        .build();
  }

  private Set<VersionObjectReferenceDto> getAllUserAvailableProductAggregator(OrderDto orderDto) {
    OrderExternal external = orderExternalRepository.findOne(orderDto.getExternalId());
    UUID requisitionId = external == null ? orderDto.getExternalId() : external.getRequisitionId();
    Requisition requisition = requisitionController.findRequisition(requisitionId,
        requisitionController.getProfiler("GET_ORDER"));

    UUID approverFacilityId = orderDto.getCreatedBy().getHomeFacilityId();
    UUID userHomeFacilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    // 10+ seconds cost when call following requisitionService.getApproveProduct
    ApproveProductsAggregator approverProductAggregator = requisitionService.getApproveProduct(
        approverFacilityId, requisition.getProgramId(), requisition.getTemplate());
    ApproveProductsAggregator userProductAggregator;
    if (approverFacilityId.equals(userHomeFacilityId)) {
      userProductAggregator = approverProductAggregator;
    } else {
      userProductAggregator = requisitionService.getApproveProduct(userHomeFacilityId,
          requisition.getProgramId(), requisition.getTemplate());
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
        .map(orderable -> new VersionObjectReferenceDto(
            orderable.getId(), serviceUrl, ORDERABLES, orderable.getVersionNumber())
        ).collect(Collectors.toSet());
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
    Page<StockCardSummaryV2Dto> stockCardSummary = siglusStockCardSummariesService
        .searchStockCardSummaryV2Dtos(multiValueMap, new PageRequest(0, Integer.MAX_VALUE));

    // to map stockCardSummaryV2Dto.getStockOnHand() return null cause NPE
    return stockCardSummary.getContent().stream().collect(Collectors.toMap(
        stockCardSummaryV2Dto -> stockCardSummaryV2Dto.getOrderable().getId(),
        stockCardSummaryV2Dto -> stockCardSummaryV2Dto
    ));
  }

  private void setOrderLineItemExtension(OrderDto orderDto) {
    setOrderLineItemExtension(orderDto.orderLineItems());
  }

  private List<org.openlmis.fulfillment.web.util.OrderLineItemDto> setOrderLineItemExtension(
      List<org.openlmis.fulfillment.web.util.OrderLineItemDto> lineItems) {
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
    return lineItems;
  }

}
