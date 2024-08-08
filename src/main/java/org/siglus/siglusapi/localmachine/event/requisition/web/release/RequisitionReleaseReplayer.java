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

package org.siglus.siglusapi.localmachine.event.requisition.web.release;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequisitionReleaseReplayer {

  private final RequisitionRepository requisitionRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;

  @Transactional
  @EventListener(classes = {RequisitionReleaseEvent.class})
  public void replay(RequisitionReleaseEvent requisitionReleaseEvent) {
    try {
      log.info("start order release event replay, requisition id = " + requisitionReleaseEvent.getRequisitionNumber());
      doReplay(requisitionReleaseEvent);
      log.info("end order release event replay, requisition id = " + requisitionReleaseEvent.getRequisitionNumber());
    } catch (Exception e) {
      log.error("fail to replay order release event, msg = " + e.getMessage(), e);
      throw e;
    }
  }

  private void doReplay(RequisitionReleaseEvent requisitionReleaseEvent) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionNumber(
        requisitionReleaseEvent.getRequisitionNumber());
    UUID requisitionId = requisitionExtension.getRequisitionId();
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    if (!RequisitionStatus.RELEASED_WITHOUT_ORDER.equals(requisition.getStatus())) {
      requisition.releaseWithoutOrder(requisitionReleaseEvent.getAuthorId());
    }
  }
}
