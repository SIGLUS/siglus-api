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

package org.siglus.common.domain.referencedata;

import static java.util.Collections.singleton;

import java.util.Objects;
import java.util.Set;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.javers.core.metamodel.annotation.TypeName;

@Entity
@DiscriminatorValue("fulfillment")
@NoArgsConstructor
@TypeName("FulfillmentRoleAssignment")
public class FulfillmentRoleAssignment extends RoleAssignment {

  @ManyToOne
  @JoinColumn(name = "warehouseid")
  @Getter
  private Facility warehouse;

  @Override
  protected Set<RightType> getAcceptableRightTypes() {
    return singleton(RightType.ORDER_FULFILLMENT);
  }

  /**
   * Check if this role assignment has a right based on specified criteria. For fulfillment, check
   * also that the warehouse matches.
   */
  @Override
  public boolean hasRight(RightQuery rightQuery) {
    boolean roleMatches = role.contains(rightQuery.getRight());
    boolean warehouseMatches = warehouse.equals(rightQuery.getWarehouse());

    return roleMatches && warehouseMatches;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof FulfillmentRoleAssignment)) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    FulfillmentRoleAssignment that = (FulfillmentRoleAssignment) obj;
    return Objects.equals(role, that.role)
        && Objects.equals(user, that.user)
        && Objects.equals(warehouse, that.warehouse);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), warehouse);
  }

}
