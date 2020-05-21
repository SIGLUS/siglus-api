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

package org.openlmis.referencedata.testbuilder;

import java.time.ZonedDateTime;
import java.util.UUID;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.ProcessingSchedule;

public class ProcessingScheduleDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private String code;
  private String name;
  private String description;
  private ZonedDateTime modifiedDate;

  /**
   * Returns instance of {@link ProcessingScheduleDataBuilder} with sample data.
   */
  public ProcessingScheduleDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = "PS" + instanceNumber;
    name = "Schedule " + instanceNumber;
    description = "description";
    modifiedDate = ZonedDateTime.now();
  }

  public ProcessingScheduleDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public ProcessingScheduleDataBuilder withoutId() {
    this.id = null;
    return this;
  }

  public ProcessingScheduleDataBuilder withCode(String code) {
    this.code = code;
    return this;
  }

  public ProcessingScheduleDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Returns instance of {@link ProcessingSchedule} with sample data.
   */
  public ProcessingSchedule build() {
    ProcessingSchedule schedule = new ProcessingSchedule(Code.code(code), description,
        modifiedDate, name);
    schedule.setId(id);
    return schedule;
  }

  public ProcessingSchedule buildWithoutId() {
    return this.withoutId().build();
  }
}
