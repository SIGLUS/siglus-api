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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EventReplayer {
  private final EventQueue eventQueue;

  @Transactional
  public void play(List<Event> events) {
    // replay event one by one in order. if the event is a group event (e.g. event2 below), should
    // check dependency first
    // |-------|
    // |event 1|
    // |event 2|--->[group event M, group event M-1,..., group event 0] (dependent events are ready)
    // |event 3|
    // |event 4|--->[group event M, group event M-1] (dependent events are not ready)
    // |.......|
    // |event N|

  }
}
