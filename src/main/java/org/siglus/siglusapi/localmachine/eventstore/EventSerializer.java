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

package org.siglus.siglusapi.localmachine.eventstore;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.ExternalEventDto;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventSerializer {

  public static final ObjectMapper LOCALMACHINE_EVENT_OBJECT_MAPPER;

  static {
    LOCALMACHINE_EVENT_OBJECT_MAPPER = new ObjectMapper();
    LOCALMACHINE_EVENT_OBJECT_MAPPER.registerModule(new JavaTimeModule());
    LOCALMACHINE_EVENT_OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    LOCALMACHINE_EVENT_OBJECT_MAPPER.configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public EventSerializer() {
  }

  @SneakyThrows
  public byte[] dump(Object events) {
    byte[] eventsBytes = LOCALMACHINE_EVENT_OBJECT_MAPPER.writeValueAsBytes(events);
    EventWrapper eventWrapper = new EventWrapper(getEventsName(events), eventsBytes);
    return LOCALMACHINE_EVENT_OBJECT_MAPPER.writeValueAsBytes(eventWrapper);
  }

  @SneakyThrows
  public List<ExternalEventDto> loadList(byte[] events) {
    EventWrapper eventWrapper = LOCALMACHINE_EVENT_OBJECT_MAPPER.readValue(events, EventWrapper.class);
    return LOCALMACHINE_EVENT_OBJECT_MAPPER.readValue(
        eventWrapper.getEvents(),
        LOCALMACHINE_EVENT_OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, ExternalEventDto.class));
  }

  public String getEventsName(Object events) {
    return events.getClass().getName();
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class EventWrapper {

    private String name;
    private byte[] events;
  }
}
