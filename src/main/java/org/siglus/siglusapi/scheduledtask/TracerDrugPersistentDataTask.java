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
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.siglus.siglusapi.service.scheduledtask.TracerDrugReportService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Profile("!localmachine")
@RequiredArgsConstructor
@Service
public class TracerDrugPersistentDataTask {

  private final TracerDrugReportService tracerDrugReportService;
  private static final int STOCK_MOVEMENT_MODIFY_LIMITED_MONTH = 1;
  private static final int TRACER_DRUG_AUTO_REFRESH_PERIOD = 7;

  @Scheduled(cron = "${report.tracer.drug.cron}", zone = "${time.zoneId}")
  @SchedulerLock(name = "tracer_drug_report")
  public void refreshForTracerDrugReport() {
    tracerDrugReportService.refreshTracerDrugPersistentData(
        LocalDate.now()
            .minusMonths(STOCK_MOVEMENT_MODIFY_LIMITED_MONTH)
            .minusDays(TRACER_DRUG_AUTO_REFRESH_PERIOD).toString(),
        LocalDate.now().toString());
  }
}
