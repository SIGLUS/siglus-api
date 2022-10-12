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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.agent.ErrorHandleService;
import org.siglus.siglusapi.localmachine.constant.ErrorType;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
public abstract class EventImporter {

  protected final EventStore eventStore;
  protected final EventReplayer replayer;
  protected final Machine machine;
  protected final ErrorHandleService errorHandleService;

  protected EventImporter(EventStore eventStore, EventReplayer replayer, Machine machine,
      ErrorHandleService errorHandleService) {
    this.eventStore = eventStore;
    this.replayer = replayer;
    this.machine = machine;
    this.errorHandleService = errorHandleService;
  }

  public void importEvents(List<Event> events) {
    List<Event> acceptedEvents = events.stream().filter(this::accept).collect(Collectors.toList());
    resetStatus(acceptedEvents);
    List<Event> nonExistentEvents = eventStore.excludeExisted(acceptedEvents);
    List<Event> newAdded = importGetNewAdded(nonExistentEvents);
    replayer.replay(newAdded);
  }

  protected abstract boolean accept(Event it);

  protected void resetStatus(List<Event> acceptedEvents) {
    // The local replayed flag is private, don't trust external ones, so reset it here.
    acceptedEvents.forEach(it -> it.setLocalReplayed(false));
    Set<String> supportedFacilityIds = machine.fetchSupportedFacilityIds();
    acceptedEvents.forEach(
        it -> {
          String facilityId =
              Optional.ofNullable(it.getReceiverId()).map(UUID::toString).orElse("");
          if (supportedFacilityIds.contains(facilityId)) {
            it.confirmedReceiverSynced();
          }
        });
  }

  private List<Event> importGetNewAdded(List<Event> acceptedEvents) {
    List<Event> newAdded = new LinkedList<>();
    acceptedEvents.forEach(
        it -> {
          try {
            eventStore.importQuietly(it);
            newAdded.add(it);
          } catch (DataIntegrityViolationException e) {
            checkIdViolationError(it.getId(), e);
            // id violation means the event exists, to make sure sender get ack, here to emit ack again.
            eventStore.emitAckForEvent(it);
          }
        });
    return newAdded;
  }

  static void checkIdViolationError(UUID eventId, DataIntegrityViolationException e) {
    boolean idExists = e.getMessage().contains("Key (id)");
    if (idExists) {
      log.info("event exists, skip it, eventid:{}", eventId);
    } else {
      throw e;
    }
  }
}
