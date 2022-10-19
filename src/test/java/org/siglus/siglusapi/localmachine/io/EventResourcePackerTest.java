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

import java.io.IOException;
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

public class EventResourcePackerTest {

  private List<Event> rawEvents;
  private ExternalEventDtoMapper mapper;

  @Before
  public void setup() {
    rawEvents =
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
    mapper = new ExternalEventDtoMapper(new PayloadSerializer());
  }

  @Test
  public void canGenerateEventResourceAfterReset() throws IOException {
    // given
    EventResourcePacker packer = new EventResourcePacker(1, mapper);
    // then
    for (Event it : rawEvents) {
      packer.writeGetRemainingCapacity(it);
      packer.reset();
    }
  }

  @Test
  public void canSucceedWhenWriteGivenNotIsFull() throws IOException {
    // given capacity is 1 byte
    EventResourcePacker packer = new EventResourcePacker(1, mapper);
    // when
    int remaining = packer.writeGetRemainingCapacity(rawEvents.get(0));
    // then
    assertThat(remaining).isLessThan(0);
  }

  @Test(expected = OutOfCapacityException.class)
  public void shouldThrowWhenWriteGivenPackerIsFull() throws IOException {
    // given capacity is 1 byte
    EventResourcePacker packer = new EventResourcePacker(1, mapper);
    packer.writeGetRemainingCapacity(rawEvents.get(0));
    // when
    int remaining = packer.writeGetRemainingCapacity(rawEvents.get(1));
    // then
    assertThat(remaining).isLessThan(0);
  }
}
