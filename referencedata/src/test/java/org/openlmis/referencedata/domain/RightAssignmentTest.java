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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class RightAssignmentTest {

  private RightAssignment rightAssignmentA;
  private RightAssignment rightAssignmentB;

  private String rightName;
  private UUID facilityId;
  private UUID programId;

  @Before
  public void setUp() {
    rightName = "rightName";
    facilityId = UUID.randomUUID();
    programId = UUID.randomUUID();
    rightAssignmentA = new RightAssignment(null, rightName, facilityId, programId);
    rightAssignmentB = new RightAssignment(null, "anotherRightName", facilityId, programId);
  }

  @Test
  public void equalsShouldReturnTrueForSameObject() {
    assertTrue(rightAssignmentA.equals(rightAssignmentA));
  }

  @Test
  public void equalsShouldReturnTrueForSameNameFacilityAndProgram() {
    RightAssignment rightAssignmentADupe = new RightAssignment(
        null, rightName, facilityId, programId);
    assertTrue(rightAssignmentA.equals(rightAssignmentADupe));
    assertTrue(rightAssignmentADupe.equals(rightAssignmentA));
  }

  @Test
  public void equalsShouldReturnFalseForDifferentNameFacilityOrProgram() {
    assertFalse(rightAssignmentA.equals(rightAssignmentB));

    rightAssignmentB = new RightAssignment(null, rightName, UUID.randomUUID(), programId);
    assertFalse(rightAssignmentA.equals(rightAssignmentB));

    rightAssignmentB = new RightAssignment(null, rightName, facilityId, UUID.randomUUID());
    assertFalse(rightAssignmentA.equals(rightAssignmentB));
  }

  @Test
  public void hashCodeShouldEnforceHashCode() {
    RightAssignment rightAssignmentADupe = new RightAssignment(
        null, rightName, facilityId, programId);
    assertEquals(rightAssignmentA.hashCode(), rightAssignmentADupe.hashCode());

    assertNotEquals(rightAssignmentA.hashCode(), rightAssignmentB.hashCode());
  }

  @Test
  public void toStringShouldReturnPipeDelimitedString() {
    assertEquals(rightName + "|" + facilityId + "|" + programId, rightAssignmentA.toString());
  }
}
