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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class SiglusProcessingPeriodService {

  @Autowired
  private SiglusProcessingPeriodReferenceDataService siglusProcessingPeriodReferenceDataService;

  @Autowired
  private ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  @Autowired
  private SiglusProcessingPeriodExtensionService siglusProcessingPeriodExtensionService;

  @Autowired
  private SiglusProcessingPeriodValidator siglusProcessingPeriodValidator;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private PeriodService periodService;

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  private RequisitionNativeSqlRepository requisitionNativeSqlRepository;

  @Autowired
  private SiglusRequisitionRepository siglusRequisitionRepository;

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

  public static final int MAX_COUNT_EMERGENCY_REQUISITION = 2;
  public static final int MAX_COUNT_REGULAR_REQUISITION = 14;

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
    ProcessingPeriodExtension extension = processingPeriodExtensionRepository.save(processingPeriodExtension);

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
    List<ProcessingPeriodExtension> extensions = siglusProcessingPeriodExtensionService.findAll();

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
    return getProcessingPeriodByOpenLmisPeriod(dto);
  }

  public List<ProcessingPeriod> getUpToNowMonthlyPeriods() {
    return processingPeriodRepository.getUpToNowMonthlyPeriods(LocalDate.now());
  }

  public ProcessingPeriodDto getProcessingPeriodByOpenLmisPeriod(ProcessingPeriodDto dto) {
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

  private List<RequisitionPeriodDto> getRequisitionPeriods(UUID programId, UUID facilityId, boolean emergency) {
    Facility facility = siglusFacilityRepository.findOne(facilityId);
    ProgramDto program = siglusProgramService.getProgram(programId);
    return emergency
        ? getEmergencyRequisitionPeriods(program, facility)
        : getRegularRequisitionPeriods(program, facility.getId());
  }

  private List<RequisitionPeriodDto> getRegularRequisitionPeriods(ProgramDto program, UUID facility) {
    Collection<ProcessingPeriodDto> periods = fillProcessingPeriodWithExtension(
        periodService.searchByProgramAndFacility(program.getId(), facility));
    List<ProcessingPeriodDto> sortedPeriods = periods.stream()
        .sorted(Comparator.comparing(ProcessingPeriodDto::getStartDate)).collect(Collectors.toList());

    List<RequisitionPeriodDto> requisitionPeriods = new ArrayList<>();
    Map<UUID, Requisition> existedRequisitions =
        siglusRequisitionRepository.findByFacilityIdAndProgramIdAndEmergency(facility, program.getId(), false)
            .stream()
            .collect(Collectors.toMap(Requisition::getProcessingPeriodId, Function.identity()));

    boolean canInit = permissionService.canInitRequisition(program.getId(), facility).isSuccess();
    boolean canAuthorize = canAuthorizeRequisition(program.getId(), facility);
    for (ProcessingPeriodDto period : sortedPeriods) {
      Requisition requisition = existedRequisitions.get(period.getId());
      if (requisition == null) {
        requisitionPeriods.add(RequisitionPeriodDto.newInstance(period));
      } else if (showRegularRequisition(requisition, canInit, canAuthorize)) {
        RequisitionPeriodDto requisitionPeriodDto = RequisitionPeriodDto.newInstance(period);
        requisitionPeriodDto.setRequisitionId(requisition.getId());
        requisitionPeriodDto.setRequisitionStatus(requisition.getStatus());
        requisitionPeriods.add(requisitionPeriodDto);
      }
      if (requisitionPeriods.size() > MAX_COUNT_REGULAR_REQUISITION) {
        break;
      }
    }
    return requisitionPeriods;
  }

  private boolean showRegularRequisition(Requisition requisition, boolean canInit, boolean canAuthorize) {
    return (requisition.getStatus().isSubmittable() && (canInit || canAuthorize))
        || (requisition.getStatus() == RequisitionStatus.SUBMITTED && canAuthorize);
  }

  private List<RequisitionPeriodDto> getEmergencyRequisitionPeriods(ProgramDto program, Facility facility) {
    if (!ProgramConstants.VIA_PROGRAM_CODE.equals(program.getCode())) {
      return new ArrayList<>();
    }
    Collection<ProcessingPeriodDto> periodDtos =
        periodService.searchByProgramAndFacility(program.getId(), facility.getId());
    Map<UUID, ProcessingPeriodDto> periodDtoMap = fillProcessingPeriodWithExtension(periodDtos)
        .stream().collect(Collectors.toMap(ProcessingPeriodDto::getId, Function.identity()));
    List<RequisitionPeriodDto> submittedRequisitionPeriod =
        getEmergencyProcessingRequisitionPeriod(program.getId(), facility, periodDtoMap);

    ProcessingPeriodDto emergencyPeriodDto =
        findEmergencyRequisitionPeriod(program.getId(), facility.getId(), periodDtoMap);
    if (ObjectUtils.isEmpty(emergencyPeriodDto)) {
      return submittedRequisitionPeriod;
    }
    if (!regularRequisitionIsAuthorized(program.getId(), facility.getId(), emergencyPeriodDto)) {
      RequisitionPeriodDto requisitionPeriodDto = buildEmergencyRequisitionPeriod(emergencyPeriodDto, facility);
      requisitionPeriodDto.setCurrentPeriodRegularRequisitionAuthorized(false);
      submittedRequisitionPeriod.add(requisitionPeriodDto);
      return submittedRequisitionPeriod;
    }
    RequisitionPeriodDto requisitionPeriod =
        processEmergencyRequisitionPeriod(program.getId(), facility, emergencyPeriodDto);
    if (requisitionPeriod != null && submittedRequisitionPeriod.stream()
          .noneMatch(submitted -> submitted.getId().equals(requisitionPeriod.getId()))) {
      submittedRequisitionPeriod.add(requisitionPeriod);
    }
    return submittedRequisitionPeriod;
  }

  private ProcessingPeriodDto findEmergencyRequisitionPeriod(UUID program, UUID facility,
                                                             Map<UUID, ProcessingPeriodDto> periodDtoMap) {
    List<ProcessingPeriodDto> currentPeriods = periodService.getCurrentPeriods(program, facility);
    if (ObjectUtils.isEmpty(currentPeriods)) {
      return null;
    }
    ProcessingPeriodDto periodDto = periodDtoMap.get(currentPeriods.get(0).getId());
    if (periodDto == null) {
      return null;
    }
    LocalDate currentDate = LocalDate.now();
    ProcessingPeriodDto findPeriod = null;
    while (findPeriod == null) {
      ProcessingPeriodDto previousPeriodDto = periodService.findPreviousPeriod(periodDto.getId());
      if (previousPeriodDto == null) {
        return null;
      }
      ProcessingPeriodDto previousPeriod = periodDtoMap.get(previousPeriodDto.getId());
      if (previousPeriod == null) {
        return null;
      }
      if (currentDate.isAfter(previousPeriod.getSubmitEndDate())) {
        findPeriod = previousPeriod;
      }
      periodDto = previousPeriod;
    }
    return findPeriod;
  }

  private boolean regularRequisitionIsAuthorized(UUID program, UUID facility, ProcessingPeriodDto periodDto) {
    List<Requisition> requisitions = requisitionRepository.searchRequisitions(
        periodDto.getId(), facility, program, false);
    if (ObjectUtils.isEmpty(requisitions)) {
      return false;
    }
    return requisitions.get(0).getStatus().isAuthorized();
  }

  private RequisitionPeriodDto processEmergencyRequisitionPeriod(
      UUID program, Facility facility, ProcessingPeriodDto periodDto) {
    List<Requisition> requisitions = requisitionRepository.searchRequisitions(
        periodDto.getId(), facility.getId(), program, Boolean.TRUE);
    if (requisitions.isEmpty()) {
      return buildEmergencyRequisitionPeriod(periodDto, facility);
    } else if (requisitions.size() <= MAX_COUNT_EMERGENCY_REQUISITION) {
      Optional<Requisition> submittableRequisition = requisitions.stream()
          .filter(requisition -> requisition.getStatus().isSubmittable())
          .findAny();
      if (submittableRequisition.isPresent()) {
        return buildEmergencyRequisitionPeriod(periodDto, facility, submittableRequisition.get());
      }
      if (requisitions.size() < MAX_COUNT_EMERGENCY_REQUISITION) {
        return buildEmergencyRequisitionPeriod(periodDto, facility);
      }
    }
    return null;
  }

  private RequisitionPeriodDto buildEmergencyRequisitionPeriod(ProcessingPeriodDto periodDto, Facility facility) {
    RequisitionPeriodDto requisitionPeriodDto = RequisitionPeriodDto.newInstance(periodDto);
    setSubmitStartAndEndDate(requisitionPeriodDto, facility.getType().getCode());
    requisitionPeriodDto.setCurrentPeriodRegularRequisitionAuthorized(true);
    return requisitionPeriodDto;
  }

  private RequisitionPeriodDto buildEmergencyRequisitionPeriod(ProcessingPeriodDto periodDto, Facility facility,
                                                               Requisition requisition) {
    RequisitionPeriodDto requisitionPeriodDto = buildEmergencyRequisitionPeriod(periodDto, facility);
    requisitionPeriodDto.setRequisitionId(requisition.getId());
    requisitionPeriodDto.setRequisitionStatus(requisition.getStatus());
    return requisitionPeriodDto;
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

  public Collection<ProcessingPeriodDto> fillProcessingPeriodWithExtension(
      Collection<ProcessingPeriodDto> periods) {
    List<ProcessingPeriodExtension> extensions = siglusProcessingPeriodExtensionService.findAll();

    Map<UUID, ProcessingPeriodExtension> map = new HashMap<>();
    extensions.forEach(extension -> map.put(extension.getProcessingPeriodId(), extension));

    periods.forEach(dto -> {
      ProcessingPeriodExtension processingPeriodExtension = map.get(dto.getId());
      combine(dto, processingPeriodExtension);
    });

    return periods;
  }

  private boolean canAuthorizeRequisition(UUID program, UUID facility) {
    Requisition dummy = new Requisition();
    dummy.setProgramId(program);
    dummy.setFacilityId(facility);
    return permissionService.canAuthorizeRequisition(dummy).isSuccess();
  }

  private List<RequisitionPeriodDto> getEmergencyProcessingRequisitionPeriod(UUID programId, Facility facility,
      Map<UUID, ProcessingPeriodDto> periodDtoMap) {
    List<RequisitionStatus> status = new ArrayList<>();
    status.add(RequisitionStatus.INITIATED);
    if (canAuthorizeRequisition(programId, facility.getId())) {
      status.add(RequisitionStatus.SUBMITTED);
    }
    List<Requisition> submittedRequisition = siglusRequisitionRepository
        .findByFacilityIdAndProgramIdAndEmergencyAndStatusIn(facility.getId(), programId, true, status);
    return submittedRequisition.stream()
        .sorted(Comparator.comparing(Requisition::getCreatedDate))
        .map(requisition -> {
          ProcessingPeriodDto processingPeriodDto = periodDtoMap.get(requisition.getProcessingPeriodId());
          return buildEmergencyRequisitionPeriod(processingPeriodDto, facility, requisition);
        })
        .collect(Collectors.toList());
  }
}
