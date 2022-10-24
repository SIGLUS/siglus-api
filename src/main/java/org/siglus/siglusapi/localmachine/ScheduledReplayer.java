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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledReplayer {
  private final EventStore eventStore;
  private final EventReplayer replayer;

  @Scheduled(fixedRate = 30 * 1000, initialDelay = 30 * 1000)
  public void start() {
    // TODO: 2022/10/21 limit size to avoid OOM
    List<Event> events = eventStore.findNotReplayedEvents();
    List<UUID> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
    log.info("start to replay events:{}", eventIds);
    replayer.replay(events);
  }
}
