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

package org.openlmis.referencedata.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.dto.RightAssignmentDto;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

@RunWith(MockitoJUnitRunner.class)
public class RightAssignmentServiceTest {

  private static final String RIGHT_NAME = "rightName";

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private RightAssignmentService rightAssignmentService;

  private UUID userId;
  private UUID supervisoryNodeId;
  private UUID programId;
  
  @Before
  public void setUp() {
    userId = UUID.randomUUID();
    supervisoryNodeId = UUID.randomUUID();
    programId = UUID.randomUUID();
  }

  @Test
  public void convertForInsertShouldConvertDirectFulfillmentAndHomeFacilityRightAssignments()
      throws IOException {
    // given
    RightAssignmentDto expected = new RightAssignmentDto(
        userId,
        RIGHT_NAME,
        UUID.randomUUID(),
        programId,
        null);

    // when
    Set<RightAssignmentDto> actual = rightAssignmentService
        .convertForInsert(Collections.singletonList(expected), null);

    // then
    assertEquals(1, actual.size());
    assertEquals(expected, actual.iterator().next());
  }

  @Test
  public void convertForInsertShouldConvertSupervisoryNodeRightAssignments()
      throws IOException {
    // given
    Resource resource = mock(Resource.class);
    when(resource.getDescription()).thenReturn("description");
    InputStream inputStream = spy(IOUtils.toInputStream("some data"));
    when(resource.getInputStream()).thenReturn(inputStream);

    RightAssignmentDto rightAssignmentDto = new RightAssignmentDto(
        userId,
        RIGHT_NAME,
        null,
        programId,
        supervisoryNodeId);

    UUID facility1Id = UUID.randomUUID();
    UUID facility2Id = UUID.randomUUID();
    List<UUID> facilityIds = Arrays.asList(facility1Id, facility2Id);
    when(jdbcTemplate.queryForList(
        any(String.class),
        any(Class.class),
        any(UUID.class),
        any(UUID.class))).thenReturn(facilityIds);
    
    // when
    Set<RightAssignmentDto> actual = rightAssignmentService
        .convertForInsert(Collections.singletonList(rightAssignmentDto), resource);

    // then
    assertEquals(2, actual.size());
    for (RightAssignmentDto current : actual) {
      assertEquals(userId, current.getUserId());
      assertEquals(RIGHT_NAME, current.getRightName());
      assertEquals(programId, current.getProgramId());
      assertTrue(facilityIds.contains(current.getFacilityId()));
    }
  }
}
