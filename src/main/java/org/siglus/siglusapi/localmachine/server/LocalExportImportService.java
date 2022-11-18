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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_EXPORT_NO_DATA;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_IMPORT_FILE_RECEIVER_NOT_MATCH;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_IMPORT_INVALID_FILE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_IMPORT_INVALID_FILE_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import org.siglus.siglusapi.localmachine.utils.FacilityNameNormalizer;
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

  static int EVENT_FILE_CAPACITY_BYTES = 50 * 1024 * 1024;
  private final EventStore eventStore;
  private final EventImporter eventImporter;
  private final ExternalEventDtoMapper externalEventDtoMapper;
  private final SiglusFacilityService siglusFacilityService;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final EventFileReader eventFileReader;
  @Value("${machine.event.zip.export.path}")
  private String zipExportPath;

  private static final String ZIP_PREFIX = "Exportar_de_";
  private static final String ZIP_SUFFIX = ".zip";
  private static final String FILE_SUFFIX = ".dat";
  private static final String PART_FILE_SUFFIX = "_part";
  private static final String PART_1_FILE_SUFFIX = "_part1" + FILE_SUFFIX;
  private static final String FILE_NAME_SPLIT = "_";
  private static final String CONTENT_TYPE = "application/zip";
  private static final String DISPOSITION_BASE = "attachment; filename=";

  @SneakyThrows
  public void exportEvents(HttpServletResponse response) {
    String zipName = getZipName();
    File directory = prepareDirectory();
    try {
      List<File> files = generateFilesForPeeringFacilities();
      if (CollectionUtils.isEmpty(files)) {
        log.warn("no data need to be exported, homeFacilityId:{}", getHomeFacilityId());
        throw new BusinessDataException(new Message(ERROR_EXPORT_NO_DATA));
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
      try {
        List<Event> events = eventFileReader.readAll(file);
        checkFacility(events);
        eventImporter.importEvents(events);
      } catch (ChecksumNotMatchedException e) {
        log.error("import error, checksum not match", e);
        throw new BusinessDataException(e, new Message(ERROR_IMPORT_INVALID_FILE));
      } catch (IOException e) {
        log.error("err occurs when import files", e);
        throw new BusinessDataException(
            e, new Message("fail to import events, file name:" + file.getName()));
      }
    }
  }

  private String getZipName() {
    UUID homeFacility = getHomeFacilityId();
    return new StringBuilder(ZIP_PREFIX)
        .append(normalizeFacilityName(
            siglusFacilityService.getFacilityIdToName(Sets.newHashSet(homeFacility)).get(homeFacility)))
        .append(FILE_NAME_SPLIT)
        .append(normalizeDateTime(ZonedDateTime.now()))
        .append(ZIP_SUFFIX)
        .toString();
  }

  private String normalizeDateTime(ZonedDateTime zonedDateTime) {
    return zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_hh'h'mm'min'"));
  }

  private File prepareDirectory() {
    File directory = new File(zipExportPath);
    return makeDirectory(directory);
  }

  File makeDirectory(File directory) {
    if (directory.isDirectory()) {
      return directory;
    }
    boolean mkdirs = directory.mkdirs();
    if (!mkdirs) {
      log.warn("export zip dir make fail, dir: {}", zipExportPath);
      throw new BusinessDataException(new Message("make dir failed"));
    }
    return directory;
  }

  @SneakyThrows
  private List<File> generateFilesForPeeringFacilities() {
    String workingDir = zipExportPath + "/" + System.currentTimeMillis() + "/";
    Files.createDirectories(Paths.get(workingDir));
    UUID homeFacilityId = getHomeFacilityId();
    Map<UUID, List<Event>> receiverIdToEvents = eventStore.getEventsForExport(homeFacilityId).stream()
        .collect(Collectors.groupingBy(Event::getReceiverId));
    Map<UUID, String> facilityIdToName = getFacilityIdToName(homeFacilityId, receiverIdToEvents);

    return receiverIdToEvents.entrySet().stream()
        .map((it) -> generateFilesForOneReceiver(
            workingDir, homeFacilityId, facilityIdToName, it.getKey(), it.getValue()))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private Map<UUID, String> getFacilityIdToName(UUID homeFacilityId, Map<UUID, List<Event>> receiverIdToEvents) {
    Map<UUID, String> facilityIdToName = siglusFacilityService.getFacilityIdToName(
        getFacilityIds(homeFacilityId, receiverIdToEvents));
    for (Map.Entry<UUID, String> entry : facilityIdToName.entrySet()) {
      entry.setValue(normalizeFacilityName(entry.getValue()));
    }
    return facilityIdToName;
  }

  private String normalizeFacilityName(String facilityName) {
    return FacilityNameNormalizer.normalize(facilityName);
  }

  @SneakyThrows
  private List<File> generateFilesForOneReceiver(String workingDir, UUID homeFacilityId,
      Map<UUID, String> facilityIdToName, UUID receiverId, List<Event> events) {
    List<EventFile> files = new LinkedList<>();
    int capacityBytes = EVENT_FILE_CAPACITY_BYTES;
    String firstFileName = getFileName(workingDir, facilityIdToName.get(homeFacilityId),
        facilityIdToName.get(receiverId), "");
    EventFile eventFile = new EventFile(capacityBytes, firstFileName, externalEventDtoMapper);
    for (int i = 0; i < events.size(); i++) {
      try {
        int remaining = eventFile.writeEventAndGetRemainingCapacity(events.get(i));
        if (remaining > 0) {
          continue;
        }
        // finalize current file, which is full now
        files.add(eventFile);
        // prepare next file
        boolean needContinue = events.size() > (i + 1);
        if (needContinue) {
          String nextFileSuffix = PART_FILE_SUFFIX + (files.size() + 1);
          String newFileName = getFileName(workingDir, facilityIdToName.get(homeFacilityId),
              facilityIdToName.get(receiverId), nextFileSuffix);
          eventFile = new EventFile(capacityBytes, newFileName, externalEventDtoMapper);
        }
      } catch (IOException e) {
        log.error("error when generate files, facility:{}, err:{}", homeFacilityId, e);
        throw new BusinessDataException(e, new Message("fail to generate files"));
      }
    }
    if (eventFile.getCount() > 0) {
      files.add(eventFile);
    }
    boolean hasMultiParts = files.size() > 1;
    if (hasMultiParts) {
      // rename the first file to xxx_part1.dat
      files.get(0).renameTo(firstFileName.replace(FILE_SUFFIX, PART_1_FILE_SUFFIX));
    }
    return files.stream().map(EventFile::getFile).collect(Collectors.toList());
  }

  private Set<UUID> getFacilityIds(UUID homeFacilityId, Map<UUID, List<Event>> receiverIdToEvents) {
    Set<UUID> facilityIds = Sets.newHashSet(receiverIdToEvents.keySet());
    facilityIds.add(homeFacilityId);
    return facilityIds;
  }

  private static String getFileName(String workingDir, String senderName, String receiverName, String filePartSuffix) {
    return workingDir
        + "Exportar_de"
        + FILE_NAME_SPLIT
        + senderName
        + FILE_NAME_SPLIT
        + "para"
        + FILE_NAME_SPLIT
        + receiverName
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
        log.error("import error, file shouldn't be imported, file receiver not match current facility, "
            + "file receiverId:{}, current user facilityId:{}", receiverId, homeFacilityId);
        throw new BusinessDataException(new Message(ERROR_IMPORT_FILE_RECEIVER_NOT_MATCH));
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
      log.error("import error, invalid file type, file suffix:{}, correct suffix:{}", suffix, FILE_SUFFIX);
      throw new BusinessDataException(new Message(ERROR_IMPORT_INVALID_FILE_TYPE));
    }
  }
}
