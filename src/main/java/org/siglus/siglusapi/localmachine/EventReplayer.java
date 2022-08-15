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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EventReplayer {
  private final LocalMachine localMachine;

  @Transactional
  public Map<UUID, Long> play(Map<UUID, List<Event>> facilityIdToEvents) {
    handleEvents(facilityIdToEvents);
    return localMachine.getWatermarks(facilityIdToEvents.keySet());
  }

  private void handleEvents(Map<UUID, List<Event>> facilityIdToEvents) {
    // fixme: confirm order is asc by sequence number
    // fixme: concurrency conflict, use distributed lock to replay events from replay queue. e.g. select for update?
    Map<UUID, Long> watermarks = localMachine.getWatermarks(facilityIdToEvents.keySet());
    facilityIdToEvents.values().stream()
        .flatMap(Collection::stream)
        .sorted(Comparator.comparing(Event::getSequenceNumber))
        .forEach(
            it -> {
              boolean isAboveWatermark =
                  it.getSequenceNumber() > watermarks.getOrDefault(it.getSenderId(), 0L);
              if (isAboveWatermark) {
                localMachine.publishEvent(it);
              }
            });
  }
}
