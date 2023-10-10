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

package org.siglus.siglusapi.localmachine.scheduledtask;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.RequisitionWithSupplyingDepotsDto;
import org.openlmis.requisition.service.RequisitionService;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.service.scheduledtask.SiglusRequisitionAutoCloseService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile({"localmachine"})
@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusAutoCloseTask {

  private final SiglusRequisitionAutoCloseService siglusRequisitionAutoCloseService;

  private final SiglusRequisitionRepository siglusRequisitionRepository;

  private final RequisitionService requisitionService;

  //@Scheduled(cron = "${requisition.close.inapproval.cron}", zone = "${time.zoneId}")
  public void closeOldInApprovalRequisition() {
    Set<Requisition> requisitions = siglusRequisitionRepository
        .findAllByStatusIn(RequisitionStatus.getAfterInApprovalStatus());
    siglusRequisitionAutoCloseService.closeOldRequisitions(requisitions);
  }

  //@Scheduled(cron = "${requisition.close.approved.cron}", zone = "${time.zoneId}")
  public void releaseWithoutOrderForExpiredRequisition() {
    Set<Requisition> requisitions = siglusRequisitionRepository.findAllByStatus(RequisitionStatus.APPROVED);
    List<RequisitionWithSupplyingDepotsDto> dtos =
        siglusRequisitionAutoCloseService.convertToRequisitionWithSupplyingDepotsDto(requisitions);

    List<RequisitionWithSupplyingDepotsDto> processedRequisitionDto =
        requisitionService.processExpiredRequisition(dtos);

    siglusRequisitionAutoCloseService.closeExpiredRequisitionWithSupplyingDepotsDtos(processedRequisitionDto);
  }
}
