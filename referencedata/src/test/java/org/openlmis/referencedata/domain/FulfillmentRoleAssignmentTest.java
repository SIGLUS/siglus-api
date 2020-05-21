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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openlmis.referencedata.domain.RightType.ORDER_FULFILLMENT;

import org.junit.Test;
import org.openlmis.referencedata.testbuilder.UserDataBuilder;

public class FulfillmentRoleAssignmentTest {

  private Right right;
  private Facility warehouse;
  private Facility hospital;
  private User user;
  private FulfillmentRoleAssignment fulfillmentRoleAssignment;
  private String roleName = "role";

  /**
   * Setup constructor.
   */
  public FulfillmentRoleAssignmentTest() {
    right = Right.newRight("right", ORDER_FULFILLMENT);
    warehouse = new Facility("C1");
    warehouse.setType(new FacilityType("warehouse"));
    hospital = new Facility("C2");
    hospital.setType(new FacilityType("hospital"));
    user = new UserDataBuilder().build();
    fulfillmentRoleAssignment = new FulfillmentRoleAssignment(
        Role.newRole(roleName, right), user, warehouse);
  }

  @Test
  public void shouldAllowCreationWithWarehouseFacilityType() {
    new FulfillmentRoleAssignment(Role.newRole(roleName, right), user, warehouse);
  }

  @Test
  public void shouldAllowCreationWithNonWarehouseFacilityType() {
    new FulfillmentRoleAssignment(Role.newRole(roleName, right), user, hospital);
  }

  @Test
  public void shouldHaveRightWhenRightAndFacilityMatch() {
    //when
    RightQuery rightQuery = new RightQuery(right, warehouse);
    boolean hasRight = fulfillmentRoleAssignment.hasRight(rightQuery);

    //then
    assertTrue(hasRight);
  }

  @Test
  public void shouldNotHaveRightWhenFacilityDoesNotMatch() {
    //when
    RightQuery rightQuery = new RightQuery(right, hospital);
    boolean hasRight = fulfillmentRoleAssignment.hasRight(rightQuery);

    //then
    assertFalse(hasRight);
  }
}
