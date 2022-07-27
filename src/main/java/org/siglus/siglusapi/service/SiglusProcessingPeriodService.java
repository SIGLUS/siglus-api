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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionPeriodDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.domain.ReportType;
import org.siglus.siglusapi.dto.ProcessingPeriodSearchParams;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.ReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
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
  private ReportTypeRepository reportTypeRepository;

  @Autowired
  private StockCardRepository stockCardRepository;

  @Autowired
  private SiglusStockCardLineItemRepository siglusStockCardLineItemRepository;

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

  public ProcessingPeriodDto getProcessingPeriod(UUID periodId) {

    ProcessingPeriodDto dto = siglusProcessingPeriodReferenceDataService.findOne(periodId);
    return getProcessingPeriodbyOpenLmisPeroid(dto);
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

  private LocalDate getFirstStockMovementDate(UUID programId,
                                              UUID facilityId) {
    List<StockCard> cards = stockCardRepository.findByProgramIdAndFacilityId(programId, facilityId);
    Comparator<StockCardLineItem> lineItemComparator
            = Comparator.comparing(
            StockCardLineItem::getOccurredDate, (s1, s2) -> s1.compareTo(s2));
    StockCardLineItem first = siglusStockCardLineItemRepository.findAllByStockCardIn(cards)
            .stream()
            .min(lineItemComparator)
            .orElseThrow(() -> new IllegalArgumentException("no first stock movement"));
    return first.getOccurredDate();
  }

  private Collection<ProcessingPeriodDto> filterPeriods(Collection<ProcessingPeriodDto> periods,
                                                        UUID programId,
                                                        UUID facilityId) {
    LocalDate firstStockMovementDate = getFirstStockMovementDate(programId, facilityId);
    String programCode = siglusProgramService.getProgram(programId).getCode();
    Optional<ReportType> reportTypeOptional = reportTypeRepository
            .findOneByFacilityIdAndProgramCodeAndActiveIsTrue(facilityId, programCode);
    if (reportTypeOptional.isPresent()) {
      ReportType reportType = reportTypeOptional.get();
      if (reportType != null && reportType.getStartDate() != null && firstStockMovementDate != null) {
        LocalDate indexDate;
        if (reportType.getStartDate().isAfter(firstStockMovementDate)) {
          indexDate = reportType.getStartDate();
        } else {
          indexDate = firstStockMovementDate;
        }
        return periods.stream()
                .filter(period -> period.getStartDate().isAfter(indexDate))
                .filter(period -> !period.getStartDate().isAfter(LocalDate.now()))
                .collect(Collectors.toList());
      }
    }
    return periods;
  }

  // get periods for initiate
  public Collection<RequisitionPeriodDto> getPeriods(UUID program,
      UUID facility, boolean emergency) {

    Collection<ProcessingPeriodDto> periods = fillProcessingPeriodWithExtension(
        periodService.searchByProgramAndFacility(program, facility));
    Collection<ProcessingPeriodDto> reportPeriods = filterPeriods(periods, program, facility);
    List<UUID> currentPeriodIds = periodService.getCurrentPeriods(program, facility)
        .stream().map(ProcessingPeriodDto::getId).collect(Collectors.toList());
    List<RequisitionPeriodDto> requisitionPeriods = new ArrayList<>();

    // TODO Optimization
    for (ProcessingPeriodDto period : reportPeriods) {

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

  private Collection<ProcessingPeriodDto> fillProcessingPeriodWithExtension(
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
