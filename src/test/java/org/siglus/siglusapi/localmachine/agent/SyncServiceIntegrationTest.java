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

package org.siglus.siglusapi.localmachine.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.siglus.siglusapi.localmachine.io.EventFileReader;
import org.siglus.siglusapi.service.SiglusCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@ActiveProfiles("localmachine")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
        ExternalEventDtoMapper.class,
        PayloadSerializer.class,
        EventFileReader.class,
        SyncService.class
    })
@SuppressWarnings("PMD.UnusedPrivateField")
public class SyncServiceIntegrationTest {

  @MockBean
  private EventStore localEventStore;
  @MockBean
  private OnlineWebClient webClient;
  @MockBean
  private EventImporter eventImporter;
  @MockBean
  private ErrorHandler errorHandler;
  @MockBean
  private SyncRecordService syncRecordService;
  @MockBean
  private Machine machine;
  @MockBean
  private SiglusCacheService siglusCacheService;
  private List<Event> rawEvents;

  @Autowired
  private ExternalEventDtoMapper mapper;
  @Autowired
  private SyncService syncService;
  private EventFileReader eventFileReader;

  @Before
  public void setup() {
    eventFileReader = new EventFileReader(mapper);
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
  }

  @Test
  public void canPushEventResourceAsMultiParts() throws IOException {
    // given
    SyncService.PUSH_CAPACITY_BYTES_PER_REQUEST = 1011;
    given(localEventStore.getEventsForOnlineWeb()).willReturn(rawEvents);
    // when
    syncService.push();
    // then
    List<Event> pushedEvents = capturePushedEvents();
    assertThat(pushedEvents).containsExactly(rawEvents.toArray(new Event[0]));
  }

  private List<Event> capturePushedEvents() throws IOException {
    ArgumentCaptor<ByteArrayResource> captor = ArgumentCaptor.forClass(ByteArrayResource.class);
    verify(webClient, VerificationModeFactory.atLeastOnce()).sync(captor.capture());
    List<ByteArrayResource> resources = captor.getAllValues();
    List<Event> pushedEvents = new LinkedList<>();
    for (ByteArrayResource resource : resources) {
      List<Event> events = eventFileReader.readAll(resource.getInputStream());
      pushedEvents.addAll(events);
    }
    return pushedEvents;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class TestedPayload {

    private String content;
  }
}
