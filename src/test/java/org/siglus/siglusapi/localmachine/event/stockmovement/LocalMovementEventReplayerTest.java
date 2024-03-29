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

package org.siglus.siglusapi.localmachine.event.stockmovement;

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.localmachine.EventPayloadCheckUtils;
import org.siglus.siglusapi.localmachine.cdc.JdbcSinker;
import org.siglus.siglusapi.localmachine.cdc.TableChangeEvent;
import org.siglus.siglusapi.localmachine.cdc.TableChangeEvent.RowChangeEvent;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class LocalMovementEventReplayerTest {

  @InjectMocks
  private LocalMovementEventReplayer replayer;

  @Mock
  private JdbcSinker jdbcSinker;

  @Test
  public void shouldReplaySuccess() {
    // given
    LocalMovementEvent event = buildLocalMovementEvent();
    int i = EventPayloadCheckUtils.checkEventSerializeChanges(event, event.getClass());

    // when
    replayer.replay(event);

    // then
    assertEquals(0, i);
  }

  private LocalMovementEvent buildLocalMovementEvent() {
    LocalMovementEvent event = new LocalMovementEvent();
    event.setTableChangeEvents(Lists.newArrayList(buildTableChangeEvent()));
    return event;
  }

  private TableChangeEvent buildTableChangeEvent() {
    return TableChangeEvent.builder()
        .tableName("stock_event")
        .schemaName("schemaName")
        .schemaVersion("schemaVersion")
        .rowChangeEvents(Lists.newArrayList(buildRowChangeEvent()))
        .columns(Lists.newArrayList("id", "status"))
        .build();
  }

  private RowChangeEvent buildRowChangeEvent() {
    RowChangeEvent rowChangeEvent = new RowChangeEvent();
    rowChangeEvent.setDeletion(Boolean.FALSE);
    rowChangeEvent.setValues(Lists.newArrayList(UUID.randomUUID().toString(), "status"));
    return rowChangeEvent;
  }
}