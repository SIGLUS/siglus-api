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

package org.openlmis.requisition.service;

import static org.apache.commons.lang.BooleanUtils.isTrue;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.service.fulfillment.ProofOfDeliveryFulfillmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProofOfDeliveryService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProofOfDeliveryService.class);

  @Autowired
  private OrderFulfillmentService orderFulfillmentService;

  @Autowired
  private ProofOfDeliveryFulfillmentService proofOfDeliveryFulfillmentService;


  ProofOfDeliveryDto get(Requisition requisition) {
    Profiler profiler = new Profiler("POD_SERVICE_GET");
    profiler.setLogger(LOGGER);

    if (RequisitionStatus.SKIPPED == requisition.getStatus()) {
      return null;
    }

    if (isTrue(requisition.getEmergency())) {
      return null;
    }

    profiler.start("SEARCH_ORDERS");
    List<OrderDto> orders = orderFulfillmentService.search(
        null, requisition.getFacilityId(), requisition.getProgramId(),
        requisition.getProcessingPeriodId(), null);

    if (isEmpty(orders)) {
      return null;
    }

    profiler.start("SEARCH_PODS");
    List<ProofOfDeliveryDto> pods = proofOfDeliveryFulfillmentService
        .getProofOfDeliveries(orders.get(0).getId());

    ProofOfDeliveryDto pod = isEmpty(pods) ? null : pods.get(0);

    profiler.stop().log();

    return pod;
  }
}
