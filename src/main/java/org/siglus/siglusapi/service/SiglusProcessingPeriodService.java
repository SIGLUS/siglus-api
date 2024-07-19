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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_WRONG_CLIENT_FACILITY;
import static org.siglus.siglusapi.util.RequisitionUtil.getRequisitionExtraData;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionPeriodDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.constant.FacilityTypeConstants;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.ProcessingPeriodSearchParams;
import org.siglus.siglusapi.dto.SimpleRequisitionDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.RequisitionNativeSqlRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.SiglusProcessingPeriodValidator;
import org.siglus.siglusapi.web.response.RequisitionPeriodExtensionResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

@Service
public class SiglusProcessingPeriodService {

  @Autowired
  private SiglusProcessingPeriodReferenceDataService siglusProcessingPeriodReferenceDataService;

  @Autowired
  private ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  @Autowired
  private SiglusProcessingPeriodValidator siglusProcessingPeriodValidator;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private PeriodService periodService;

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Autowired
  private RequisitionNativeSqlRepository requisitionNativeSqlRepository;

  @Autowired
  private SiglusProgramService siglusProgramService;

  @Autowired
  private PeriodReferenceDataService periodReferenceDataService;

  @Autowired
  private ProcessingPeriodRepository processingPeriodRepository;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private SiglusFacilityService siglusFacilityService;

  @Autowired
  private SiglusFacilityRepository siglusFacilityRepository;

  public static final Integer MAX_COUNT_EMERGENCY_REQUISITION = 2;

  public LocalDate getPreviousPeriodStartDateSinceInitiate(String programCode, UUID facilityId) {
    ProgramDto program = siglusProgramService.getProgramByCode(programCode)
        .orElseThrow(() -> new NotFoundException("Program code" + programCode + " Not Found"));

    List<Requisition> requisitions = requisitionRepository.searchRequisitions(
        null, facilityId, program.getId(), false);
    if (CollectionUtils.isEmpty(requisitions)) {
      return null;
    }
    Set<UUID> startedPeriodIds = requisitions.stream()
        .map(Requisition::getProcessingPeriodId).collect(Collectors.toSet());
    List<ProcessingPeriodDto> sortedPeriods = periodReferenceDataService
        .searchByProgramAndFacility(program.getId(), facilityId)
        .stream()
        .sorted(Comparator.comparing(ProcessingPeriodDto::getStartDate))
        .collect(Collectors.toList());

    int lastStartedIndex = -1;
    for (int i = sortedPeriods.size() - 1; i >= 0; i--) {
      if (startedPeriodIds.contains(sortedPeriods.get(i).getId())) {
        lastStartedIndex = i;
        break;
      }
    }

    if (lastStartedIndex > 0) {
      return sortedPeriods.get(lastStartedIndex - 1).getStartDate();
    }
    return null;
  }

  @Transactional
  public ProcessingPeriodDto createProcessingPeriod(ProcessingPeriodDto periodDto) {

    siglusProcessingPeriodValidator.validSubmitDuration(periodDto);

    ProcessingPeriodDto savedDto = siglusProcessingPeriodReferenceDataService
        .saveProcessingPeriod(periodDto);

    ProcessingPeriodExtension processingPeriodExtension = new ProcessingPeriodExtension();
    processingPeriodExtension.setSubmitStartDate(periodDto.getSubmitStartDate());
    processingPeriodExtension.setSubmitEndDate(periodDto.getSubmitEndDate());
    processingPeriodExtension.setProcessingPeriodId(savedDto.getId());
    ProcessingPeriodExtension extension = processingPeriodExtensionRepository
        .save(processingPeriodExtension);

    return combine(savedDto, extension);
  }

  public Page<ProcessingPeriodDto> getAllProcessingPeriods(
      MultiValueMap<String, Object> requestParams, Pageable pageable) {

    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(requestParams);
    Page<ProcessingPeriodDto> page = siglusProcessingPeriodReferenceDataService
        .searchProcessingPeriods(
            params.getProcessingScheduleId(),
            params.getProgramId(),
            params.getFacilityId(),
            params.getStartDate(),
            params.getEndDate(),
            params.getIds(),
            pageable
        );
    List<ProcessingPeriodExtension> extensions = processingPeriodExtensionRepository.findAll();

    Map<UUID, ProcessingPeriodExtension> map = new HashMap<>();
    extensions.forEach(extension -> map.put(extension.getProcessingPeriodId(), extension));

    page.getContent().forEach(dto -> {
      ProcessingPeriodExtension processingPeriodExtension = map.get(dto.getId());
      combine(dto, processingPeriodExtension);
    });

    return page;
  }

