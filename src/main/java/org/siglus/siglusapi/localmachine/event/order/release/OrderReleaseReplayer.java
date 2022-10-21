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
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.StatusChangeRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderReleaseReplayer {

  private final RequisitionRepository requisitionRepository;

  private final StatusChangeRepository statusChangeRepository;

  @EventListener(classes = {OrderReleaseEvent.class})
  public void replay(OrderReleaseEvent orderReleaseEvent) {
    try {
      log.info("start order release event replay, requisition id = " + orderReleaseEvent.getRequisitionId());
      doReplay(orderReleaseEvent);
      log.info("end order release event replay, requisition id = " + orderReleaseEvent.getRequisitionId());
    } catch (Exception e) {
      log.error("fail to replay order release event, msg = " + e.getMessage(), e);
      throw e;
    }
  }

  private void doReplay(OrderReleaseEvent orderReleaseEvent) {
    UUID requisitionId = orderReleaseEvent.getRequisitionId();
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    requisition.setStatus(RequisitionStatus.RELEASED_WITHOUT_ORDER);
    StatusChange statusChange = StatusChange.newStatusChange(requisition, orderReleaseEvent.getAuthorId());
    log.info("do relay save to status change, requisition id = " + requisitionId);
    statusChangeRepository.save(statusChange);

    log.info("do replay save to requisition, requisition id = " + requisitionId);
    requisitionRepository.save(requisition);
  }
}
