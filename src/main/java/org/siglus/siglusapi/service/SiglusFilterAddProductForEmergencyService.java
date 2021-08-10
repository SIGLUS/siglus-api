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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.siglus.common.repository.OrderExternalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SiglusFilterAddProductForEmergencyService {

  @Autowired
  private OrderExternalRepository orderExternalRepository;

  @Autowired
  private OrderRepository orderRepository;

  public Set<UUID> getNotFullyShippedProducts(List<RequisitionV2Dto> previousEmergencyReqs) {
    return previousEmergencyReqs.stream()
        .filter(req -> req.getStatus() == RELEASED)
        .map(this::mapToNotFullyShippedProductIds)
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  public Set<UUID> getInProgressProducts(List<RequisitionV2Dto> previousEmergencyReqs) {
    return previousEmergencyReqs.stream()
        .filter(req -> req.getStatus().isInProgress())
        .map(RequisitionV2Dto::getLineItems)
        .flatMap(Collection::stream)
        .map(lineItem -> lineItem.getOrderableIdentity().getId())
        .collect(toSet());
  }

  private Set<UUID> mapToNotFullyShippedProductIds(RequisitionV2Dto requisition) {
    List<String> orderExternals = orderExternalRepository.findOrderExternalIdByRequisitionId(requisition.getId());
    List<UUID> externalIds = CollectionUtils.isEmpty(orderExternals)
        ? Collections.singletonList(requisition.getId())
        : orderExternals.stream().map(UUID::fromString).collect(toList());
    Order order = orderRepository.findCanFulfillOrderByExternalIdIn(externalIds);
    if (order != null) {
      return order.getOrderLineItems().stream()
          .filter(orderLineItem -> orderLineItem.getOrderedQuantity() > 0)
          .map(orderLineItem -> orderLineItem.getOrderable().getId())
          .collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

}
