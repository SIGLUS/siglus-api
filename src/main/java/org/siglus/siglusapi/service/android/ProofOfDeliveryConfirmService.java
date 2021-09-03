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

import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.service.FulfillmentPermissionService;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.domain.SyncUpHash;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.dto.android.response.PodResponse;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
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

  @Transactional
  public PodResponse confirmProofsOfDelivery(@Valid PodRequest podRequest) {
    UserDto user = authHelper.getCurrentUser();
    String syncUpHash = podRequest.getSyncUpHash(user);
    ProofOfDelivery pod = podRepository.findByOrderCode(podRequest.getOrder().getCode());
    if (syncUpHashRepository.findOne(syncUpHash) != null) {
      log.info("skip confirm ProofsOfDelivery as syncUpHash: {} existed", syncUpHash);
      return toPodResponse(pod);
    }
    return toPodResponse(updatePod(pod, podRequest, syncUpHash));
  }

  private ProofOfDelivery updatePod(ProofOfDelivery toUpdate, PodRequest podRequest, String syncUpHash) {
    fulfillmentPermissionService.canManagePod(toUpdate);
    log.info("confirm android proofOfDelivery: {}", toUpdate);
    ProofOfDelivery updatedPdo = podRepository.save(toUpdate);
    log.info("save ProofsOfDelivery syncUpHash: {}", syncUpHash);
    syncUpHashRepository.save(new SyncUpHash(syncUpHash));
    return updatedPdo;
  }

  private PodResponse toPodResponse(ProofOfDelivery proofOfDelivery) {
    return new PodResponse();
  }
}
