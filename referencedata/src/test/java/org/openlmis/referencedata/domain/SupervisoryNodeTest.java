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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.referencedata.dto.SupervisoryNodeDto;
import org.openlmis.referencedata.testbuilder.SupervisoryNodeDataBuilder;
import org.openlmis.referencedata.testbuilder.SupportedProgramDataBuilder;

@SuppressWarnings({"PMD.TooManyMethods"})
public class SupervisoryNodeTest {

  private SupervisoryNode supervisoryNode1;
  private Facility facility2;
  private RequisitionGroup requisitionGroup1;
  private Program program;
  private ProcessingSchedule processingSchedule;

  @Before
  public void setUp() {
    program = new Program("P1");
    facility2 = new Facility("C2");
    supervisoryNode1 = new SupervisoryNodeDataBuilder().build();
    requisitionGroup1 = new RequisitionGroup("RG1", "RGN1", supervisoryNode1);
    requisitionGroup1.setId(UUID.randomUUID());
    requisitionGroup1.setMemberFacilities(Sets.newHashSet(facility2, new Facility("C3")));
    addSupportedPrograms(requisitionGroup1);
    processingSchedule = new ProcessingSchedule(Code.code("PS1"), "Schedule1");
    RequisitionGroupProgramSchedule requisitionGroupProgramSchedule1 =
        RequisitionGroupProgramSchedule.newRequisitionGroupProgramSchedule(
            requisitionGroup1, program, processingSchedule, false);
    requisitionGroup1.setRequisitionGroupProgramSchedules(
        Collections.singletonList(requisitionGroupProgramSchedule1));
    supervisoryNode1.setRequisitionGroup(requisitionGroup1);
  }

  @Test
  public void shouldExportInstance() throws Exception {
    SupervisoryNodeDto dto = new SupervisoryNodeDto();
    supervisoryNode1.export(dto);

    assertThat(dto.getId(), equalTo(supervisoryNode1.getId()));
    assertThat(dto.getCode(), equalTo(supervisoryNode1.getCode()));
    assertThat(dto.getName(), equalTo(supervisoryNode1.getName()));
    assertThat(dto.getDescription(), equalTo(supervisoryNode1.getDescription()));
    assertThat(dto.getFacility().getId(), equalTo(supervisoryNode1.getFacility().getId()));
    assertThat(
        dto.getRequisitionGroup().getId(),
        equalTo(supervisoryNode1.getRequisitionGroup().getId())
    );
  }

  @Test
  public void shouldCreateInstanceFromImporter() throws Exception {
    SupervisoryNodeDto dto = new SupervisoryNodeDto();
    supervisoryNode1.export(dto);

    SupervisoryNode sn = SupervisoryNode.newSupervisoryNode(dto);
    assertThat(sn.getId(), equalTo(dto.getId()));
    assertThat(sn.getCode(), equalTo(dto.getCode()));
    assertThat(sn.getName(), equalTo(dto.getName()));
    assertThat(sn.getDescription(), equalTo(dto.getDescription()));
    assertThat(sn.getFacility().getId(), equalTo(dto.getFacility().getId()));
    assertThat(sn.getRequisitionGroup().getId(), equalTo(dto.getRequisitionGroup().getId()));
  }

  @Test
  public void shouldGetAllDirectSupervisedFacilities() {
    //when
    Set<Facility> facilities = supervisoryNode1.getAllSupervisedFacilities(program);

    //then
    assertThat(facilities.size(), is(2));
  }

  @Test
  public void shouldGetAllIndirectSupervisedFacilities() {
    testGetAllIndirectSupervisedFacilities(program);
  }

  @Test
  public void shouldGetAllSupervisedFacilitiesIfProgramIsNotProvided() {
    testGetAllIndirectSupervisedFacilities(null);
  }

