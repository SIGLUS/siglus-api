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

package org.openlmis.referencedata.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.domain.ProcessingSchedule;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProcessingPeriodDto extends BaseDto
    implements ProcessingPeriod.Exporter, ProcessingPeriod.Importer {

  private ProcessingScheduleDto processingSchedule;
  private String name;
  private String description;
  private LocalDate startDate;
  private LocalDate endDate;
  private Integer durationInMonths;
  private Map<String, Object> extraData;

  @Override
  public int hashCode() {
    return Objects.hash(name, processingSchedule);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ProcessingPeriodDto)) {
      return false;
    }
    ProcessingPeriodDto periodDto = (ProcessingPeriodDto) obj;
    return Objects.equals(name, periodDto.name)
          && Objects.equals(processingSchedule, periodDto.processingSchedule);
  }

  @Override
  public Optional<ProcessingSchedule.Exporter> provideProcessingScheduleExporter() {
    return Optional.of(new ProcessingScheduleDto());
  }

  @Override
  public void includeProcessingSchedule(ProcessingSchedule.Exporter processingScheduleExporter) {
    processingSchedule = (ProcessingScheduleDto) processingScheduleExporter;
  }

  @Override
  public boolean supportsDurationInMonths() {
    return true;
  }
}
