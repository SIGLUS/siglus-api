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

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.siglus.siglusapi.localmachine.agent.ErrorHandler;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Profile({"localmachine"})
@Component
@Slf4j
public class LocalEventImporter extends EventImporter {

  public LocalEventImporter(EventStore localEventStore, EventReplayer replayer, Machine machine,
      ErrorHandler errorHandler) {
    super(localEventStore, replayer, machine, errorHandler);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void importMasterData(List<Event> events) {
    if (CollectionUtils.isEmpty(events)) {
      return;
    }
    final long offset = this.eventStore.getCurrentMasterDataOffset();
    List<Event> masterDataToSync = new LinkedList<>();
    long newOffset = offset;
    for (Event it: events) {
      if (it.getLocalSequenceNumber() <= offset) {
        continue;
      }
      it.setSyncedTime(ZonedDateTime.now());
      masterDataToSync.add(it);
      newOffset = Math.max(it.getLocalSequenceNumber(), newOffset);
    }
    try {
      replayer.playNonGroupEvents(masterDataToSync);
      log.info("update local master data offset from {} to {}", offset, newOffset);
      eventStore.updateLocalMasterDataOffset(newOffset);
    } catch (InterruptedException | TimeoutException e) {
      log.warn("fail to replay master data due to retryable reason, err:{}", e.getMessage());
    }
  }

  @Override
  protected boolean accept(Event it) {
    return isTheReceiverAndEventNotBeConfirmedYet(it);
  }

  private boolean isTheReceiverAndEventNotBeConfirmedYet(Event it) {
    return supportedFacility(it.getReceiverId()) && !it.isReceiverSynced();
  }

  boolean supportedFacility(UUID receiverFacilityId) {
    return Optional.ofNullable(receiverFacilityId)
        .map(it -> machine.fetchSupportedFacilityIds().contains(it.toString()))
        .orElse(false);
  }
}
