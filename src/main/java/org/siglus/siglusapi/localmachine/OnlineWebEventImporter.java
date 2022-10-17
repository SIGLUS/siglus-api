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
import org.siglus.siglusapi.localmachine.agent.ErrorHandler;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile({"!localmachine"})
@Component
public class OnlineWebEventImporter extends EventImporter {

  public OnlineWebEventImporter(EventStore localEventStore, EventReplayer replayer, Machine machine,
      ErrorHandler errorHandler) {
    super(localEventStore, replayer, machine, errorHandler);
  }

  @Override
  protected boolean accept(Event it) {
    return !it.isOnlineWebSynced();
  }

  @Override
  protected void resetStatus(List<Event> acceptedEvents) {
    super.resetStatus(acceptedEvents);
    acceptedEvents.forEach(it -> it.setOnlineWebSynced(true));
  }
}
