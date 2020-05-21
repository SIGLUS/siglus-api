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
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.requisition.domain.requisition.Versionable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public final class ApprovedProductDto implements Versionable {
  private UUID id;
  private OrderableDto orderable;
  private ProgramDto program;
  private Double maxPeriodsOfStock;
  private Double minPeriodsOfStock;
  private Double emergencyOrderPoint;
  private MetadataDto meta = new MetadataDto();

  public Long getVersionNumber() {
    return this.meta.getVersionNumber();
  }

  public VersionIdentityDto getIdentity() {
    return new VersionIdentityDto(this.id, getVersionNumber());
  }
}
