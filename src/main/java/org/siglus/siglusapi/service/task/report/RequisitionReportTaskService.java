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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.domain.referencedata.ProcessingPeriod;
import org.siglus.common.dto.ProgramAdditionalOrderableDto;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.constant.AndroidConstants;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.domain.ProgramRequisitionNameMapping;
import org.siglus.siglusapi.domain.RequisitionMonthlyReport;
import org.siglus.siglusapi.domain.report.RequisitionMonthlyNotSubmitReport;
import org.siglus.siglusapi.domain.report.RequisitionMonthlyReportFacility;
import org.siglus.siglusapi.dto.ProcessingPeriodExtensionDto;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.ProgramRequisitionNameMappingRepository;
import org.siglus.siglusapi.repository.RequisitionMonthReportRepository;
import org.siglus.siglusapi.repository.RequisitionMonthlyNotSubmitReportRepository;
import org.siglus.siglusapi.repository.dto.FacilityProgramPeriodScheduleDto;
import org.siglus.siglusapi.repository.dto.FacillityStockCardDateDto;
import org.siglus.siglusapi.service.SiglusProgramAdditionalOrderableService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionReportTaskService {

  public static final int NO_NEED_TO_HANDLE = -1;
  private final ProgramReferenceDataService programDataService;
  private final FacilityReferenceDataService facilityReferenceDataService;
  private final RequisitionMonthReportRepository requisitionMonthReportRepository;
  private final RequisitionMonthlyNotSubmitReportRepository requisitionMonthlyNotSubmitReportRepository;
  private final FacilityNativeRepository facilityNativeRepository;
  private final ProgramRequisitionNameMappingRepository programRequisitionNameMappingRepository;
  private final ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;
  private final ProcessingPeriodRepository processingPeriodRepository;
  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  private final SiglusProgramAdditionalOrderableService siglusProgramAdditionalOrderableService;


  public void refresh(boolean needCheckPermission) {
    log.info("refresh. start = " + System.currentTimeMillis());
    try {
      doRefresh(needCheckPermission, getDataWrapper());
    } catch (Exception e) {
      log.error("refresh with exception. msg = " + e.getMessage(), e);
      throw e;
    }
    log.info("refresh. end = " + System.currentTimeMillis());
  }

  private DataWrapper getDataWrapper() {
    return new DataWrapper();
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

  public org.siglus.siglusapi.dto.FacilityDto findFacility(UUID facilityId) {
    return siglusFacilityReferenceDataService.findOne(facilityId);
  }

  public void doRefresh(boolean needCheckPermission, DataWrapper dataWrapper) {
    for (FacilityDto facilityDto : dataWrapper.allFacilityDto) {
      RequisitionMonthlyReportFacility facilityInfo = dataWrapper.monthlyReportFacilityMap.get(facilityDto.getId());
      findFacilityCanViewRequisitionProgramIdList(facilityDto.getId());
      org.siglus.siglusapi.dto.FacilityDto facilityWithSupportedPrograms =
          siglusFacilityReferenceDataService.findOne(facilityDto.getId());
      if (Boolean.FALSE.equals(facilityWithSupportedPrograms.getActive()) || CollectionUtils.isEmpty(
          facilityWithSupportedPrograms.getSupportedPrograms())) {
        continue;
      }
      List<SupportedProgramDto> supportedPrograms = facilityWithSupportedPrograms.getSupportedPrograms();

      List<RequisitionMonthlyNotSubmitReport> notSubmitList =
          getFacilityNotSubmitRequisitions(needCheckPermission, facilityInfo, supportedPrograms, dataWrapper);

      updateBatch(facilityInfo.getFacilityId(), notSubmitList);
    }
  }

  private List<RequisitionMonthlyNotSubmitReport> getFacilityNotSubmitRequisitions(boolean needCheckPermission,
      RequisitionMonthlyReportFacility facilityInfo,
      List<SupportedProgramDto> supportedPrograms, DataWrapper dataWrapper) {
    List<RequisitionMonthlyNotSubmitReport> notSubmitList = new ArrayList<>();
    for (ProgramDto programDto : dataWrapper.allProgramDto) {
      UUID programId = programDto.getId();
      String programCode = dataWrapper.requisitionNameMappingMap.get(programId).getRequisitionName();
      FacillityStockCardDateDto facillityStockCardDateDto = dataWrapper.facilityStockInventoryDateMap.get(
          dataWrapper.getUniqueKey(facilityInfo.getFacilityId(), programId));
      if (facillityStockCardDateDto == null) {
        log.info(String.format("has no Stock in this program , programCode=%s, facilityId=%s", programCode,
            facilityInfo.getFacilityId()));
        continue;
      }
      Optional<SupportedProgramDto> supportedProgramDto =
          supportedPrograms.stream().filter(item -> programId.equals(item.getId())).findFirst();
      if (needCheckPermission || !supportedProgramDto.isPresent()) {
        log.info(String.format("cannot InitRequisition, programCode=%s, facilityId=%s", programCode,
            facilityInfo.getFacilityId()));
        continue;
      }
      FacilityProgramPeriodScheduleDto facilityProgramPeriodScheduleDto =
          dataWrapper.facilityProgramPeriodScheduleDtoMap.get(dataWrapper.getUniqueKey(facilityInfo.getFacilityId(),
              programId));
      if (facilityProgramPeriodScheduleDto == null) {
        log.info(String.format("not facilityProgramPeriodScheduleDto, programCode=%s, facilityId=%s",
            programCode, facilityInfo.getFacilityId()));
        continue;
      }

      List<RequisitionMonthlyNotSubmitReport> programNotSubmitRequisitions =
          getProgramNotSubmitRequisitions(facilityInfo, dataWrapper, programDto, programCode, facillityStockCardDateDto,
              supportedProgramDto, facilityProgramPeriodScheduleDto);
      notSubmitList.addAll(programNotSubmitRequisitions);

    }
    return notSubmitList;
  }

  private List<RequisitionMonthlyNotSubmitReport> getProgramNotSubmitRequisitions(
      RequisitionMonthlyReportFacility facilityInfo, DataWrapper dataWrapper,
      ProgramDto programDto, String programCode,
      FacillityStockCardDateDto facillityStockCardDateDto, Optional<SupportedProgramDto> supportedProgramDto,
      FacilityProgramPeriodScheduleDto facilityProgramPeriodScheduleDto) {
    List<ProcessingPeriodExtensionDto> facilityProgramSupportedPeriods =
        dataWrapper.processingPeriodExtensionDtos.stream()
            .filter(item -> {
              if (Boolean.TRUE.equals(facillityStockCardDateDto.getIsAndroid())) {
                return AndroidConstants.SCHEDULE_CODE.equals(item.getProcessingSchedule().getCode());
              } else {
                return facilityProgramPeriodScheduleDto.getProcessingScheduleId()
                    .equals(item.getProcessingSchedule().getId());
              }
            }).sorted(Comparator.comparing(ProcessingPeriodExtensionDto::getStartDate))
            .collect(Collectors.toList());
    log.info(String.format("facilityProgramSupportedPeriods size = %s,programCode=%s, facilityId=%s",
        facilityProgramSupportedPeriods.size(), programCode, facilityInfo.getFacilityId()));

    int startPeriodIndexOfFacility = findStartPeriodIndexOfFacility(facilityProgramSupportedPeriods,
        supportedProgramDto.get().getSupportStartDate(), facillityStockCardDateDto.getOccurredDate().toLocalDate(),
        Boolean.TRUE.equals(facillityStockCardDateDto.getIsAndroid()),
        ProgramConstants.RAPIDTEST_PROGRAM_CODE.equals(programDto.getCode()));

    return getPeriodNotSubmitRequisitions(
        facilityInfo, programDto, programCode,
        facilityProgramSupportedPeriods, startPeriodIndexOfFacility, dataWrapper);
  }

  private List<RequisitionMonthlyNotSubmitReport> getPeriodNotSubmitRequisitions(
      RequisitionMonthlyReportFacility facilityInfo, ProgramDto programDto,
      String programCode, List<ProcessingPeriodExtensionDto> facilityProgramSupportedPeriods,
      int startPeriodIndexOfFacility, DataWrapper dataWrapper) {
    List<RequisitionMonthlyNotSubmitReport> notSubmitPeriodList = new ArrayList<>();
    for (int i = startPeriodIndexOfFacility; i < facilityProgramSupportedPeriods.size(); i++) {
      ProcessingPeriodExtensionDto processingPeriodExtension = facilityProgramSupportedPeriods.get(i);
      if (LocalDate.now().isBefore(processingPeriodExtension.getSubmitStartDate())) {
        // future period not take into account
        log.info(String.format("future period not take into account, programCode=%s,facilityId=%s, date=%s",
            programCode, facilityInfo.getFacilityId(), processingPeriodExtension.getSubmitStartDate()));
        break;
      }

      RequisitionMonthlyReport requisitionMonthlyReport = dataWrapper.requisitionMonthlyReportMap.get(
          dataWrapper.getUniqueKey(facilityInfo.getFacilityId(), processingPeriodExtension.getProcessingPeriodId(),
              programDto.getId()));
      if (requisitionMonthlyReport != null) {
        // already exists rr
        continue;
      }
      RequisitionMonthlyNotSubmitReport notSubmit = getRequisitionMonthlyNotSubmitReport(
          dataWrapper.requisitionNameMappingMap, facilityInfo.getFacilityId(), facilityInfo, programDto.getId(),
          processingPeriodExtension);
      notSubmitPeriodList.add(notSubmit);
    }
    return notSubmitPeriodList;
  }

  private List<FacillityStockCardDateDto> getAllFacilityStockCardDateDtos(List<ProgramDto> allProgramDto) {
    List<FacillityStockCardDateDto> firstStockCardGroupByFacility
        = facilityNativeRepository.findFirstStockCardGroupByFacility();
    firstStockCardGroupByFacility.addAll(findMalariaFacilityStockCardDate(allProgramDto));
    return firstStockCardGroupByFacility;
  }

  private List<FacillityStockCardDateDto> findMalariaFacilityStockCardDate(List<ProgramDto> allProgramDto) {
    Optional<ProgramDto> malariaProgram =
        allProgramDto.stream().filter(item -> ProgramConstants.MALARIA_PROGRAM_CODE.equals(item.getCode())).findFirst();
    Optional<ProgramDto> viaProgram =
        allProgramDto.stream().filter(item -> ProgramConstants.VIA_PROGRAM_CODE.equals(item.getCode())).findFirst();
    if (!malariaProgram.isPresent() || !viaProgram.isPresent()) {
      return new ArrayList<>();
    }
    Set<UUID> malariaAdditionalOrderableIds =
        siglusProgramAdditionalOrderableService.searchAdditionalOrderables(malariaProgram.get().getId())
            .stream().map(ProgramAdditionalOrderableDto::getAdditionalOrderableId).collect(Collectors.toSet());

    List<FacillityStockCardDateDto> result =
        facilityNativeRepository.findMalariaFirstStockCardGroupByFacility(malariaAdditionalOrderableIds,
            viaProgram.get().getId());
    result.forEach(facilityStockCardDateDto -> {
      facilityStockCardDateDto.setProgramId(malariaProgram.get().getId());
    });
    return result;
  }

  @Transactional
  public void updateBatch(UUID facilityId, List<RequisitionMonthlyNotSubmitReport> notSubmitList) {
    requisitionMonthlyNotSubmitReportRepository.deleteByFacilityId(facilityId);
    if (CollectionUtils.isNotEmpty(notSubmitList)) {
      log.info(String.format("[save] notSubmitList size =%s, facilityId=%s", notSubmitList.size(), facilityId));
      requisitionMonthlyNotSubmitReportRepository.save(notSubmitList);
    } else {
      log.info(String.format("[all rr is submitted or in the future], facilityId=%s", facilityId));
    }
  }

  private List<ProcessingPeriodExtensionDto> getProcessingPeriodExtensionDto() {
    List<ProcessingPeriodExtension> allProcessingPeriodExtensionDto = processingPeriodExtensionRepository.findAll();
    List<ProcessingPeriod> allProcessingPeriodDto = processingPeriodRepository.findAll();

    Map<UUID, ProcessingPeriod> processingPeriodMap = allProcessingPeriodDto.stream()
        .collect(Collectors.toMap(ProcessingPeriod::getId, Function.identity()));
    List<ProcessingPeriodExtensionDto> result = new ArrayList<>();
    for (ProcessingPeriodExtension item : allProcessingPeriodExtensionDto) {
      ProcessingPeriodExtensionDto dto = new ProcessingPeriodExtensionDto();
      BeanUtils.copyProperties(item, dto);

      ProcessingPeriod processingPeriod = processingPeriodMap.get(item.getProcessingPeriodId());

      dto.setStartDate(processingPeriod.getStartDate());
      dto.setEndDate(processingPeriod.getEndDate());
      dto.setProcessingSchedule(processingPeriod.getProcessingSchedule());

      result.add(dto);
    }
    return result;
  }

  private Set<UUID> findFacilityCanViewRequisitionProgramIdList(UUID facilityId) {
    org.siglus.siglusapi.dto.FacilityDto facilityDto = siglusFacilityReferenceDataService.findOne(facilityId);
    if (facilityDto == null || Boolean.FALSE.equals(facilityDto.getActive()) || CollectionUtils.isEmpty(
        facilityDto.getSupportedPrograms())) {
      return new HashSet<>();
    }
    List<SupportedProgramDto> supportedPrograms = facilityDto.getSupportedPrograms();

    return supportedPrograms.stream()
        .filter(item -> item.isSupportActive() && item.isProgramActive()).map(SupportedProgramDto::getId)
        .collect(Collectors.toSet());
  }

  private RequisitionMonthlyNotSubmitReport getRequisitionMonthlyNotSubmitReport(
      Map<UUID, ProgramRequisitionNameMapping> requisitionNameMappingMap, UUID facilityId,
      RequisitionMonthlyReportFacility requisitionMonthlyReportFacility, UUID programId,
      ProcessingPeriodExtensionDto processingPeriodExtension) {
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

        .originalPeriod(formatDate(processingPeriodExtension))
        .reportName(requisitionNameMappingMap.get(programId).getRequisitionName())
        .inventorydDate(null)
        .statusDetail(null)
        .submittedStatus("Not submitted")
        .requisitionPeriod(processingPeriodExtension.getEndDate())
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

  private String formatDate(ProcessingPeriodExtensionDto processingPeriod) {
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

  public int findStartPeriodIndexOfFacility(List<ProcessingPeriodExtensionDto> allProcessingPeriodDto,
      LocalDate reportStartDate, LocalDate firstStockOccurDate, boolean isAndroid, boolean isRapidTest) {
    LocalDate startDate = reportStartDate.isBefore(firstStockOccurDate) ? firstStockOccurDate : reportStartDate;
    // todo web differ from android, temp logic
    if (reportStartDate.isBefore(firstStockOccurDate) && !isRapidTest) {
      return findStartPeriodIndexByStartDate(allProcessingPeriodDto, startDate, 17);
    } else {
      return findStartPeriodIndexByStartDate(allProcessingPeriodDto, startDate, 20);
    }
  }

  private int findStartPeriodIndexByStartDate(List<ProcessingPeriodExtensionDto> allProcessingPeriodDto,
      LocalDate startDate, int dayOfMonthThreshold) {
    if (startDate.getDayOfMonth() > dayOfMonthThreshold) {
      startDate = startDate.plusMonths(1);
    }
    int month = startDate.getMonthValue();
    int year = startDate.getYear();
    if (startDate.isBefore(allProcessingPeriodDto.get(0).getSubmitStartDate())) {
      return 0;
    }
    for (int i = 0; i + 1 < allProcessingPeriodDto.size(); i++) {
      if (year == allProcessingPeriodDto.get(i).getSubmitStartDate().getYear()
          && month == allProcessingPeriodDto.get(i).getSubmitStartDate().getMonthValue()) {
        return i;
      }
    }
    log.info("first occurred date is in the future, so no need to handle, startDate = " + startDate);
    return Integer.MAX_VALUE;
  }

  public class DataWrapper {
    protected List<FacilityDto> allFacilityDto;
    protected Map<UUID, RequisitionMonthlyReportFacility> monthlyReportFacilityMap;
    protected List<ProgramDto> allProgramDto;
    protected Map<UUID, ProgramRequisitionNameMapping> requisitionNameMappingMap;
    protected Map<String, FacillityStockCardDateDto> facilityStockInventoryDateMap;
    protected Map<String, FacilityProgramPeriodScheduleDto> facilityProgramPeriodScheduleDtoMap;
    protected List<ProcessingPeriodExtensionDto> processingPeriodExtensionDtos;
    protected Map<String, RequisitionMonthlyReport> requisitionMonthlyReportMap;

    public DataWrapper() {
      allFacilityDto = facilityReferenceDataService.findAll();
      log.info("allFacilityDto.size = " + allFacilityDto.size());

      List<RequisitionMonthlyReportFacility> requisitionMonthlyReportFacilities
          = facilityNativeRepository.queryAllFacilityInfo();

      monthlyReportFacilityMap = requisitionMonthlyReportFacilities.stream()
          .collect(Collectors.toMap(RequisitionMonthlyReportFacility::getFacilityId, Function.identity()));

      allProgramDto = programDataService.findAll();
      log.info("allProgramDto.size = " + allProgramDto.size());

      List<ProgramRequisitionNameMapping> requisitionNameMapping = programRequisitionNameMappingRepository.findAll();
      requisitionNameMappingMap = requisitionNameMapping.stream()
          .collect(Collectors.toMap(ProgramRequisitionNameMapping::getProgramId, Function.identity()));


      List<FacillityStockCardDateDto> firstStockCardGroupByFacility =
          getAllFacilityStockCardDateDtos(allProgramDto);
      facilityStockInventoryDateMap = firstStockCardGroupByFacility.stream()
          .collect(Collectors.toMap(
              item -> getUniqueKey(item.getFacilityId(),
                  item.getProgramId()), Function.identity()));

      List<FacilityProgramPeriodScheduleDto> facilityProgramPeriodSchedule
          = facilityNativeRepository.findFacilityProgramPeriodSchedule();

      facilityProgramPeriodScheduleDtoMap = facilityProgramPeriodSchedule.stream().collect(Collectors.toMap(
          facilityProgramPeriodScheduleDto -> getUniqueKey(facilityProgramPeriodScheduleDto.getFacilityId(),
              facilityProgramPeriodScheduleDto.getProgramId()), Function.identity()));
      processingPeriodExtensionDtos = getProcessingPeriodExtensionDto();

      // TODO: data is big ( 7/4/22 by kourengang)
      List<RequisitionMonthlyReport> requisitionMonthlyReports = requisitionMonthReportRepository.findAll();
      requisitionMonthlyReports = requisitionMonthlyReports.stream()
          .filter(item -> item.getRequisitionCreatedDate() != null).collect(Collectors.toList());
      requisitionMonthlyReportMap = requisitionMonthlyReports.stream()
          .collect(Collectors.toMap(this::getUniqueKey, Function.identity(), (e1, e2) -> e1));
      log.info("requisitionMonthlyReports.size = " + requisitionMonthlyReports.size());

      Set<UUID> existedPhysicalInventoryFacilityIds = firstStockCardGroupByFacility.stream()
          .map(FacillityStockCardDateDto::getFacilityId).collect(
              Collectors.toSet());

      allFacilityDto =
          allFacilityDto.stream().filter(item -> existedPhysicalInventoryFacilityIds.contains(item.getId()))
              .collect(Collectors.toList());
      log.info("filtered allFacilityDto.size = " + allFacilityDto.size());
    }

    private String getUniqueKey(RequisitionMonthlyReport item) {
      return getUniqueKey(item.getFacilityId(), item.getProcessingPeriodId(), item.getProgramId());
    }

    private String getUniqueKey(UUID facilityId, UUID processingPeriodId, UUID programId) {
      return facilityId + "&" + processingPeriodId + "&" + programId;
    }

    private String getUniqueKey(UUID facilityId, UUID programId) {
      return facilityId + "&" + programId;
    }
  }
}
