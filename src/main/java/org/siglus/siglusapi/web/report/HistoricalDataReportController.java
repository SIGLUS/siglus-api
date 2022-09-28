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

package org.siglus.siglusapi.web.report;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.siglus.siglusapi.interceptor.OperationGuardAspect.Guarded;
import org.siglus.siglusapi.service.task.report.HistoricalDataPersistentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/siglusapi/report/historicalData")
public class HistoricalDataReportController {

  private final HistoricalDataPersistentService historicalDataPersistentService;

  @PostMapping("/refresh")
  @Guarded
  public ResponseEntity<String> refresh() {
    historicalDataPersistentService.refreshHistoricalDataReport();
    return ResponseEntity.ok("refresh begin");
  }

  @PostMapping("/updateAll")
  @Guarded
  public ResponseEntity<String> updateAll() {
    historicalDataPersistentService.updateAllFacilityHistoricalData();
    return ResponseEntity.ok("refresh begin");
  }

  @PostMapping("/updateByFacility")
  @Guarded
  public ResponseEntity<String> update(@RequestBody Map<UUID, LocalDate> request) {
    historicalDataPersistentService.updateHistoricalDataByFacility(request);
    return ResponseEntity.ok("refresh begin");
  }
}