  public ProcessingPeriodDto getProcessingPeriodDto(UUID periodId) {
    ProcessingPeriodDto dto = siglusProcessingPeriodReferenceDataService.findOne(periodId);
    return getProcessingPeriodbyOpenLmisPeroid(dto);
  }

  public List<ProcessingPeriod> getUpToNowMonthlyPeriods() {
    return processingPeriodRepository.getUpToNowMonthlyPeriods(LocalDate.now());
  }

  public ProcessingPeriodDto getProcessingPeriodbyOpenLmisPeroid(ProcessingPeriodDto dto) {
    ProcessingPeriodExtension extension = processingPeriodExtensionRepository
        .findByProcessingPeriodId(dto.getId());

    return combine(dto, extension);
  }

  private ProcessingPeriodDto combine(ProcessingPeriodDto dto,
      ProcessingPeriodExtension extension) {
    if (extension != null) {
      dto.setSubmitStartDate(extension.getSubmitStartDate());
      dto.setSubmitEndDate(extension.getSubmitEndDate());
    }
    return dto;
  }

  public List<RequisitionPeriodExtensionResponse> getRequisitionPeriodExtensionResponses(UUID program, UUID facility,
      boolean emergency) {
    UUID homeFacilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    if (!ObjectUtils.isEmpty(homeFacilityId) && !homeFacilityId.equals(facility)) {
      checkClientFacilityId(homeFacilityId, facility, program);
    }
    Collection<RequisitionPeriodDto> requisitionPeriodDtos = getRequisitionPeriods(program, facility, emergency);
    if (CollectionUtils.isEmpty(requisitionPeriodDtos)) {
      return requisitionPeriodDtos.stream().map(this::convertToRequisitionPeriodExtensionResponse)
          .collect(Collectors.toList());
    }

    Map<UUID, SimpleRequisitionDto> requisitionIdToSimpleRequisition = requisitionNativeSqlRepository
        .findSimpleRequisitionDto(getRequisitionIds(requisitionPeriodDtos)).stream()
        .collect(Collectors.toMap(SimpleRequisitionDto::getId, e -> e));

    return requisitionPeriodDtos.stream()
        .map(dto -> buildRequisitionPeriodExtensionResponse(dto, requisitionIdToSimpleRequisition))
        .collect(Collectors.toList());
  }

  private void checkClientFacilityId(UUID supplyFacilityId, UUID clientFacilityId, UUID programId) {
    Set<UUID> clientFacilityIds = siglusFacilityService.getAllClientFacilities(supplyFacilityId, programId).stream()
        .map(Facility::getId)
        .collect(Collectors.toSet());
    if (!clientFacilityIds.contains(clientFacilityId)) {
      throw new BusinessDataException(new Message(ERROR_WRONG_CLIENT_FACILITY));
    }
  }

  public RequisitionPeriodExtensionResponse buildRequisitionPeriodExtensionResponse(RequisitionPeriodDto dto,
      Map<UUID, SimpleRequisitionDto> requisitionIdToSimpleRequisition) {
    SimpleRequisitionDto simpleRequisitionDto = requisitionIdToSimpleRequisition.get(dto.getRequisitionId());
    RequisitionPeriodExtensionResponse response = convertToRequisitionPeriodExtensionResponse(dto);
    response.setRequisitionExtraData(getRequisitionExtraData(simpleRequisitionDto));
    return response;
  }

  private Set<UUID> getRequisitionIds(Collection<RequisitionPeriodDto> requisitionPeriodDtos) {
    return requisitionPeriodDtos.stream()
        .map(RequisitionPeriodDto::getRequisitionId)
        .collect(Collectors.toSet());
  }

  private RequisitionPeriodExtensionResponse convertToRequisitionPeriodExtensionResponse(RequisitionPeriodDto dto) {
    RequisitionPeriodExtensionResponse response = new RequisitionPeriodExtensionResponse();
    BeanUtils.copyProperties(dto, response);
    return response;
  }

