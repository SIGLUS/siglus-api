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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Collections;
import javax.servlet.http.HttpServletResponse;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.service.scheduledtask.TracerDrugReportService;
import org.siglus.siglusapi.web.report.request.ExportTracerDrugReportExcelRequest;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class TracerDrugReportExportControllerTest extends TestCase {

  @InjectMocks
  private TracerDrugReportExportController tracerDrugReportController;

  @Mock
  private TracerDrugReportService tracerDrugReportService;

  private final String startDate = "2022-01-01";
  private final String endDate = "2023-01-01";

  @Test
  public void shouldCallServiceWhenRefreshBetweenSpecifyDateAndByFacility() throws IOException {
    // given
    ReflectionTestUtils.setField(
        tracerDrugReportController, "dateUrlFormat", "yyyy_MM_dd_HH_mm_ss.SSS");
    ExportTracerDrugReportExcelRequest request = new ExportTracerDrugReportExcelRequest();
    request.setProductCode("22A05");
    String districtCode = "0102";
    request.setDistrictList(Collections.singletonList(districtCode));
    request.setEndDate(endDate);
    request.setStartDate(startDate);
    HttpServletResponse response = mock(HttpServletResponse.class);
    doNothing()
        .when(tracerDrugReportService)
        .getTracerDrugExcel(response, request.getProductCode(), request.getDistrictList(), startDate, endDate);
    // when
    tracerDrugReportController.getTracerDrugExcel(response, request);
    // then
    verify(tracerDrugReportService)
        .getTracerDrugExcel(response, request.getProductCode(), request.getDistrictList(), startDate, endDate);
  }

}