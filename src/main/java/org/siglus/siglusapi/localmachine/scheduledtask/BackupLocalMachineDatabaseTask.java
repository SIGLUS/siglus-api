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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.domain.BackupDatabaseRecord;
import org.siglus.siglusapi.localmachine.domain.ErrorPayload;
import org.siglus.siglusapi.localmachine.domain.ErrorRecord;
import org.siglus.siglusapi.localmachine.repository.BackupDatabaseRecordRepository;
import org.siglus.siglusapi.localmachine.repository.ErrorPayloadRepository;
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

  private static final int BACKUP_DURATION_DAYS = 7;
  private static final String UNDERSCORE = "_";
  private static final String S3_FOLDER = "localmachine-dbdump/";

  private final Machine machine;
  private final BackupDatabaseRecordRepository backupDatabaseRecordRepository;
  private final S3FileHandler s3FileHandler;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final ErrorRecordRepository errorRecordsRepository;
  private final ErrorPayloadRepository errorPayloadRepository;

  @Value("${spring.datasource.username}")
  private String dbUsername;

  @Value("${spring.datasource.password}")
  private String dbPassword;

  @Value("${machine.dbdump.path}")
  private String machineDbdumpPath;

  @Scheduled(cron = "${localmachine.backup.database.cron}", zone = "${time.zoneId}")
  @Transactional
  public void backupDatabase() {
    if (!isInternetAvailable()) {
      log.info("Internet not available");
      return;
    }
    log.info("Internet available, start check");
    UUID facilityId = machine.getLocalFacilityId();
    FacilityDto facilityDto = facilityReferenceDataService.findOne(facilityId);
    BackupDatabaseRecord backupRecord = getOrCreateBackupRecord(facilityId, facilityDto);
    ErrorRecord errorRecord = errorRecordsRepository.findLastErrorRecord();
    if (errorRecord == null) {
      handleNoErrorRecord(backupRecord);
      return;
    }
    handleErrorRecord(backupRecord, facilityDto, errorRecord);
  }

  private void handleNoErrorRecord(BackupDatabaseRecord backupRecord) {
    log.info("Health, no error record");
    deletePreviousBackupFileFromS3(backupRecord);
    updateBackupRecordWithoutError(backupRecord);
  }

  private void handleErrorRecord(BackupDatabaseRecord backupRecord, FacilityDto facilityDto,
      ErrorRecord lastErrorRecord) {
    log.info("Not health, has error record");
    if (shouldBackupDatabase(backupRecord)) {
      log.info("Need to backup database");
      String dbDumpFile = generateDbDumpFileName(facilityDto);
      if (dumpDatabaseAndUploadToS3(dbDumpFile)) {
        deletePreviousBackupFileFromS3(backupRecord);
        backupRecord.setBackupFile(dbDumpFile);
        backupRecord.setBackupTime(LocalDateTime.now());
        updateBackupRecordWithError(backupRecord, lastErrorRecord);
      }
    } else {
      log.info("No need to backup database");
      updateBackupRecordWithError(backupRecord, lastErrorRecord);
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

  private void updateBackupRecordWithoutError(BackupDatabaseRecord backupRecord) {
    backupRecord.setHealth(true);
    backupRecord.setErrorMessage(null);
    backupRecord.setBackupFile(null);
    backupRecord.setBackupTime(null);
    backupRecord.setLastUpdateTime(LocalDateTime.now());
    log.info("save BackupDatabaseRecord without error: {}", backupRecord);
    backupDatabaseRecordRepository.save(backupRecord);
  }

  private void updateBackupRecordWithError(BackupDatabaseRecord backupRecord, ErrorRecord lastErrorRecord) {
    ErrorPayload errorPayload = errorPayloadRepository.findOne(lastErrorRecord.getErrorPayload().getId());
    if (errorPayload != null) {
      backupRecord.setErrorMessage(lastErrorRecord.getType().name() + " error. " + '\'' + errorPayload);
    }
    backupRecord.setHealth(false);
    backupRecord.setLastUpdateTime(LocalDateTime.now());
    log.info("save BackupDatabaseRecord with error: {}", backupRecord);
    backupDatabaseRecordRepository.save(backupRecord);
  }

  private void deletePreviousBackupFileFromS3(BackupDatabaseRecord backupRecord) {
    if (!StringUtils.isEmpty(backupRecord.getBackupFile())) {
      log.info("delete backup file on S3: {}", backupRecord.getBackupFile());
      s3FileHandler.deleteFileFromS3(S3_FOLDER + backupRecord.getBackupFile());
    }
  }

  private boolean shouldBackupDatabase(BackupDatabaseRecord backupRecord) {
    return backupRecord == null
        || backupRecord.getBackupTime() == null
        || LocalDateTime.now().isAfter(backupRecord.getBackupTime().plusDays(BACKUP_DURATION_DAYS));
  }

  private String generateDbDumpFileName(FacilityDto facilityDto) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    return facilityDto.getCode() + UNDERSCORE
        + facilityDto.getName().replaceAll("\\s+", UNDERSCORE) + UNDERSCORE
        + LocalDateTime.now().format(formatter) + ".sql.gz";
  }

  private boolean dumpDatabaseAndUploadToS3(String dbDumpFile) {
    try {
      log.info("dump LM DB file {}", dbDumpFile);
      File localDirectory = new File(machineDbdumpPath);
      log.info("delete local dbdump folder if exists {}", machineDbdumpPath);
      FileUtils.deleteDirectory(localDirectory);
      boolean mkdirs = localDirectory.mkdirs();
      if (!mkdirs) {
        log.info("mkdir failed: {}", machineDbdumpPath);
      }
      String dumpCommand = "pg_dump -h db -U " + dbUsername + " open_lmis | gzip > " + machineDbdumpPath + dbDumpFile;
      ProcessBuilder dumpProcessBuilder = new ProcessBuilder("/bin/bash", "-c", dumpCommand);
      dumpProcessBuilder.environment().put("PGPASSWORD", dbPassword);
      Process dumpProcess = dumpProcessBuilder.start();
      if (dumpProcess.waitFor() != 0) {
        printStream(dumpProcess.getInputStream());
        return false;
      }
      log.info("upload {} to S3", dbDumpFile);
      s3FileHandler.uploadFileToS3(machineDbdumpPath + dbDumpFile, S3_FOLDER + dbDumpFile);
      log.info("delete local dbdump folder {}", machineDbdumpPath);
      FileUtils.deleteDirectory(localDirectory);
      return true;
    } catch (Exception e) {
      log.error("Exception occurred", e);
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
