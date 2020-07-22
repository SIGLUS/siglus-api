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

package org.siglus.siglusapi.service;

import java.util.Set;
import java.util.UUID;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.service.client.SiglusProofOfDeliveryFulfillmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SiglusProofOfDeliveryService {

  @Autowired
  private SiglusProofOfDeliveryFulfillmentService fulfillmentService;

  @Autowired
  private SiglusOrderService siglusOrderService;

  @Autowired
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Autowired
  private OrderExternalRepository orderExternalRepository;

  public ProofOfDeliveryDto getProofOfDelivery(UUID id,
      Set<String> expand) {
    ProofOfDeliveryDto proofOfDeliveryDto = fulfillmentService.searchProofOfDelivery(id, expand);
    OrderObjectReferenceDto order = proofOfDeliveryDto.getShipment().getOrder();
    OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
    UUID requisitionId = external == null ? order.getExternalId() : external.getRequisitionId();
    order.setRequisitionNumber(
        siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId));
    if (CollectionUtils.isNotEmpty(order.getOrderLineItems())) {
      proofOfDeliveryDto.getShipment().setOrder(siglusOrderService.getExtensionOrder(order));
    }
    return proofOfDeliveryDto;
  }

}
