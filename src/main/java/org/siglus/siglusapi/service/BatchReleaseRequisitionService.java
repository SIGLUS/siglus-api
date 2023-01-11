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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_REQUISITION_EXPIRED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_REQUISITION_NOT_FOUND;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ReleasableRequisitionBatchDto;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.dto.RequisitionsProcessingStatusDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.web.BatchRequisitionController;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unchecked")
public class BatchReleaseRequisitionService {

  private final RequisitionService requisitionService;
  private final SiglusRequisitionRepository siglusRequisitionRepository;

  private final SiglusProcessingPeriodReferenceDataService siglusProcessingPeriodReferenceDataService;

  private final BatchRequisitionController batchRequisitionController;

  private final SiglusOrderService siglusOrderService;

  private final ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  public ResponseEntity<RequisitionsProcessingStatusDto> getRequisitionsProcessingStatusDtoResponse(
      ReleasableRequisitionBatchDto releaseDto) {
    if (Boolean.FALSE.equals(releaseDto.getCreateOrder())) {
      return batchRequisitionController.batchReleaseRequisitions(releaseDto);
    }
    validateParam(releaseDto);
    ReleasableRequisitionDto releasableRequisitionDto = releaseDto.getRequisitionsToRelease().get(0);

    UUID requisitionId = releasableRequisitionDto.getRequisitionId();
    Requisition requisition = validRequisitionId(requisitionId);

    checkIfRequisitionExpired(requisition);
    return batchRequisitionController.batchReleaseRequisitions(releaseDto);
  }

  private void checkIfRequisitionExpired(Requisition requisition) {
    UUID programId = requisition.getProgramId();
    UUID facilityId = requisition.getFacilityId();
    UUID processingPeriodId = requisition.getProcessingPeriodId();
    ProcessingPeriodExtension processingPeriodExtension = processingPeriodExtensionRepository
        .findByProcessingPeriodId(processingPeriodId);
    List<YearMonth> calculatedFulfillOrderMonth = requisitionService
            .calculateFulfillOrderYearMonth(processingPeriodExtension);

    LocalDate submitEndDate = processingPeriodExtension.getSubmitEndDate();
    YearMonth requisitionYearMonth = YearMonth.of(submitEndDate.getYear(), submitEndDate.getMonth());
    if (!calculatedFulfillOrderMonth.contains(requisitionYearMonth)) {
      throw new BusinessDataException(new Message(ERROR_REQUISITION_EXPIRED));
    }

    List<Requisition> requisitions = siglusRequisitionRepository
        .findByFacilityIdAndProgramIdAndStatus(facilityId, programId, RequisitionStatus.APPROVED);
    List<UUID> processingPeriodIds = requisitions.stream().map(Requisition::getProcessingPeriodId)
        .collect(Collectors.toList());
    List<ProcessingPeriodDto> processingPeriodDtos = siglusProcessingPeriodReferenceDataService
        .findByIds(processingPeriodIds);
    processingPeriodDtos.sort((p1, p2) -> p2.getEndDate().compareTo(p1.getEndDate()));

    if (!processingPeriodDtos.get(0).getId().equals(requisition.getProcessingPeriodId())) {
      throw new BusinessDataException(new Message(ERROR_REQUISITION_EXPIRED),
          "Already have new R&R(s) for this facility, current R&R is expired");
    }
  }

  private Requisition validRequisitionId(UUID requisitionId) {
    Requisition requisition = siglusRequisitionRepository.findOne(requisitionId);
    if (requisition == null) {
      throw new NotFoundException(ERROR_REQUISITION_NOT_FOUND);
    }
    return requisition;
  }

  private void validateParam(ReleasableRequisitionBatchDto releaseDto) {
    List<ReleasableRequisitionDto> requisitionsToRelease = releaseDto.getRequisitionsToRelease();
    if (requisitionsToRelease.isEmpty()) {
      throw new IllegalArgumentException("not found releasable requisition");
    }
  }
}