  private void testGetAllIndirectSupervisedFacilities(Program program) {
    //given
    SupervisoryNode supervisoryNode2 = new SupervisoryNodeDataBuilder().build();
    RequisitionGroup requisitionGroup2 = new RequisitionGroup("RG2", "RGN2", supervisoryNode2);
    requisitionGroup2.setMemberFacilities(Sets.newHashSet(new Facility("C5")));
    addSupportedPrograms(requisitionGroup2);
    RequisitionGroupProgramSchedule requisitionGroupProgramSchedule2 =
        RequisitionGroupProgramSchedule.newRequisitionGroupProgramSchedule(
            requisitionGroup2, this.program, processingSchedule, false);
    requisitionGroup2.setRequisitionGroupProgramSchedules(
        Collections.singletonList(requisitionGroupProgramSchedule2));
    supervisoryNode2.setRequisitionGroup(requisitionGroup2);

    supervisoryNode2.assignParentNode(supervisoryNode1);

    //when
    Set<Facility> facilities = supervisoryNode1.getAllSupervisedFacilities(program);

    //then
    assertThat(facilities.size(), is(3));
  }

  @Test
  public void shouldNotGetSupervisedFacilitiesIfNoRequisitionGroup() {
    //given
    supervisoryNode1.setRequisitionGroup(null);

    //when
    Set<Facility> facilities = supervisoryNode1.getAllSupervisedFacilities(program);

    //then
    assertThat(facilities.size(), is(0));
  }

  @Test
  public void shouldNotGetSupervisedFacilitiesIfNoMemberFacilities() {
    //given
    requisitionGroup1.setMemberFacilities(Collections.emptySet());

    //when
    Set<Facility> facilities = supervisoryNode1.getAllSupervisedFacilities(program);

    //then
    assertThat(facilities.size(), is(0));
  }

  @Test
  public void shouldNotGetSupervisedFacilitiesIfNoProgramsInRequisitionGroup() {
    //given
    requisitionGroup1.setRequisitionGroupProgramSchedules(Collections.emptyList());

    //when
    Set<Facility> facilities = supervisoryNode1.getAllSupervisedFacilities(program);

    //then
    assertThat(facilities.size(), is(0));
  }

  @Test
  public void shouldNotGetSupervisedFacilitiesIfNoMatchingProgramInRequisitionGroup() {
    //when
    Set<Facility> facilities = supervisoryNode1.getAllSupervisedFacilities(new Program("another"));

    //then
    assertThat(facilities.size(), is(0));
  }

  @Test
  public void shouldReturnTrueIfSupervisesFacilityByProgram() {
    assertTrue(supervisoryNode1.supervises(facility2, program));
  }

  @Test
  public void shouldReturnFalseIfDoesNotSuperviseFacility() {
    assertFalse(supervisoryNode1.supervises(new Facility("New Facility"), program));
  }

  @Test
  public void shouldReturnFalseIfSupervisesFacilityNotByProgram() {
    assertFalse(supervisoryNode1.supervises(facility2, new Program("another")));
  }

  @Test
  public void shouldAssignParentNode() {
    SupervisoryNode parent = new SupervisoryNodeDataBuilder().build();
    SupervisoryNode child = new SupervisoryNodeDataBuilder().build();

    child.assignParentNode(parent);

    assertThat(parent.getChildNodes(), hasItem(child));
    assertThat(child.getParentNode(), is(parent));
  }

  @Test
  public void shouldRemoveParentNode() {
    SupervisoryNode parent = new SupervisoryNodeDataBuilder().build();
    SupervisoryNode child = new SupervisoryNodeDataBuilder().build();

    child.assignParentNode(parent);
    child.assignParentNode(null);

    assertThat(parent.getChildNodes(), hasSize(0));
    assertThat(child.getParentNode(), is(nullValue()));
  }

