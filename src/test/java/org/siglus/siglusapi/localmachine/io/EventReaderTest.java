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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;

public class EventReaderTest {
  private final ExternalEventDtoMapper mapper = new ExternalEventDtoMapper(new PayloadSerializer());

  @Test
  public void shouldReadEventSucceedGivenCompressedData() throws IOException {
    // given
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    EventWriter writer = new EventWriter(new EntryWriter(new DeflaterOutputStream(out)), mapper);
    List<Event> rawEvents =
        IntStream.range(0, 10)
            .mapToObj(
                it ->
                    Event.builder()
                        .id(UUID.randomUUID())
                        .senderId(UUID.randomUUID())
                        .receiverId(UUID.randomUUID())
                        .payload(new TestedPayload("this is the content of seq#" + it))
                        .build())
            .collect(Collectors.toList());
    for (Event it : rawEvents) {
      writer.write(it);
      writer.flush();
    }
    writer.close();
    EventReader reader =
        new EventReader(
            new EntryReader(new InflaterInputStream(new ByteArrayInputStream(out.toByteArray()))),
            mapper);
    for (Event rawEvent : rawEvents) {
      // when
      Event event = reader.read();
      // then
      assertThat(event).isEqualTo(rawEvent);
    }
    reader.close();
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class TestedPayload {
    private String content;
  }
}
