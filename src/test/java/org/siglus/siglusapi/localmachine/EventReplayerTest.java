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

package org.siglus.siglusapi.localmachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.localmachine.ShedLockFactory.AutoClosableLock;
import org.siglus.siglusapi.localmachine.agent.LocalSyncResultsService;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;

@RunWith(MockitoJUnitRunner.class)
public class EventReplayerTest {

  private static final String GROUP_1 = "group1";
  @Mock private EventStore eventStore;
  @Mock private ShedLockFactory lockFactory;
  @Mock private EventPublisher eventPublisher;
  @Mock private LocalSyncResultsService localSyncResultsService;
  @InjectMocks private EventReplayer eventReplayer;

  @Before
  public void setup() {
    mockLock();
  }

  @Test
  public void shouldSendEventsToPublisherWhenReplayGivenGroupEvents() {
    // given
    Event groupEvent1 =
        Event.builder().id(UUID.randomUUID()).groupId(GROUP_1).groupSequenceNumber(0).build();
    Event groupEvent2 =
        Event.builder().id(UUID.randomUUID()).groupId(GROUP_1).groupSequenceNumber(1).build();
    Event groupEvent4MissingDependency =
        Event.builder().id(UUID.randomUUID()).groupId(GROUP_1).groupSequenceNumber(3).build();
    List<Event> groupEvents = Arrays.asList(groupEvent2, groupEvent1, groupEvent4MissingDependency);
    given(eventStore.loadSortedGroupEvents(GROUP_1))
        .willReturn(
            groupEvents.stream()
                .sorted(Comparator.comparingLong(Event::getGroupSequenceNumber))
                .collect(Collectors.toList()));
    List<Event> publishedEvents = getPublishedEvents();
    // when
    doNothing().when(localSyncResultsService).storeLastReplayRecord();
    eventReplayer.replay(groupEvents);
    // then
    assertThat(publishedEvents)
        .extracting(Event::getId)
        .containsExactly(groupEvent1.getId(), groupEvent2.getId());
  }

  @Test
  public void shouldSendEventsToPublisherWhenReplayGivenNonGroupEvents() {
    // given
    Event event1 =
        Event.builder().id(UUID.randomUUID()).groupId(null).localSequenceNumber(0).build();
    Event event2 =
        Event.builder().id(UUID.randomUUID()).groupId(null).localSequenceNumber(1).build();
    Event event3 =
        Event.builder().id(UUID.randomUUID()).groupId(null).localSequenceNumber(2).build();
    List<Event> nonGroupEvents = Arrays.asList(event2, event1, event3);
    List<Event> publishedEvents = getPublishedEvents();
    // when
    doNothing().when(localSyncResultsService).storeLastReplayRecord();
    eventReplayer.replay(nonGroupEvents);
    // then
    assertThat(publishedEvents)
        .extracting(Event::getId)
        .containsExactly(event1.getId(), event2.getId(), event3.getId());
  }

  private void mockLock() {
    AutoClosableLock lock = new AutoClosableLock(Optional.ofNullable(mock(SimpleLock.class)));
    given(lockFactory.lock(anyString())).willReturn(lock);
  }

  private List<Event> getPublishedEvents() {
    List<Event> publishedEvents = new LinkedList<>();
    doAnswer(invocation -> publishedEvents.add(invocation.getArgumentAt(0, Event.class)))
        .when(eventPublisher)
        .publishEvent(any());
    return publishedEvents;
  }
}
