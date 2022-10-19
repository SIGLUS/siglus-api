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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.siglus.siglusapi.localmachine.io.EventReaderTest.TestedPayload;
import org.springframework.core.io.ByteArrayResource;

public class EventFileReaderIntegrationTest {
  private List<Event> rawEvents;
  private ExternalEventDtoMapper mapper;
  private EventFileReader eventReader;

  @Before
  public void setup() {
    rawEvents =
        IntStream.range(0, 100)
            .mapToObj(
                it ->
                    Event.builder()
                        .id(UUID.randomUUID())
                        .senderId(UUID.randomUUID())
                        .receiverId(UUID.randomUUID())
                        .payload(new TestedPayload("this is the content of seq#" + it))
                        .build())
            .collect(Collectors.toList());
    mapper = new ExternalEventDtoMapper(new PayloadSerializer());
    eventReader = new EventFileReader(mapper);
  }

  @Test
  public void canReadAllEventsFromCapacityLimitedEventFile() throws IOException {
    // given
    EventFile eventFile =
        new EventFile(777, "/tmp/" + System.currentTimeMillis() + ".dat", mapper);
    List<File> files = new LinkedList<>();
    int currentFileIndex = 1;
    for (Event it : rawEvents) {
      int remaining = eventFile.writeGetRemainingCapacity(it);
      if (remaining > 0) {
        continue;
      }
      if (eventFile.getCount() > 0) {
        files.add(eventFile.getFile());
      }
      eventFile.close();
      String newPath = "/tmp/" + System.currentTimeMillis() + currentFileIndex + ".dat";
      eventFile = new EventFile(1, newPath, mapper);
      currentFileIndex += 1;
    }
    if (eventFile.getCount() > 0) {
      files.add(eventFile.getFile());
    }
    // when
    List<Event> events = new LinkedList<>();
    for (File file : files) {
      events.addAll(eventReader.readAll(new FileInputStream(file)));
    }
    // then
    assertThat(events).containsExactly(rawEvents.toArray(new Event[] {}));
  }

  @Test
  public void canReadAllEventsFromCapacityLimitedEventResource() throws IOException {
    // given
    EventResourcePacker packer = new EventResourcePacker(1023, mapper);
    List<ByteArrayResource> resources = new LinkedList<>();
    for (Event it : rawEvents) {
      int remaining = packer.writeGetRemainingCapacity(it);
      if (remaining > 0) {
        continue;
      }
      resources.add(packer.toResource());
      packer.reset();
    }
    resources.add(packer.toResource());
    // when
    List<Event> events = new LinkedList<>();
    for (ByteArrayResource resource : resources) {
      events.addAll(eventReader.readAll(resource.getInputStream()));
    }
    // then
    assertThat(events).containsExactly(rawEvents.toArray(new Event[] {}));
  }
}
