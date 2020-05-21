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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.openlmis.referencedata.domain.RightType.SUPERVISION;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import org.openlmis.referencedata.testbuilder.SupervisoryNodeDataBuilder;
import org.openlmis.referencedata.testbuilder.SupportedProgramDataBuilder;
import org.openlmis.referencedata.testbuilder.UserDataBuilder;

@SuppressWarnings({"PMD.TooManyMethods"})
public class SupervisionRoleAssignmentTest {

  private Right right;
  private Role role;
  private Program program;
  private Facility homeFacility;
  private SupervisionRoleAssignment homeFacilityRoleAssignment;
  private SupervisoryNode supervisoryNode;
  private Facility supervisedFacility;
  private SupervisionRoleAssignment supervisedRoleAssignment;
  private User user;

  /**
   * Constructor for tests.
   */
  public SupervisionRoleAssignmentTest() {
    right = Right.newRight("right", SUPERVISION);
    role = Role.newRole("role", right);
    program = new Program("P1");
    homeFacility = new Facility("F1");
    UUID homeFacilityId = UUID.randomUUID();
    homeFacility.setId(homeFacilityId);

    user = new UserDataBuilder()
        .withHomeFacilityId(homeFacilityId)
        .build();

    homeFacilityRoleAssignment = new SupervisionRoleAssignment(role, user, program);

    supervisoryNode = new SupervisoryNodeDataBuilder().build();
    RequisitionGroup requisitionGroup = new RequisitionGroup("RG1", "RGN1", supervisoryNode);
    supervisedFacility = new Facility("F2");
    SupportedProgram supportedProgram = new SupportedProgramDataBuilder()
        .withFacility(supervisedFacility)
        .withProgram(program)
        .build();
    supervisedFacility.setSupportedPrograms(Sets.newHashSet(supportedProgram));
    requisitionGroup.setMemberFacilities(Sets.newHashSet(supervisedFacility));
    ProcessingSchedule processingSchedule = new ProcessingSchedule(Code.code("PS1"), "Schedule1");
    RequisitionGroupProgramSchedule requisitionGroupProgramSchedule =
        RequisitionGroupProgramSchedule.newRequisitionGroupProgramSchedule(
            requisitionGroup, program, processingSchedule, false);
    requisitionGroup.setRequisitionGroupProgramSchedules(
        Collections.singletonList(requisitionGroupProgramSchedule));
    supervisoryNode.setRequisitionGroup(requisitionGroup);
    supervisedRoleAssignment = new SupervisionRoleAssignment(role, user, program, supervisoryNode);
  }

  @Test
  public void shouldHaveRightWhenRightAndProgramAndHomeFacilityMatch() {

    //when
    RightQuery rightQuery = new RightQuery(right, program, homeFacility);
    boolean hasRight = homeFacilityRoleAssignment.hasRight(rightQuery);

    //then
    assertTrue(hasRight);
  }

  @Test
  public void shouldHaveRightWhenRightAndProgramAndSupervisedFacilityMatch() {

    //when
    RightQuery rightQuery = new RightQuery(right, program, supervisedFacility);
    boolean hasRight = supervisedRoleAssignment.hasRight(rightQuery);

    //then
    assertTrue(hasRight);
  }

  @Test
  public void shouldNotHaveRightWhenRightDoesNotMatch() {

    //when
    RightQuery rightQuery = new RightQuery(Right.newRight("right2", SUPERVISION), program,
        homeFacility);
    boolean hasRight = supervisedRoleAssignment.hasRight(rightQuery);

    //then
    assertFalse(hasRight);
  }

  @Test
  public void shouldNotHaveRightWhenProgramDoesNotMatch() {

    //when
    RightQuery rightQuery = new RightQuery(right, new Program("test"), homeFacility);
    boolean hasRight = supervisedRoleAssignment.hasRight(rightQuery);

    //then
    assertFalse(hasRight);
  }

  @Test
  public void shouldNotHaveRightWhenFacilityDoesNotMatch() {

    //when
    RightQuery rightQuery = new RightQuery(right, program, new Facility("Another"));
    boolean hasRight = supervisedRoleAssignment.hasRight(rightQuery);

    //then
    assertFalse(hasRight);
  }

  @Test
  public void shouldGetSupervisedFacilitiesWhenRightAndProgramMatch() {

    //when
    Set<Facility> supervisedFacilities = supervisedRoleAssignment.getSupervisedFacilities(right,
        program);

    //then
    assertThat(supervisedFacilities.size(), is(1));
    assertEquals(supervisedFacility, supervisedFacilities.iterator().next());
  }

  @Test
  public void shouldNotGetSupervisedFacilitiesWhenRightDoesNotMatch() {

    //when
    Set<Facility> supervisedFacilities = supervisedRoleAssignment.getSupervisedFacilities(
        Right.newRight("another", SUPERVISION), program);

    //then
    assertThat(supervisedFacilities.size(), is(0));
  }

  @Test
  public void shouldNotGetSupervisedFacilitiesWhenProgramDoesNotMatch() {

    //when
    Set<Facility> supervisedFacilities = supervisedRoleAssignment.getSupervisedFacilities(right,
        new Program("another"));

    //then
    assertThat(supervisedFacilities.size(), is(0));
  }

  @Test
  public void shouldNotGetSupervisedFacilitiesForHomeFacilitySupervision() {

    //when
    Set<Facility> supervisedFacilities = homeFacilityRoleAssignment.getSupervisedFacilities(right,
        program);

    //then
    assertThat(supervisedFacilities.size(), is(0));
  }

  @Test
  public void shouldAddRightAssignmentsIfUserHasHomeFacility() {
    user.getRightAssignments().clear();

    new SupervisionRoleAssignment(role, user, program);

    assertThat(
        user.getRightAssignments(),
        hasItem(hasProperty("facilityId", is(user.getHomeFacilityId())))
    );
  }

  @Test
  public void shouldNotAddRightAssignmentsIfUserHomeFacilityIsNull() {
    user.setHomeFacilityId(null);
    user.getRightAssignments().clear();

    new SupervisionRoleAssignment(role, user, program);

    assertThat(
        user.getRightAssignments(),
        not(hasItem(hasProperty("facilityId", is(user.getHomeFacilityId()))))
    );
  }
}
