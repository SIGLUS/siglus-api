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
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.siglus.siglusapi.service.scheduledtask.RequisitionReportTaskService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("!localmachine")
@RequiredArgsConstructor
@Service
public class RequisitionReportTask {
  private final RequisitionReportTaskService requisitionReportTaskService;

  @Scheduled(cron = "${report.requisition.monthly.cron}", zone = "${time.zoneId}")
  @SchedulerLock(name = "requisition_not_submit_monthly_report")
  @Transactional
  public void refresh() {
    requisitionReportTaskService.refresh();
  }
}
