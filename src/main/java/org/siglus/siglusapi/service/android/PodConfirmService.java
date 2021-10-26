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

package org.siglus.siglusapi.service.android;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.UpdateDetails;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.util.DateHelper;
import org.openlmis.fulfillment.web.MissingPermissionException;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.siglusapi.domain.PodConfirmBackup;
import org.siglus.siglusapi.domain.SyncUpHash;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.request.LotBasicRequest;
import org.siglus.siglusapi.dto.android.request.PodLotLineRequest;
import org.siglus.siglusapi.dto.android.request.PodProductLineRequest;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.dto.android.response.PodProductLineResponse;
import org.siglus.siglusapi.dto.android.response.PodResponse;
import org.siglus.siglusapi.exception.NoPermissionException;
import org.siglus.siglusapi.exception.UnsupportedProductsException;
import org.siglus.siglusapi.repository.OrderLineItemRepository;
import org.siglus.siglusapi.repository.PodConfirmBackupRepository;
import org.siglus.siglusapi.repository.PodNativeSqlRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryLineItemRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusShipmentLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.android.context.ContextHolder;
import org.siglus.siglusapi.service.android.context.CurrentUserContext;
import org.siglus.siglusapi.service.android.context.LotContext;
import org.siglus.siglusapi.service.android.context.ProductContext;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.slf4j.profiler.Profiler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods"})
public class PodConfirmService {

  private final SyncUpHashRepository syncUpHashRepository;
  private final SiglusProofOfDeliveryRepository podRepository;
  private final PermissionService fulfillmentPermissionService;
  private final SiglusProofOfDeliveryLineItemRepository podLineItemRepository;
  private final SiglusValidReasonAssignmentService validReasonAssignmentService;
  private final DateHelper dateHelper;
  private final OrderRepository orderRepository;
  private final PodNativeSqlRepository podNativeSqlRepository;
  private final SiglusShipmentLineItemRepository shipmentLineItemRepository;
  private final OrderLineItemRepository orderLineItemRepository;
  private final SiglusStockCardLineItemRepository stockCardLineItemRepository;
  private final SiglusApprovedProductReferenceDataService approvedProductDataService;
  private final PodConfirmBackupRepository podConfirmBackupRepository;

  @Transactional
  @SuppressWarnings("PMD.PreserveStackTrace")
  public void confirmPod(PodRequest podRequest, ProofOfDelivery toUpdate, PodResponse podResponse) {
    Profiler profiler = new Profiler("confirm pod");
    profiler.setLogger(log);
    profiler.start("check hash");
    UserDto user = ContextHolder.getContext(CurrentUserContext.class).getCurrentUser();
    String syncUpHash = podRequest.getSyncUpHash(user);
    if (syncUpHashRepository.findOne(syncUpHash) != null) {
      log.info("skip confirm pod as syncUpHash: {} existed", syncUpHash);
      return;
    }
    profiler.start("check updated");
    if (toUpdate.isConfirmed()) {
      log.warn("pod orderCode: {} has been confirmed:", podRequest.getOrderCode());
      return;
    }
    profiler.start("check permission");
    try {
      fulfillmentPermissionService.canManagePod(toUpdate);
    } catch (MissingPermissionException e) {
      log.warn("forbidden!", e);
      throw NoPermissionException.general();
    }
    profiler.start("check supported products");
    checkSupportedProducts(user.getHomeFacilityId(), podRequest);
    profiler.start("do update pod");
    updatePod(toUpdate, podRequest, user, podResponse);
    profiler.stop().log();
  }

