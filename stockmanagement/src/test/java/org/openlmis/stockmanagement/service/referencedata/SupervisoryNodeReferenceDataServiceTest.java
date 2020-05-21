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

package org.openlmis.stockmanagement.service.referencedata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.openlmis.stockmanagement.service.referencedata.SupervisoryNodeReferenceDataService.FACILITY_ID;
import static org.openlmis.stockmanagement.service.referencedata.SupervisoryNodeReferenceDataService.PROGRAM_ID;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.stockmanagement.dto.referencedata.SupervisoryNodeDto;
import org.openlmis.stockmanagement.testutils.DummyPage;
import org.openlmis.stockmanagement.util.RequestParameters;

public class SupervisoryNodeReferenceDataServiceTest {

  private UUID facility = UUID.randomUUID();
  private UUID program = UUID.randomUUID();
  private SupervisoryNodeDto supervisoryNode = new SupervisoryNodeDto();
  private SupervisoryNodeReferenceDataService spy;

  @Before
  public void setUp() {
    supervisoryNode.setId(UUID.randomUUID());
    spy = spy(new SupervisoryNodeReferenceDataService());
  }

  @Test
  public void shouldReturnNullIfEmptyPage() {
    doReturn(new DummyPage<SupervisoryNodeDto>(Collections.emptyList()))
        .when(spy)
        .getPage(RequestParameters.init()
            .set(PROGRAM_ID, program)
            .set(FACILITY_ID, facility));

    assertNull(spy.findSupervisoryNode(program, facility));
  }

  @Test
  public void shouldReturnFirstElementIfMoreThanOneFound() {
    SupervisoryNodeReferenceDataService spy = spy(new SupervisoryNodeReferenceDataService());
    SupervisoryNodeDto secondNode = new SupervisoryNodeDto();
    List<SupervisoryNodeDto> found = Arrays.asList(supervisoryNode, secondNode);
    doReturn(new DummyPage<>(found))
        .when(spy)
        .getPage(RequestParameters.init()
            .set(PROGRAM_ID, program)
            .set(FACILITY_ID, facility));

    SupervisoryNodeDto foundNode = spy.findSupervisoryNode(program, facility);

    assertEquals(supervisoryNode, foundNode);
    assertNotEquals(secondNode, foundNode);
  }

  @Test
  public void shouldReturnFirstElementFoundElementIfOneFound() {
    SupervisoryNodeReferenceDataService spy = spy(new SupervisoryNodeReferenceDataService());
    doReturn(new DummyPage<>(Collections.singletonList(supervisoryNode)))
        .when(spy)
        .getPage(RequestParameters.init()
            .set(PROGRAM_ID, program)
            .set(FACILITY_ID, facility));

    SupervisoryNodeDto foundNode = spy.findSupervisoryNode(program, facility);

    assertEquals(supervisoryNode, foundNode);
  }
}
