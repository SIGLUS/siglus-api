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

import static org.mockito.Mockito.mock;
import static org.openlmis.referencedata.domain.RightType.GENERAL_ADMIN;
import static org.openlmis.referencedata.domain.RightType.ORDER_FULFILLMENT;
import static org.openlmis.referencedata.util.messagekeys.RoleAssignmentMessageKeys.ERROR_TYPE_NOT_ACCEPTABLE;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;

public class RoleAssignmentTest {

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();

  private static final String ROLE_NAME = "role";

  private static class TestStub extends RoleAssignment {
    TestStub(Role role, User user) {
      super(role, user);
    }

    @Override
    protected Set<RightType> getAcceptableRightTypes() {
      Set<RightType> acceptableRightTypes = new HashSet<>();
      acceptableRightTypes.add(GENERAL_ADMIN);
      return acceptableRightTypes;
    }

    @Override
    public boolean hasRight(RightQuery rightQuery) {
      return false;
    }
  }

  @Test
  public void shouldAllowCreationWithMatchingRoleTypes() {
    new TestStub(Role.newRole(ROLE_NAME, Right.newRight("adminRight1", GENERAL_ADMIN)),
        mock(User.class));
  }

  @Test
  public void shouldNotAllowCreationWithMismatchingRoleTypes() {
    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage(
        new Message(ERROR_TYPE_NOT_ACCEPTABLE, ORDER_FULFILLMENT,
            Collections.singleton(GENERAL_ADMIN)).toString());
    new TestStub(Role.newRole(ROLE_NAME, Right.newRight("fulfillmentRight1", ORDER_FULFILLMENT)),
        mock(User.class));
  }
}