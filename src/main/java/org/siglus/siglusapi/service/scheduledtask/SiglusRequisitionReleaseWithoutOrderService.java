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
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ReleasableRequisitionBatchDto;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.dto.RequisitionWithSupplyingDepotsDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.service.RequestParameters;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupplyLineReferenceDataService;
import org.openlmis.requisition.web.BasicRequisitionDtoBuilder;
import org.siglus.siglusapi.localmachine.event.requisition.web.release.RequisitionReleaseEmitter;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.service.BatchReleaseRequisitionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusRequisitionReleaseWithoutOrderService {

  private final RequisitionService requisitionService;

  private final SiglusRequisitionRepository siglusRequisitionRepository;

  private final BasicRequisitionDtoBuilder basicRequisitionDtoBuilder;

  private final FacilityReferenceDataService facilityReferenceDataService;

  private final ProgramReferenceDataService programReferenceDataService;

  private final SupplyLineReferenceDataService supplyLineReferenceDataService;

  private final RequisitionReleaseEmitter requisitionReleaseEmitter;

  private final BatchReleaseRequisitionService batchReleaseRequisitionService;

  @Scheduled(cron = "${requisition.close.cron}", zone = "${time.zoneId}")
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
