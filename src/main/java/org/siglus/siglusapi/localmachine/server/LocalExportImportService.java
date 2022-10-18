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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletResponse;
import jersey.repackaged.com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.EventImporter;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.siglus.siglusapi.localmachine.eventstore.EventStore;
import org.siglus.siglusapi.localmachine.io.ChecksumNotMatchedException;
import org.siglus.siglusapi.localmachine.io.EventFile;
import org.siglus.siglusapi.localmachine.io.EventFileReader;
import org.siglus.siglusapi.service.SiglusFacilityService;
import org.siglus.siglusapi.util.FileUtil;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalExportImportService {

  private final EventStore eventStore;
  private final EventImporter eventImporter;
  private final ExternalEventDtoMapper externalEventDtoMapper;
  private final SiglusFacilityService siglusFacilityService;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final EventFileReader eventFileReader;
  @Value("${machine.event.zip.export.path}")
  private String zipExportPath;

  private static final String ZIP_PREFIX = "event_export_";
  private static final String ZIP_SUFFIX = ".zip";
  private static final String FILE_SUFFIX = ".dat";
  private static final String FILE_NAME_SPLIT = "_";
  private static final String CONTENT_TYPE = "application/zip";
  private static final String DISPOSITION_BASE = "attachment; filename=";

  @SneakyThrows
  public void exportEvents(HttpServletResponse response) {
    String zipName = ZIP_PREFIX + System.currentTimeMillis() + ZIP_SUFFIX;
    File directory = makeDirectory();
    try {
      List<File> files = generateFilesForEachFacility();
      if (CollectionUtils.isEmpty(files)) {
        log.warn("no data need to be exported, homeFacilityId:{}", getHomeFacilityId());
        throw new BusinessDataException(new Message("no data need to be exported"));
      }
      File zipFile = generateZipFile(zipName, files);
      setResponseAttribute(response, zipName);
      FileInputStream fileInputStream = new FileInputStream(zipFile);
      IOUtils.copy(fileInputStream, response.getOutputStream());
      response.flushBuffer();
    } finally {
      FileUtils.deleteDirectory(directory);
    }
  }

  @Transactional
  public void importEvents(MultipartFile[] files) {
    for (MultipartFile file : files) {
      checkFileSuffix(file);
      try{
        List<Event> events = eventFileReader.readAll(file);
        checkFacility(events);
        eventImporter.importEvents(events);
      } catch (ChecksumNotMatchedException e) {
        throw new BusinessDataException(new Message("file may be modified"));
      } catch (IOException e) {
        throw new BusinessDataException(new Message("fail to import events, file name:" + file.getName()));
      }
    }
  }

  private File makeDirectory() {
    File directory = new File(zipExportPath);
    boolean mkdirs = directory.mkdirs();
    if (!mkdirs) {
      log.warn("export zip dir make fail, dir: {}", zipExportPath);
      throw new BusinessDataException(new Message("make dir failed"));
    }
    return directory;
  }

  private List<File> generateFilesForEachFacility() {
    String workingDir = zipExportPath + System.currentTimeMillis() + "/";
    UUID homeFacilityId = getHomeFacilityId();
    Map<UUID, List<Event>> receiverIdToEvents = eventStore.getEventsForExport(homeFacilityId).stream()
        .collect(Collectors.groupingBy(Event::getReceiverId));
    Map<UUID, String> facilityIdToCode = siglusFacilityService.getFacilityIdToCode(
        getFacilityIds(homeFacilityId, receiverIdToEvents));

    return receiverIdToEvents.entrySet().stream()
        .map((it) -> generateFilesForOneReceiver(
            workingDir, homeFacilityId, facilityIdToCode, it.getKey(), it.getValue()))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private List<File> generateFilesForOneReceiver(String workingDir, UUID homeFacilityId, Map<UUID, String> facilityIdToCode,
      UUID receiverId, List<Event> events) {
    List<File> files = new LinkedList<>();
    int currentFilePartSeq = 1;
    boolean needMultipleFiles = false;
    String fileName =
        getFileName(
            workingDir,
            facilityIdToCode.get(homeFacilityId),
            facilityIdToCode.get(receiverId),
            "");
    int capacityBytes = 50 * 1024 * 1024;
    EventFile eventFile = new EventFile(capacityBytes, fileName, externalEventDtoMapper);
    for (int i = 0; i < events.size(); i++) {
      try {
        boolean canAddMore = eventFile.write(events.get(i));
        if (canAddMore) {
          continue;
        }
        // finalize current file
        int remainingCount = events.size() - (i + 1);
        if (remainingCount > 0) {
          needMultipleFiles = true;
        }
        if (needMultipleFiles) {
          String currentFilePartSuffix = "_part" + currentFilePartSeq;
          String newFileName =
              getFileName(
                  workingDir,
                  facilityIdToCode.get(homeFacilityId),
                  facilityIdToCode.get(receiverId),
                  currentFilePartSuffix);
          eventFile.renameTo(newFileName);
        }
        files.add(eventFile.getFile());
        eventFile.close();
        // prepare for new event file
        eventFile = new EventFile(capacityBytes, fileName, externalEventDtoMapper);
        currentFilePartSeq += 1;
      } catch (IOException e) {
        log.error("error when generate files, facility:{}, err:{}", homeFacilityId, e);
        throw new BusinessDataException(new Message("fail to generate files"));
      }
    }
    return files;
  }

  private Set<UUID> getFacilityIds(UUID homeFacilityId, Map<UUID, List<Event>> receiverIdToEvents) {
    Set<UUID> facilityIds = Sets.newHashSet(receiverIdToEvents.keySet());
    facilityIds.add(homeFacilityId);
    return facilityIds;
  }

  private String getFileName(String workingDir, String fromFacilityCode, String toFacilityCode,
      String filePartSuffix) {
    return workingDir
        + "from"
        + FILE_NAME_SPLIT
        + fromFacilityCode
        + FILE_NAME_SPLIT
        + "to"
        + FILE_NAME_SPLIT
        + toFacilityCode
        + filePartSuffix
        + FILE_SUFFIX;
  }

  private void setResponseAttribute(HttpServletResponse response, String zipName) {
    response.setContentType(CONTENT_TYPE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, DISPOSITION_BASE + zipName);
  }

  private File generateZipFile(String zipName, List<File> files) {
    File zipFile = new File(zipExportPath + zipName);
    try (FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
      FileUtil.write(files, zipOutputStream);
    } catch (Exception e) {
      log.error("generate zip file fail", e);
      throw new BusinessDataException(e, new Message("generate zip file fail"));
    }
    return zipFile;
  }

  private void checkFacility(List<Event> events) {
    UUID homeFacilityId = getHomeFacilityId();
    events.forEach(it -> {
      UUID receiverId = it.getReceiverId();
      if (!receiverId.equals(homeFacilityId)) {
        log.error("file shouldn't be imported, facilityId not match, file receiverId:{}, current user facilityId:{}",
            receiverId, homeFacilityId);
        throw new BusinessDataException(new Message("file shouldn't be imported"));
      }
    });
  }

  private UUID getHomeFacilityId() {
    return authenticationHelper.getCurrentUser().getHomeFacilityId();
  }

  private void checkFileSuffix(MultipartFile file) {
    String filename = file.getOriginalFilename();
    String suffix = filename.substring(filename.lastIndexOf('.'));
    if (!FILE_SUFFIX.equals(suffix)) {
      log.error("file may be modified, file suffix:{}, correct suffix:{}", suffix, FILE_SUFFIX);
      throw new BusinessDataException(new Message("file may be modified"));
    }
  }
}
