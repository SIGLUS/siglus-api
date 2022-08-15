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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalMachine {
  private final ApplicationEventPublisher eventPublisher;

  public boolean isWebServer() {
    // FIXME: 2022/8/14
    return false;
  }

  public List<String> getKnownFacilityIds() {
    // FIXME: 2022/8/14 owner id + peering facility ids
    return null;
  }

  public Map<UUID, Long> getWatermarks(Set<UUID> facilityIds) {
    // FIXME: 2022/8/14
    return null;
  }

  public void sendOutgoingEvent(Event event) {
    eventPublisher.publishEvent(new OutgoingEvent(event));
  }

  public void publishEvent(Event event) {
    eventPublisher.publishEvent(event);
    // FIXME: 2022/8/14 update watermark of sender post event published
  }
}
