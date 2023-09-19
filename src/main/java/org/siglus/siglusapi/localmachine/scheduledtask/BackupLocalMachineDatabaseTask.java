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

package org.siglus.siglusapi.localmachine.scheduledtask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.domain.BackupDatabaseRecord;
import org.siglus.siglusapi.localmachine.domain.ErrorRecord;
import org.siglus.siglusapi.localmachine.repository.BackupDatabaseRecordRepository;
import org.siglus.siglusapi.localmachine.repository.ErrorRecordRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.S3FileHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Profile("localmachine")
@RequiredArgsConstructor
@Service
public class BackupLocalMachineDatabaseTask {

  private static final int BACKUP_DURATION_DAYS = 1;
  private static final String UNDERSCORE = "_";
  private static final String FOLDER = "localmachine-dbdump/";

  private final Machine machine;
  private final BackupDatabaseRecordRepository backupDatabaseRecordRepository;
  private final S3FileHandler s3FileHandler;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final ErrorRecordRepository errorRecordsRepository;

  @Value("${machine.db.docker.container}")
  private String dbDockerContainer;

  @Scheduled(cron = "${localmachine.backup.database.cron}", zone = "${time.zoneId}")
  @Transactional
  public void backupDatabase() {
    if (!isInternetAvailable()) {
      log.info("Internet not available");
      return;
    }
    UUID facilityId = machine.getLocalFacilityId();
    FacilityDto facilityDto = facilityReferenceDataService.findOne(facilityId);
    BackupDatabaseRecord backupRecord = getOrCreateBackupRecord(facilityId, facilityDto);
    ErrorRecord lastErrorRecord = errorRecordsRepository.findLastErrorRecord();
    if (lastErrorRecord == null) {
      log.info("No error record");
      removeErrorFromBackupRecord(backupRecord);
      return;
    }
    if (shouldBackupDatabase(backupRecord)) {
      String dbDumpFile = generateDbDumpFileName(facilityDto);
      if (dumpDatabaseAndUploadToS3(dbDumpFile)) {
        updateBackupRecordWithNewBackup(backupRecord, dbDumpFile, lastErrorRecord);
      }
    }
  }

  private BackupDatabaseRecord getOrCreateBackupRecord(UUID facilityId, FacilityDto facilityDto) {
    BackupDatabaseRecord backupRecord = backupDatabaseRecordRepository.findTopByFacilityId(facilityId);
    if (backupRecord == null) {
      backupRecord = new BackupDatabaseRecord();
      backupRecord.setFacilityId(facilityId);
      backupRecord.setFacilityCode(facilityDto.getCode());
      backupRecord.setFacilityName(facilityDto.getName());
    }
    return backupRecord;
  }

  private void removeErrorFromBackupRecord(BackupDatabaseRecord backupRecord) {
    backupRecord.setHasError(false);
    backupRecord.setErrorMessage(null);
    log.info("remove error flag for {}", backupRecord.getFacilityName());
    backupDatabaseRecordRepository.save(backupRecord);
  }

  private boolean shouldBackupDatabase(BackupDatabaseRecord backupRecord) {
    return backupRecord == null
        || backupRecord.getLastBackupTime().plusDays(BACKUP_DURATION_DAYS).isBefore(LocalDateTime.now());
  }

  private String generateDbDumpFileName(FacilityDto facilityDto) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    return facilityDto.getCode() + UNDERSCORE
        + facilityDto.getName().replaceAll("\\s+", UNDERSCORE) + UNDERSCORE
        + LocalDateTime.now().format(formatter) + ".sql.gz";
  }

  private void updateBackupRecordWithNewBackup(BackupDatabaseRecord backupRecord, String dbDumpFile,
      ErrorRecord lastErrorRecord) {
    if (backupRecord.getBackupFile() != null) {
      log.info("delete backup file on S3: {}", backupRecord.getBackupFile());
      s3FileHandler.deleteFileFromS3(FOLDER + backupRecord.getBackupFile());
    }
    backupRecord.setBackupFile(dbDumpFile);
    backupRecord.setLastBackupTime(LocalDateTime.now());
    backupRecord.setHasError(true);
    backupRecord.setErrorMessage(lastErrorRecord.toString());
    log.info("save BackupDatabaseRecord: {}", backupRecord);
    backupDatabaseRecordRepository.save(backupRecord);
  }

  private boolean dumpDatabaseAndUploadToS3(String dbDumpFile) {
    try {
      log.info("start dumping {}", dbDumpFile);
      ProcessBuilder dumpProcessBuilder = new ProcessBuilder("bash", "-c",
          "docker exec " + dbDockerContainer + " /bin/sh -c 'pg_dump -h localhost -U postgres open_lmis "
              + "> /tmp/simam-db.sql && gzip -f /tmp/simam-db.sql' && docker cp " + dbDockerContainer
              + ":/tmp/simam-db.sql.gz /tmp/"
              + dbDumpFile);
      dumpProcessBuilder.redirectErrorStream(true);
      Process dumpProcess = dumpProcessBuilder.start();
      if (dumpProcess.waitFor() != 0) {
        printStream(dumpProcess.getInputStream());
        return false;
      }
      log.info("upload {} to S3", dbDumpFile);
      s3FileHandler.uploadFileToS3("/tmp/" + dbDumpFile, FOLDER + dbDumpFile);
      return true;
    } catch (Exception e) {
      log.error(e.getMessage());
      return false;
    }
  }

  private void printStream(InputStream inputStream) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    String line;
    while ((line = reader.readLine()) != null) {
      log.error(line);
    }
  }

  private boolean isInternetAvailable() {
    try {
      InetAddress address = InetAddress.getByName("simam.cmam.gov.mz");
      return address.isReachable(2000);
    } catch (Exception e) {
      log.error(e.getMessage());
      return false;
    }
  }
}
