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

package org.openlmis.referencedata.domain;

import java.util.Objects;
import lombok.Getter;

@Getter
public class RightQuery {

  private Right right;
  private Program program;
  private Facility facility;
  private Facility warehouse;

  /**
   * Constructor to create query if user has a right. This is for general admin and report rights.
   *
   * @param right the right to check
   */
  public RightQuery(Right right) {
    this.right = right;
  }

  /**
   * Constructor to create query if user has a right in a specified program at a specified facility.
   * This is for all supervision rights.
   *
   * @param right    the right to check
   * @param program  the program to check
   * @param facility the facility to check
   */
  public RightQuery(Right right, Program program, Facility facility) {
    this.right = right;
    this.program = program;
    this.facility = facility;
  }

  /**
   * Constructor to create query if user has a right at a specified warehouse. This is for order
   * fulfillment rights.
   *
   * @param right     the right to check
   * @param warehouse the warehouse to check
   */
  public RightQuery(Right right, Facility warehouse) {
    this.right = right;
    this.warehouse = warehouse;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RightQuery)) {
      return false;
    }
    RightQuery that = (RightQuery) other;
    return Objects.equals(right, that.right)
        && Objects.equals(program, that.program)
        && Objects.equals(facility, that.facility)
        && Objects.equals(warehouse, that.warehouse);
  }

  @Override
  public int hashCode() {
    return Objects.hash(right, program, facility, warehouse);
  }
}
