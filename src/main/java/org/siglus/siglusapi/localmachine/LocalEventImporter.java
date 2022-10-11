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

import java.util.Optional;
import java.util.UUID;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.repository.ReplayErrorRecordsRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile({"localmachine"})
@Component
public class LocalEventImporter extends EventImporter {

  public LocalEventImporter(EventStore localEventStore, EventReplayer replayer, Machine machine,
      ReplayErrorRecordsRepository replayErrorRecordsRepository) {
    super(localEventStore, replayer, machine, replayErrorRecordsRepository);
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
