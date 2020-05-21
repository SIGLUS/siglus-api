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

package org.openlmis.requisition.repository.custom;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public final class DefaultRequisitionSearchParams implements RequisitionSearchParams {
  private UUID facility;
  private UUID program;
  private UUID processingPeriod;
  private UUID supervisoryNode;

  private Boolean emergency;

  private LocalDate initiatedDateFrom;
  private LocalDate initiatedDateTo;

  private ZonedDateTime modifiedDateFrom;
  private ZonedDateTime modifiedDateTo;

  private Set<RequisitionStatus> requisitionStatuses;
}
