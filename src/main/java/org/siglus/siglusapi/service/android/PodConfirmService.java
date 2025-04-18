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
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
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
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.android.context.ContextHolder;
import org.siglus.siglusapi.service.android.context.CurrentUserContext;
import org.siglus.siglusapi.service.android.context.LotContext;
import org.siglus.siglusapi.service.android.context.ProductContext;
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
  private static final String REJECT_REASON_INSUFFICIENT_2_0_18 = "Recebido a menos";

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
  private final RequisitionService requisitionService;
  private final PodConfirmBackupRepository podConfirmBackupRepository;
  private final SiglusNotificationService siglusNotificationService;

  @Transactional
  @SuppressWarnings("PMD.PreserveStackTrace")
  public void confirmPod(PodRequest podRequest, ProofOfDelivery toUpdate, PodResponse podResponse) {
    UserDto user = ContextHolder.getContext(CurrentUserContext.class).getCurrentUser();
    String syncUpHash = podRequest.getSyncUpHash(user);
    if (syncUpHashRepository.findOne(syncUpHash) != null) {
      log.info("skip confirm pod as syncUpHash: {} existed", syncUpHash);
      return;
    }
    if (toUpdate.isConfirmed()) {
      log.warn("pod orderCode: {} has been confirmed:", podRequest.getOrderCode());
      return;
    }
    try {
      fulfillmentPermissionService.canManagePod(toUpdate);
    } catch (MissingPermissionException e) {
      log.warn("forbidden!", e);
      throw NoPermissionException.general();
    }
    checkSupportedProducts(user.getHomeFacilityId(), podRequest);
    Order order = updatePod(toUpdate, podRequest, user, podResponse);
    log.info("generate notification for confirm pod: {}", podRequest.getOrderCode());
    siglusNotificationService.postConfirmPod(toUpdate.getId(), order);
  }

  private Order updatePod(ProofOfDelivery toUpdatePod, PodRequest podRequest, UserDto user, PodResponse existedPod) {
    backUpIfNotMatchedExistedPod(user.getHomeFacilityId(), podRequest, existedPod);
    log.info("confirm android pod: {}", toUpdatePod);
    podRepository.updatePodById(podRequest.getDeliveredBy(), podRequest.getReceivedBy(), podRequest.getReceivedDate(),
        ProofOfDeliveryStatus.CONFIRMED.toString(), toUpdatePod.getId());
    deletePodLineItems(toUpdatePod.getLineItems());
    deleteShipmentLineItems(toUpdatePod.getShipment().getLineItems());
    ProductContext productContext = ContextHolder.getContext(ProductContext.class);
    LotContext lotContext = ContextHolder.getContext(LotContext.class);
    lotContext.preload(podRequest.getProducts(), PodProductLineRequest::getCode,
        p -> UUID.fromString(productContext.getProduct(p.getCode()).getTradeItemIdentifier()),
        p -> p.getLots().stream()
            .filter(l -> l.getLot() != null)
            .map(this::convertLotFromRequest)
            .collect(toList()));
    updateStockCardLineItems(user.getHomeFacilityId(), podRequest);
    FacilityDto homeFacility = ContextHolder.getContext(CurrentUserContext.class).getHomeFacility();
    Map<String, UUID> reasonNameToId = validReasonAssignmentService.getAllReasons(homeFacility.getType().getId())
        .stream()
        .map(ValidReasonAssignmentDto::getReason)
        .filter(reason -> !REJECT_REASON_INSUFFICIENT_2_0_18.equals(reason.getName()))
        .distinct()
        .collect(toMap(StockCardLineItemReason::getName, StockCardLineItemReason::getId));
    podNativeSqlRepository.insertPodAndShipmentLineItems(toUpdatePod.getId(), toUpdatePod.getShipment().getId(),
        podRequest.getProducts(), reasonNameToId);
    log.info("save pod syncUpHash: {}", podRequest.getSyncUpHash(user));
    SyncUpHash syncUpHashDomain = SyncUpHash.builder()
        .hash(podRequest.getSyncUpHash(user))
        .type("POD")
        .referenceId(toUpdatePod.getId())
        .build();
    syncUpHashRepository.save(syncUpHashDomain);
    List<OrderableDto> requestOrderables = podRequest.getProducts()
        .stream().map(o -> productContext.getProduct(o.getCode())).collect(toList());
    return updateOrder(toUpdatePod, user, podRequest, requestOrderables);
  }

  private Lot convertLotFromRequest(PodLotLineRequest lotLine) {
    LotBasicRequest lotBasicRequest = lotLine.getLot();
    return Lot.of(lotBasicRequest.getCode(), lotBasicRequest.getExpirationDate());
  }

  private void updateStockCardLineItems(UUID facilityId, PodRequest podRequest) {
    if (StringUtils.isEmpty(podRequest.getOriginNumber())) {
      return;
    }
    List<StockCardLineItem> stockCardLineItems = stockCardLineItemRepository
        .findByFacilityIdAndLotIdIn(facilityId, podRequest.getOriginNumber());
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
          requisitionService.getApprovedProductsWithoutAdditional(homeFacilityId, supportedProgramId)
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

  private Order updateOrder(ProofOfDelivery toUpdatePod, UserDto user, PodRequest podRequest,
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
    return orderRepository.save(order);
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
