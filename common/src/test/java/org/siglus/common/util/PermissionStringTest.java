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

package org.siglus.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PermissionStringTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private String rightName = "RIGHT_NAME";

  @Test
  public void should_parse_right_name_only_when_new_instance_given_right_name_only() {
    // when
    PermissionString permissionString = new PermissionString(rightName);

    // then
    assertEquals(rightName, permissionString.getRightName());
    assertNull(permissionString.getFacilityId());
    assertNull(permissionString.getProgramId());
  }

  @Test
  public void should_parse_right_and_facility_only_when_new_instance_given_right_and_facility() {
    // given
    UUID facilityId = UUID.randomUUID();
    String origin = rightName + "|" + facilityId;

    // when
    PermissionString permissionString = new PermissionString(origin);

    // then
    assertEquals(rightName, permissionString.getRightName());
    assertEquals(facilityId, permissionString.getFacilityId());
    assertNull(permissionString.getProgramId());
  }

  @Test
  public void should_parse_all_when_new_instance_given_all() {
    // given
    UUID facilityId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    String origin = rightName + "|" + facilityId + "|" + programId;

    // when
    PermissionString permissionString = new PermissionString(origin);

    // then
    assertEquals(rightName, permissionString.getRightName());
    assertEquals(facilityId, permissionString.getFacilityId());
    assertEquals(programId, permissionString.getProgramId());
  }

  @Test
  public void should_parse_all_when_new_instance_given_all1() {
    // given
    String bad = "|||";
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(bad);

    // when
    new PermissionString(bad);

  }

}
