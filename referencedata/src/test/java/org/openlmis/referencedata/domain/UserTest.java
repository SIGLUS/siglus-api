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

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.referencedata.dto.UserDto;
import org.openlmis.referencedata.testbuilder.DirectRoleAssignmentDataBuilder;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;
import org.openlmis.referencedata.testbuilder.SupervisoryNodeDataBuilder;
import org.openlmis.referencedata.testbuilder.SupportedProgramDataBuilder;
import org.openlmis.referencedata.testbuilder.UserDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
public class UserTest {

  private static final String RIGHT_NAME = "right1";

  private RightQuery rightQuery = new RightQuery(Right.newRight("supervisionRight1",
      RightType.SUPERVISION));

  private RoleAssignment assignment1 = mock(RoleAssignment.class);

  private RoleAssignment assignment2 = mock(RoleAssignment.class);

  private User user;
  private Program program;

  private String roleName = "role";

  @Before
  public void setUp() {
    user = new UserDataBuilder().build();
    program = new Program("P1");
  }

  @Test
  public void shouldBeAbleToAssignRoleToUser() {
    //when
    user.assignRoles(new DirectRoleAssignment(Role.newRole(roleName, Right.newRight("reportRight1",
        RightType.REPORTS)), user));

    //then
    assertThat(user.getRoleAssignments().size()).isEqualTo(1);
  }

  @Test
  public void shouldHaveRightIfAnyRoleAssignmentHasRight() {
    //given
    user.assignRoles(assignment1);
    user.assignRoles(assignment2);

    when(assignment1.hasRight(rightQuery)).thenReturn(true);
    when(assignment2.hasRight(rightQuery)).thenReturn(false);

    //when
    boolean hasRight = user.hasRight(rightQuery);

    //then
    assertThat(hasRight).isTrue();
  }

  @Test
  public void shouldNotHaveRightIfNoRoleAssignmentHasRight() {
    //given
    user.assignRoles(assignment1);
    user.assignRoles(assignment2);

    when(assignment1.hasRight(rightQuery)).thenReturn(false);
    when(assignment2.hasRight(rightQuery)).thenReturn(false);

    //when
    boolean hasRight = user.hasRight(rightQuery);

    //then
    assertThat(hasRight).isFalse();
  }

  @Test
  public void shouldGetSupervisedFacilities() {
    //given
    SupervisoryNode provinceNode = getSupervisoryHierarchy();

    Right right = Right.newRight(RIGHT_NAME, RightType.SUPERVISION);
    Role role = Role.newRole(roleName, right);

    RoleAssignment assignment = new SupervisionRoleAssignment(role, user, program, provinceNode);

    user.assignRoles(assignment);

    //when
    Set<Facility> facilities = user.getSupervisedFacilities(right, program);

    //then
    assertThat(facilities.size()).isEqualTo(3);
  }

  @Test
  public void shouldNotGetSupervisedFacilitiesForNonMatchingPrograms() {
    //given
    SupervisoryNode provinceNode = getSupervisoryHierarchy();

    Right right = Right.newRight(RIGHT_NAME, RightType.SUPERVISION);
    Role role = Role.newRole(roleName, right);

    Program anotherProgram = new Program("another");

    RoleAssignment assignment = new SupervisionRoleAssignment(role, user, anotherProgram,
        provinceNode);

    user.assignRoles(assignment);

    //when
    Set<Facility> facilities = user.getSupervisedFacilities(right, anotherProgram);

    //then
    assertThat(facilities.size()).isEqualTo(0);
  }

  @Test
  public void shouldNotGetSupervisedFacilitiesForNonMatchingRight() {
    //given
    SupervisoryNode provinceNode = getSupervisoryHierarchy();

    Right right = Right.newRight(RIGHT_NAME, RightType.SUPERVISION);
    Role role = Role.newRole(roleName, right);

    RoleAssignment assignment = new SupervisionRoleAssignment(role, user, program, provinceNode);

    user.assignRoles(assignment);

    //when
    Set<Facility> facilities = user.getSupervisedFacilities(
        Right.newRight("anotherRight", RightType.SUPERVISION), program);

    //then
    assertThat(facilities.size()).isEqualTo(0);
  }

