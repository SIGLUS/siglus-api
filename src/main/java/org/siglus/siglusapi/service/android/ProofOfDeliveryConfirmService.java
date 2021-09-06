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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.openlmis.fulfillment.service.FulfillmentPermissionService;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.openlmis.stockmanagement.service.referencedata.StockmanagementLotReferenceDataService;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.constant.PodConstants;
import org.siglus.siglusapi.domain.SyncUpHash;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.dto.android.response.ConfirmPodResponse;
import org.siglus.siglusapi.dto.android.response.PodResponse;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryLineItemRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final ProofOfDeliveryRepository proofOfDeliveryRepository;
  private final SiglusProofOfDeliveryLineItemRepository podLineItemRepository;
  private final SiglusValidReasonAssignmentService validReasonAssignmentService;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Transactional
  public ConfirmPodResponse confirmProofsOfDelivery(@Valid PodRequest podRequest) {
    UserDto user = authHelper.getCurrentUser();
    FacilityDto homeFacility = facilityReferenceDataService.getFacilityById(user.getHomeFacilityId());
    String syncUpHash = podRequest.getSyncUpHash(user);
    ProofOfDelivery pod = podRepository.findInitiatedPodByOrderNumber(podRequest.getOrder().getCode());
    if (pod == null) {
      return ConfirmPodResponse.builder()
          .status(HttpStatus.NOT_FOUND.value())
          .orderNumber(podRequest.getOrder().getCode())
          .message(PodConstants.NOT_EXIST_MESSAGE)
          .messageInPortuguese(PodConstants.NOT_EXIST_MESSAGE_PT)
          .build();
    }
    if (syncUpHashRepository.findOne(syncUpHash) != null) {
      log.info("skip confirm ProofsOfDelivery as syncUpHash: {} existed", syncUpHash);
      return ConfirmPodResponse.builder()
          .status(HttpStatus.OK.value())
          .orderNumber(podRequest.getOrder().getCode())
          .podResponse(new PodResponse())
          .build();
    }
    fulfillmentPermissionService.canManagePod(pod);
    updatePod(pod, podRequest, syncUpHash);
    updatePodLineItems(pod, podRequest, homeFacility);
    return ConfirmPodResponse.builder()
        .status(HttpStatus.OK.value())
        .orderNumber(podRequest.getOrder().getCode())
        .podResponse(new PodResponse())
        .build();
  }

  private void updatePod(ProofOfDelivery toUpdatePod, PodRequest podRequest, String syncUpHash) {
    log.info("confirm android proofOfDelivery: {}", toUpdatePod);
    podRepository.updatePodById(podRequest.getDeliveredBy(), podRequest.getReceivedBy(), podRequest.getReceivedDate(),
        ProofOfDeliveryStatus.CONFIRMED, toUpdatePod.getId());
    log.info("save ProofsOfDelivery syncUpHash: {}", syncUpHash);
    syncUpHashRepository.save(new SyncUpHash(syncUpHash));
  }

  private void updatePodLineItems(ProofOfDelivery toUpdatePod, PodRequest podRequest, FacilityDto homeFacility) {
    Map<String, String> orderableCodeToTradeItemId = podRequest.getProducts().stream()
        .map(o -> orderableService.getOrderableByCode(o.getCode()))
        .collect(Collectors.toMap(OrderableDto::getProductCode, OrderableDto::getTradeItemIdentifier));
    Map<UUID, String> reasonNamesById = validReasonAssignmentService
        .getValidReasonsForAllProducts(homeFacility.getType().getId(), null, null).stream()
        .collect(toMap(ValidReasonAssignmentDto::getId, r -> r.getReason().getName()));

    List<ProofOfDeliveryLineItem> podLineItems = podLineItemRepository.findByProofOfDeliveryId(toUpdatePod.getId());
    log.info("delete pod line items by pod id: {}", toUpdatePod.getId());
    podLineItemRepository.delete(podLineItems);
    // insert podLineItems
  }
}
