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

package org.siglus.siglusapi.localmachine.event.order.release;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderReleaseEmitter {

  private final EventPublisher eventPublisher;

  private final RequisitionExtensionRepository requisitionExtensionRepository;

  private final RequisitionRepository requisitionRepository;

  public OrderReleaseEvent emit(ReleasableRequisitionDto releasableRequisitionDto, UUID authorId) {
    OrderReleaseEvent orderReleaseEvent = getEvent(releasableRequisitionDto, authorId);
    eventPublisher.emitGroupEvent(getGroupId(orderReleaseEvent.getRequisitionId()),
        getReceiverId(orderReleaseEvent.getRequisitionId()), orderReleaseEvent);
    return orderReleaseEvent;
  }

  private OrderReleaseEvent getEvent(ReleasableRequisitionDto releasableRequisitionDto, UUID authorId) {
    return OrderReleaseEvent
        .builder()
        .requisitionId(releasableRequisitionDto.getRequisitionId())
        .supplyingDepotId(releasableRequisitionDto.getSupplyingDepotId())
        .authorId(authorId)
        .build();
  }

  private String getGroupId(UUID requisitionId) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
    return requisitionExtension.getRequisitionNumberPrefix() + requisitionExtension.getRequisitionNumber();
  }

  private UUID getReceiverId(UUID requisitionId) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    return requisition.getFacilityId();
  }
}
