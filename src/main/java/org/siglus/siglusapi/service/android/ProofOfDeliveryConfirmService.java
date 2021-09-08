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

import static java.util.stream.Collectors.toMap;
import static org.openlmis.fulfillment.i18n.MessageKeys.PROOF_OF_DELIVERY_ALREADY_CONFIRMED;
import static org.siglus.siglusapi.i18n.ProofOfDeliveryMessageKeys.PROOFSOFDELIVERY_NOT_FOUND;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.domain.UpdateDetails;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.FulfillmentPermissionService;
import org.openlmis.fulfillment.util.FulfillmentDateHelper;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.openlmis.stockmanagement.dto.referencedata.LotDto;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.openlmis.stockmanagement.service.referencedata.StockmanagementLotReferenceDataService;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.constant.PodConstants;
import org.siglus.siglusapi.domain.ProofsOfDeliveryRequestBackup;
import org.siglus.siglusapi.domain.SyncUpHash;
import org.siglus.siglusapi.dto.android.request.PodProductLineRequest;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.dto.android.response.ConfirmPodResponse;
import org.siglus.siglusapi.dto.android.response.PodResponse;
import org.siglus.siglusapi.repository.OrderLineItemRepository;
import org.siglus.siglusapi.repository.PodNativeSqlRepository;
import org.siglus.siglusapi.repository.PodRequestBackupRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryLineItemRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusShipmentLineItemRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.util.HashEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProofOfDeliveryConfirmService {

  private final SyncUpHashRepository syncUpHashRepository;
  private final SiglusAuthenticationHelper authHelper;
  private final SiglusProofOfDeliveryRepository podRepository;
  private final FulfillmentPermissionService fulfillmentPermissionService;
  private final SiglusOrderableService orderableService;
  private final StockmanagementLotReferenceDataService lotService;
  private final SiglusProofOfDeliveryLineItemRepository podLineItemRepository;
  private final SiglusValidReasonAssignmentService validReasonAssignmentService;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final FulfillmentDateHelper dateHelper;
  private final OrderRepository orderRepository;
  private final PodRequestBackupRepository podBackupRepository;
  private final PodNativeSqlRepository podNativeSqlRepository;
  private final SiglusShipmentLineItemRepository shipmentLineItemRepository;
  private final OrderLineItemRepository orderLineItemRepository;
  private final StockCardLineItemRepository stockCardLineItemRepository;

  @Transactional
  public ConfirmPodResponse confirmProofsOfDelivery(@Valid PodRequest podRequest) {
    UserDto user = authHelper.getCurrentUser();
    ProofOfDelivery toUpdate = podRepository.findInitiatedPodByOrderNumber(podRequest.getOrderCode());
    if (toUpdate == null) {
      log.warn("ProofsOfDelivery orderCode: {} not found:", podRequest.getOrderCode());
      backupPodRequest(podRequest, PodConstants.ERROR_MESSAGE, user);
      throw new ValidationMessageException(PROOFSOFDELIVERY_NOT_FOUND, podRequest.getOrderCode());
    }
    if (toUpdate.isConfirmed()) {
      log.warn("ProofsOfDelivery orderCode: {} has been confirmed:", podRequest.getOrderCode());
      throw new ValidationException(PROOF_OF_DELIVERY_ALREADY_CONFIRMED, podRequest.getOrderCode());
    }
    String syncUpHash = podRequest.getSyncUpHash(user);
    if (syncUpHashRepository.findOne(syncUpHash) != null) {
      log.info("skip confirm ProofsOfDelivery as syncUpHash: {} existed", syncUpHash);
      throw new ValidationMessageException(PROOF_OF_DELIVERY_ALREADY_CONFIRMED);
    }
    if (StringUtils.isEmpty(podRequest.getOriginNumber())) {
      backupPodRequest(podRequest, "", user);
      // StockCardLineItem stockCardLineItem = new StockCardLineItem();
      // stockCardLineItemRepository.findOne();
    }
    fulfillmentPermissionService.canManagePod(toUpdate);
    updatePod(toUpdate, podRequest, user);
    return ConfirmPodResponse.builder()
        .status(HttpStatus.OK.value())
        .orderNumber(podRequest.getOrderCode())
        .podResponse(new PodResponse())
        .build();
  }

  private void updatePod(ProofOfDelivery toUpdatePod, PodRequest podRequest, UserDto user) {
    FacilityDto homeFacility = facilityReferenceDataService.getFacilityById(user.getHomeFacilityId());
    log.info("confirm android proofOfDelivery: {}", toUpdatePod);
    podRepository.updatePodById(podRequest.getDeliveredBy(), podRequest.getReceivedBy(), podRequest.getReceivedDate(),
        ProofOfDeliveryStatus.CONFIRMED, toUpdatePod.getId());

    List<OrderableDto> requestOrderables = podRequest.getProducts().stream()
        .map(o -> orderableService.getOrderableByCode(o.getCode()))
        .collect(Collectors.toList());
    Map<String ,OrderableDto> orderableCodeToOrderable = requestOrderables.stream()
        .collect(Collectors.toMap(OrderableDto::getProductCode, Function.identity()));
    Map<String, String> orderableCodeToTradeItemId = requestOrderables.stream()
        .collect(Collectors.toMap(OrderableDto::getProductCode, OrderableDto::getTradeItemIdentifier));
    Map<String, Map<String, UUID>> orderableCodeToLots = orderableCodeToTradeItemId.entrySet().stream()
        .collect(toMap(e -> e.getKey(), e -> buildLotCodeToId(e.getValue())));
    Map<String, UUID> reasonNameToId = validReasonAssignmentService
        .getValidReasonsForAllProducts(homeFacility.getType().getId(), null, null).stream()
        .collect(toMap(r -> r.getReason().getName(), ValidReasonAssignmentDto::getId));

    log.info("delete pod line items by pod id: {}", toUpdatePod.getId());
    podLineItemRepository.delete(toUpdatePod.getLineItems());
    log.info("delete shipment line items by pod id: {}", toUpdatePod.getId());
    shipmentLineItemRepository.delete(toUpdatePod.getShipment().getLineItems());
    podNativeSqlRepository.insertPodAndShipmentLineItems(
        toUpdatePod.getId(), toUpdatePod.getShipment().getId(), podRequest.getProducts(), orderableCodeToOrderable,
            orderableCodeToLots, reasonNameToId);
    Order order = toUpdatePod.getShipment().getOrder();
    order.updateStatus(OrderStatus.RECEIVED, new UpdateDetails(
        user.getId(), dateHelper.getCurrentDateTimeWithSystemZone()));
    log.info("update order status, order code: {}", order.getOrderCode());
    orderRepository.save(order);
    List<OrderLineItem> orderLineItems = buildToUpdateOrderLineItems(order, podRequest, requestOrderables);
    if (!CollectionUtils.isEmpty(orderLineItems)) {
      log.info("update order lineItems, order code: {}", order.getOrderCode());
      orderLineItemRepository.save(orderLineItems);
    }
    log.info("save ProofsOfDelivery syncUpHash: {}", podRequest.getSyncUpHash(user));
    syncUpHashRepository.save(new SyncUpHash(podRequest.getSyncUpHash(user)));
  }

  public void backupPodRequest(PodRequest podRequest, String errorMessage, UserDto user) {
    String syncUpHash = HashEncoder.hash(podRequest.getOrderCode() + user.getHomeFacilityId() + user.getId());
    ProofsOfDeliveryRequestBackup existedBackup = podBackupRepository.findOneByHash(syncUpHash);
    if (existedBackup != null) {
      log.info("skip backup pod request as syncUpHash: {} existed", syncUpHash);
      return;
    }
    ProofsOfDeliveryRequestBackup backup = ProofsOfDeliveryRequestBackup.builder()
        .hash(syncUpHash)
        .facilityId(user.getHomeFacilityId())
        .userId(user.getId())
        .errorMessage(errorMessage)
        .requestBody(podRequest)
        .build();
    log.info("backup proofOfDelivery request, syncUpHash: {}", syncUpHash);
    podBackupRepository.save(backup);
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
    List<OrderLineItem> toUpdateOrderLineItems = toUpdateOrderables.stream()
        .map(t -> buildOrderLineItem(order, podRequest, t))
        .collect(Collectors.toList());
    return toUpdateOrderLineItems;
  }

  private OrderLineItem buildOrderLineItem(Order order, PodRequest podRequest, OrderableDto orderableDto) {
    PodProductLineRequest podProductLineRequest = podRequest.getProducts().stream()
        .filter(p -> orderableDto.getProductCode().equals(p.getCode()))
        .findFirst()
        .orElse(null);
    OrderLineItem orderLineItem = new OrderLineItem(order, new VersionEntityReference(orderableDto.getId(),
        orderableDto.getVersionNumber()), podProductLineRequest.getOrderedQuantity().longValue());
    orderLineItem.setId(UUID.randomUUID());
    return orderLineItem;
  }
}
