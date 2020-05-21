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

package org.openlmis.requisition.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ProcessingPeriodDto extends BasicProcessingPeriodDto {
  static final String REPORT_ONLY = "reportOnly";

  private ProcessingScheduleDto processingSchedule;
  private String description;
  private Integer durationInMonths;
  private Map<String, String> extraData;

  /**
   * Constructor for {@link ProcessingPeriodDto} with all parameters.
   *
   * @param id period id
   * @param name period name
   * @param startDate date when period starts
   * @param endDate date when period ends
   * @param processingSchedule schedule for period
   * @param description period description
   * @param durationInMonths number of months in period
   * @param extraData map that hold additional information about period
   */
  public ProcessingPeriodDto(UUID id, String name, LocalDate startDate, LocalDate endDate,
      ProcessingScheduleDto processingSchedule, String description, Integer durationInMonths,
      Map<String, String> extraData) {
    super(id, name, startDate, endDate);
    this.processingSchedule = processingSchedule;
    this.description = description;
    this.durationInMonths = durationInMonths;
    this.extraData = extraData;
  }

  /**
   * Returns the value of {@link #REPORT_ONLY} key from the {@link #extraData} map. If the map does
   * not exist or the key does not exist or it contains non-boolean value, the method will return
   * false.
   *
   * @return true if key exists and the value is true; otherwise false.
   */
  @JsonIgnore
  public boolean isReportOnly() {
    return Boolean.parseBoolean(
        Optional
            .ofNullable(extraData)
            .orElse(Collections.emptyMap())
            .get(REPORT_ONLY)
    );
  }

}
