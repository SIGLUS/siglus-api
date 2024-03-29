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

import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.interceptor.OperationGuardAspect.Guarded;
import org.siglus.siglusapi.service.scheduledtask.RequisitionReportTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/siglusapi/task/requisition")
public class RequisitionReportTaskController {

  private final RequisitionReportTaskService requisitionReportTaskService;

  @PostMapping("/refresh")
  @Guarded
  public ResponseEntity<String> refresh() {
    requisitionReportTaskService.refresh();
    return ResponseEntity.ok("ok");
  }
}
