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

package org.siglus.siglusapi.scheduledtask;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.siglus.siglusapi.localmachine.eventstore.backup.EventPayloadBackupRepository;
import org.siglus.siglusapi.repository.StockCardRequestBackupRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class ClearBackupDataTask {

  private final EventPayloadBackupRepository eventPayloadBackupRepository;
  private final StockCardRequestBackupRepository stockCardRequestBackupRepository;


  @Scheduled(cron = "${clear.backup.data.cron}", zone = "${time.zoneId}")
  @SchedulerLock(name = "clear_backup_data_task")
  @Transactional
  public void clear() {
    log.info("clear EventPayloadBackup data 1 month before");
    eventPayloadBackupRepository.clearEventPayloadBackupOneMonthBefore();

    log.info("clear StockCardRequestBackup data 1 month before");
    stockCardRequestBackupRepository.clearStockCardRequestBackupOneMonthBefore();

  }
}
