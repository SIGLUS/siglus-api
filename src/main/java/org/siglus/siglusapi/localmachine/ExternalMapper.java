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

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalMapper {

  private final PayloadSerializer payloadSerializer;

  public ExternalEventDto map(Event event) {
    return ExternalEventDto.builder()
        .event(event)
        .payloadClassName(payloadSerializer.getPayloadName(event.getPayload()))
        .build();
  }

  @SneakyThrows
  public Event map(ExternalEventDto externalEventDto) {
    Event event = externalEventDto.getEvent();
    Object payload = event.getPayload();
    Class<?> payloadClass = payloadSerializer.getPayloadClass(externalEventDto.getPayloadClassName());
    Object originPayload = PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER.convertValue(payload, payloadClass);
    event.setPayload(originPayload);
    return event;
  }

}
