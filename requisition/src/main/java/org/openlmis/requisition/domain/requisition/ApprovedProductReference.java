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

package org.openlmis.requisition.domain.requisition;

import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public final class ApprovedProductReference {

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(name = "orderableId")),
      @AttributeOverride(name = "versionNumber", column = @Column(name = "orderableVersionNumber"))
  })
  private VersionEntityReference orderable;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(
          name = "facilityTypeApprovedProductId")),
      @AttributeOverride(name = "versionNumber", column = @Column(
          name = "facilityTypeApprovedProductVersionNumber"))
  })
  private VersionEntityReference facilityTypeApprovedProduct;

  public ApprovedProductReference(UUID id, Long versionNumber,
      UUID orderableId, Long orderableVersionNumber) {
    this.facilityTypeApprovedProduct = new VersionEntityReference(id, versionNumber);
    this.orderable = new VersionEntityReference(orderableId, orderableVersionNumber);
  }

}
