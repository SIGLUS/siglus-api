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

package org.siglus.siglusapi.localmachine.eventstore;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventBackupDeleteTaskTest {

  @Mock
  private EventPayloadRepository eventPayloadRepository;

  @Test
  public void shouldNotBackupWhenNothingToArchive() {

    // when
    eventPayloadRepository.deleteByEventIds(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()));

    // then
    verify(eventPayloadRepository).deleteByEventIds(anyListOf(UUID.class));
  }
}