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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_PERIOD_MATCH;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
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
import org.siglus.common.dto.ProgramAdditionalOrderableDto;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.ProcessingPeriodSearchParams;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.dto.FacillityStockCardDateDto;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.validator.SiglusProcessingPeriodValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

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
  private SiglusProgramService siglusProgramService;

  @Autowired
  private FacilityNativeRepository facilityNativeRepository;

  @Autowired
  private SiglusProgramAdditionalOrderableService siglusProgramAdditionalOrderableService;

  @Autowired
  private PeriodReferenceDataService periodReferenceDataService;

  @Autowired
  private ProcessingPeriodRepository processingPeriodRepository;

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

  public ProcessingPeriod getPeriodDateIn(List<ProcessingPeriod> processingPeriods, LocalDate localDate) {
    return processingPeriods.stream().filter(period -> isDateInPeriod(period, localDate)).findFirst()
        .orElseThrow(() -> new BusinessDataException(new Message(ERROR_NO_PERIOD_MATCH)));
  }

  public List<ProcessingPeriod> getUpToNowMonthlyPeriods() {
    return processingPeriodRepository.getUpToNowMonthlyPeriods(LocalDate.now());
  }

  public boolean isDateInPeriod(ProcessingPeriod period, LocalDate localDate) {
    return !isDateNotInPeriod(period, localDate);
  }

  private boolean isDateNotInPeriod(ProcessingPeriod period, LocalDate localDate) {
    return localDate.isBefore(period.getStartDate()) || localDate.isAfter(period.getEndDate());
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

  private LocalDate getFirstStockMovementDate(ProgramDto program,
                                              UUID facilityId) {
    FacillityStockCardDateDto facillityStockCardDateDto;
    if (ProgramConstants.MALARIA_PROGRAM_CODE.equals(program.getCode())) {
      Set<UUID> malariaAdditionalOrderableIds =
              siglusProgramAdditionalOrderableService.searchAdditionalOrderables(program.getId())
                      .stream().map(ProgramAdditionalOrderableDto::getAdditionalOrderableId)
                      .collect(Collectors.toSet());
      ProgramDto viaProgram = siglusProgramService.getProgramByCode(ProgramConstants.VIA_PROGRAM_CODE)
          .orElseThrow(() -> new IllegalArgumentException("no via program found"));

      List<FacillityStockCardDateDto> result =
          facilityNativeRepository.findMalariaFirstStockCardGroupByFacility(malariaAdditionalOrderableIds,
              viaProgram.getId());
      facillityStockCardDateDto = result
              .stream()
              .filter(dto -> facilityId.equals(dto.getFacilityId()))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("no first stock movement"));
    } else {
      facillityStockCardDateDto = facilityNativeRepository
              .findFirstStockCardGroupByFacilityIdAndProgramId(facilityId, program.getId())
              .stream()
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("no first stock movement"));
    }
    return facillityStockCardDateDto.getOccurredDate().toLocalDate();
  }

  // get periods for initiate
  public Collection<RequisitionPeriodDto> getPeriods(UUID program,
      UUID facility, boolean emergency) {
    ProgramDto programDto = siglusProgramService.getProgram(program);
    if (emergency && !ProgramConstants.VIA_PROGRAM_CODE.equals(programDto.getCode())) {
      return new ArrayList<>();
    }
    Collection<ProcessingPeriodDto> periods = fillProcessingPeriodWithExtension(
        periodService.searchByProgramAndFacility(program, facility));

    List<UUID> currentPeriodIds = periodService.getCurrentPeriods(program, facility)
        .stream().map(ProcessingPeriodDto::getId).collect(Collectors.toList());
    List<RequisitionPeriodDto> requisitionPeriods = new ArrayList<>();

    // TODO Optimization
    for (ProcessingPeriodDto period : periods) {

      List<Requisition> requisitions = requisitionRepository.searchRequisitions(
          period.getId(), facility, program, emergency);

      List<Requisition> preAuthorizeRequisitions = getPreAuthorizedRequisitions(program, facility,
          requisitions);

      if (emergency) {
        processingEmergencyRequisitionPeriod(requisitionPeriods, currentPeriodIds,
            preAuthorizeRequisitions, period, program, facility);
        if (!requisitionPeriods.isEmpty()) {
          break;
        }
      } else {
        RequisitionPeriodDto requisitionPeriod = RequisitionPeriodDto.newInstance(period);
        requisitionPeriods.add(requisitionPeriod);
        if (!requisitions.isEmpty()) {
          if (preAuthorizeRequisitions.isEmpty()) {
            requisitionPeriods.remove(requisitionPeriod);
          } else {
            requisitionPeriod.setRequisitionId(preAuthorizeRequisitions.get(0).getId());
            requisitionPeriod.setRequisitionStatus(preAuthorizeRequisitions.get(0).getStatus());
          }
        }
      }
    }

    return requisitionPeriods;
  }

  private void processingEmergencyRequisitionPeriod(
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
    if (CollectionUtils.isEmpty(requisitionPeriods) && currentPeriodIds.contains(period.getId())) {
      requisitionPeriods.add(RequisitionPeriodDto.newInstance(period));
    }
    Set<String> statusSet = RequisitionStatus.getAfterAuthorizedStatus().stream().map(Enum::name)
        .collect(Collectors.toSet());
    if (CollectionUtils.isNotEmpty(requisitionPeriods)
        && CollectionUtils.isNotEmpty(siglusRequisitionRepository.searchAfterAuthorizedRequisitions(
        facility, program, period.getId(), Boolean.FALSE, statusSet))) {
      // for emergency, requisitionPeriods only have one element
      requisitionPeriods.forEach(requisitionPeriodDto ->
          requisitionPeriodDto.setCurrentPeriodRegularRequisitionAuthorized(true));
    }
  }

  private void getFirstPreAuthorizeRequisition(List<Requisition> preAuthorizeRequisitions,
      RequisitionPeriodDto requisitionPeriod) {
    Requisition firstPreAuthorizeRequisition = preAuthorizeRequisitions.stream().min(
        Comparator.comparing(Requisition::getCreatedDate)).orElseThrow(
            () -> new NotFoundException("first Requisition Not Found"));
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
