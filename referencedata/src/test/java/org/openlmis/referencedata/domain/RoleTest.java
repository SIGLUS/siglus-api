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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.referencedata.exception.ValidationMessageException;

public class RoleTest {

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();

  private String roleName = "role";

  private String right1Name = "right1";
  private String right2Name = "right2";

  @Test
  public void shouldGroupRightsOfSameType() {
    //given
    Right right1 = Right.newRight(right1Name, RightType.ORDER_FULFILLMENT);
    Right right2 = Right.newRight(right2Name, RightType.ORDER_FULFILLMENT);

    //when
    Role role = Role.newRole(roleName, right1, right2);

    //then
    Set<Right> rights = role.getRights();
    assertThat(rights.size(), is(2));
    assertTrue(rights.contains(right1));
    assertTrue(rights.contains(right2));
  }

  @Test
  public void shouldNotGroupRightsOfDifferentTypes() {
    //given
    Right right1 = Right.newRight(right1Name, RightType.ORDER_FULFILLMENT);
    Right right2 = Right.newRight(right2Name, RightType.SUPERVISION);

    //when and then
    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage("referenceData.error.role.rightsAreDifferentTypes");
    Role.newRole(roleName, right1, right2);
  }

  @Test
  public void shouldThrowExceptionWhenNoRightsProvided() {
    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage("referenceData.error.role.mustHaveARight");
    Role.newRole(roleName);
  }

  @Test
  public void shouldBeAbleToAddRightsOfSameTypeToExistingRole() {
    //given
    Role role = Role.newRole(roleName, Right.newRight(right1Name, RightType.SUPERVISION));

    //when
    Right additionalRight = Right.newRight(right2Name, RightType.SUPERVISION);
    role.add(additionalRight);

    //then
    Set<Right> rights = role.getRights();
    assertThat(rights.size(), is(2));
    assertTrue(rights.contains(additionalRight));
  }

  @Test
  public void shouldNotBeAbleToAddRightsOfDifferentTypeToExistingRole() {
    //given
    Role role = Role.newRole(roleName, Right.newRight(right1Name, RightType.SUPERVISION));

    //when and then
    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage("referenceData.error.role.rightsAreDifferentTypes");

    Right rightOfDifferentType = Right.newRight(right2Name, RightType.ORDER_FULFILLMENT);
    role.add(rightOfDifferentType);
  }

  @Test
  public void shouldIndicateIfItContainsARight() {
    //given
    Right right1 = Right.newRight(right1Name, RightType.SUPERVISION);
    Right right2 = Right.newRight(right2Name, RightType.SUPERVISION);
    right1.attach(right2);

    Role role = Role.newRole(roleName, right1);

    //when
    boolean containsRight1 = role.contains(right1);
    boolean containsRight2 = role.contains(right2);

    //then
    assertTrue(containsRight1);
    assertFalse(containsRight2);
  }
}
