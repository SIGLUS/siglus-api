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

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.siglus.siglusapi.service.scheduledtask.CalculateCmmService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Profile("localmachine")
@RequiredArgsConstructor
@Service
public class CalculateLocalMachineCmmTask {

  private final CalculateCmmService calculateCmmService;
  private final SiglusAuthenticationHelper authHelper;

  @Scheduled(cron = "${cmm.calculate.cron}", zone = "${time.zoneId}")
  @SchedulerLock(name = "calculate_cmm_task")
  public void calculate() {
    calculateCmmService.calculateLocalMachineCmms(LocalDate.now(), authHelper.getCurrentUser().getHomeFacilityId());
  }
}