  @Test
  public void shouldGetFulfillmentFacilities() {
    //given
    FulfillmentRoleAssignment fulfillmentRoleAssignment1 = mock(FulfillmentRoleAssignment.class);
    FulfillmentRoleAssignment fulfillmentRoleAssignment2 = mock(FulfillmentRoleAssignment.class);
    Right fulfillmentRight1 = mock(Right.class);
    Facility facility1 = mock(Facility.class);
    Facility facility2 = mock(Facility.class);

    when(fulfillmentRoleAssignment1.getWarehouse()).thenReturn(facility1);
    when(fulfillmentRoleAssignment2.getWarehouse()).thenReturn(facility2);

    when(fulfillmentRoleAssignment1.hasRight(new RightQuery(fulfillmentRight1, facility1)))
        .thenReturn(true);
    when(fulfillmentRoleAssignment2.hasRight(new RightQuery(fulfillmentRight1, facility2)))
        .thenReturn(false);

    user.assignRoles(fulfillmentRoleAssignment1);
    user.assignRoles(fulfillmentRoleAssignment2);
    user.assignRoles(assignment1);
    user.assignRoles(assignment2);

    //when
    Set<Facility> facilities = user.getFulfillmentFacilities(fulfillmentRight1);

    //then - only facilities where we have the right are returned
    assertThat(facilities.size()).isEqualTo(1);
    assertThat(facilities.iterator().next()).isEqualTo(facility1);
  }

  @Test
  public void shouldExportData() {
    UserDto exporter = new UserDto();

    user.export(exporter);

    assertThat(exporter).isEqualToComparingFieldByField(user);
  }

  @Test
  public void shouldImportData() {
    UserDto importer = new UserDto();
    user.export(importer);

    User newUser = User.newUser(importer);

    assertThat(newUser).isEqualToComparingFieldByField(user);
  }

  @Test
  public void shouldCleanRoleAndRightAssignments() {
    user.assignRoles(new DirectRoleAssignmentDataBuilder().withUser(user).build());

    assertTrue(user.getRightAssignments().size() > 0);
    assertTrue(user.getRoleAssignments().size() > 0);

    user.clearRoleAssignments();

    assertEquals(user.getRightAssignments().size(), 0);
    assertEquals(user.getRoleAssignments().size(), 0);
  }

  private SupervisoryNode getSupervisoryHierarchy() {
    Facility facility = new FacilityDataBuilder().build();

    SupervisoryNode districtNode = new SupervisoryNodeDataBuilder().build();
    RequisitionGroup districtGroup = new RequisitionGroup("DG", "DGN", districtNode);
    districtGroup.setMemberFacilities(newHashSet(new Facility("C2")));
    addSupportedPrograms(districtGroup);
    districtGroup.getMemberFacilities().add(facility);
    ProcessingSchedule processingSchedule = new ProcessingSchedule(Code.code("PS1"), "Schedule1");
    RequisitionGroupProgramSchedule districtGroupProgramSchedule =
        RequisitionGroupProgramSchedule.newRequisitionGroupProgramSchedule(
            districtGroup, program, processingSchedule, false);
    districtGroup.setRequisitionGroupProgramSchedules(
        Collections.singletonList(districtGroupProgramSchedule));
    districtNode.setRequisitionGroup(districtGroup);

    SupervisoryNode provinceNode = new SupervisoryNodeDataBuilder().build();
    RequisitionGroup provinceGroup = new RequisitionGroup("PG", "PGN", provinceNode);
    provinceGroup.setMemberFacilities(newHashSet(new Facility("C4"),
        new Facility("C5")));
    addSupportedPrograms(provinceGroup);
    RequisitionGroupProgramSchedule provinceGroupProgramSchedule =
        RequisitionGroupProgramSchedule.newRequisitionGroupProgramSchedule(
            provinceGroup, program, processingSchedule, false);
    provinceGroup.setRequisitionGroupProgramSchedules(
        Collections.singletonList(provinceGroupProgramSchedule));
    provinceNode.setRequisitionGroup(provinceGroup);

    districtNode.assignParentNode(provinceNode);

    return provinceNode;
  }

  private void addSupportedPrograms(RequisitionGroup group) {
    group
        .getMemberFacilities()
        .forEach(facility -> facility
            .setSupportedPrograms(newHashSet(new SupportedProgramDataBuilder()
                .withFacility(facility).withProgram(program).build())));
  }
}
