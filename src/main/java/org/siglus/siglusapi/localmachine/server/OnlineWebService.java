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
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.FileOperationException;
import org.siglus.siglusapi.exception.UnableGetLockException;
import org.siglus.siglusapi.localmachine.ShedLockFactory;
import org.siglus.siglusapi.localmachine.ShedLockFactory.AutoClosableLock;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataEventRecord;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataEventRecordRepository;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataOffset;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataOffsetRepository;
import org.siglus.siglusapi.localmachine.repository.MasterDataSql;
import org.siglus.siglusapi.localmachine.repository.MovementSql;
import org.siglus.siglusapi.localmachine.repository.RequisitionOrderSql;
import org.siglus.siglusapi.localmachine.repository.TableCopyRepository;
import org.siglus.siglusapi.localmachine.webapi.ResyncMasterDataResponse;
import org.siglus.siglusapi.util.FileUtil;
import org.siglus.siglusapi.util.S3FileHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineWebService {

  private final TableCopyRepository tableCopyRepository;
  private final MasterDataEventRecordRepository masterDataEventRecordRepository;
  private final MasterDataOffsetRepository masterDataOffsetRepository;
  private final S3FileHandler s3FileHandler;
  private final ShedLockFactory lockFactory;
  private final Map<String, String> tableNameToMasterSql = MasterDataSql.getMasterDataSqlMap();
  private final Map<String, String> tableNameToMovementSql = MovementSql.getMovementSql();
  private final Map<String, String> tableNameToRequisitionOrderSql = RequisitionOrderSql.getRequisitionOrderSql();
  private static final String ZIP_SUFFIX = ".zip";
  private static final String CONTENT_TYPE = "application/zip";
  private static final String DISPOSITION_BASE = "attachment; filename=";
  private static final String MASTER_DATA = "masterData";
  private static final String DEFAULT_REPLAY_GROUP_LOCK = "lock.replay.group.default.";
  private final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
  @Value("${resync.zip.export.path}")
  private String zipExportPath;

  public ResyncMasterDataResponse resyncMasterData(UUID facilityId) {
    MasterDataEventRecord eventRecord =
        masterDataEventRecordRepository.findTopBySnapshotVersionIsNotNullOrderByIdDesc();
    if (eventRecord == null) {
      eventRecord = generateMasterData();
    }
    MasterDataOffset masterDataOffset = masterDataOffsetRepository.findByFacilityIdIs(facilityId);
    if (masterDataOffset == null) {
      masterDataOffset = new MasterDataOffset();
      masterDataOffset.setId(UUID.randomUUID());
      masterDataOffset.setFacilityId(facilityId);
    }
    masterDataOffset.setSnapshotVersion(eventRecord.getSnapshotVersion());
    masterDataOffset.setRecordOffset(eventRecord.getId());
    log.info("save facility resync master data offset: {}", masterDataOffset);
    masterDataOffsetRepository.save(masterDataOffset);
    String s3Url = s3FileHandler.getUrlFromS3(eventRecord.getSnapshotVersion());
    Long latestId = masterDataEventRecordRepository.findLatestRecordId();
    String maxFlywaySersion = masterDataOffsetRepository.findMaxFlywayVersion();
    latestId = latestId == null || latestId < eventRecord.getId() ? eventRecord.getId() : latestId;
    return new ResyncMasterDataResponse(
        eventRecord.getId(), latestId, s3Url, eventRecord.getSnapshotVersion(), maxFlywaySersion);
  }

  public void resyncData(UUID homeFacilityId, HttpServletResponse response) {
    try (AutoClosableLock waitLock = lockFactory.waitLock(DEFAULT_REPLAY_GROUP_LOCK + homeFacilityId, 180000)) {
      if (!waitLock.isPresent()) {
        throw new UnableGetLockException(new Message("facility resync unable to get group lock," + homeFacilityId));
      }
      waitLock.ifPresent(() -> generateBusinessDataToResponse(homeFacilityId, response, null));
    } catch (InterruptedException e) {
      log.error("facility: {} resync group lock interrupt: {}", homeFacilityId, e);
      Thread.currentThread().interrupt();
    }
  }

  public MasterDataEventRecord generateMasterData() {
    String zipName = generateMasterDataToS3();
    MasterDataEventRecord saveRecord = new MasterDataEventRecord();
    saveRecord.setSnapshotVersion(zipName);
    saveRecord.setOccurredTime(ZonedDateTime.now());
    log.info("save master data snapshot version: {}", zipName);
    return masterDataEventRecordRepository.save(saveRecord);
  }

  public void generateBusinessDataToResponse(UUID homeFacilityId, HttpServletResponse response, String type) {
    String facilityDir = homeFacilityId + "_" + format.format(new Date());
    String zipDirectory = zipExportPath + facilityDir + "/";
    String zipName = facilityDir + ZIP_SUFFIX;
    File directory = new File(zipDirectory);
    boolean mkdirs = directory.mkdirs();
    if (!mkdirs) {
      log.warn("facility {} resync zip dir make fail", homeFacilityId);
    }
    File zipFile = generateZipFile(zipDirectory, zipName, homeFacilityId, type);
    response.setContentType(CONTENT_TYPE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, DISPOSITION_BASE + zipName);
    try (FileInputStream fileInputStream = new FileInputStream(zipFile)) {
      IOUtils.copy(fileInputStream, response.getOutputStream());
      response.flushBuffer();
      FileUtils.deleteDirectory(directory);
    } catch (IOException e) {
      log.error("facilityId {} delete directory fail,{}", homeFacilityId, e);
      throw new FileOperationException(e, new Message(homeFacilityId + " flush zip file to response fail"));
    }
  }

  private String generateMasterDataToS3() {
    String masterDataDir = MASTER_DATA + "_" + format.format(new Date());
    String zipDirectory = zipExportPath + masterDataDir + "/";
    String zipName = masterDataDir + ZIP_SUFFIX;
    File directory = new File(zipDirectory);
    boolean mkdirs = directory.mkdirs();
    if (!mkdirs) {
      log.warn("master data zip dir make fail");
    }
    generateZipFile(zipDirectory, zipName, null, MASTER_DATA);
    s3FileHandler.uploadFileToS3(zipDirectory + zipName, zipName);
    try {
      FileUtils.deleteDirectory(directory);
    } catch (IOException e) {
      log.error("Online web delete master data directory fail", e);
      throw new FileOperationException(e, new Message("Online web delete master data directory fail"));
    }
    return zipName;
  }

  private File generateZipFile(String zipDirectory, String zipName, UUID homeFacilityId, String type) {
    File zipFile = new File(zipDirectory + zipName);
    List<File> tableFiles;
    if (MASTER_DATA.equals(type)) {
      tableFiles = tableCopyRepository.copyMasterDateToFile(zipDirectory, tableNameToMasterSql, null);
    } else {
      tableFiles = Stream
          .of(tableCopyRepository.copyDateToFile(zipDirectory, tableNameToMovementSql, homeFacilityId),
              tableCopyRepository.copyDateToFile(zipDirectory, tableNameToRequisitionOrderSql, homeFacilityId))
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    }
    try (FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
      FileUtil.write(tableFiles, zipOutputStream);
    } catch (Exception e) {
      log.error("facilityId {} generate zip file fail,{}", homeFacilityId, e);
      throw new FileOperationException(e, new Message(homeFacilityId + " generate zip file fail"));
    }
    return zipFile;
  }

}
