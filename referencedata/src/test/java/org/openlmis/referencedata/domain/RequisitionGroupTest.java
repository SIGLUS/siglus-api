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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.referencedata.dto.RequisitionGroupDto;

public class RequisitionGroupTest {

  RequisitionGroup requisitionGroup;
  Program program;

  @Before
  public void setUp() {
    requisitionGroup = new RequisitionGroup("RG", "Requisition Group", mock(SupervisoryNode.class));
    program = new Program("P1");
    requisitionGroup.setRequisitionGroupProgramSchedules(Collections
        .singletonList(RequisitionGroupProgramSchedule.newRequisitionGroupProgramSchedule(
            requisitionGroup, program, mock(ProcessingSchedule.class), false)));
  }

  @Test
  public void shouldAssociateRequisitionGroupAndScheduleProperlyFromImporter() {
    RequisitionGroupDto dto = new RequisitionGroupDto();
    dto.setCode("RG1");
    dto.setName("RequisitionGroup1");
    dto.setSupervisoryNode(mock(SupervisoryNode.class));
    RequisitionGroupProgramSchedule schedule = RequisitionGroupProgramSchedule
        .newRequisitionGroupProgramSchedule(null, program, mock(ProcessingSchedule.class), false);
    dto.setRequisitionGroupProgramSchedules(Collections.singletonList(schedule));

    RequisitionGroup actual = RequisitionGroup.newRequisitionGroup(dto);

    assertNotNull(actual);
    assertNotNull(actual.getRequisitionGroupProgramSchedules());
    assertEquals(1, actual.getRequisitionGroupProgramSchedules().size());
    assertEquals("RG1", actual.getRequisitionGroupProgramSchedules().get(0).getRequisitionGroup()
        .getCode());
    assertEquals("RequisitionGroup1", actual.getRequisitionGroupProgramSchedules().get(0)
        .getRequisitionGroup().getName());
  }

  @Test
  public void supportsShouldReturnTrueIfSupportsProgram() {
    assertTrue(requisitionGroup.supports(program));
  }

  @Test
  public void supportsShouldReturnFalseIfDoesNotSupportProgram() {
    assertFalse(requisitionGroup.supports(new Program("another")));
  }
}
