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

import static org.siglus.siglusapi.constant.FieldConstants.ATTACHMENT_FILENAME;
import static org.siglus.siglusapi.constant.FieldConstants.EXCEL_CONTENT_TYPE;
import static org.siglus.siglusapi.constant.FieldConstants.TRACER_DRUG_INFORMATION;
import static org.siglus.siglusapi.constant.FieldConstants.UTF_8;
import static org.siglus.siglusapi.constant.FieldConstants.XLSX_SUFFIX;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.TracerDrugExportDto;
import org.siglus.siglusapi.interceptor.OperationGuardAspect.Guarded;
import org.siglus.siglusapi.service.scheduledtask.TracerDrugReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/siglusapi/report/tracerDrug")
public class TracerDrugReportController {

  private final TracerDrugReportService tracerDrugReportService;

  @Value("${dateTimeUrlFormat}")
  private String dateUrlFormat;

  @PostMapping("/refresh")
  @Guarded
  public void refresh(@RequestParam String startDate, @RequestParam String endDate) {
    tracerDrugReportService.refreshTracerDrugPersistentData(startDate, endDate);
  }

  @PostMapping("/initialize")
  @Guarded
  public void initialize() {
    tracerDrugReportService.initializeTracerDrugPersistentData();
  }

  @PostMapping("/refreshByFacility")
  @Guarded
  public void refreshByFacility(@RequestBody List<String> facilityCodes,
      @RequestParam String startDate,
      @RequestParam String endDate) {
    tracerDrugReportService.refreshTracerDrugPersistentDataByFacility(facilityCodes, startDate, endDate);
  }

  @GetMapping("/exportFilter")
  public TracerDrugExportDto getTracerDrugExportDto() {
    return tracerDrugReportService.getTracerDrugExportDto();
  }

  @GetMapping("/excel")
  public void getTracerDrugExcel(HttpServletResponse response,
      String productCode,
      String districtCode,
      String provinceCode,
      String startDate,
      String endDate) throws IOException {
    response.setContentType(EXCEL_CONTENT_TYPE);
    response.setCharacterEncoding(UTF_8);
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateUrlFormat);
    String fileName = URLEncoder.encode(
        TRACER_DRUG_INFORMATION + simpleDateFormat.format(System.currentTimeMillis()), UTF_8);
    response.setHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + fileName + XLSX_SUFFIX);
    tracerDrugReportService.getTracerDrugExcel(response, productCode, districtCode, provinceCode, startDate, endDate);
  }
}
