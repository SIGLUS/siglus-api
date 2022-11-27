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
import static org.mockito.Mockito.when;

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
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class TracerDrugReportControllerTest extends TestCase {

  @InjectMocks
  private TracerDrugReportController tracerDrugReportController;

  @Mock
  private TracerDrugReportService tracerDrugReportService;

  private final String startDate = "2022-01-01";
  private final String endDate = "2023-01-01";

  @Test
  public void shouldCallServiceWhenGetTracerDrugExportDto() {
    // given
    when(tracerDrugReportService.getTracerDrugExportDto()).thenReturn(null);
    // when
    tracerDrugReportController.getTracerDrugExportDto();
    // then
    verify(tracerDrugReportService).getTracerDrugExportDto();
  }

  @Test
  public void shouldCallServiceWhenOnlyRefreshBetweenSpecifyDate() {
    // given
    doNothing().when(tracerDrugReportService).refreshTracerDrugPersistentData(startDate, endDate);
    // when
    tracerDrugReportController.refresh(startDate, endDate);
    // then
    verify(tracerDrugReportService).refreshTracerDrugPersistentData(startDate, endDate);
  }

  @Test
  public void shouldCallServiceWhenInitialize() {
    // given
    doNothing().when(tracerDrugReportService).initializeTracerDrugPersistentData();
    // when
    tracerDrugReportController.initialize();
    // then
    verify(tracerDrugReportService).initializeTracerDrugPersistentData();
  }

  @Test
  public void shouldCallServiceWhenGetTracerDrugExcel() {
    // given
    String facilityCode = "HF001";
    doNothing()
        .when(tracerDrugReportService)
        .refreshTracerDrugPersistentDataByFacility(Collections.singletonList(facilityCode), startDate, endDate);
    // when
    tracerDrugReportController.refreshByFacility(Collections.singletonList(facilityCode), startDate, endDate);
    // then
    verify(tracerDrugReportService)
        .refreshTracerDrugPersistentDataByFacility(Collections.singletonList(facilityCode), startDate, endDate);
  }

  @Test
  public void shouldCallServiceWhenRefreshBetweenSpecifyDateAndByFacility() throws IOException {
    // given
    ReflectionTestUtils.setField(
        tracerDrugReportController, "dateUrlFormat", "yyyy_MM_dd_HH_mm_ss.SSS");
    HttpServletResponse response = mock(HttpServletResponse.class);
    String productCode = "22A05";
    String districtCode = "0102";
    String provinceCode = "01";
    doNothing()
        .when(tracerDrugReportService)
        .getTracerDrugExcel(response, productCode, districtCode, provinceCode, startDate, endDate);
    // when
    tracerDrugReportController.getTracerDrugExcel(response, productCode, districtCode, provinceCode, startDate,
        endDate);
    // then
    verify(tracerDrugReportService)
        .getTracerDrugExcel(response, productCode, districtCode, provinceCode, startDate, endDate);
  }

}