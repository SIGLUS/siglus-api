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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RequisitionCompletionDto {
  /**
   * Name for the grouping, telling what is this completion for (ex. period, zone).
   */
  private String grouping;

  /**
   * Number of completed requisitions (approved in requisition due).
   */
  private int completed;

  /**
   * Number of missed requisitions (not reported).
   */
  private int missed;

  /**
   * Number of requisitions reported on time.
   */
  private int onTime;

  /**
   * Number of requisitions reported late.
   */
  private int late;

  /**
   * Number of total reported requisitions.
   */
  private int total;
}