  // get periods for initiate
  @SuppressWarnings("PMD.CyclomaticComplexity")
  private Collection<RequisitionPeriodDto> getRequisitionPeriods(UUID program, UUID facility, boolean emergency) {
    ProgramDto programDto = siglusProgramService.getProgram(program);
    if (emergency && !ProgramConstants.VIA_PROGRAM_CODE.equals(programDto.getCode())) {
      return new ArrayList<>();
    }
    Collection<ProcessingPeriodDto> periods = fillProcessingPeriodWithExtension(
        periodService.searchByProgramAndFacility(program, facility));

    List<UUID> currentPeriodIds = periodService.getCurrentPeriods(program, facility)
        .stream().map(ProcessingPeriodDto::getId).collect(Collectors.toList());
    List<RequisitionPeriodDto> requisitionPeriods = new ArrayList<>();
    LocalDate maxEndDate = LocalDate.of(2000, 1, 1);

    // TODO Optimization
    for (ProcessingPeriodDto period : periods) {

      List<Requisition> requisitions = requisitionRepository.searchRequisitions(
          period.getId(), facility, program, emergency);

      List<Requisition> preAuthorizeRequisitions = getPreAuthorizedRequisitions(program, facility,
          requisitions);

      if (emergency) {
        if (requisitions.size() > MAX_COUNT_EMERGENCY_REQUISITION) {
          continue;
        }
        processingRemainningEmergencyRequisitionPeriod(requisitionPeriods, currentPeriodIds,
            preAuthorizeRequisitions, period, program, facility);
        if (!requisitionPeriods.isEmpty()) {
          break;
        }
      } else {
        RequisitionPeriodDto requisitionPeriod = RequisitionPeriodDto.newInstance(period);
        requisitionPeriods.add(requisitionPeriod);
        if (!requisitions.isEmpty()) {
          LocalDate endDate = requisitionPeriod.getEndDate();
          maxEndDate = maxEndDate.isAfter(endDate) ? maxEndDate : endDate;
          if (preAuthorizeRequisitions.isEmpty()) {
            requisitionPeriods.remove(requisitionPeriod);
          } else {
            requisitionPeriod.setRequisitionId(preAuthorizeRequisitions.get(0).getId());
            requisitionPeriod.setRequisitionStatus(preAuthorizeRequisitions.get(0).getStatus());
          }
        }
      }
    }

    if (emergency && requisitionPeriods.isEmpty()) {
      processingNewCreatedEmergencyRequisitionPeriod(program, facility, currentPeriodIds, requisitionPeriods);
    }

    Iterator<RequisitionPeriodDto> iterator = requisitionPeriods.iterator();
    while (iterator.hasNext()) {
      RequisitionPeriodDto requisitionPeriod = iterator.next();
      LocalDate endDate = requisitionPeriod.getEndDate();
      if (endDate.isBefore(maxEndDate)) {
        iterator.remove();
      }
    }

    return limitFirstPeriodToOneYear(requisitionPeriods, periods);
  }

