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

package org.siglus.siglusapi.service.task.report;

import com.alibaba.fastjson.JSON;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.domain.referencedata.ProcessingPeriod;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.domain.ProgramRequisitionNameMapping;
import org.siglus.siglusapi.domain.RequisitionMonthlyReport;
import org.siglus.siglusapi.domain.report.RequisitionMonthlyNotSubmitReport;
import org.siglus.siglusapi.domain.report.RequisitionMonthlyReportFacility;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.ProgramRequisitionNameMappingRepository;
import org.siglus.siglusapi.repository.RequisitionMonthReportRepository;
import org.siglus.siglusapi.repository.RequisitionMonthlyNotSubmitReportRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.dto.FacillityStockCardDateDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RequisitionReportTaskService {

  public static final int NO_NEED_TO_HANDLE = -1;
  private final ProgramReferenceDataService programDataService;
  private final FacilityReferenceDataService facilityReferenceDataService;
  private final RequisitionMonthReportRepository requisitionMonthReportRepository;
  private final RequisitionMonthlyNotSubmitReportRepository requisitionMonthlyNotSubmitReportRepository;
  private final FacilityNativeRepository facilityNativeRepository;
  private final SiglusStockCardRepository siglusStockCardRepository;
  private final ProgramRequisitionNameMappingRepository programRequisitionNameMappingRepository;
  private final PermissionService permissionService;
  private final ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;
  private final ProcessingPeriodRepository processingPeriodRepository;
  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  public void refresh(boolean needCheckPermission) {
    log.info("refresh. start = " + System.currentTimeMillis());
    try {
      doRefresh(needCheckPermission);
    } catch (Exception e) {
      log.error("refresh with exception. msg = " + e.getMessage(), e);
      throw e;
    }
    log.info("refresh. end = " + System.currentTimeMillis());
  }

  public void update(boolean needCheckPermission) {
    log.info("refresh. start = " + System.currentTimeMillis());
    try {
      doUpdate(needCheckPermission);
    } catch (Exception e) {
      log.error("refresh with exception. msg = " + e.getMessage(), e);
      throw e;
    }
    log.info("refresh. end = " + System.currentTimeMillis());
  }

  private void doUpdate(boolean needCheckPermission) {
    // TODO:  add implement ( 7/4/22 by kourengang)
  }

  public boolean testPermission(UUID programId, UUID facilityId) {
    ValidationResult validationResult = permissionService.canInitRequisition(programId, facilityId);
    log.info("testPermision validationResult=" + JSON.toJSONString(validationResult));
    return validationResult.isSuccess();
  }

  public org.siglus.siglusapi.dto.FacilityDto findFacility(UUID facilityId) {
    return siglusFacilityReferenceDataService.findOne(facilityId);
  }

  public void doRefresh(boolean needCheckPermission) {
    List<FacilityDto> allFacilityDto = facilityReferenceDataService.findAll();
    log.info("allFacilityDto.size = " + allFacilityDto.size());
    List<ProgramDto> allProgramDto = programDataService.findAll();
    log.info("allProgramDto.size = " + allProgramDto.size());
    List<ProcessingPeriodExtension> allProcessingPeriodExtensionDto = processingPeriodExtensionRepository.findAll();
    List<ProcessingPeriod> allProcessingPeriodDto = processingPeriodRepository.findAll();
    Map<UUID, ProcessingPeriod> processingPeriodMap = allProcessingPeriodDto.stream()
        .collect(Collectors.toMap(ProcessingPeriod::getId, Function.identity()));

    allProcessingPeriodExtensionDto.sort(Comparator.comparing(ProcessingPeriodExtension::getSubmitStartDate));

    // TODO: data is big ( 7/4/22 by kourengang)
    List<RequisitionMonthlyReport> requisitionMonthlyReports = requisitionMonthReportRepository.findAll();
    Map<String, RequisitionMonthlyReport> requisitionMonthlyReportMap = requisitionMonthlyReports.stream()
        .collect(Collectors.toMap(this::getUniqueKey, Function.identity(), (e1, e2) -> e1));
    log.info("requisitionMonthlyReports.size = " + requisitionMonthlyReports.size());

    List<FacillityStockCardDateDto> firstStockCardGroupByFacility
        = facilityNativeRepository.findFirstStockCardGroupByFacility();
    Map<String, FacillityStockCardDateDto> facilityStockInventoryDateMap = firstStockCardGroupByFacility.stream()
        .collect(Collectors.toMap(
            facillityStockCardDateDto -> getFirstStockCardUniqueKey(facillityStockCardDateDto.getFacilityId(),
                facillityStockCardDateDto.getProgramId()), Function.identity()));

    Set<UUID> existedPhysicalInventoryFacilityIds = firstStockCardGroupByFacility.stream()
        .map(FacillityStockCardDateDto::getFacilityId).collect(
            Collectors.toSet());

    allFacilityDto = allFacilityDto.stream().filter(item -> existedPhysicalInventoryFacilityIds.contains(item.getId()))
        .collect(Collectors.toList());
    log.info("filtered allFacilityDto.size = " + allFacilityDto.size());

    List<RequisitionMonthlyReportFacility> requisitionMonthlyReportFacilities
        = facilityNativeRepository.queryAllFacilityInfo();

    Map<UUID, RequisitionMonthlyReportFacility> monthlyReportFacilityMap = requisitionMonthlyReportFacilities.stream()
        .collect(Collectors.toMap(RequisitionMonthlyReportFacility::getFacilityId, Function.identity()));

    List<ProgramRequisitionNameMapping> requisitionNameMapping = programRequisitionNameMappingRepository.findAll();
    Map<UUID, ProgramRequisitionNameMapping> requisitionNameMappingMap = requisitionNameMapping.stream()
        .collect(Collectors.toMap(ProgramRequisitionNameMapping::getProgramId, Function.identity()));
    for (FacilityDto facilityDto : allFacilityDto) {
      UUID facilityId = facilityDto.getId();
      RequisitionMonthlyReportFacility facilityInfo = monthlyReportFacilityMap.get(facilityId);
      for (ProgramDto programDto : allProgramDto) {
        UUID programId = programDto.getId();
        if (!facilityStockInventoryDateMap.keySet().contains(getFirstStockCardUniqueKey(facilityId, programId))) {
          log.info(String.format("has no Stock in this program , programIds=%s, facilityId=%s", programId, facilityId));
          continue;
        }
        if (needCheckPermission) {
          log.info(String.format("cannot InitRequisition, programIds=%s, facilityId=%s", programId, facilityId));
          continue;
        }
        FacillityStockCardDateDto facillityStockCardDateDto = facilityStockInventoryDateMap.get(
            getFirstStockCardUniqueKey(facilityId, programId));
        int startPeriodIndexOfFacility = getStartPeriodIndexOfFacility(allProcessingPeriodExtensionDto, facilityId,
            programId, facillityStockCardDateDto);
        if (startPeriodIndexOfFacility == NO_NEED_TO_HANDLE) {
          continue;
        }
        List<RequisitionMonthlyNotSubmitReport> notSubmitList = new ArrayList<>();
        for (int i = startPeriodIndexOfFacility; i < allProcessingPeriodExtensionDto.size(); i++) {
          ProcessingPeriodExtension processingPeriodExtension = allProcessingPeriodExtensionDto.get(i);
          if (LocalDate.now().isBefore(processingPeriodExtension.getSubmitStartDate())) {
            // future period not take into account
            log.info(String.format("future period not take into account, programId=%s,facilityId=%s, date=%s",
                programId, facilityId, processingPeriodExtension.getSubmitStartDate()));
            break;
          }

          RequisitionMonthlyReport requisitionMonthlyReport = requisitionMonthlyReportMap.get(
              getUniqueKey(facilityId, processingPeriodExtension.getProcessingPeriodId(), programDto.getId()));
          if (requisitionMonthlyReport != null) {
            // already exists rr
            continue;
          }
          RequisitionMonthlyNotSubmitReport notSubmit = getRequisitionMonthlyNotSubmitReport(
              processingPeriodMap, requisitionNameMappingMap, facilityId, facilityInfo, programId,
              processingPeriodExtension);
          notSubmitList.add(notSubmit);
        }
        if (CollectionUtils.isNotEmpty(notSubmitList)) {
          log.info("save notSubmitList size = " + notSubmitList.size());
          saveBatchByFacilityId(facilityId, notSubmitList);
        }
      }
    }
  }

  @Transactional
  public void saveBatchByFacilityId(UUID facilityId, List<RequisitionMonthlyNotSubmitReport> notSubmitList) {
    requisitionMonthlyNotSubmitReportRepository.deleteByFacilityId(facilityId);
    requisitionMonthlyNotSubmitReportRepository.save(notSubmitList);
  }

  private int getStartPeriodIndexOfFacility(List<ProcessingPeriodExtension> allProcessingPeriodExtensionDto,
      UUID facilityId, UUID programId, FacillityStockCardDateDto facillityStockCardDateDto) {
    int startPeriodIndexOfFacility;

    if (facillityStockCardDateDto == null) {
      log.info(String.format("facility has no stock card in this program(but other program exists stock card)"
          + ", programId=%s,facilityId=%s", programId, facilityId));
      startPeriodIndexOfFacility = 0;
    } else {
      Date firstOccurredDate = facillityStockCardDateDto.getOccurredDate();
      startPeriodIndexOfFacility = findStartPeriodIndexOfFacility(allProcessingPeriodExtensionDto,
          firstOccurredDate.toLocalDate());
    }
    return startPeriodIndexOfFacility;
  }

  private RequisitionMonthlyNotSubmitReport getRequisitionMonthlyNotSubmitReport(
      Map<UUID, ProcessingPeriod> processingPeriodMap,
      Map<UUID, ProgramRequisitionNameMapping> requisitionNameMappingMap, UUID facilityId,
      RequisitionMonthlyReportFacility requisitionMonthlyReportFacility, UUID programId,
      ProcessingPeriodExtension processingPeriodExtension) {
    return RequisitionMonthlyNotSubmitReport.builder()
        .programId(programId)
        .processingPeriodId(processingPeriodExtension.getProcessingPeriodId())

        .district(requisitionMonthlyReportFacility.getDistrict())
        .province(requisitionMonthlyReportFacility.getProvince())
        .facilityName(requisitionMonthlyReportFacility.getFacilityName())
        .ficilityCode(requisitionMonthlyReportFacility.getFicilityCode())
        .facilityType(requisitionMonthlyReportFacility.getFacilityType())
        .facilityMergeType(requisitionMonthlyReportFacility.getFacilityMergeType())
        .districtFacilityCode(requisitionMonthlyReportFacility.getDistrictFacilityCode())
        .provinceFacilityCode(requisitionMonthlyReportFacility.getProvinceFacilityCode())

        .originalPeriod(
            formatDate(processingPeriodMap.get(processingPeriodExtension.getProcessingPeriodId())))
        .reportName(requisitionNameMappingMap.get(programId).getRequisitionName())
        .inventorydDate(null)
        .statusDetail(null)
        .submittedStatus("Not submitted")
        .requisitionPeriod(processingPeriodMap.get(processingPeriodExtension.getProcessingPeriodId()).getEndDate())
        .reportType(null)
        .submittedTime(null)
        .syncTime(null)
        .facilityId(facilityId)
        .submittedUser(null)
        .clientSubmittedTime(null)
        .requisitionCreatedDate(null)
        .statusLastCreateDdate(null)
        .submitStartDate(processingPeriodExtension.getSubmitStartDate())
        .submitEndDate(processingPeriodExtension.getSubmitEndDate())
        .build();
  }

  private String formatDate(ProcessingPeriod processingPeriod) {
    return
        processingPeriod.getStartDate().getDayOfMonth() + " " + getMonth(
            processingPeriod.getStartDate().getMonth().getValue()) + " " + processingPeriod.getStartDate().getYear()
            + " - "
            + processingPeriod.getEndDate().getDayOfMonth() + " " + getMonth(
            processingPeriod.getEndDate().getMonth().getValue()) + " " + processingPeriod.getEndDate().getYear();
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  private String getMonth(int month) {
    switch (month) {
      case 1:
        return "Jan";
      case 2:
        return "Fev";
      case 3:
        return "Mar";
      case 4:
        return "Abr";
      case 5:
        return "Mai";
      case 6:
        return "Jun";
      case 7:
        return "Jul";
      case 8:
        return "Ago";
      case 9:
        return "Set";
      case 10:
        return "Out";
      case 11:
        return "Nov";
      case 12:
        return "Dez";
      default:
        throw new IllegalArgumentException();
    }

  }

  private int findStartPeriodIndexOfFacility(List<ProcessingPeriodExtension> allProcessingPeriodDto,
      LocalDate firstOccurredDate) {
    for (int i = 0; i + 1 < allProcessingPeriodDto.size(); i++) {
      if (!firstOccurredDate.isBefore(allProcessingPeriodDto.get(i).getSubmitStartDate())
          && firstOccurredDate.isBefore(allProcessingPeriodDto.get(i + 1).getSubmitStartDate())) {
        return i + 1;
      }
    }
    // not found in between period
    if (firstOccurredDate.isBefore(allProcessingPeriodDto.get(0).getSubmitStartDate())) {
      return 0;
    } else {
      // in this case firstOccurredDate is in the future, so no need to handle
      return NO_NEED_TO_HANDLE;
    }
  }


  private String getUniqueKey(RequisitionMonthlyReport item) {
    return getUniqueKey(item.getFacilityId(), item.getProcessingPeriodId(), item.getProgramId());
  }

  private String getUniqueKey(UUID facilityId, UUID processingPeriodId, UUID programId) {
    return facilityId + "&" + processingPeriodId + "&" + programId;
  }

  private String getFirstStockCardUniqueKey(UUID facilityId, UUID programId) {
    return facilityId + "&" + programId;
  }


}
