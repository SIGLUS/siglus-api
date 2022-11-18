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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.service.scheduledtask.CalculateCmmService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Profile("localmachine")
@RequiredArgsConstructor
@Service
public class CalculateLocalMachineCmmTask {

  private final CalculateCmmService calculateCmmService;
  private final Machine machine;

  @Scheduled(cron = "${localmachine.cmm.calculate.cron}", zone = "${time.zoneId}")
  public void calculate() {
    UUID facilityId = machine.getLocalFacilityId();
    log.info("calculate local machine cmm start, facilityId: {}", facilityId);
    long startTime = System.currentTimeMillis();
    calculateCmmService.calculateOneFacilityCmm(LocalDate.now(), facilityId);
    log.info("calculate local machine cmm end, facilityId: {}, cost: {}ms", facilityId,
        System.currentTimeMillis() - startTime);
  }
}
