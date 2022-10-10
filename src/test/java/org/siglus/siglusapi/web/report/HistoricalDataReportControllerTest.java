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

import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.service.scheduledtask.HistoricalDataPersistentService;
import org.siglus.siglusapi.web.request.HistoricalDataRequest;

@RunWith(MockitoJUnitRunner.class)
public class HistoricalDataReportControllerTest {

  @InjectMocks
  HistoricalDataReportController controller;
  @Mock
  HistoricalDataPersistentService historicalDataPersistentService;

  private static final UUID facilityId = UUID.randomUUID();

  private static final LocalDate endDate = LocalDate.of(2022, 12, 31);
  private static final LocalDate startDate = LocalDate.of(2022, 1, 1);

  @Test
  public void shouldCallRefreshWithServiceWhenCallByController() {
    controller.refresh();
    verify(historicalDataPersistentService).refreshHistoricalDataReport();
  }

  @Test
  public void shouldCallUpdateAllWithServiceWhenCallByController() {
    controller.updateAll();
    verify(historicalDataPersistentService).updateAllFacilityHistoricalData();
  }

  @Test
  public void shouldCallUpdateByFacilityWithServiceWhenCallByController() {
    HistoricalDataRequest historicalDataRequest = new HistoricalDataRequest(facilityId, startDate, endDate);
    controller.update(Collections.singletonList(historicalDataRequest));
    verify(historicalDataPersistentService).updateHistoricalDataByFacility(
        Collections.singletonList(historicalDataRequest));
  }
}