  private void updatePod(ProofOfDelivery toUpdatePod, PodRequest podRequest, UserDto user, PodResponse existedPod) {
    Profiler profiler = new Profiler("update pod");
    profiler.setLogger(log);
    profiler.start("backUp");
    backUpIfNotMatchedExistedPod(user.getHomeFacilityId(), podRequest, existedPod);
    log.info("confirm android pod: {}", toUpdatePod);
    profiler.start("update pod");
    podRepository.updatePodById(podRequest.getDeliveredBy(), podRequest.getReceivedBy(), podRequest.getReceivedDate(),
        ProofOfDeliveryStatus.CONFIRMED.toString(), toUpdatePod.getId());
    profiler.start("delete pod lines");
    deletePodLineItems(toUpdatePod.getLineItems());
    profiler.start("delete shipment lines");
    deleteShipmentLineItems(toUpdatePod.getShipment().getLineItems());
    profiler.start("load products");
    ProductContext productContext = ContextHolder.getContext(ProductContext.class);
    profiler.start("load lots");
    LotContext lotContext = ContextHolder.getContext(LotContext.class);
    lotContext.preload(podRequest.getProducts(), PodProductLineRequest::getCode,
        p -> UUID.fromString(productContext.getProduct(p.getCode()).getTradeItemIdentifier()),
        p -> p.getLots().stream().map(this::convertLotFromRequest).collect(toList()));
    profiler.start("update stock lines");
    List<OrderableDto> requestOrderables = podRequest.getProducts()
        .stream().map(o -> productContext.getProduct(o.getCode())).collect(toList());
    updateStockCardLineItems(user.getHomeFacilityId(), podRequest, lotContext);
    profiler.start("update order");
    updateOrder(toUpdatePod, user, podRequest, requestOrderables);
    profiler.start("get home facility");
    profiler.start("load reasons");
    FacilityDto homeFacility = ContextHolder.getContext(CurrentUserContext.class).getHomeFacility();
    Map<String, UUID> reasonNameToId = validReasonAssignmentService.getAllReasons(homeFacility.getType().getId())
        .stream()
        .map(ValidReasonAssignmentDto::getReason)
        .distinct()
        .collect(toMap(StockCardLineItemReason::getName, StockCardLineItemReason::getId));
    profiler.start("insert lines");
    podNativeSqlRepository.insertPodAndShipmentLineItems(toUpdatePod.getId(), toUpdatePod.getShipment().getId(),
        podRequest.getProducts(), reasonNameToId);
    log.info("save pod syncUpHash: {}", podRequest.getSyncUpHash(user));
    profiler.start("save hash");
    syncUpHashRepository.save(new SyncUpHash(podRequest.getSyncUpHash(user)));
    profiler.stop().log();
  }

  private Lot convertLotFromRequest(PodLotLineRequest lotLine) {
    LotBasicRequest lotBasicRequest = lotLine.getLot();
    return Lot.of(lotBasicRequest.getCode(), lotBasicRequest.getExpirationDate());
  }

  private void updateStockCardLineItems(UUID facilityId, PodRequest podRequest,
      LotContext lotContext) {
    if (StringUtils.isEmpty(podRequest.getOriginNumber())) {
      return;
    }
    Set<UUID> lotIds = podRequest.getProducts().stream()
        .map(p -> p.getLots().stream()
            .map(l -> lotContext.getLot(p.getCode(), l.getLot().getCode()).getId())
            .collect(toSet()))
        .flatMap(Collection::stream)
        .collect(toSet());
    List<StockCardLineItem> stockCardLineItems = stockCardLineItemRepository
        .findByFacilityIdAndLotIdIn(facilityId, podRequest.getOriginNumber(), lotIds);
    if (stockCardLineItems.isEmpty()) {
      return;
    }
    stockCardLineItems.forEach(s -> s.setDocumentNumber(podRequest.getOrderCode()));
    if (log.isDebugEnabled()) {
      String stockCardLineItemIds = stockCardLineItems.stream()
          .map(s -> s.getId().toString())
          .collect(Collectors.joining(","));
      log.debug("update stockCardLineItems ids in : {}", stockCardLineItemIds);
    }
    stockCardLineItemRepository.save(stockCardLineItems);
  }

  private List<OrderLineItem> buildToUpdateOrderLineItems(Order order, PodRequest podRequest,
      List<OrderableDto> requestOrderables) {
    Set<UUID> existedOrderableIds = order.getOrderLineItems().stream()
        .map(o -> o.getOrderable().getId())
        .collect(toSet());
    List<OrderableDto> toUpdateOrderables = requestOrderables.stream()
        .filter(r -> !existedOrderableIds.contains(r.getId())).collect(toList());
    if (CollectionUtils.isEmpty(toUpdateOrderables)) {
      return Collections.emptyList();
    }
    return toUpdateOrderables.stream().map(t -> buildOrderLineItem(order, podRequest, t)).collect(toList());
  }

  private OrderLineItem buildOrderLineItem(Order order, PodRequest podRequest, OrderableDto orderableDto) {
    PodProductLineRequest podProductLineRequest = podRequest.getProducts().stream()
        .filter(p -> orderableDto.getProductCode().equals(p.getCode()))
        .findFirst()
        .orElse(null);
    Long orderQuantity = podProductLineRequest == null ? 0 : podProductLineRequest.getOrderedQuantity().longValue();
    OrderLineItem orderLineItem = new OrderLineItem(order, new VersionEntityReference(orderableDto.getId(),
        orderableDto.getVersionNumber()), orderQuantity);
    orderLineItem.setId(UUID.randomUUID());
    return orderLineItem;
  }

