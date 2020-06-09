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

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.referencedata.service.ProcessingPeriodSearchParams;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionPeriodDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.PermissionStringDto;
import org.openlmis.requisition.service.referencedata.PermissionStrings;
import org.openlmis.requisition.utils.RequisitionAuthenticationHelper;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.validator.SiglusProcessingPeriodValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
  private RequisitionAuthenticationHelper requisitionAuthenticationHelper;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private PeriodService periodService;

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  private RequisitionService requisitionService;

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
    extensions.stream()
        .forEach(extension -> map.put(extension.getProcessingPeriodId(), extension));

    page.getContent().stream().forEach(dto -> {
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

  // get periods for initiate
  public Collection<RequisitionPeriodDto> getPeriods(UUID program,
      UUID facility, boolean emergency) {
    UserDto user = requisitionAuthenticationHelper.getCurrentUser();

    List<String> permissionStrings = findPermissionStringsByUser(user);

    Collection<ProcessingPeriodDto> periods;
    if (emergency) {
      periods = fillProcessingPeriodWithExtension(
          periodService.getCurrentPeriods(program, facility));
    } else {
      periods = fillProcessingPeriodWithExtension(
          periodService.searchByProgramAndFacility(program, facility));
    }

    List<RequisitionPeriodDto> requisitionPeriods = new ArrayList<>();

    for (ProcessingPeriodDto period : periods) {
      RequisitionPeriodDto requisitionPeriod = RequisitionPeriodDto.newInstance(period);
      requisitionPeriods.add(requisitionPeriod);

      List<Requisition> requisitions = requisitionRepository.searchRequisitions(
          period.getId(), facility, program, emergency);

      List<Requisition> preAuthorizeRequisitions = getPreAuthorizedRequisitions(program, facility,
          permissionStrings, requisitions);

      if (emergency) {
        if (!preAuthorizeRequisitions.isEmpty()) {
          Requisition earliestRequisition = preAuthorizeRequisitions.stream().min(
              Comparator.comparing(Requisition::getCreatedDate)).orElseThrow(
                  () -> new NotFoundException("Earlier Rquisition Not Found"));
          requisitionPeriod.setRequisitionId(earliestRequisition.getId());
          requisitionPeriod.setRequisitionStatus(earliestRequisition.getStatus());
        }

        if (!requisitionService.searchAuthorizedRequisitions(facility, program,
            period.getId(), false).isEmpty()) {
          //for emergency, requisitionPeriods only have one element
          requisitionPeriods.forEach(requisitionPeriodDto ->
              requisitionPeriodDto.setCurrentPeriodRegularRequisitionAuthorized(true));
        }
      } else {
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

  private List<String> findPermissionStringsByUser(UserDto user) {
    PermissionStrings.Handler handler = permissionService.getPermissionStrings(user.getId());
    return handler.get()
        .stream()
        .map(PermissionStringDto::toString)
        .collect(toList());
  }

  private Collection<ProcessingPeriodDto> fillProcessingPeriodWithExtension(
      Collection<ProcessingPeriodDto> periods) {
    List<ProcessingPeriodExtension> extensions = processingPeriodExtensionRepository.findAll();

    Map<UUID, ProcessingPeriodExtension> map = new HashMap<>();
    extensions.stream()
        .forEach(extension -> map.put(extension.getProcessingPeriodId(), extension));

    periods.stream().forEach(dto -> {
      ProcessingPeriodExtension processingPeriodExtension = map.get(dto.getId());
      combine(dto, processingPeriodExtension);
    });

    return periods;
  }

  private List<Requisition> getPreAuthorizedRequisitions(UUID program, UUID facility,
      List<String> permissionStrings, List<Requisition> requisitions) {
    List<Requisition> preAuthorizeRequisitions = new ArrayList<>();
    if (permissionStrings.contains(PermissionService.REQUISITION_CREATE + "|"
        + facility + "|" + program)) {
      preAuthorizeRequisitions.addAll(requisitions.stream()
          .filter(requisition -> requisition.getStatus().isSubmittable())
          .collect(Collectors.toList()));
    }

    if (permissionStrings.contains(PermissionService.REQUISITION_AUTHORIZE + "|"
        + facility + "|" + program)) {
      preAuthorizeRequisitions.addAll(requisitions.stream()
          .filter(requisition -> requisition.getStatus().isPreAuthorize())
          .collect(Collectors.toList()));
    }
    return preAuthorizeRequisitions;
  }

}
