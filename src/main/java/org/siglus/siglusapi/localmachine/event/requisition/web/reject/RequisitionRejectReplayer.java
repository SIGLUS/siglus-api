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

package org.siglus.siglusapi.localmachine.event.requisition.web.reject;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.event.EventCommonService;
import org.siglus.siglusapi.localmachine.event.NotificationService;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.SiglusRequisitionService;
import org.siglus.siglusapi.service.android.RequisitionCreateService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionRejectReplayer {

  private final RequisitionRepository requisitionRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final EventCommonService eventCommonService;
  private final NotificationService notificationService;
  private final SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;
  private final SiglusRequisitionService siglusRequisitionService;
  private final RequisitionCreateService requisitionCreateService;

  @EventListener(classes = {RequisitionRejectEvent.class})
  public void replay(RequisitionRejectEvent event) {
    try {
      log.info("start replay requisition = " + event.getRequisitionNumber());
      doReplay(event);
      log.info("end replay requisition = " + event.getRequisitionNumber());
    } catch (Exception e) {
      log.error("fail to reject requisition, msg = " + e.getMessage(), e);
      throw e;
    }
  }

  private void doReplay(RequisitionRejectEvent event) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionNumber(
        event.getRequisitionNumber());
    UUID requisitionId = requisitionExtension.getRequisitionId();
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    Map<VersionIdentityDto, OrderableDto> orderableDtoMap = eventCommonService.getOrderableDtoMap(requisition);
    requisition.reject(orderableDtoMap, event.getUserId());
    resetSupervisoryNodeId(requisition);
    requisition.setDraftStatusMessage(StringUtils.EMPTY);
    siglusRequisitionService.revertRequisition(requisitionId, requisition);
    requisitionRepository.saveAndFlush(requisition);

    notificationService.postReject(event.getUserId(),
        requisitionCreateService.buildBaseRequisitionDto(requisition));
  }

  private void resetSupervisoryNodeId(Requisition requisition) {
    UUID supervisoryNode = supervisoryNodeReferenceDataService.findSupervisoryNode(requisition.getProgramId(),
        requisition.getFacilityId()).getId();
    requisition.setSupervisoryNodeId(supervisoryNode);
  }
}
