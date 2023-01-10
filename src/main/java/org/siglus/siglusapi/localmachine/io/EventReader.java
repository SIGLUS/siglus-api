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

package org.siglus.siglusapi.localmachine.io;

import static org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.ExternalEventDto;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;

public class EventReader implements AutoCloseable {

  private final EntryReader entryReader;
  private final ExternalEventDtoMapper mapper;
  private static final ObjectMapper objectMapper = LOCALMACHINE_EVENT_OBJECT_MAPPER;

  public EventReader(EntryReader entryReader, ExternalEventDtoMapper mapper) {
    this.entryReader = entryReader;
    this.mapper = mapper;
  }

  public Event read() throws IOException {
    Entry entry = entryReader.read();
    ExternalEventDto externalEventDto =
        objectMapper.readValue(entry.getData(), ExternalEventDto.class);
    return mapper.map(externalEventDto);
  }

  @Override
  public void close() throws IOException {
    this.entryReader.close();
  }
}
