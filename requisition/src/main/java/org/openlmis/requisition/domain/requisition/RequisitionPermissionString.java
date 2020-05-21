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

import java.util.Objects;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.requisition.domain.BaseEntity;

@Entity
@Table(name = "requisition_permission_strings")
@NoArgsConstructor
@AllArgsConstructor
public class RequisitionPermissionString extends BaseEntity {

  @ManyToOne(cascade = {CascadeType.REFRESH})
  @JoinColumn(name = "requisitionId", nullable = false)
  @Getter
  @Setter
  private Requisition requisition;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION, nullable = false)
  @Getter
  @Setter
  private String permissionString;

  /**
   * Convenience constructor to create permission string based on a set of values, which all must
   * not be null.
   * 
   * @param requisition requisition associated with permission string
   * @param rightName right name of string
   * @param facilityId facility ID of string
   * @param programId program ID of string
   */
  public static RequisitionPermissionString newRequisitionPermissionString(Requisition requisition,
      String rightName, UUID facilityId, UUID programId) {
    Objects.requireNonNull(requisition);
    Objects.requireNonNull(rightName);
    Objects.requireNonNull(facilityId);
    Objects.requireNonNull(programId);
    return new RequisitionPermissionString(requisition,
        String.join("|", rightName, facilityId.toString(), programId.toString()));
  }
}
