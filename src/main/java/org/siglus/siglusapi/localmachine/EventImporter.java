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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventImporter {
  private final EventQueue localEventQueue;
  private final EventReplayer replayer;

  public EventImporter(EventQueue localEventQueue, EventReplayer replayer) {
    this.localEventQueue = localEventQueue;
    this.replayer = replayer;
  }

  public void importEvents(List<Event> events) {
    // TODO: drop local attributes (replay flag), define a external event dto for external events
    List<Event> acceptedEvents = events.stream().filter(this::accept).collect(Collectors.toList());
    List<Event> newAdded = localEventQueue.putAllGetNewAdded(acceptedEvents);
    replay(newAdded);
  }

  protected boolean accept(Event it) {
    // fixme: 2022/8/18 check event should receiver should be local facility
    return true;
  }

  protected void replay(List<Event> events) {
    if (isEmpty(events)) {
      return;
    }
    // replay event one by one in order. if the event is a group event (e.g. event2 below), should
    // check dependency first
    // |-------|
    // |event 1|
    // |event 2|--->[group event M, group event M-1,..., group event 0] (dependent events are ready)
    // |event 3|
    // |event 4|--->[group event M, group event M-1] (dependent events are not ready)
    // |.......|
    // |event N|
    checkAllEventsBelongToOneSender(events);
    Map<String, List<Event>> eventGroups =
        events.stream().collect(groupingBy(Event::getGroupId, LinkedHashMap::new, toList()));
    List<Event> defaultGroup = eventGroups.remove(null);
    replayer.playDefaultGroupEvents(defaultGroup);
    eventGroups.forEach((groupId, value) -> replayer.playGroupEvents(groupId));
  }

  private void checkAllEventsBelongToOneSender(List<Event> events) {
    long senderCount = events.stream().map(Event::getSenderId).distinct().count();
    if (senderCount > 1) {
      throw new IllegalStateException("events are raised by different senders");
    }
  }
}
