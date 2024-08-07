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

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.repository.SiglusReportAccessRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("!localmachine")
@Service
@Slf4j
public class ClearExpiredReportAccessRecordTask {

  private final SiglusReportAccessRecordRepository siglusReportAccessRecordRepository;

  public ClearExpiredReportAccessRecordTask(SiglusReportAccessRecordRepository siglusReportAccessRecordRepository) {
    this.siglusReportAccessRecordRepository = siglusReportAccessRecordRepository;
  }

  @Scheduled(cron = "${clear.expired.report.access.record.cron}", zone = "${time.zoneId}")
  @Transactional
  public void clear() {
    log.info("clear expired report access record start");
    siglusReportAccessRecordRepository.clearExpiredRecords(LocalDate.now().minusYears(1));
    log.info("clear expired report access record end");
  }
}
