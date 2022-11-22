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

package org.siglus.siglusapi.localmachine.event.proofofdelivery.andriod;

import static org.siglus.siglusapi.dto.enums.EventCategoryEnum.POD_CONFIRMED_ANDROID;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AndroidProofOfDeliverySyncedEmitter {

  private final EventPublisher eventPublisher;
  private final SiglusAuthenticationHelper authHelper;
  private final OrderExternalRepository orderExternalRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;

  public AndroidProofOfDeliverySyncedEvent emit(PodRequest request, UUID externalId, UUID supplyingFacilityId) {
    log.info("android pod externalId : {}, supplyFacilityId : {}", externalId, supplyingFacilityId);
    UserDto user = authHelper.getCurrentUser();
    AndroidProofOfDeliverySyncedEvent event = AndroidProofOfDeliverySyncedEvent.builder()
        .userId(user.getId())
        .request(request)
        .build();

    eventPublisher.emitGroupEvent(getGroupId(externalId), supplyingFacilityId, event, POD_CONFIRMED_ANDROID);
    log.info("android pod event : {}", event);
    return event;
  }

  private String getGroupId(UUID externalId) {
    List<OrderExternal> orderExternal = orderExternalRepository.findByIdIn(Collections.singleton(externalId));
    UUID requisitionId = orderExternal.isEmpty() ? externalId : orderExternal.get(0).getRequisitionId();
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
    log.info("android pod getGroupId : {}", requisitionExtension.getRequisitionNumberPrefix()
        + requisitionExtension.getRequisitionNumber());
    return requisitionExtension.getRealRequisitionNumber();
  }
}
