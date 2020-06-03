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

package org.openlmis.fulfillment.testutils;

import java.time.LocalDate;
import java.util.UUID;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingScheduleDto;

public class ProcessingPeriodDataBuilder {

  private UUID id;
  private ProcessingScheduleDto processingSchedule;
  private String name;
  private String description;
  private LocalDate startDate;
  private LocalDate endDate;

  /**
   * Builder for {@link ProcessingPeriodDto}.
   */
  public ProcessingPeriodDataBuilder() {
    id = UUID.randomUUID();
    processingSchedule = new ProcessingScheduleDataBuilder().build();
    name = "period";
    description = "desc";
    startDate = LocalDate.now();
    endDate = LocalDate.now();
  }

  /**
   * Build instanceof {@link ProcessingPeriodDto}.
   */
  public ProcessingPeriodDto build() {
    ProcessingPeriodDto period = new ProcessingPeriodDto(processingSchedule, name, description,
        startDate, endDate);
    period.setId(id);
    return period;
  }

  public ProcessingPeriodDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }
}
