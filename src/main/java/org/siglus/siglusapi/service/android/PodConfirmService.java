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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.openlmis.fulfillment.i18n.MessageKeys.PROOF_OF_DELIVERY_ALREADY_CONFIRMED;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.Valid;
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
import org.openlmis.fulfillment.web.NotFoundException;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.service.referencedata.LotReferenceDataService;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.domain.SyncUpHash;
import org.siglus.siglusapi.dto.android.request.PodProductLineRequest;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.repository.OrderLineItemRepository;
import org.siglus.siglusapi.repository.PodNativeSqlRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryLineItemRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusShipmentLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class PodConfirmService {

  private final SyncUpHashRepository syncUpHashRepository;
  private final SiglusProofOfDeliveryRepository podRepository;
  private final PermissionService fulfillmentPermissionService;
  private final SiglusOrderableService orderableService;
  private final LotReferenceDataService lotService;
  private final SiglusProofOfDeliveryLineItemRepository podLineItemRepository;
  private final SiglusValidReasonAssignmentService validReasonAssignmentService;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final DateHelper dateHelper;
  private final OrderRepository orderRepository;
  private final PodNativeSqlRepository podNativeSqlRepository;
  private final SiglusShipmentLineItemRepository shipmentLineItemRepository;
  private final OrderLineItemRepository orderLineItemRepository;
  private final SiglusStockCardLineItemRepository stockCardLineItemRepository;
  private final SiglusApprovedProductReferenceDataService approvedProductReferenceDataService;
  private final SiglusProgramService siglusProgramService;

  @Transactional
  public void confirmPod(@Valid PodRequest podRequest, ProofOfDelivery toUpdate, UserDto user) {
    String syncUpHash = podRequest.getSyncUpHash(user);
    if (syncUpHashRepository.findOne(syncUpHash) != null) {
      log.info("skip confirm ProofsOfDelivery as syncUpHash: {} existed", syncUpHash);
      return;
    }
    if (toUpdate.isConfirmed()) {
      log.warn("ProofsOfDelivery orderCode: {} has been confirmed:", podRequest.getOrderCode());
      throw new ValidationException(PROOF_OF_DELIVERY_ALREADY_CONFIRMED);
    }
    fulfillmentPermissionService.canManagePod(toUpdate);
    if (!CollectionUtils.isEmpty(unSupportProductCodes(user.getHomeFacilityId(), podRequest))) {
      throw new NotFoundException("contain unsupported products");
    }
    updatePod(toUpdate, podRequest, user);
  }

  private void updatePod(ProofOfDelivery toUpdatePod, PodRequest podRequest, UserDto user) {
    log.info("confirm android proofOfDelivery: {}", toUpdatePod);
    podRepository.updatePodById(podRequest.getDeliveredBy(), podRequest.getReceivedBy(), podRequest.getReceivedDate(),
        ProofOfDeliveryStatus.CONFIRMED.toString(), toUpdatePod.getId());
    deletePodLineItems(toUpdatePod.getLineItems());
    deleteShipmentLineItems(toUpdatePod.getShipment().getLineItems());
    List<OrderableDto> requestOrderables = podRequest.getProducts().stream()
        .map(o -> orderableService.getOrderableByCode(o.getCode()))
        .collect(Collectors.toList());
    Map<String, String> orderableCodeToTradeItemId = requestOrderables.stream()
        .collect(Collectors.toMap(OrderableDto::getProductCode, OrderableDto::getTradeItemIdentifier));
    Map<String, Map<String, UUID>> orderableCodeToLots = orderableCodeToTradeItemId.entrySet().stream()
        .collect(toMap(Entry::getKey, e -> buildLotCodeToId(e.getValue())));
    Map<String, OrderableDto> orderableCodeToOrderable = requestOrderables.stream()
        .collect(Collectors.toMap(OrderableDto::getProductCode, Function.identity()));
    updateStockCardLineItems(user.getHomeFacilityId(), podRequest, orderableCodeToLots);
    updateOrder(toUpdatePod, user, podRequest, requestOrderables);
    FacilityDto homeFacility = facilityReferenceDataService.getFacilityById(user.getHomeFacilityId());
    Map<String, UUID> reasonNameToId = validReasonAssignmentService
        .getValidReasonsForAllProducts(homeFacility.getType().getId(), null, null)
        .stream()
        .map(ValidReasonAssignmentDto::getReason)
        .distinct()
        .collect(toMap(StockCardLineItemReason::getName, StockCardLineItemReason::getId));
    podNativeSqlRepository.insertPodAndShipmentLineItems(toUpdatePod.getId(), toUpdatePod.getShipment().getId(),
        user.getHomeFacilityId(), podRequest.getProducts(), orderableCodeToOrderable, reasonNameToId);
    log.info("save pod syncUpHash: {}", podRequest.getSyncUpHash(user));
    syncUpHashRepository.save(new SyncUpHash(podRequest.getSyncUpHash(user)));
  }

  private void updateStockCardLineItems(UUID facilityId, PodRequest podRequest,
      Map<String, Map<String, UUID>> orderableCodeToLots) {
    if (StringUtils.isEmpty(podRequest.getOriginNumber())) {
      return;
    }
    Set<UUID> lotIds = podRequest.getProducts().stream()
        .map(p -> p.getLots().stream()
            .map(l -> orderableCodeToLots.get(p.getCode()).get(l.getLot().getCode()))
            .collect(Collectors.toSet()))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
    List<StockCardLineItem> stockCardLineItems = stockCardLineItemRepository
        .findByFacilityIdAndLotIdIn(facilityId, lotIds);
    if (stockCardLineItems.isEmpty()) {
      return;
    }
    stockCardLineItems.forEach(s -> s.setDocumentNumber(podRequest.getOrderCode()));
    String stockCardLineItemIds = stockCardLineItems.stream()
        .map(s -> s.getId().toString())
        .collect(Collectors.joining(","));
    log.info("update stockCardLineItems ids in : {}", stockCardLineItemIds);
    stockCardLineItemRepository.save(stockCardLineItems);
  }

  private Map<String, UUID> buildLotCodeToId(String tradeItemId) {
    return lotService.getAllLotsOf(UUID.fromString(tradeItemId)).stream()
        .collect(toMap(LotDto::getLotCode, LotDto::getId));
  }

  private List<OrderLineItem> buildToUpdateOrderLineItems(Order order, PodRequest podRequest,
      List<OrderableDto> requestOrderables) {
    Set<UUID> existedOrderableIds = order.getOrderLineItems().stream()
        .map(o -> o.getOrderable().getId())
        .collect(Collectors.toSet());
    List<OrderableDto> toUpdateOrderables = requestOrderables.stream()
        .filter(r -> !existedOrderableIds.contains(r.getId())).collect(Collectors.toList());
    if (CollectionUtils.isEmpty(toUpdateOrderables)) {
      return Collections.emptyList();
    }
    return toUpdateOrderables.stream().map(t -> buildOrderLineItem(order, podRequest, t)).collect(Collectors.toList());
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

  private Set<String> unSupportProductCodes(UUID facilityId, PodRequest podRequest) {
    UUID programId = siglusProgramService.getProgramIdByCode(podRequest.getProgramCode());
    Set<String> approvedProductCodes = approvedProductReferenceDataService.getApprovedProducts(
        facilityId, programId, emptyList())
        .stream()
        .map(p -> p.getOrderable().getProductCode())
        .collect(Collectors.toSet());
    return podRequest.getProducts().stream()
        .map(PodProductLineRequest::getCode)
        .filter(code -> !approvedProductCodes.contains(code))
        .collect(Collectors.toSet());
  }

  private void updateOrder(ProofOfDelivery toUpdatePod, UserDto user, PodRequest podRequest,
      List<OrderableDto> requestOrderables) {
    Order order = toUpdatePod.getShipment().getOrder();
    order.updateStatus(OrderStatus.RECEIVED, new UpdateDetails(user.getId(),
        dateHelper.getCurrentDateTimeWithSystemZone()));
    log.info("update order status, order code: {}", order.getOrderCode());
    List<OrderLineItem> orderLineItems = buildToUpdateOrderLineItems(order, podRequest, requestOrderables);
    if (!CollectionUtils.isEmpty(orderLineItems)) {
      log.info("update order lineItems, order code: {}", order.getOrderCode());
      orderLineItemRepository.save(orderLineItems);
    }
    orderRepository.save(order);
  }

  private void deletePodLineItems(List<ProofOfDeliveryLineItem> podLineItems) {
    if (StringUtils.isEmpty(podLineItems)) {
      return;
    }
    Set<UUID> podLineItemIds = podLineItems.stream().map(ProofOfDeliveryLineItem::getId).collect(Collectors.toSet());
    log.info("delete pod line items by id: {}", podLineItemIds);
    podLineItemRepository.deletePodLineItemByIdsIn(podLineItemIds);
  }

  private void deleteShipmentLineItems(List<ShipmentLineItem> shipmentLineItems) {
    if (StringUtils.isEmpty(shipmentLineItems)) {
      return;
    }
    Set<UUID> shipmentLineItemIds = shipmentLineItems.stream().map(ShipmentLineItem::getId).collect(Collectors.toSet());
    log.info("delete shipment line items by id: {}", shipmentLineItemIds);
    shipmentLineItemRepository.deleteShipmentLineItemByIdsIn(shipmentLineItemIds);
  }
}
