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

package org.siglus.siglusapi.web;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openlmis.requisition.dto.ReleasableRequisitionBatchDto;
import org.openlmis.requisition.dto.RequisitionsProcessingStatusDto;
import org.siglus.siglusapi.localmachine.event.requisition.web.release.RequisitionReleaseEmitter;
import org.siglus.siglusapi.service.BatchReleaseRequisitionService;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/siglusapi/requisitions")
public class SiglusBatchRequisitionController {

  private final BatchReleaseRequisitionService batchReleaseRequisitionService;

  private final SiglusNotificationService notificationService;

  private final RequisitionReleaseEmitter requisitionReleaseEmitter;

  private final SiglusAuthenticationHelper siglusAuthenticationHelper;

  /**
   * why we redo this api? to support #245?<br> we refactor the
   * {@linkplain org.openlmis.fulfillment.service.OrderService#setOrderStatus}
   * method}
   */
  @PostMapping("/batchReleases")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  @Transactional
  public ResponseEntity<RequisitionsProcessingStatusDto> batchReleaseRequisitions(
      @RequestBody ReleasableRequisitionBatchDto releaseDto) {
    ResponseEntity<RequisitionsProcessingStatusDto> responseEntity = batchReleaseRequisitionService
        .getRequisitionsProcessingStatusDtoResponse(releaseDto);
    RequisitionsProcessingStatusDto body = responseEntity.getBody();
    body.getRequisitionDtos().forEach(notificationService::postConvertToOrder);
    if (!releaseDto.getCreateOrder()) {
      UUID authorId = siglusAuthenticationHelper.getCurrentUser().getId();
      releaseDto.getRequisitionsToRelease().forEach(releasableRequisition ->
          requisitionReleaseEmitter.emit(releasableRequisition, authorId));
    }
    return responseEntity;
  }
}
