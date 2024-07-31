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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionWithSupplyingDepotsDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.service.RequestParameters;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupplyLineReferenceDataService;
import org.openlmis.requisition.web.BasicRequisitionDtoBuilder;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.service.SiglusRequisitionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusRequisitionAutoCloseService {
  public static final String SIGNATURE = "signaure";
  public static final String APPROVE = "approve";
  public static final String AUTO_CLOSE = "AUTO_CLOSE";
  private final SiglusRequisitionService siglusRequisitionService;

  private final SiglusRequisitionRepository siglusRequisitionRepository;

  private final BasicRequisitionDtoBuilder basicRequisitionDtoBuilder;

  private final FacilityReferenceDataService facilityReferenceDataService;

  private final ProgramReferenceDataService programReferenceDataService;

  private final SupplyLineReferenceDataService supplyLineReferenceDataService;

  private final PeriodReferenceDataService periodReferenceDataService;

  private final SiglusAuthenticationHelper authenticationHelper;

  public void closeOldRequisitions(Collection<Requisition> requisitions) {
    Map<String, List<Requisition>> requisitionsMap = requisitions.stream()
        .collect(Collectors.groupingBy(this::buildForGroupKey));
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    Set<Requisition> toCloseRequisitions = requisitions.stream()
        .filter(requisition -> !facilityId.equals(requisition.getFacilityId()))
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
    toCloseRequisitions.forEach(requisition -> {
      requisition.getRequisitionLineItems().forEach(lineItem -> lineItem.setApprovedQuantity(0));
      Map<String, Object> extraData = new HashMap<String, Object>(requisition.getExtraData());
      LinkedHashMap signatureDto = (LinkedHashMap) extraData.get(SIGNATURE);
      if (signatureDto == null) {
        signatureDto = new LinkedHashMap();
      }
      List<String> approves;
      if (signatureDto.get(APPROVE) == null) {
        approves = new ArrayList<>();
      } else {
        approves = (ArrayList) signatureDto.get(APPROVE);
      }
      approves.add(AUTO_CLOSE);
      signatureDto.put(APPROVE, approves.toArray(new String[0]));
      extraData.put(SIGNATURE, signatureDto);
      requisition.setExtraData(extraData);
    });
    siglusRequisitionRepository.save(toCloseRequisitions);
    toCloseRequisitions.forEach(requisition ->
        siglusRequisitionService.approveAndReleaseWithoutOrder(requisition, programSupervisoryNodeFacilities));
    log.info("auto close requisition end");
  }

  private String buildForGroupKey(Requisition requisition) {
    return requisition.getFacilityId() + FieldConstants.SEPARATOR + requisition.getProgramId();
  }

  public void closeExpiredRequisitionWithSupplyingDepotsDtos(
      List<RequisitionWithSupplyingDepotsDto> processedRequisitionDto) {
    log.info("auto close requisition start");
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    processedRequisitionDto
        .stream()
        .filter(RequisitionWithSupplyingDepotsDto::isExpired)
        .filter(requisition -> !facilityId.equals(requisition.getRequisition().getFacility().getId()))
        .forEach(siglusRequisitionService::releaseWithoutOrder);
    log.info("auto close requisition end");
  }

  public List<RequisitionWithSupplyingDepotsDto> convertToRequisitionWithSupplyingDepotsDto(
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

  public boolean needCloseRequisition(Requisition requisition, List<Requisition> groupedRequisitions) {
    Collection<ProcessingPeriodDto> periodDtos = periodReferenceDataService
        .searchByProgramAndFacility(requisition.getProgramId(), requisition.getFacilityId());
    Map<UUID, ProcessingPeriodDto> idToPeriod = periodDtos.stream()
        .collect(Collectors.toMap(ProcessingPeriodDto::getId, Function.identity()));
    ProcessingPeriodDto currentPeriod = idToPeriod.get(requisition.getProcessingPeriodId());
    if (currentPeriod == null) {
      log.info("ProcessingPeriodDto notFound requisitionId {}, periodId {}",
          requisition.getId(), requisition.getProcessingPeriodId());
    }
    return groupedRequisitions.stream().anyMatch(r -> {
      ProcessingPeriodDto targetPeriod = idToPeriod.get(r.getProcessingPeriodId());
      if (targetPeriod == null || currentPeriod == null) {
        log.info("ProcessingPeriodDto notFound requisitionId {}, periodId {}",
            r.getId(), r.getProcessingPeriodId());
        return false;
      }
      return targetPeriod.getEndDate().isAfter(currentPeriod.getEndDate())
            && RequisitionStatus.getAfterInApprovalStatus().contains(r.getStatus());
    });
  }

}
