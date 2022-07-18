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

package org.siglus.siglusapi.service.task.report;

import java.time.LocalDate;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.repository.TracerDrugRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TracerDrugTaskService {

  private final TracerDrugRepository tracerDrugRepository;

  @Value("${tracer.drug.initialize.date}")
  private String tracerDrugInitializeDate;

  @Transactional
  public void refreshTracerDrugPersistentData(String startDate, String endDate) {
    log.info("tracer drug persistentData refresh. start = " + System.currentTimeMillis());
    try {
      tracerDrugRepository.insertDataWithinSpecifiedTime(startDate, endDate);
    } catch (Exception e) {
      log.error("tracer drug persistentData  refresh with exception. msg = " + e.getMessage(), e);
      throw e;
    }
    log.info("tracer drug persistentData  refresh. end = " + System.currentTimeMillis());
  }


  @Transactional
  public void initializeTracerDrugPersistentData() {
    log.info("tracer drug persistentData initialize. start = " + System.currentTimeMillis());
    refreshTracerDrugPersistentData(tracerDrugInitializeDate, LocalDate.now().toString());
    log.info("tracer drug persistentData initialize. end = " + System.currentTimeMillis());
  }


}
