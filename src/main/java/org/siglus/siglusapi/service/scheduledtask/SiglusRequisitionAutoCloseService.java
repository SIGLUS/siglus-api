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

package org.siglus.siglusapi.service.scheduledtask;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ReleasableRequisitionBatchDto;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.dto.RequisitionWithSupplyingDepotsDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.service.RequestParameters;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupplyLineReferenceDataService;
import org.openlmis.requisition.web.BasicRequisitionDtoBuilder;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.localmachine.event.requisition.web.finalapprove.RequisitionFinalApproveEmitter;
import org.siglus.siglusapi.localmachine.event.requisition.web.release.RequisitionReleaseEmitter;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.service.BatchReleaseRequisitionService;
import org.siglus.siglusapi.service.SiglusRequisitionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusRequisitionAutoCloseService {

  private final RequisitionService requisitionService;
  private final SiglusRequisitionService siglusRequisitionService;

  private final SiglusRequisitionRepository siglusRequisitionRepository;

  private final BasicRequisitionDtoBuilder basicRequisitionDtoBuilder;

  private final FacilityReferenceDataService facilityReferenceDataService;

  private final ProgramReferenceDataService programReferenceDataService;

  private final SupplyLineReferenceDataService supplyLineReferenceDataService;

  private final RequisitionReleaseEmitter requisitionReleaseEmitter;

  private final BatchReleaseRequisitionService batchReleaseRequisitionService;

  private final PeriodReferenceDataService periodReferenceDataService;

  private final RequisitionFinalApproveEmitter requisitionFinalApproveEmitter;

  @Scheduled(cron = "${requisition.close.inapproval.cron}", zone = "${time.zoneId}")
  public void closeOldInApprovalRequisition() {
    Set<Requisition> requisitions = siglusRequisitionRepository
        .findAllByStatusIn(RequisitionStatus.getPostApproveStatus());
    closeOldRequisitions(requisitions);
  }

  public void closeOldRequisitions(Collection<Requisition> requisitions) {
    Map<String, List<Requisition>> requisitionsMap = requisitions.stream()
        .collect(Collectors.groupingBy(r -> buildForGroupKey(r)));
    Set<Requisition> toCloseRequisitions = requisitions.stream()
        .filter(requisition -> RequisitionStatus.IN_APPROVAL.equals(requisition.getStatus()))
        .filter(requisition -> needCloseRequisition(requisition, requisitionsMap.get(buildForGroupKey(requisition))))
        .collect(Collectors.toSet());

    List<SupplyLineDto> supplyLines = supplyLineReferenceDataService.getPage(RequestParameters.init()).getContent();
    Table<UUID, UUID, UUID> programSupervisoryNodeFacilities = HashBasedTable.create();
    supplyLines.forEach((supplyLineDto) -> {
      programSupervisoryNodeFacilities.put(supplyLineDto.getProgram().getId(),
            supplyLineDto.getSupervisoryNode().getId(), supplyLineDto.getSupplyingFacility().getId());
    });
    log.info("auto close requisition start");
    toCloseRequisitions.forEach(requisition ->
        approveAndReleaseWithoutOrder(requisition, programSupervisoryNodeFacilities));
    log.info("auto close requisition end");
  }


  private void approveAndReleaseWithoutOrder(Requisition requisition,
                                             Table<UUID, UUID, UUID> programSupervisoryNodeFacilities) {
    log.info("auto close requisition id: {}", requisition.getId());
    requisition.getRequisitionLineItems().forEach(lineItem -> lineItem.setApprovedQuantity(0));
    siglusRequisitionRepository.save(requisition);
    siglusRequisitionService.approveRequisition(requisition.getId(), null, null);

    if (RequisitionStatus.APPROVED.equals(requisition.getStatus())) {
      requisitionFinalApproveEmitter.emit(requisition.getId());
      ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
      releasableRequisitionDto.setRequisitionId(requisition.getId());
      releasableRequisitionDto.setSupplyingDepotId(programSupervisoryNodeFacilities
          .get(requisition.getProgramId(), requisition.getSupervisoryNodeId()));

      ReleasableRequisitionBatchDto releasableRequisitionBatchDto = new ReleasableRequisitionBatchDto();
      releasableRequisitionBatchDto.setCreateOrder(false);
      releasableRequisitionBatchDto.setRequisitionsToRelease(Arrays.asList(releasableRequisitionDto));
      // release without order
      batchReleaseRequisitionService
          .getRequisitionsProcessingStatusDtoResponse(releasableRequisitionBatchDto);
      requisitionReleaseEmitter.emit(releasableRequisitionDto, null);
    } else if (RequisitionStatus.RELEASED_WITHOUT_ORDER.equals(requisition.getStatus())) {
      ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
      releasableRequisitionDto.setRequisitionId(requisition.getId());
      releasableRequisitionDto.setSupplyingDepotId(programSupervisoryNodeFacilities
          .get(requisition.getProgramId(), requisition.getSupervisoryNodeId()));
      requisitionReleaseEmitter.emit(releasableRequisitionDto, null);
    }
  }

  private boolean needCloseRequisition(Requisition requisition, List<Requisition> group) {
    Collection<ProcessingPeriodDto> periodDtos = periodReferenceDataService
        .searchByProgramAndFacility(requisition.getProgramId(), requisition.getFacilityId());
    Map<UUID, ProcessingPeriodDto> idToPeriod = periodDtos.stream()
        .collect(Collectors.toMap(ProcessingPeriodDto::getId, Function.identity()));
    ProcessingPeriodDto currentPeriod = idToPeriod.get(requisition.getProcessingPeriodId());
    return group.stream().anyMatch(r -> {
      ProcessingPeriodDto period = idToPeriod.get(r.getProcessingPeriodId());
      return period.getEndDate().isAfter(currentPeriod.getEndDate())
            && RequisitionStatus.getPostApproveStatus().contains(r.getStatus());
    });
  }

  private String buildForGroupKey(Requisition requisition) {
    return requisition.getFacilityId() + FieldConstants.SEPARATOR + requisition.getProgramId();
  }

  @Scheduled(cron = "${requisition.close.approved.cron}", zone = "${time.zoneId}")
  public void releaseWithoutOrderForExpiredRequisition() {
    Set<Requisition> requisitions = siglusRequisitionRepository.findAllByStatus(RequisitionStatus.APPROVED);
    List<RequisitionWithSupplyingDepotsDto> dtos = convertToRequisitionWithSupplyingDepotsDto(requisitions);

    List<RequisitionWithSupplyingDepotsDto> processedRequisitionDto =
        requisitionService.processExpiredRequisition(dtos);

    closeExpiredRequisitionWithSupplyingDepotsDtos(processedRequisitionDto);
  }

  public void closeExpiredRequisitionWithSupplyingDepotsDtos(
      List<RequisitionWithSupplyingDepotsDto> processedRequisitionDto) {
    log.info("auto close requisition start");
    processedRequisitionDto
        .stream().filter(r -> r.isExpired())
        .forEach(requisitionDto -> {
          ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
          releasableRequisitionDto.setRequisitionId(requisitionDto.getRequisition().getId());
          List<FacilityDto> supplyingDepots = requisitionDto.getSupplyingDepots();
          if (CollectionUtils.isNotEmpty(supplyingDepots)) {
            releasableRequisitionDto.setSupplyingDepotId(supplyingDepots.get(0).getId());
          }
          log.info("auto close requisition id: {}", releasableRequisitionDto.getRequisitionId());
          ReleasableRequisitionBatchDto releasableRequisitionBatchDto = new ReleasableRequisitionBatchDto();
          releasableRequisitionBatchDto.setCreateOrder(false);
          releasableRequisitionBatchDto.setRequisitionsToRelease(Arrays.asList(releasableRequisitionDto));
          // release without order
          batchReleaseRequisitionService
              .getRequisitionsProcessingStatusDtoResponse(releasableRequisitionBatchDto);
          // TODO no auth id in auto close
          requisitionReleaseEmitter.emit(releasableRequisitionDto, null);
        });
    log.info("auto close requisition end");
  }

  private List<RequisitionWithSupplyingDepotsDto> convertToRequisitionWithSupplyingDepotsDto(
      Set<Requisition> requisitions) {
    Set<UUID> programIds = requisitions.stream().map(Requisition::getProgramId).collect(Collectors.toSet());
    Map<UUID, ProgramDto> idToProgramDto = programReferenceDataService.search(programIds)
        .stream().collect(Collectors.toMap(BaseDto::getId, Function.identity()));

    Set<UUID> facilityIds = requisitions.stream().map(Requisition::getFacilityId).collect(Collectors.toSet());

    List<SupplyLineDto> supplyLines = supplyLineReferenceDataService.getPage(RequestParameters.init()).getContent();
    Table<UUID, UUID, UUID> programSupervisoryNodeFacilities = HashBasedTable.create();
    supplyLines.forEach((supplyLineDto) -> {
      programSupervisoryNodeFacilities.put(supplyLineDto.getProgram().getId(),
          supplyLineDto.getSupervisoryNode().getId(), supplyLineDto.getSupplyingFacility().getId());
      facilityIds.add(supplyLineDto.getSupplyingFacility().getId());
    });

    Map<UUID, FacilityDto> idToFacilityDto = facilityReferenceDataService.search(facilityIds)
        .stream().collect(Collectors.toMap(BaseDto::getId, Function.identity()));

    return requisitions.stream().map(requisition -> {
      BasicRequisitionDto requisitionDto = this.basicRequisitionDtoBuilder.build(requisition,
          idToFacilityDto.get(requisition.getFacilityId()), idToProgramDto.get(requisition.getProgramId()));
      return new RequisitionWithSupplyingDepotsDto(requisitionDto,
        Collections.singletonList(idToFacilityDto.get(
            programSupervisoryNodeFacilities.get(requisition.getProgramId(), requisition.getSupervisoryNodeId()))));
    }).collect(Collectors.toList());
  }
}
