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

import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataEventRecord;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataEventRecordRepository;
import org.siglus.siglusapi.localmachine.server.OnlineWebService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"!localmachine"})
public class OnlineWebTask {

  private final OnlineWebService onlineWebService;

  private final MasterDataEventRecordRepository masterDataEventRecordRepository;

  @Value("${masterdata.changes.count}")
  private Integer masterDataChangesCount;

  @Value("${masterdata.not.update.interval}")
  private Integer masterDataNotUpdateInterval;

  // ensure to execute the task in evening of Moz time, since it will lock tables to generate master data snapshot
  @Scheduled(cron = "${generate.masterdata.cron}", zone = "${time.zoneId}")
  @SchedulerLock(name = "generate_masterdata_zip", lockAtMostFor = "PT1M")
  public void generateMasterDataZip() {
    log.info("start scheduled generate master data zip on online web");
    if (needToGenerate()) {
      onlineWebService.generateMasterData();
    }
    onlineWebService.cleanMasterDataSnapshot();
    onlineWebService.cleanIncrementalMasterData();
  }

  private boolean needToGenerate() {
    MasterDataEventRecord masterDataEventRecord = masterDataEventRecordRepository
        .findTopBySnapshotVersionIsNotNullOrderByIdDesc();
    if (masterDataEventRecord == null) {
      return true;
    }
    return verifyByTime(masterDataEventRecord.getOccurredTime()) || verifyByChangesCount(masterDataEventRecord.getId());
  }

  private boolean verifyByChangesCount(Long id) {
    Integer count = masterDataEventRecordRepository.findChangesCountAfterLatestSnapshotVersion(id);
    return count > masterDataChangesCount;
  }

  private boolean verifyByTime(ZonedDateTime occurredTime) {
    return occurredTime.plusDays(masterDataNotUpdateInterval).isBefore(ZonedDateTime.now());
  }

}
