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

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.PodLineItemsByLocation;
import org.siglus.siglusapi.localmachine.event.NotificationService;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsByLocationRepository;
import org.siglus.siglusapi.repository.SiglusOrdersRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusShipmentRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProofOfDeliveryReplayer {

  private final ProofOfDeliveryRepository proofOfDeliveryRepository;
  private final SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;
  private final PodExtensionRepository podExtensionRepository;
  private final SiglusOrdersRepository siglusOrdersRepository;
  private final PodLineItemsByLocationRepository podLineItemsByLocationRepository;
  private final NotificationService notificationService;
  private final SiglusShipmentRepository siglusShipmentRepository;

  @Transactional
  @EventListener(classes = {ProofOfDeliveryEvent.class})
  public void replay(ProofOfDeliveryEvent event) {
    try {
      log.info("start replay podId = " + event.getProofOfDelivery().getId());
      doReplay(event);
      log.info("end replay podId = " + event.getProofOfDelivery().getId());
    } catch (Exception e) {
      log.error("fail to save ProofOfDelivery Event internal approve event, msg = " + e.getMessage(), e);
      throw e;
    }
  }

  private void doReplay(ProofOfDeliveryEvent event) {
    ProofOfDelivery proofOfDelivery = event.getProofOfDelivery();
    Shipment shipment = proofOfDelivery.getShipment();
    Order order = shipment.getOrder();
    Order orderByOrderCode = siglusOrdersRepository.findByOrderCode(order.getOrderCode());
    Shipment shipmentByOrderId = siglusShipmentRepository.findShipmentByOrderId(orderByOrderCode.getId());

    // use the db pod id rather than event pod id
    ProofOfDelivery existedPod = siglusProofOfDeliveryRepository.findByShipmentId(shipmentByOrderId.getId());
    proofOfDelivery.setId(existedPod.getId());

    proofOfDelivery.setShipment(shipmentByOrderId);
    log.info("save proofOfDelivery, id = {}", proofOfDelivery.getId());
    proofOfDeliveryRepository.saveAndFlush(proofOfDelivery);

    PodExtension podExtension = event.getPodExtension();
    if (Objects.nonNull(podExtension)) {
      log.info("save podExtension, pod id = {}", event.getProofOfDelivery().getId());
      podExtensionRepository.saveAndFlush(podExtension);
    }

    orderByOrderCode.setStatus(order.getStatus());

    List<PodLineItemsByLocation> podLineItemsByLocations = event.getPodLineItemsByLocation();
    if (Objects.nonNull(podLineItemsByLocations) && !podLineItemsByLocations.isEmpty()) {
      podLineItemsByLocationRepository.save(podLineItemsByLocations);
    }
    notificationService.postConfirmPod(event.getUserId(), proofOfDelivery.getId(), orderByOrderCode);
  }

}
