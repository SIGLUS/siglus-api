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

package org.openlmis.requisition.web;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BasicRequisitionDtoBuilder {
  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(
      BasicRequisitionDtoBuilder.class);

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private PeriodReferenceDataService periodReferenceDataService;

  @Autowired
  private PeriodService periodService;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  /**
   * Create a list of {@link BasicRequisitionDto} based on passed data.
   *
   * @param requisitions a list of requisitions that will be converted into basic DTOs.
   * @return a list of {@link BasicRequisitionDto}
   */
  public List<BasicRequisitionDto> build(Collection<Requisition> requisitions) {
    Set<UUID> facilityIds = Sets.newHashSet();
    Set<UUID> periodIds = Sets.newHashSet();
    Set<UUID> programIds = Sets.newHashSet();

    for (Requisition requisition : requisitions) {
      facilityIds.add(requisition.getFacilityId());
      periodIds.add(requisition.getProcessingPeriodId());
      programIds.add(requisition.getProgramId());
    }

    Map<UUID, FacilityDto> facilities = facilityReferenceDataService
        .search(facilityIds)
        .stream()
        .collect(Collectors.toMap(FacilityDto::getId, Function.identity()));

    Map<UUID, ProcessingPeriodDto> periods = periodReferenceDataService
        .search(periodIds)
        .stream()
        .collect(Collectors.toMap(ProcessingPeriodDto::getId, Function.identity()));

    Map<UUID, ProgramDto> programs = programReferenceDataService
        .search(programIds)
        .stream()
        .collect(Collectors.toMap(ProgramDto::getId, Function.identity()));

    List<BasicRequisitionDto> dtos = Lists.newArrayList();
    for (Requisition requisition : requisitions) {
      FacilityDto facility = facilities.get(requisition.getFacilityId());
      ProcessingPeriodDto period = periods.get(requisition.getProcessingPeriodId());
      ProgramDto program = programs.get(requisition.getProgramId());

      dtos.add(build(requisition, facility, program, period));
    }

    return dtos;
  }

  /**
   * Create a new instance of BasicRequisitionDto based on data from {@link Requisition}.
   *
   * @param requisition instance used to create {@link BasicRequisitionDto} (can be {@code null})
   * @return new instance of {@link BasicRequisitionDto}. {@code null} if passed argument is {@code
   * null}.
   */
  public BasicRequisitionDto build(Requisition requisition) {
    return build(requisition, null, null);
  }

  /**
   * Create a new instance of BasicRequisitionDto based on data from {@link Requisition}.
   *
   * @param requisition instance used to create {@link BasicRequisitionDto} (can be {@code null})
   * @param facility    minimal facility that will be assigned to new {@link BasicRequisitionDto}
   *                    if {@code null} it will be fetched from ReferenceData service
   * @param program     program that will be assigned to new {@link BasicRequisitionDto}
   *                    if {@code null} it will be fetched from ReferenceData service
   * @return new instance of {@link BasicRequisitionDto}. {@code null} if passed argument is {@code
   * null}.
   */
  public BasicRequisitionDto build(Requisition requisition, MinimalFacilityDto facility,
      ProgramDto program) {
    return build(requisition, facility, program, null);
  }

  private BasicRequisitionDto build(Requisition requisition, MinimalFacilityDto facility,
      ProgramDto program, ProcessingPeriodDto period) {
    XLOGGER.entry(requisition);

    if (null == requisition) {
      XLOGGER.exit();
      return null;
    }

    Profiler profiler = new Profiler("BASIC_REQUISITION_DTO_BUILD");
    profiler.setLogger(XLOGGER);

    BasicRequisitionDto requisitionDto = new BasicRequisitionDto();

    profiler.start("EXPORT");
    requisition.export(requisitionDto);

    profiler.start("SET_SUB_RESOURCES");
    requisitionDto.setFacility(
        Optional
        .ofNullable(facility)
        .orElseGet(() -> facilityReferenceDataService.findOne(requisition.getFacilityId()))
    );
    requisitionDto.setProgram(
        Optional
            .ofNullable(program)
            .orElseGet(() -> programReferenceDataService.findOne(requisition.getProgramId()))
    );
    requisitionDto.setProcessingPeriod(
        Optional
            .ofNullable(period)
            .orElseGet(() -> periodService.getPeriod(requisition.getProcessingPeriodId()))
    );

    profiler.stop().log();
    XLOGGER.exit(requisitionDto);
    return requisitionDto;
  }
}
