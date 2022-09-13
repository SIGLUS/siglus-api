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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ExternalEventDtoMapper.class, PayloadSerializer.class})
public class ExternalEventDtoMapperTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private ExternalEventDtoMapper externalEventDtoMapper;

  @Test
  public void canMapExternalEventDtoToEventGivenDeserializedExternalEventDto() throws IOException {
    // given
    TestPayload testPayload = new TestPayload("test-payload-1");
    Event originEvent = Event.builder()
        .id(UUID.randomUUID())
        .onlineWebSynced(false)
        .payload(testPayload)
        .build();
    ExternalEventDto receivedExternalEventDto =
        objectMapper.readValue(
            objectMapper.writeValueAsString(externalEventDtoMapper.map(originEvent)),
            ExternalEventDto.class);
    // when
    Event event = externalEventDtoMapper.map(receivedExternalEventDto);
    // then
    assertThat(event).isEqualTo(originEvent);
    assertThat(receivedExternalEventDto.getPayloadClassName()).isEqualTo("TestPayload");
  }

  @EventPayload
  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  static class TestPayload {
    private String name;
  }
}