  private void checkSupportedProducts(UUID homeFacilityId, PodRequest podRequest) {
    CurrentUserContext currentUserContext = ContextHolder.getContext(CurrentUserContext.class);
    for (SupportedProgramDto supportedProgram : currentUserContext.getHomeFacilitySupportedPrograms()) {
      if (!supportedProgram.getCode().equals(podRequest.getProgramCode())) {
        continue;
      }
      UUID supportedProgramId = supportedProgram.getId();
      Set<String> approvedProductCodes =
          approvedProductDataService.getApprovedProducts(homeFacilityId, supportedProgramId)
              .stream().map(product -> product.getOrderable().getProductCode()).collect(toSet());
      Set<String> productCodesInRequest = podRequest.getProducts()
          .stream().map(PodProductLineRequest::getCode).collect(toSet());
      checkProductsAreAllSupported(productCodesInRequest, approvedProductCodes);
    }

  }

  private void checkProductsAreAllSupported(Set<String> productCodesInRequest, Set<String> supportedProductCodes) {
    Set<String> unsupportedProductCodes = productCodesInRequest.stream()
        .filter(podProductCode -> !supportedProductCodes.contains(podProductCode))
        .collect(toSet());
    if (isNotEmpty(unsupportedProductCodes)) {
      throw UnsupportedProductsException.asNormalException(unsupportedProductCodes.toArray(new String[0]));
    }
  }

  private void updateOrder(ProofOfDelivery toUpdatePod, UserDto user, PodRequest podRequest,
      List<OrderableDto> requestOrderables) {
    Order order = toUpdatePod.getShipment().getOrder();
    order.updateStatus(OrderStatus.RECEIVED, new UpdateDetails(user.getId(),
        dateHelper.getCurrentDateTimeWithSystemZone()));
    log.info("update order status, orderCode: {}", order.getOrderCode());
    List<OrderLineItem> orderLineItems = buildToUpdateOrderLineItems(order, podRequest, requestOrderables);
    if (!CollectionUtils.isEmpty(orderLineItems)) {
      log.info("update order lineItems, orderCode: {}", order.getOrderCode());
      orderLineItemRepository.save(orderLineItems);
    }
    orderRepository.save(order);
  }

  private void deletePodLineItems(List<ProofOfDeliveryLineItem> podLineItems) {
    if (CollectionUtils.isEmpty(podLineItems)) {
      return;
    }
    Set<UUID> podLineItemIds = podLineItems.stream().map(ProofOfDeliveryLineItem::getId).collect(toSet());
    log.info("delete pod line items by id: {}", podLineItemIds);
    podLineItemRepository.deletePodLineItemByIdsIn(podLineItemIds);
  }

  private void deleteShipmentLineItems(List<ShipmentLineItem> shipmentLineItems) {
    if (CollectionUtils.isEmpty(shipmentLineItems)) {
      return;
    }
    Set<UUID> shipmentLineItemIds = shipmentLineItems.stream().map(ShipmentLineItem::getId).collect(toSet());
    log.info("delete shipment line items by id: {}", shipmentLineItemIds);
    shipmentLineItemRepository.deleteShipmentLineItemByIdsIn(shipmentLineItemIds);
  }

  private void backUpIfNotMatchedExistedPod(UUID facilityId, PodRequest podRequest, PodResponse existedPod) {
    if (!isMatchedExistedPod(podRequest, existedPod)) {
      PodConfirmBackup podConfirmBackup = PodConfirmBackup.builder()
          .orderNumber(podRequest.getOrderCode())
          .facilityId(facilityId)
          .newPod(podRequest)
          .oldPod(existedPod)
          .build();
      log.info("backup not matched pod, orderCode: {}", podRequest.getOrderCode());
      podConfirmBackupRepository.save(podConfirmBackup);
    }
  }

  private boolean isMatchedExistedPod(PodRequest podRequest, PodResponse podResponse) {
    Set<String> existedMatchKeys = podResponse.getProducts().stream()
        .map(this::buildExistedMatchKeys)
        .flatMap(Collection::stream)
        .collect(toSet());
    Set<String> requestMatchKeys = podRequest.getProducts().stream()
        .map(this::buildRequestMatchKeys)
        .flatMap(Collection::stream)
        .collect(toSet());
    return existedMatchKeys.size() == requestMatchKeys.size() && existedMatchKeys.containsAll(requestMatchKeys);
  }

  private Set<String> buildExistedMatchKeys(PodProductLineResponse podProductLineResponse) {
    String productCode = podProductLineResponse.getCode();
    return podProductLineResponse.getLots().stream()
        .map(l -> {
          String lotCode = l.getLot() == null ? "" : l.getLot().getCode();
          return productCode + lotCode + l.getShippedQuantity();
        })
        .collect(toSet());
  }

  private Set<String> buildRequestMatchKeys(PodProductLineRequest podProductLineRequest) {
    String productCode = podProductLineRequest.getCode();
    return podProductLineRequest.getLots().stream()
        .map(l -> {
          String lotCode = l.getLot() == null ? "" : l.getLot().getCode();
          return productCode + lotCode + l.getShippedQuantity();
        })
        .collect(toSet());
  }
}
