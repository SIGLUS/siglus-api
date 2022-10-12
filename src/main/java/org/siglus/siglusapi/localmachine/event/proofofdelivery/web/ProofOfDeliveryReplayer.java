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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.repository.OrdersRepository;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProofOfDeliveryReplayer {

  private final ProofOfDeliveryRepository proofOfDeliveryRepository;
  private final PodExtensionRepository podExtensionRepository;
  private final OrdersRepository ordersRepository;

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

  public void doReplay(ProofOfDeliveryEvent event) {

    ProofOfDelivery proofOfDelivery = event.getProofOfDelivery();
    System.out.println("pod = " + proofOfDelivery);
    proofOfDeliveryRepository.saveAndFlush(proofOfDelivery);
    log.info("proofOfDelivery has been saved! id = {}", event.getProofOfDelivery().getId());
    PodExtension proofsOfDeliveryExtension = event.getPodExtension();
    podExtensionRepository.saveAndFlush(proofsOfDeliveryExtension);
    log.info("ProofsOfDeliveryExtension has been saved! ProofOfDelivery id = {}", event.getProofOfDelivery().getId());

    Shipment shipment = proofOfDelivery.getShipment();
    String orderCode = shipment.getOrder().getOrderCode();
    Order order = ordersRepository.findByOrderCode(orderCode);
    order.setStatus(shipment.getOrder().getStatus());

  }

}
