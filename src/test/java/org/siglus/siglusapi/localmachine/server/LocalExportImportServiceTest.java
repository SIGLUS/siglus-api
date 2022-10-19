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
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.assertj.core.util.Arrays;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.eventstore.ExternalEventDtoSerializer;
import org.siglus.siglusapi.localmachine.io.ChecksumNotMatchedException;
import org.siglus.siglusapi.localmachine.io.EventFileReader;
import org.siglus.siglusapi.service.SiglusFacilityService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"checkstyle:LineLength", "PMD.UnusedPrivateField"})
public class LocalExportImportServiceTest {

  private final UUID receiverId1 = UUID.randomUUID();
  private final UUID receiverId2 = UUID.randomUUID();
  private final UUID homeFacilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  @InjectMocks private LocalExportImportService localExportImportService;
  @Mock private EventStore eventStore;
  @Mock private EventImporter eventImporter;
  @Mock private EventFileReader eventFileReader;
  @Mock private ExternalEventDtoSerializer externalEventDtoSerializer;
  @Mock private ExternalEventDtoMapper externalEventDtoMapper;
  @Mock private SiglusFacilityService siglusFacilityService;
  @Mock private SiglusAuthenticationHelper authenticationHelper;

  @Test
  public void shouldReturnDirItselfWhenMkDirGivenDirCreatedSuccessfully() {
    // given
    File directory = mock(File.class);
    given(directory.isDirectory()).willReturn(false);
    given(directory.mkdirs()).willReturn(true);
    // when
    File fi = localExportImportService.makeDirectory(directory);
    // then
    assertThat(fi).isEqualTo(directory);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenFailToMakeDirectory() {
    // given
    File directory = mock(File.class);
    given(directory.isDirectory()).willReturn(false);
    given(directory.mkdirs()).willReturn(false);
    // when
    localExportImportService.makeDirectory(directory);
  }

  @Test
  public void shouldGenerateZipWhenExport() throws IOException {
    // given
    setExportPath();
    when(eventStore.getEventsForExport(homeFacilityId)).thenReturn(buildMockEvents());
    when(siglusFacilityService.getFacilityIdToName(
            Sets.newHashSet(receiverId1, receiverId2, homeFacilityId)))
        .thenReturn(buildMockFacilityIdToName());
    when(authenticationHelper.getCurrentUser()).thenReturn(buildMockUserDto());
    when(externalEventDtoSerializer.dump(anyList())).thenReturn(buildMockData());

    // when
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    localExportImportService.exportEvents(httpServletResponse);

    // then
    verify(siglusFacilityService).getFacilityIdToName(anySet());
    verify(eventStore).getEventsForExport(homeFacilityId);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenImportGivenExportedEventEmpty() throws IOException {
    // given
    setExportPath();
    when(eventStore.getEventsForExport(homeFacilityId)).thenReturn(Lists.newArrayList());
    when(siglusFacilityService.getFacilityIdToName(
            Sets.newHashSet(receiverId1, receiverId2, homeFacilityId)))
        .thenReturn(buildMockFacilityIdToName());
    when(authenticationHelper.getCurrentUser()).thenReturn(buildMockUserDto());

    // when
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    localExportImportService.exportEvents(httpServletResponse);
  }

  @Test
  public void shouldSuccessWhenImport() {
    // given
    when(authenticationHelper.getCurrentUser()).thenReturn(buildMockUserDto());
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getOriginalFilename()).thenReturn("test.dat");
    MultipartFile[] multipartFiles = Arrays.array(multipartFile);

    // when
    localExportImportService.importEvents(multipartFiles);

    // then
    verify(eventImporter).importEvents(anyList());
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenImportGivenWrongFileSuffix() {
    // given
    when(authenticationHelper.getCurrentUser()).thenReturn(buildMockUserDto());

    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
    MultipartFile[] multipartFiles = Arrays.array(multipartFile);

    // when
    localExportImportService.importEvents(multipartFiles);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenImportGivenChecksumNotMatchedThrown() throws Exception {
    // given
    given(eventFileReader.readAll(any(MultipartFile.class)))
        .willThrow(new ChecksumNotMatchedException("invalid checksum"));
    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getOriginalFilename()).thenReturn("test.dat");
    MultipartFile[] multipartFiles = Arrays.array(multipartFile);

    // when
    localExportImportService.importEvents(multipartFiles);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowWhenImportGivenWrongReceiver() throws Exception {
    // given
    when(authenticationHelper.getCurrentUser()).thenReturn(buildMockUserDto());
    given(eventFileReader.readAll(any(MultipartFile.class)))
        .willReturn(buildEventWithWrongReciecerId());

    MultipartFile multipartFile = mock(MultipartFile.class);
    when(multipartFile.getOriginalFilename()).thenReturn("test.dat");
    MultipartFile[] multipartFiles = Arrays.array(multipartFile);

    // when
    localExportImportService.importEvents(multipartFiles);
  }

  private byte[] buildMockData() {
    return "data".getBytes();
  }

  private UserDto buildMockUserDto() {
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    userDto.setHomeFacilityId(homeFacilityId);
    return userDto;
  }

  private List<Event> buildMockEvents() {
    Event event1 = Event.builder().receiverId(receiverId1).build();
    Event event2 = Event.builder().receiverId(receiverId1).build();
    return Lists.newArrayList(event1, event2);
  }

  private List<Event> buildEventWithWrongReciecerId() {
    Event dto1 = Event.builder().receiverId(receiverId1).build();
    Event dto2 = Event.builder().receiverId(homeFacilityId).build();
    return Lists.newArrayList(dto1, dto2);
  }

  private Map<UUID, String> buildMockFacilityIdToName() {
    Map<UUID, String> facilityIdToName = Maps.newHashMap();
    facilityIdToName.put(receiverId1, "receiverCode1");
    facilityIdToName.put(receiverId2, "receiverCode2");
    facilityIdToName.put(homeFacilityId, "homeFacilityCode");
    return facilityIdToName;
  }

  private void setExportPath() throws IOException {
    File tempDir = Files.createTempDirectory("event-export").toFile();
    ReflectionTestUtils.setField(
        localExportImportService, "zipExportPath", tempDir.getAbsolutePath());
  }
}
