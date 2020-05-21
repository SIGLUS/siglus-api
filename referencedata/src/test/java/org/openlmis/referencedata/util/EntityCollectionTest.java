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

package org.openlmis.referencedata.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.BaseEntity;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.testbuilder.ProgramDataBuilder;

@RunWith(MockitoJUnitRunner.class)
public class EntityCollectionTest {

  private static final UUID PROGRAM_1_ID = UUID.randomUUID();
  private static final UUID PROGRAM_2_ID = UUID.randomUUID();
  private static final List<UUID> ALL_PROGRAM_IDS = Arrays.asList(PROGRAM_1_ID, PROGRAM_2_ID);

  private EntityCollection<Program> collection;

  @Test
  public void getByIdShouldFindByUuid() {
    createCollectionWithPrograms();

    assertNotNull(collection.getById(PROGRAM_1_ID));
    assertNotNull(collection.getById(PROGRAM_2_ID));
  }

  @Test
  public void getByIdShouldReturnNullIfElementNotExists() {
    createCollectionWithPrograms();

    assertNull(collection.getById(UUID.randomUUID()));
  }

  @Test
  public void valuesShouldReturnAllElements() {
    createCollectionWithPrograms();

    Set<UUID> collect = collection.values().stream().map(BaseEntity::getId)
        .collect(Collectors.toSet());

    assertTrue(ALL_PROGRAM_IDS.containsAll(collect));
    assertTrue(collect.containsAll(ALL_PROGRAM_IDS));
  }

  @Test
  public void valuesShouldReturnEmptyCollectionIfCreatedWithEmptyCollection() {
    collection = new EntityCollection<>(new ArrayList<>());

    assertNotNull(collection.values());
    assertEquals(0, collection.values().size());
  }

  private void createCollectionWithPrograms() {
    collection = new EntityCollection<>(Arrays.asList(
        new ProgramDataBuilder().withId(PROGRAM_1_ID).build(),
        new ProgramDataBuilder().withId(PROGRAM_2_ID).build()
    ));
  }
}
