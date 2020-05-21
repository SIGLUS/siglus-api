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
import java.util.HashMap;
import java.util.Map;
import org.openlmis.requisition.domain.StatusLogEntry;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.BasicProcessingPeriodDto;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class BasicRequisitionDtoDataBuilder implements DtoDataBuilder<BasicRequisitionDto> {

  private Boolean emergency;
  private Boolean reportOnly;
  private RequisitionStatus status;
  private ZonedDateTime modifiedDate;
  private ZonedDateTime createdDate;
  private Map<String, StatusLogEntry> statusChanges;
  private BasicProcessingPeriodDto processingPeriod;
  private MinimalFacilityDto facility;
  private BasicProgramDto program;
  private Map<String, Object> extraData;

  /**
   * Builder for {@link BasicRequisitionDto}.
   */
  public BasicRequisitionDtoDataBuilder() {
    emergency = false;
    reportOnly = false;
    status = RequisitionStatus.INITIATED;
    modifiedDate = ZonedDateTime.now();
    createdDate = ZonedDateTime.now();
    statusChanges = new HashMap<>();
    processingPeriod = new BasicProcessingPeriodDtoDataBuilder().buildAsDto();
    facility = new MinimalFacilityDtoDataBuilder().buildAsDto();
    program = new BasicProgramDtoDataBuilder().buildAsDto();
    extraData = new HashMap<>();
  }

  @Override
  public BasicRequisitionDto buildAsDto() {
    return new BasicRequisitionDto(
        emergency,
        reportOnly,
        status,
        modifiedDate,
        createdDate,
        statusChanges,
        processingPeriod,
        facility,
        program,
        extraData
    );
  }
}
