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

package org.siglus.siglusapi.localmachine.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.siglus.siglusapi.localmachine.io.EventFileReader;
import org.siglus.siglusapi.service.SiglusFacilityService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {
      ExternalEventDtoMapper.class,
      PayloadSerializer.class,
      EventFileReader.class,
      LocalExportImportService.class
    })
public class ExportImportIntegrationTest {
  private final UUID receiverId1 = UUID.randomUUID();
  private final UUID receiverId2 = UUID.randomUUID();
  private final UUID homeFacilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  @MockBean private EventStore eventStore;
  @MockBean private EventImporter eventImporter;
  @MockBean private SiglusFacilityService siglusFacilityService;
  @MockBean private SiglusAuthenticationHelper authenticationHelper;
  @Autowired private LocalExportImportService exportImportService;

  @Before
  public void setup() throws IOException {
    setExportPath();
    buildMockFacilityIdToName();
  }

  @Test
  public void canImportEventsFromExportedZipFile() throws IOException {
    // given
    LocalExportImportService.EVENT_FILE_CAPACITY_BYTES = 777;
    MockHttpServletResponse response = new MockHttpServletResponse();
    List<Event> events = buildMockEvents(receiverId1, "content", 11);
    when(eventStore.getEventsForExport(homeFacilityId)).thenReturn(events);
    when(authenticationHelper.getCurrentUser()).thenReturn(buildMockUserDto(homeFacilityId));
    exportImportService.exportEvents(response);
    File dest = File.createTempFile("event-export", ".tmp");
    IOUtils.copy(
        new ByteArrayInputStream(response.getContentAsByteArray()), new FileOutputStream(dest));
    // when
    final List<String> fileNames = new LinkedList<>();
    final List<Event> capturedEvents = doImportAndCaptureEvents(dest, fileNames);
    // then
    assertThat(fileNames)
        .containsExactly(
            "from_HF S01_to_HF R1_part1.dat",
            "from_HF S01_to_HF R1_part2.dat",
            "from_HF S01_to_HF R1_part3.dat",
            "from_HF S01_to_HF R1_part4.dat",
            "from_HF S01_to_HF R1_part5.dat",
            "from_HF S01_to_HF R1_part6.dat");
    assertThat(capturedEvents).containsExactly(events.toArray(new Event[0]));
  }

  private List<Event> doImportAndCaptureEvents(File dest, List<String> fileNames)
      throws IOException {
    when(authenticationHelper.getCurrentUser()).thenReturn(buildMockUserDto(receiverId1));
    ZipFile zipFile = new ZipFile(dest);
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      fileNames.add(entry.getName());
      exportImportService.importEvents(
          new MultipartFile[] {
            new MockMultipartFile(
                entry.getName(), entry.getName(), null, zipFile.getInputStream(entry))
          });
    }
    ArgumentCaptor<? extends List> eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventImporter, VerificationModeFactory.atLeastOnce())
        .importEvents(eventCaptor.capture());
    return getCapturedEvents(eventCaptor);
  }

  @SuppressWarnings("unchecked")
  private List<Event> getCapturedEvents(ArgumentCaptor<? extends List> eventCaptor) {
    final List<Event> capturedEvents = new LinkedList<>();
    eventCaptor.getAllValues().stream()
        .flatMap(it -> it.stream())
        .forEach(
            it -> {
              Event ret = (Event) it;
              capturedEvents.add(ret);
            });
    return capturedEvents;
  }

  private List<Event> buildMockEvents(UUID receiverId, String content, int count) {
    List<Event> events = new LinkedList<>();
    for (int i = 0; i < count; i++) {
      Event event =
          Event.builder()
              .senderId(homeFacilityId)
              .receiverId(receiverId)
              .payload(new TestedPayload(content + " seq#" + i + " for " + receiverId))
              .build();
      events.add(event);
    }
    return events;
  }

  private UserDto buildMockUserDto(UUID homeFacilityId) {
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    userDto.setHomeFacilityId(homeFacilityId);
    return userDto;
  }

  private Map<UUID, String> buildMockFacilityIdToName() {
    Map<UUID, String> facilityIdToName = Maps.newHashMap();
    facilityIdToName.put(receiverId1, "HF R1");
    facilityIdToName.put(receiverId2, "HF R2");
    facilityIdToName.put(homeFacilityId, "HF S01");
    when(siglusFacilityService.getFacilityIdToName(anySet())).thenReturn(facilityIdToName);
    return facilityIdToName;
  }

  private void setExportPath() throws IOException {
    File tempDir = Files.createTempDirectory("event-export").toFile();
    ReflectionTestUtils.setField(exportImportService, "zipExportPath", tempDir.getAbsolutePath());
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class TestedPayload {
    private String content;
  }
}
