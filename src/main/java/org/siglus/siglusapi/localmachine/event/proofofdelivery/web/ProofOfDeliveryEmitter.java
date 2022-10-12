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

package org.siglus.siglusapi.localmachine.event.proofofdelivery.web;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProofOfDeliveryEmitter {

  private final EventPublisher eventPublisher;
  private final ProofOfDeliveryRepository proofOfDeliveryRepository;
  private final PodExtensionRepository podExtensionRepository;
  private final OrderExternalRepository orderExternalRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;

  public ProofOfDeliveryEvent emit(UUID podId) {
    ProofOfDeliveryEvent event = getEvent(podId);
    System.out.println("groupid = " + getGroupId(event));
    System.out.println("facilityid = " + event.getProofOfDelivery().getSupplyingFacilityId());
    eventPublisher.emitGroupEvent(getGroupId(event), event.getProofOfDelivery().getSupplyingFacilityId(), event);
    return event;
  }


  public ProofOfDeliveryEvent getEvent(UUID podId) {
    ProofOfDeliveryEvent event = new ProofOfDeliveryEvent();
    ProofOfDelivery proofOfDelivery = proofOfDeliveryRepository.findOne(podId);
    if (proofOfDelivery == null) {
      throw new IllegalStateException("no proofOfDelivery found, id = " + podId);
    }
    event.setProofOfDelivery(proofOfDelivery);
    PodExtension podExtension = podExtensionRepository.findByPodId(podId);
    event.setPodExtension(podExtension);
    return event;
  }

  private String getGroupId(ProofOfDeliveryEvent event) {
    UUID externalId = event.getProofOfDelivery().getShipment().getOrder().getExternalId();
    List<OrderExternal> orderExternal = orderExternalRepository.findByIdIn(Collections.singleton(externalId));
    UUID requisitionId = orderExternal.isEmpty() ? externalId : orderExternal.get(0).getRequisitionId();
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
    return requisitionExtension.getRequisitionNumberPrefix() + requisitionExtension.getRequisitionNumber();
  }

}
