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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.referencedata.service.ProcessingPeriodSearchParams;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
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
}
