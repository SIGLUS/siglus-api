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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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
import org.apache.commons.lang3.StringUtils;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.dto.ProgramAdditionalOrderableDto;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.domain.ProgramRequisitionNameMapping;
import org.siglus.siglusapi.domain.RequisitionMonthlyReport;
import org.siglus.siglusapi.domain.SiglusReportType;
import org.siglus.siglusapi.domain.report.RequisitionMonthlyNotSubmitReport;
import org.siglus.siglusapi.domain.report.RequisitionMonthlyReportFacility;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.ProgramRequisitionNameMappingRepository;
import org.siglus.siglusapi.repository.RequisitionMonthReportRepository;
import org.siglus.siglusapi.repository.RequisitionMonthlyNotSubmitReportRepository;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
import org.siglus.siglusapi.repository.dto.FacillityStockCardDateDto;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
import org.siglus.siglusapi.service.SiglusProgramAdditionalOrderableService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionReportTaskService {
  private final ProgramReferenceDataService programDataService;
  private final FacilityReferenceDataService facilityReferenceDataService;
  private final RequisitionMonthReportRepository requisitionMonthReportRepository;
  private final RequisitionMonthlyNotSubmitReportRepository requisitionMonthlyNotSubmitReportRepository;
  private final FacilityNativeRepository facilityNativeRepository;
  private final ProgramRequisitionNameMappingRepository programRequisitionNameMappingRepository;
  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  private final SiglusProgramAdditionalOrderableService siglusProgramAdditionalOrderableService;
  private final SiglusReportTypeRepository reportTypeRepository;
  private final SiglusProcessingPeriodService siglusProcessingPeriodService;
  private final PeriodService periodService;
  private static final Map<Integer, String> MONTH_NAME_MAP = new HashMap<>();

  static {
    MONTH_NAME_MAP.put(1, "Jan");
    MONTH_NAME_MAP.put(2, "Fev");
    MONTH_NAME_MAP.put(3, "Mar");
    MONTH_NAME_MAP.put(4, "Abr");
    MONTH_NAME_MAP.put(5, "Mai");
    MONTH_NAME_MAP.put(6, "Jun");
    MONTH_NAME_MAP.put(7, "Jul");
    MONTH_NAME_MAP.put(8, "Ago");
    MONTH_NAME_MAP.put(9, "Set");
    MONTH_NAME_MAP.put(10, "Out");
    MONTH_NAME_MAP.put(11, "Nov");
    MONTH_NAME_MAP.put(12, "Dez");
  }

  public void refresh() {
    log.info("refresh. start = " + System.currentTimeMillis());
    try {
      doRefresh(getDataWrapper());
    } catch (Exception e) {
      log.error("refresh with exception. msg = " + e.getMessage(), e);
      throw e;
    }
    log.info("refresh. end = " + System.currentTimeMillis());
  }

  private DataWrapper getDataWrapper() {
    return new DataWrapper();
  }

  public void doRefresh(DataWrapper dataWrapper) {
    for (FacilityDto facilityDto : dataWrapper.allFacilityDto) {
      RequisitionMonthlyReportFacility facilityInfo = dataWrapper.monthlyReportFacilityMap.get(facilityDto.getId());
      List<SiglusReportType> facilityReportTypeList = reportTypeRepository.findByFacilityId(facilityDto.getId());

      org.siglus.siglusapi.dto.FacilityDto facilityWithSupportedPrograms =
          siglusFacilityReferenceDataService.findOne(facilityDto.getId());
      if (Boolean.FALSE.equals(facilityWithSupportedPrograms.getActive())) {
        continue;
      }
      List<RequisitionMonthlyNotSubmitReport> notSubmitList =
          getFacilityNotSubmitRequisitions(facilityInfo, facilityReportTypeList, dataWrapper);

      updateBatch(facilityInfo.getFacilityId(), notSubmitList);
    }
  }

  private List<RequisitionMonthlyNotSubmitReport> getFacilityNotSubmitRequisitions(
      RequisitionMonthlyReportFacility facilityInfo, List<SiglusReportType> reportTypes, DataWrapper dataWrapper) {
    List<RequisitionMonthlyNotSubmitReport> notSubmitList = new ArrayList<>();
    for (ProgramDto programDto : dataWrapper.allProgramDto) {
      UUID programId = programDto.getId();
      String programCode = dataWrapper.programIdToRequisitionNameMapping.get(programId).getReportName();
      FacillityStockCardDateDto facillityStockCardDateDto = dataWrapper.facilityProgramToStockCardDate.get(
          dataWrapper.getUniqueKey(facilityInfo.getFacilityId(), programId));
      if (facillityStockCardDateDto == null) {
        log.info(String.format("has no Stock in this program , programCode=%s, facilityId=%s", programCode,
            facilityInfo.getFacilityId()));
        continue;
      }
      Optional<SiglusReportType> reportType =
          reportTypes.stream().filter(item -> programDto.getCode().equals(item.getProgramCode())).findFirst();
      if (!reportType.isPresent()) {
        log.info(String.format("reportType not exists, programCode=%s, facilityId=%s", programCode,
            facilityInfo.getFacilityId()));
        continue;
      }
      List<RequisitionMonthlyNotSubmitReport> programNotSubmitRequisitions =
          getPeriodNotSubmitRequisitions(facilityInfo, programDto, dataWrapper);
      notSubmitList.addAll(programNotSubmitRequisitions);

    }
    return notSubmitList;
  }

  private List<RequisitionMonthlyNotSubmitReport> getPeriodNotSubmitRequisitions(
      RequisitionMonthlyReportFacility facilityInfo, ProgramDto programDto, DataWrapper dataWrapper) {
    Collection<ProcessingPeriodDto> periods = siglusProcessingPeriodService.fillProcessingPeriodWithExtension(
        periodService.searchByProgramAndFacility(programDto.getId(), facilityInfo.getFacilityId()));

    List<ProcessingPeriodDto> sortedPeriods =
        periods.stream().sorted(Comparator.comparing(ProcessingPeriodDto::getStartDate)).collect(Collectors.toList());
    List<RequisitionMonthlyNotSubmitReport> notSubmitPeriodList = new ArrayList<>();
    for (ProcessingPeriodDto processingPeriodDto : sortedPeriods) {
      RequisitionMonthlyReport requisitionMonthlyReport = dataWrapper.requisitionMonthlyReportKeyToReport.get(
          dataWrapper.getUniqueKey(facilityInfo.getFacilityId(), processingPeriodDto.getId(),
              programDto.getId()));
      if (requisitionMonthlyReport != null) {
        // already exists rr
        continue;
      }
      RequisitionMonthlyNotSubmitReport notSubmit = getRequisitionMonthlyNotSubmitReport(
          dataWrapper.programIdToRequisitionNameMapping, facilityInfo.getFacilityId(), facilityInfo, programDto.getId(),
          processingPeriodDto);
      notSubmitPeriodList.add(notSubmit);
    }
    return notSubmitPeriodList;
  }



  public void updateBatch(UUID facilityId, List<RequisitionMonthlyNotSubmitReport> notSubmitList) {
    requisitionMonthlyNotSubmitReportRepository.deleteByFacilityId(facilityId);
    if (CollectionUtils.isEmpty(notSubmitList)) {
      return;
    }
    log.info(String.format("[save] notSubmitList size =%s, facilityId=%s", notSubmitList.size(), facilityId));
    requisitionMonthlyNotSubmitReportRepository.save(notSubmitList);
  }

  private RequisitionMonthlyNotSubmitReport getRequisitionMonthlyNotSubmitReport(
      Map<UUID, ProgramRequisitionNameMapping> requisitionNameMappingMap, UUID facilityId,
      RequisitionMonthlyReportFacility requisitionMonthlyReportFacility, UUID programId,
      ProcessingPeriodDto processingPeriodDto) {
    return RequisitionMonthlyNotSubmitReport.builder()
        .programId(programId)
        .processingPeriodId(processingPeriodDto.getId())

        .district(requisitionMonthlyReportFacility.getDistrict())
        .province(requisitionMonthlyReportFacility.getProvince())
        .facilityName(requisitionMonthlyReportFacility.getFacilityName())
        .ficilityCode(requisitionMonthlyReportFacility.getFicilityCode())
        .facilityType(requisitionMonthlyReportFacility.getFacilityType())
        .facilityMergeType(requisitionMonthlyReportFacility.getFacilityMergeType())
        .districtFacilityCode(requisitionMonthlyReportFacility.getDistrictFacilityCode())
        .provinceFacilityCode(requisitionMonthlyReportFacility.getProvinceFacilityCode())

        .originalPeriod(formatDate(processingPeriodDto))
        .reportName(requisitionNameMappingMap.get(programId).getReportName())
        .inventorydDate(null)
        .statusDetail(null)
        .submittedStatus("Not submitted")
        .requisitionPeriod(processingPeriodDto.getEndDate())
        .reportType(null)
        .submittedTime(null)
        .syncTime(null)
        .facilityId(facilityId)
        .submittedUser(null)
        .clientSubmittedTime(null)
        .requisitionCreatedDate(null)
        .statusLastCreateDdate(null)
        .submitStartDate(processingPeriodDto.getSubmitStartDate())
        .submitEndDate(processingPeriodDto.getSubmitEndDate())
        .build();
  }

  private String formatDate(ProcessingPeriodDto processingPeriod) {

    return
        processingPeriod.getStartDate().getDayOfMonth() + " "
            + MONTH_NAME_MAP.getOrDefault(processingPeriod.getStartDate().getMonth().getValue(), StringUtils.EMPTY)
            + " " + processingPeriod.getStartDate().getYear()
            + " - "
            + processingPeriod.getEndDate().getDayOfMonth() + " "
            + MONTH_NAME_MAP.getOrDefault(processingPeriod.getEndDate().getMonth().getValue(), StringUtils.EMPTY)
            + " " + processingPeriod.getEndDate().getYear();
  }

  public class DataWrapper {
    protected List<FacilityDto> allFacilityDto;
    protected Map<UUID, RequisitionMonthlyReportFacility> monthlyReportFacilityMap;
    protected List<ProgramDto> allProgramDto;
    protected Map<UUID, ProgramRequisitionNameMapping> programIdToRequisitionNameMapping;
    protected Map<String, FacillityStockCardDateDto> facilityProgramToStockCardDate;
    protected Map<String, RequisitionMonthlyReport> requisitionMonthlyReportKeyToReport;

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
      programIdToRequisitionNameMapping = requisitionNameMapping.stream()
          .collect(Collectors.toMap(ProgramRequisitionNameMapping::getProgramId, Function.identity()));


      List<FacillityStockCardDateDto> firstStockCardGroupByFacility =
          getAllFacilityStockCardDateDtos(allProgramDto);

      facilityProgramToStockCardDate = firstStockCardGroupByFacility.stream()
          .collect(Collectors.toMap(
              item -> getUniqueKey(item.getFacilityId(),
                  item.getProgramId()), Function.identity()));

      List<RequisitionMonthlyReport> requisitionMonthlyReports = requisitionMonthReportRepository.findAll();
      requisitionMonthlyReports = requisitionMonthlyReports.stream()
          .filter(item -> item.getRequisitionCreatedDate() != null).collect(Collectors.toList());
      requisitionMonthlyReportKeyToReport = requisitionMonthlyReports.stream()
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

    private List<FacillityStockCardDateDto> getAllFacilityStockCardDateDtos(List<ProgramDto> allProgramDto) {
      List<FacillityStockCardDateDto> firstStockCardGroupByFacility
          = facilityNativeRepository.findFirstStockCardGroupByFacility();
      firstStockCardGroupByFacility.addAll(findMalariaFacilityStockCardDate(allProgramDto));
      return firstStockCardGroupByFacility;
    }

    private List<FacillityStockCardDateDto> findMalariaFacilityStockCardDate(List<ProgramDto> allProgramDto) {
      Optional<ProgramDto> malariaProgram =
          allProgramDto.stream()
              .filter(item -> ProgramConstants.MALARIA_PROGRAM_CODE.equals(item.getCode())).findFirst();
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
      result.forEach(facilityStockCardDateDto -> facilityStockCardDateDto.setProgramId(malariaProgram.get().getId()));
      return result;
    }
  }
}