  private void processingNewCreatedEmergencyRequisitionPeriod(UUID program, UUID facility, List<UUID> currentPeriodIds,
      List<RequisitionPeriodDto> requisitionPeriods) {
    LocalDate currentDate = LocalDate.now();
    ProcessingPeriodDto currentPeriod = periodService.getPeriod(currentPeriodIds.get(0));
    String facilityTypeCode = siglusFacilityRepository.findOne(facility).getType().getCode();
    Set<String> firstLevelTypes = FacilityTypeConstants.getFirstLevelTypes();
    Set<String> secondLevelTypes = FacilityTypeConstants.getSecondLevelTypes();
    Set<String> thirdLevelTypes = FacilityTypeConstants.getThirdLevelTypes();
    Set<String> statusSet = RequisitionStatus.getAfterAuthorizedStatus().stream().map(Enum::name)
        .collect(Collectors.toSet());
    if (firstLevelTypes.contains(facilityTypeCode)) {
      // if current period: 5.21-6.20 June
      // when today is 5.21, the emergency show the May period
      // when today is 6.10, the emergency show the May period
      // when today is 6.11, the emergency show the May period
      // when today is 6.17, the emergency show the May period
      // when today is 6.18, the emergency show the June period
      // when today is 6.20, the emergency show the June period
      // when today is 6.21, then the current period: 6.21-7.20 July, the emergency still show the June period
      // when today is 7.11, the current period: 6.21-7.20 July, the emergency should still show the june period
      // when today is 7.18, the current period: 6.21-7.20 July, the emergency should show the July period
      if (currentDate.isBefore(
          LocalDate.of(currentPeriod.getEndDate().getYear(), currentPeriod.getEndDate().getMonth(), 18))) {
        ProcessingPeriodDto previousPeriod = periodService.findPreviousPeriod(currentPeriod.getId());
        processEmergencyList(program, facility, requisitionPeriods, statusSet, previousPeriod, facilityTypeCode);
      } else {
        processEmergencyList(program, facility, requisitionPeriods, statusSet, currentPeriod, facilityTypeCode);
      }
    } else if (secondLevelTypes.contains(facilityTypeCode)) {
      if (currentDate.isBefore(
          LocalDate.of(currentPeriod.getEndDate().getYear(), currentPeriod.getEndDate().getMonth(), 25))) {
        ProcessingPeriodDto previousPeriod = periodService.findPreviousPeriod(currentPeriod.getId());
        processEmergencyList(program, facility, requisitionPeriods, statusSet, previousPeriod, facilityTypeCode);
      } else {
        processEmergencyList(program, facility, requisitionPeriods, statusSet, currentPeriod, facilityTypeCode);
      }
    } else if (thirdLevelTypes.contains(facilityTypeCode)) {
      YearMonth yearMonth = YearMonth.from(currentPeriod.getEndDate()).minusMonths(1);
      if (currentDate.isBefore(LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), 30))
          || currentDate.equals(LocalDate.of(yearMonth.getYear(), yearMonth.getMonth(), 30))) {
        ProcessingPeriodDto previousPeriod = periodService.findPreviousPeriod(currentPeriod.getId());
        processEmergencyList(program, facility, requisitionPeriods, statusSet, previousPeriod, facilityTypeCode);
      } else {
        processEmergencyList(program, facility, requisitionPeriods, statusSet, currentPeriod, facilityTypeCode);
      }
    }
  }

  private void processEmergencyList(UUID program, UUID facility, List<RequisitionPeriodDto> requisitionPeriods,
      Set<String> statusSet, ProcessingPeriodDto periodDto, String facilityTypeCode) {
    List<Requisition> requisitions = requisitionRepository.searchRequisitions(
        periodDto.getId(), facility, program, Boolean.TRUE);
    if (requisitions.size() < MAX_COUNT_EMERGENCY_REQUISITION) {
      setSubmitStartAndEndDate(periodDto, facilityTypeCode);
      requisitionPeriods.add(RequisitionPeriodDto.newInstance(periodDto));
      if (CollectionUtils.isNotEmpty(siglusRequisitionRepository.searchAfterAuthorizedRequisitions(
          facility, program, periodDto.getId(), Boolean.FALSE, statusSet))) {
        // for emergency, requisitionPeriods only have one element
        requisitionPeriods.forEach(requisitionPeriodDto ->
            requisitionPeriodDto.setCurrentPeriodRegularRequisitionAuthorized(true));
      }
    }
  }

  private void setSubmitStartAndEndDate(ProcessingPeriodDto periodDto, String facilityTypeCode) {
    Set<String> firstLevelTypes = FacilityTypeConstants.getFirstLevelTypes();
    Set<String> secondLevelTypes = FacilityTypeConstants.getSecondLevelTypes();
    Set<String> thirdLevelTypes = FacilityTypeConstants.getThirdLevelTypes();
    LocalDate submitStartDate = null;
    LocalDate submitEndDate = null;
    YearMonth yearMonth = YearMonth.from(periodDto.getEndDate()).plusMonths(1);
    if (firstLevelTypes.contains(facilityTypeCode)) {
      submitStartDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 11);
      submitEndDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 17);
    } else if (secondLevelTypes.contains(facilityTypeCode)) {
      submitStartDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 11);
      submitEndDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 24);
    } else if (thirdLevelTypes.contains(facilityTypeCode)) {
      submitStartDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 16);
      yearMonth = yearMonth.plusMonths(1);
      submitEndDate = LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), 30);
    }
    periodDto.setSubmitStartDate(submitStartDate);
    periodDto.setSubmitEndDate(submitEndDate);
  }

  private List<RequisitionPeriodDto> limitFirstPeriodToOneYear(List<RequisitionPeriodDto> requisitionPeriods,
      Collection<ProcessingPeriodDto> periods) {
    int limitPeriodNumber = 14;
    if (periods.size() < limitPeriodNumber + 1) {
      return requisitionPeriods;
    }
    List<ProcessingPeriodDto> periodDtos = new ArrayList<>(periods);
    LocalDate limitPeriodStartDate = periodDtos.get(periodDtos.size() - (limitPeriodNumber + 1)).getStartDate();
    LocalDate limitPeriodEndDate = periodDtos.get(periodDtos.size() - (limitPeriodNumber + 1)).getEndDate();

    List<RequisitionPeriodDto> requisitionPeriodDtos = requisitionPeriods.subList(
        Math.max(requisitionPeriods.size() - limitPeriodNumber, 0),
        requisitionPeriods.size());

    return requisitionPeriodDtos.stream()
        .filter(requisitionPeriodDto -> requisitionPeriodDto.getStartDate().isAfter(limitPeriodStartDate)
            && requisitionPeriodDto.getEndDate().isAfter(limitPeriodEndDate))
        .collect(Collectors.toList());
  }

  private void processingRemainningEmergencyRequisitionPeriod(
      List<RequisitionPeriodDto> requisitionPeriods,
      List<UUID> currentPeriodIds,
      List<Requisition> preAuthorizeRequisitions,
      ProcessingPeriodDto period, UUID program, UUID facility
  ) {
    if (!preAuthorizeRequisitions.isEmpty()) {
      RequisitionPeriodDto requisitionPeriod = RequisitionPeriodDto.newInstance(period);
      requisitionPeriods.add(requisitionPeriod);
      getFirstPreAuthorizeRequisition(preAuthorizeRequisitions, requisitionPeriod);
    }
    Set<String> statusSet = RequisitionStatus.getAfterAuthorizedStatus().stream().map(Enum::name)
        .collect(Collectors.toSet());
    if (CollectionUtils.isNotEmpty(requisitionPeriods)) {
      String facilityTypeCode = siglusFacilityRepository.findOne(facility).getType().getCode();
      requisitionPeriods.forEach(
          requisitionPeriodDto -> setSubmitStartAndEndDate(requisitionPeriodDto, facilityTypeCode));
      if (CollectionUtils.isNotEmpty(siglusRequisitionRepository.searchAfterAuthorizedRequisitions(
          facility, program, period.getId(), Boolean.FALSE, statusSet))) {
        // for emergency, requisitionPeriods only have one element
        requisitionPeriods.forEach(requisitionPeriodDto ->
            requisitionPeriodDto.setCurrentPeriodRegularRequisitionAuthorized(true));
      }
    }
  }

  private void getFirstPreAuthorizeRequisition(List<Requisition> preAuthorizeRequisitions,
      RequisitionPeriodDto requisitionPeriod) {
    Requisition firstPreAuthorizeRequisition = preAuthorizeRequisitions.stream()
        .min(Comparator.comparing(Requisition::getCreatedDate))
        .orElseThrow(() -> new NotFoundException("first Requisition Not Found"));
    requisitionPeriod.setRequisitionId(firstPreAuthorizeRequisition.getId());
    requisitionPeriod.setRequisitionStatus(firstPreAuthorizeRequisition.getStatus());
  }

  public Collection<ProcessingPeriodDto> fillProcessingPeriodWithExtension(
      Collection<ProcessingPeriodDto> periods) {
    List<ProcessingPeriodExtension> extensions = processingPeriodExtensionRepository.findAll();

    Map<UUID, ProcessingPeriodExtension> map = new HashMap<>();
    extensions.forEach(extension -> map.put(extension.getProcessingPeriodId(), extension));

    periods.forEach(dto -> {
      ProcessingPeriodExtension processingPeriodExtension = map.get(dto.getId());
      combine(dto, processingPeriodExtension);
    });

    return periods;
  }

  private List<Requisition> getPreAuthorizedRequisitions(UUID program, UUID facility,
      List<Requisition> requisitions) {
    List<Requisition> preAuthorizeRequisitions = new ArrayList<>();
    if (permissionService.canInitRequisition(program, facility).isSuccess()) {
      preAuthorizeRequisitions.addAll(requisitions.stream()
          .filter(requisition -> requisition.getStatus().isSubmittable())
          .collect(Collectors.toList()));
    }

    Requisition dummy = new Requisition();
    dummy.setProgramId(program);
    dummy.setFacilityId(facility);
    if (permissionService.canAuthorizeRequisition(dummy).isSuccess()) {
      preAuthorizeRequisitions.addAll(requisitions.stream()
          .filter(requisition -> requisition.getStatus().isPreAuthorize())
          .collect(Collectors.toList()));
    }
    return preAuthorizeRequisitions;
  }

}
