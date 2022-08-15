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

import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;

public class ShadowFacility {
  private String id;
  private EventQueue self;
  private Map<String, EventQueue> facilityIdToPeeringEventQueue;

  public void sendEvent(Event event) {
    event.setSenderId(id);
    // TODO: 2022/8/13 ensure event_seq is greater than the maxium of peerings's
    // CREATE SEQUENCE event_seq;
    // SELECT setval('event_seq', greatest(nextval('event_seq'), 99), false);
    // fixme
    // get max seq + 1 as the seq of event
    // save the event to this facility owned queue
    throw new NotImplementedException("todo");
  }
}
