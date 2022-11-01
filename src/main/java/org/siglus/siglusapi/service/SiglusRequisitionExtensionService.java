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

import static org.siglus.siglusapi.constant.ProgramConstants.MALARIA_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.MMC_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.MTB_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.RAPIDTEST_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.TARV_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.VIA_PROGRAM_CODE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.enums.RequisitionPrefixEnum;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusRequisitionExtensionService {

  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  private final SiglusGeneratedNumberService siglusGeneratedNumberService;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final ProcessingPeriodRepository processingPeriodRepository;
  private final SiglusProgramService siglusProgramService;
  private static final String DOT = ".";

  public RequisitionExtension createRequisitionExtension(SiglusRequisitionDto siglusRequisitionDto) {
    ProcessingPeriod period = processingPeriodRepository.findOneById(siglusRequisitionDto.getProcessingPeriodId());
    RequisitionExtension requisitionExtension = buildRequisitionExtension(siglusRequisitionDto.getId(),
        siglusRequisitionDto.getEmergency(), siglusRequisitionDto.getFacilityId(),
        siglusRequisitionDto.getProgramId(), period.getEndDate());
    log.info("save requisition extension: {}", requisitionExtension);
    return requisitionExtensionRepository.save(requisitionExtension);
  }

  public RequisitionExtension buildRequisitionExtension(UUID requisitionId, Boolean emergency, UUID facilityId,
      UUID programId, LocalDate endDate) {
    String facilityCode = siglusFacilityReferenceDataService.findOne(facilityId).getCode();
    Integer consequentialNumber = siglusGeneratedNumberService.getGeneratedNumber(facilityId, programId,
        endDate.getYear(), emergency);
    String programCode = siglusProgramService.getProgram(programId).getCode();
    String prefix;
    switch (programCode) {
      case VIA_PROGRAM_CODE:
        prefix = emergency ? RequisitionPrefixEnum.REM.toString() : RequisitionPrefixEnum.RNO.toString();
        break;
      case MTB_PROGRAM_CODE:
        prefix = RequisitionPrefixEnum.MTB.toString();
        break;
      case RAPIDTEST_PROGRAM_CODE:
        prefix = RequisitionPrefixEnum.MIT.toString();
        break;
      case TARV_PROGRAM_CODE:
        prefix = RequisitionPrefixEnum.MIA.toString();
        break;
      case MALARIA_PROGRAM_CODE:
        prefix = RequisitionPrefixEnum.ALS.toString();
        break;
      case MMC_PROGRAM_CODE:
        prefix = RequisitionPrefixEnum.MMC.toString();
        break;
      default:
        prefix = "";
    }
    String yearMonth = endDate.format(DateTimeFormatter.ofPattern("yyMM"));
    return RequisitionExtension.builder()
        .requisitionId(requisitionId)
        .requisitionNumberPrefix(prefix + DOT + facilityCode + DOT + yearMonth + DOT)
        .requisitionNumber(consequentialNumber)
        .facilityId(facilityId)
        .build();
  }

  public String formatRequisitionNumber(RequisitionExtension requisitionExtension) {
    if (null == requisitionExtension) {
      return null;
    }
    return requisitionExtension.getRealRequisitionNumber();
  }

  public String formatRequisitionNumber(UUID requisitionId) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
    return formatRequisitionNumber(requisitionExtension);
  }

  // requisitionId: requisitionNumber
  public Map<UUID, String> getRequisitionNumbers(Set<UUID> requisitionIds) {
    List<RequisitionExtension> requisitionExtensions = requisitionExtensionRepository.findByRequisitionIdIn(
        requisitionIds);
    return requisitionExtensions.stream()
        .collect(Collectors.toMap(RequisitionExtension::getRequisitionId, this::formatRequisitionNumber));
  }


  public void deleteRequisitionExtension(UUID requisitionId) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
    if (null == requisitionExtension) {
      return;
    }
    log.info("delete requisition extension by requisition id: {}", requisitionId);
    requisitionExtensionRepository.delete(requisitionExtension);
  }

}
