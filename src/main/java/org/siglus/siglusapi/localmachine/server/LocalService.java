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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletResponse;
import jersey.repackaged.com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.ExternalEventDto;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.eventstore.EventSerializer;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.service.SiglusFacilityService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalService {

  private final EventStore eventStore;
  private final EventImporter eventImporter;
  private final EventSerializer eventSerializer;
  private final ExternalEventDtoMapper externalEventDtoMapper;
  private final SiglusFacilityService siglusFacilityService;
  private final SiglusAuthenticationHelper authenticationHelper;

  @Value("${machine.event.zip.export.path}")
  private String zipExportPath;

  private static final String ZIP_PREFIX = "event_export_";
  private static final String ZIP_SUFFIX = ".zip";
  private static final String FILE_SUFFIX = ".dat";
  private static final String CONTENT_TYPE = "application/zip";
  private static final String DISPOSITION_BASE = "attachment; filename=";
  private static final String CHECKSUM_SPLIT = "_";

  @SneakyThrows
  public void exportEvent(HttpServletResponse response) {
    // TODO: 2022/9/29 需要考虑阿丹代码删除directory
    String zipName = ZIP_PREFIX + System.currentTimeMillis() + ZIP_SUFFIX;
    File directory = makeDirectory();
    try {
      List<File> files = generateFilesForEachFacility();
      File zipFile = generateZipFile(zipName, files);
      setResponseAttribute(response, zipName);
      FileInputStream fileInputStream = new FileInputStream(zipFile);
      IOUtils.copy(fileInputStream, response.getOutputStream());
      response.flushBuffer();
    } catch (IOException e) {
      log.error("delete directory fail, ", e);
    } finally {
      FileUtils.deleteDirectory(directory);
    }
  }

  private File makeDirectory() {
    File directory = new File(zipExportPath);
    boolean mkdirs = directory.mkdirs();
    if (!mkdirs) {
      log.warn("export zip dir make fail, dir: {}", zipExportPath);
      throw new RuntimeException("make dir failed");
    }
    return directory;
  }

  private List<File> generateFilesForEachFacility() {
    Map<UUID, List<Event>> receiverIdToEvents = eventStore.getEventsForExport().stream()
        .collect(Collectors.groupingBy(Event::getReceiverId));
    Map<UUID, String> facilityIdToCode = siglusFacilityService.getFacilityIdToCode(receiverIdToEvents.keySet());

    List<File> files = Lists.newArrayListWithExpectedSize(receiverIdToEvents.size());
    receiverIdToEvents.forEach((receiverId, events) ->
        // TODO: 2022/9/29 校验 events 数据量, 如果太大则拆分
        files.add(generateFile(zipExportPath + facilityIdToCode.get(receiverId) + FILE_SUFFIX,
            eventSerializer.dump(events.stream()
                .map(externalEventDtoMapper::map)
                .collect(Collectors.toList())))));
    return files;
  }

  private void setResponseAttribute(HttpServletResponse response, String zipName) {
    response.setContentType(CONTENT_TYPE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, DISPOSITION_BASE + zipName);
  }

  private File generateZipFile(String zipName, List<File> files) {
    File zipFile = new File(zipExportPath + zipName);
    try (FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);) {
      byte[] buffer = new byte[8 * 1024];
      for (File srcFile : files) {
        try (FileInputStream fileInputStream = new FileInputStream(srcFile);) {
          zipOutputStream.putNextEntry(new ZipEntry(srcFile.getName()));
          int length;
          while ((length = fileInputStream.read(buffer)) > 0) {
            zipOutputStream.write(buffer, 0, length);
          }
          zipOutputStream.closeEntry();
          if (srcFile.exists()) {
            Files.delete(srcFile.toPath());
          }
        }
      }
    } catch (Exception e) {
      log.error("generate zip file fail", e);
      throw new RuntimeException(" generate zip file fail");
    }
    return zipFile;
  }

  @SneakyThrows
  private File generateFile(String fileFullName, byte[] data) {
    File file = new File(fileFullName);
    if (file.exists()) {
      file.delete();
    }
    FileOutputStream fos = new FileOutputStream(file);
    byte[] concatBytes = ArrayUtils.addAll(getChecksumPrefix(data).getBytes(), data);
    fos.write(concatBytes, 0, concatBytes.length);
    fos.flush();
    fos.close();
    return file;
  }

  private String getChecksumPrefix(byte[] data) {
    return new StringBuilder(getChecksum(data)).append(CHECKSUM_SPLIT).toString();
  }

  private String getChecksum(byte[] data) {
    return DigestUtils.md5Hex(data);
  }

  @Transactional
  public void importEvent(MultipartFile[] files) {
    // TODO: 2022/9/29 files 大小校验?
    for (MultipartFile file : files) {
      eventImporter.importEvents(getEvents(file).stream()
          .map(externalEventDtoMapper::map)
          .collect(Collectors.toList()));
    }
  }

  @SneakyThrows
  private List<ExternalEventDto> getEvents(MultipartFile file) {
    checkFileSuffix(file);

    String readData = getReadData(file);
    int checksumIndex = readData.indexOf(CHECKSUM_SPLIT);
    String readChecksum = readData.substring(0, checksumIndex);
    byte[] events = readData.substring(checksumIndex + 1).getBytes();

    checkChecksum(readChecksum, events);

    List<ExternalEventDto> externalEventDtos = eventSerializer.loadList(events);
    checkFacility(externalEventDtos);
    return externalEventDtos;
  }

  private void checkChecksum(String readChecksum, byte[] events) {
    String eventsChecksum = getChecksum(events);
    if (!eventsChecksum.equals(readChecksum)) {
      log.error("file may be modified, readChecksum:{}, eventsChecksum: {}", readChecksum, eventsChecksum);
      throw new BusinessDataException(new Message("file may be modified"));
    }
  }

  private void checkFacility(List<ExternalEventDto> externalEventDtos) {
    externalEventDtos.forEach(externalEventDto -> {
      UUID receiverId = externalEventDto.getEvent().getReceiverId();
      UUID homeFacilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
      if (!receiverId.equals(homeFacilityId)) {
        log.error("file shouldn't be imported, facilityId not match, file receiverId:{}, current user facilityId:{}",
            receiverId, homeFacilityId);
        throw new BusinessDataException(new Message("file shouldn't be imported"));
      }
    });
  }

  private String getReadData(MultipartFile file) throws IOException {
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file.getInputStream()));
    StringBuilder stringBuilder = new StringBuilder();
    String lineTxt;
    while ((lineTxt = bufferedReader.readLine()) != null) {
      stringBuilder.append(lineTxt);
    }
    return stringBuilder.toString();
  }

  private void checkFileSuffix(MultipartFile file) {
    String filename = file.getOriginalFilename();
    String suffix = filename.substring(filename.lastIndexOf("."));
    if (!FILE_SUFFIX.equals(suffix)) {
      log.error("file may be modified, file suffix:{}, correct suffix:{}", suffix, FILE_SUFFIX);
      throw new BusinessDataException(new Message("file may be modified"));
    }
  }
}
