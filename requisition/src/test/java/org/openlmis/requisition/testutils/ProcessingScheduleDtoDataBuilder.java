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

package org.openlmis.requisition.testutils;

import java.time.ZonedDateTime;
import java.util.UUID;
import org.openlmis.requisition.dto.ProcessingScheduleDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class ProcessingScheduleDtoDataBuilder implements DtoDataBuilder<ProcessingScheduleDto> {

  private static int instanceNumber = 0;

  private UUID id;
  private String code;
  private String description;
  private ZonedDateTime modifiedDate;
  private String name;

  /**
   * Creates builder for creating new instance of {@link ProcessingScheduleDto}.
   */
  public ProcessingScheduleDtoDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = "PS"  + instanceNumber;
    description = "Processing Schedule " + instanceNumber;
    modifiedDate = ZonedDateTime.now();
    name = "Schedule" + instanceNumber;
  }

  public ProcessingScheduleDto buildAsDto() {
    return new ProcessingScheduleDto(id, code, description, modifiedDate, name);
  }
}
