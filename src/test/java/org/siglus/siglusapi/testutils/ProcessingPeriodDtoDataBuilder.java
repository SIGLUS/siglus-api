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

package org.siglus.siglusapi.testutils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProcessingScheduleDto;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.siglusapi.testutils.api.DtoDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class ProcessingPeriodDtoDataBuilder implements DtoDataBuilder<ProcessingPeriodDto> {

  private static int instanceNumber = 0;

  private UUID id;
  private String name;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDate submitStartDate;
  private LocalDate submitEndDate;
  private LocalDate preStartDate;
  private LocalDate preEndDate;
  private LocalDate preSubmitStartDate;
  private LocalDate preSubmitEndDate;
  private ProcessingScheduleDto processingSchedule;
  private String description;
  private Integer durationInMonths;
  private Map<String, String> extraData;
  private UUID preId = UUID.randomUUID();

  /**
   * Creates builder for creating new instance of {@link ProcessingPeriodDto}.
   */
  public ProcessingPeriodDtoDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    name = "Period " + instanceNumber;
    endDate = LocalDate.of(2020, 6, 1);
    startDate = LocalDate.of(2020, 6, 30);
    submitStartDate = LocalDate.of(2020, 6, 1);
    submitEndDate = LocalDate.of(2020, 6, 30);

    preEndDate = LocalDate.of(2020, 6, 1);
    preStartDate = LocalDate.of(2020, 6, 30);
    preSubmitStartDate = LocalDate.of(2020, 6, 1);
    preSubmitEndDate = LocalDate.of(2020, 6, 30);

    processingSchedule = new ProcessingScheduleDto();
    description = "Processing Period " + instanceNumber;
    durationInMonths = 1;
    extraData = new HashMap<>();
  }

  public ProcessingPeriodDto buildAsDto() {
    return new ProcessingPeriodDto(id, name, startDate, endDate, processingSchedule, description,
        durationInMonths, extraData);
  }

  public ProcessingPeriodDtoDataBuilder withStartDate(LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  public ProcessingPeriodDtoDataBuilder withEndDate(LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  public ProcessingPeriodDtoDataBuilder withProcessingSchedule(ProcessingScheduleDto schedule) {
    this.processingSchedule = schedule;
    return this;
  }

  public ProcessingPeriodDtoDataBuilder withDurationInMonths(Integer durationInMonths) {
    this.durationInMonths = durationInMonths;
    return this;
  }

  public ProcessingPeriodDtoDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public ProcessingPeriodDtoDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public ProcessingPeriodDtoDataBuilder withDescription(String description) {
    this.description = description;
    return this;
  }


  public ProcessingPeriodDto buildFullDto() {
    ProcessingPeriodDto dto = new ProcessingPeriodDto(id, name, startDate, endDate,
        processingSchedule, description, durationInMonths, extraData);
    dto.setSubmitStartDate(submitStartDate);
    dto.setSubmitEndDate(submitEndDate);
    return dto;
  }

  public ProcessingPeriodDto buildDto() {
    return new ProcessingPeriodDto(id, name, startDate, endDate,
        processingSchedule, description, durationInMonths, extraData);
  }

  public ProcessingPeriodDto buildPerDto() {
    return new ProcessingPeriodDto(preId, "perPeriod", preStartDate, preEndDate,
        processingSchedule, description, durationInMonths, extraData);
  }

  public ProcessingPeriodExtension buildPreExtenstion() {
    ProcessingPeriodExtension extension = new ProcessingPeriodExtension();
    extension.setProcessingPeriodId(preId);
    extension.setSubmitStartDate(preSubmitStartDate);
    extension.setSubmitEndDate(preSubmitEndDate);
    return extension;
  }

  public ProcessingPeriodExtension buildExtenstion() {
    ProcessingPeriodExtension extension = new ProcessingPeriodExtension();
    extension.setProcessingPeriodId(id);
    extension.setSubmitStartDate(submitStartDate);
    extension.setSubmitEndDate(submitEndDate);
    return extension;
  }
}