  @Test
  public void shouldAssignChildNodes() {
    SupervisoryNode parent = new SupervisoryNodeDataBuilder()
        .withChildNode(new SupervisoryNodeDataBuilder().build())
        .withChildNode(new SupervisoryNodeDataBuilder().build())
        .build();
    SupervisoryNode child = new SupervisoryNodeDataBuilder().build();
    Set<SupervisoryNode> children = Sets.newHashSet(child);

    parent.assignChildNodes(children);

    assertThat(parent.getChildNodes(), hasSize(1));
    assertThat(parent.getChildNodes(), hasItem(child));
    assertThat(child.getParentNode(), is(parent));
  }

  @Test
  public void shouldRemoveChildNodes() {
    SupervisoryNode child1 = new SupervisoryNodeDataBuilder().build();
    SupervisoryNode child2 = new SupervisoryNodeDataBuilder().build();
    SupervisoryNode parent = new SupervisoryNodeDataBuilder()
        .withChildNode(child1)
        .withChildNode(child2)
        .build();

    Set<SupervisoryNode> children = Sets.newHashSet(child1, child2);

    parent.assignChildNodes(children);
    parent.assignChildNodes(Sets.newHashSet());

    assertThat(parent.getChildNodes(), hasSize(0));
    assertThat(child1.getParentNode(), is(nullValue()));
    assertThat(child2.getParentNode(), is(nullValue()));
  }

  @Test
  public void shouldAssignPartnerNodes() {
    SupervisoryNode partnerOf = new SupervisoryNodeDataBuilder()
        .withPartnerNode(new SupervisoryNodeDataBuilder().build())
        .withPartnerNode(new SupervisoryNodeDataBuilder().build())
        .build();
    SupervisoryNode partner = new SupervisoryNodeDataBuilder().build();
    Set<SupervisoryNode> partners = Sets.newHashSet(partner);

    partnerOf.assignPartnerNodes(partners);

    assertThat(partnerOf.getPartnerNodes(), hasSize(1));
    assertThat(partnerOf.getPartnerNodes(), hasItem(partner));
    assertThat(partner.getPartnerNodeOf(), is(partnerOf));
  }

  @Test
  public void shouldRemovePartnerNodes() {
    SupervisoryNode partner1 = new SupervisoryNodeDataBuilder().build();
    SupervisoryNode partner2 = new SupervisoryNodeDataBuilder().build();
    SupervisoryNode partnerOf = new SupervisoryNodeDataBuilder()
        .withPartnerNode(partner1)
        .withPartnerNode(partner2)
        .build();

    Set<SupervisoryNode> partners = Sets.newHashSet(partner1, partner2);

    partnerOf.assignPartnerNodes(partners);
    partnerOf.assignPartnerNodes(Sets.newHashSet());

    assertThat(partnerOf.getPartnerNodes(), hasSize(0));
    assertThat(partner1.getPartnerNodeOf(), is(nullValue()));
    assertThat(partner2.getPartnerNodeOf(), is(nullValue()));
  }

  @Test
  public void shouldAssignPartnerNodeOf() {
    SupervisoryNode partnerOf = new SupervisoryNodeDataBuilder().build();
    SupervisoryNode partner = new SupervisoryNodeDataBuilder().build();

    partner.assignPartnerNodeOf(partnerOf);

    assertThat(partnerOf.getPartnerNodes(), hasItem(partner));
    assertThat(partner.getPartnerNodeOf(), is(partnerOf));
  }

  @Test
  public void shouldRemovePartnerNodeOf() {
    SupervisoryNode partnerOf = new SupervisoryNodeDataBuilder().build();
    SupervisoryNode partner = new SupervisoryNodeDataBuilder().build();

    partner.assignPartnerNodeOf(partnerOf);
    partner.assignPartnerNodeOf(null);

    assertThat(partnerOf.getChildNodes(), hasSize(0));
    assertThat(partner.getParentNode(), is(nullValue()));
  }

  private void addSupportedPrograms(RequisitionGroup group) {
    group
        .getMemberFacilities()
        .forEach(facility -> facility
            .setSupportedPrograms(newHashSet(new SupportedProgramDataBuilder()
                .withFacility(facility).withProgram(program).build())));
  }
}
