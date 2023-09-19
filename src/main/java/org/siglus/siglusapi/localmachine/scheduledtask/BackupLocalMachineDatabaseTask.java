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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.domain.BackupDatabaseRecord;
import org.siglus.siglusapi.localmachine.repository.BackupDatabaseRecordRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.S3FileHandler;
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

  private final Machine machine;
  private final BackupDatabaseRecordRepository backupDatabaseRecordRepository;
  private final S3FileHandler s3FileHandler;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Scheduled(cron = "0 */5 * * * ?", zone = "${time.zoneId}")
  @Transactional
  public void backupDatabase() {
    UUID facilityId = machine.getLocalFacilityId();
    FacilityDto facilityDto = facilityReferenceDataService.findOne(facilityId);
    BackupDatabaseRecord backupRecord = backupDatabaseRecordRepository.findTopByFacilityIdOrderByLastBackupTimeDesc(
        facilityId);
    if (backupRecord == null ||
        backupRecord.getLastBackupTime().plusDays(BACKUP_DURATION_DAYS).isBefore(LocalDateTime.now())) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
      String dbDumpFile =
          facilityDto.getCode() + "_" + facilityDto.getName() + "_" + LocalDateTime.now().format(formatter) + ".sql.gz";
      if (dumpDatabaseAndUploadToS3(dbDumpFile)) {
        if (backupRecord == null) {
          backupRecord = new BackupDatabaseRecord();
          backupRecord.setFacilityId(facilityId);
          backupRecord.setFacilityCode(facilityDto.getCode());
          backupRecord.setFacilityName(facilityDto.getName());
        }
        backupRecord.setBackupFile(dbDumpFile);
        backupRecord.setLastBackupTime(LocalDateTime.now());
        backupDatabaseRecordRepository.save(backupRecord);
      }
    }
  }

  private boolean dumpDatabaseAndUploadToS3(String dbDumpFile) {
    try {
      ProcessBuilder dumpProcessBuilder = new ProcessBuilder("bash", "-c",
          "docker exec simam-db-1 /bin/sh -c 'pg_dump -h localhost -U postgres open_lmis "
              + "> /tmp/simam-db.sql && gzip -f /tmp/simam-db.sql' && docker cp simam-db-1:/tmp/simam-db.sql.gz /tmp/"
              + dbDumpFile);
      dumpProcessBuilder.redirectErrorStream(true);
      Process dumpProcess = dumpProcessBuilder.start();
      if (dumpProcess.waitFor() != 0) {
        printStream(dumpProcess.getInputStream());
        return false;
      }
      s3FileHandler.uploadFileToS3("/tmp/" + dbDumpFile, "localmachine-dbdump/" + dbDumpFile);
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
}
