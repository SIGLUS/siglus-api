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

import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;

@Data
@EqualsAndHashCode(callSuper = true)
public class RequisitionPeriodDto extends ProcessingPeriodDto {
  private UUID requisitionId;
  private RequisitionStatus requisitionStatus;

  /**
   * Create a new instance of RequisitionPeriodDto based on ProcessingPeriodDto.
   * @param period processing period dto
   * @return requisition period dto
   */
  public static RequisitionPeriodDto newInstance(ProcessingPeriodDto period) {
    RequisitionPeriodDto dto = new RequisitionPeriodDto();
    dto.setId(period.getId());
    dto.setName(period.getName());
    dto.setStartDate(period.getStartDate());
    dto.setEndDate(period.getEndDate());
    dto.setProcessingSchedule(period.getProcessingSchedule());
    dto.setDescription(period.getDescription());
    dto.setDurationInMonths(period.getDurationInMonths());
    return dto;
  }
}
