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

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventSerializer {
  private final PayloadSerializer payloadSerializer;

  public Event dump(Event event) {
    event.setPayload(payloadSerializer.dump(event.getPayload()));
    return event;
  }

  public Event load(Event it) {
    if (String.class.equals(it.getPayload().getClass())) {
      Object payload =
          payloadSerializer.load(
              Base64.decode(((String) it.getPayload()).getBytes(StandardCharsets.UTF_8)));
      it.setPayload(payload);
    }
    return it;
  }
}
