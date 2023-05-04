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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.localmachine.EventBackupTask;
import org.siglus.siglusapi.localmachine.eventstore.backup.EventPayloadBackupRepository;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class EventBackupTaskTest {

  @InjectMocks
  private EventBackupTask eventBackupTask;
  @Mock
  private EventRecordRepository eventRecordRepository;
  @Mock
  private AgentInfoRepository agentInfoRepository;
  @Mock
  private EventPayloadBackupRepository eventRecordBackupRepository;
  @Mock
  private EventPayloadRepository eventPayloadRepository;

  @Test
  public void shouldNotBackupWhenNothingToArchive() {
    // given
    when(eventRecordRepository
        .findFirst100ByArchivedFalseAndReceiverSyncedTrueAndOnlineWebSyncedTrueAndLocalReplayedTrue())
        .thenReturn(Collections.emptyList());

    // when
    eventBackupTask.run();

    // then
    verify(eventPayloadRepository, never()).delete(anyListOf(EventPayload.class));
  }

  @Test
  public void shouldBackupWhenNeedArchiveForOnlineWeb() {
    // given
    final EventRecord event = new EventRecord();
    event.setPayload(new byte[]{0, 1, 2});
    event.setArchived(true);
    event.setId(UUID.randomUUID());
    event.setOnlineWebSynced(true);
    event.setReceiverSynced(true);
    event.setLocalReplayed(true);
    final List<EventRecord> list = new ArrayList<>();
    list.add(event);
    when(eventRecordRepository
        .findFirst100ByArchivedFalseAndReceiverSyncedTrueAndOnlineWebSyncedTrueAndLocalReplayedTrue())
        .thenReturn(list)
        .thenReturn(list)
        .thenReturn(Collections.emptyList());

    // when
    eventBackupTask.run();

    // then
    verify(eventPayloadRepository, times(2)).delete(anyListOf(EventPayload.class));
  }
}
