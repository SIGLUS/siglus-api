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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.siglus.siglusapi.service.scheduledtask.CalculateCmmService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Profile("!localmachine")
@RequiredArgsConstructor
@Service
public class CalculateWebCmmTask {

  private final CalculateCmmService calculateCmmService;

  @Scheduled(cron = "${web.cmm.calculate.cron}", zone = "${time.zoneId}")
  @SchedulerLock(name = "web_calculate_cmm_task")
  public void calculate() {
    log.info("calculate web cmm start");
    long startTime = System.currentTimeMillis();
    calculateCmmService.calculateAllWebCmm(LocalDate.now());
    log.info("calculate web cmm end, cost: {}ms", System.currentTimeMillis() - startTime);
  }
}
