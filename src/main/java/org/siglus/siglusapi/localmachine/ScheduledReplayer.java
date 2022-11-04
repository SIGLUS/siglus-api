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

import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledReplayer {
  private final EventStore eventStore;
  private final EventReplayer replayer;

  @Scheduled(fixedRate = 30 * 1000, initialDelay = 30 * 1000)
  @Transactional
  public void start() {
    log.info("start scheduled replay task");
    Spliterator<Event> spliterator = eventStore.streamNotReplayedEvents().sequential().spliterator();
    int batchSize = 24;
    List<Event> currentBatch = new LinkedList<>();
    boolean hasNext;
    // split and replay events in batches
    do {
      hasNext = spliterator.tryAdvance(currentBatch::add);
      if (currentBatch.size() >= batchSize) {
        // flush and reset
        replayer.replay(currentBatch);
        currentBatch.clear();
      }
    } while (hasNext);
    // flush remaining
    replayer.replay(currentBatch);
  }
}
