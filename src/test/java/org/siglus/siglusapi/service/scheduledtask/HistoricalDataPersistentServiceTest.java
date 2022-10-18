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

package org.siglus.siglusapi.service.scheduledtask;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.repository.HistoricalDataRepository;
import org.siglus.siglusapi.repository.dto.FacilityLastRequisitionTimeDto;
import org.siglus.siglusapi.web.request.HistoricalDataRequest;

@RunWith(MockitoJUnitRunner.class)
public class HistoricalDataPersistentServiceTest {

  @InjectMocks
  HistoricalDataPersistentService service;

  @Mock
  HistoricalDataRepository historicalDataRepository;

  private static final UUID facilityId = UUID.randomUUID();

  private static final LocalDate startDate = LocalDate.of(2022, 01, 01);
  private static final LocalDate endDate = LocalDate.of(2022, 12, 31);


  @Test
  public void refreshHistoricalDataReport() {
    service.refreshHistoricalDataReport();
    verify(historicalDataRepository).count();
  }

  @Test
  public void shouleUpdateAllFacilityHistoricalData() {
    //given
    HashMap<UUID, LocalDate> requestMap = new HashMap<>();
    requestMap.put(facilityId, endDate);
    FacilityLastRequisitionTimeDto facilityLastRequisitionTimeDto =
        new FacilityLastRequisitionTimeDto(facilityId, startDate);
    //when
    when(historicalDataRepository.getFacilityLatestRequisitionDate())
        .thenReturn(Collections.singletonList(facilityLastRequisitionTimeDto));
    service.updateAllFacilityHistoricalData();
    //then
    verify(historicalDataRepository).updateFacilityHistoricalData(facilityId, startDate, LocalDate.of(2099, 12, 31));
  }

  @Test
  public void shouleUpdateFacilityHistoricalDataByFacility() {
    //given
    HistoricalDataRequest historicalDataRequest = new HistoricalDataRequest(facilityId, startDate, endDate);
    FacilityLastRequisitionTimeDto facilityLastRequisitionTimeDto =
        new FacilityLastRequisitionTimeDto(facilityId, null);
    //when
    when(historicalDataRepository.getFacilityLatestRequisitionDate(Sets.newHashSet(facilityId)))
        .thenReturn(Collections.singletonList(facilityLastRequisitionTimeDto));
    service.updateHistoricalDataByFacility(Collections.singletonList(historicalDataRequest));
    //then
    verify(historicalDataRepository).updateFacilityHistoricalData(facilityId,
        startDate, endDate);
  }

  @Test
  public void shouleUpdateFacilityHistoricalDataByFacilityWithoutEndDate() {
    //given
    HistoricalDataRequest historicalDataRequest = new HistoricalDataRequest(facilityId, startDate, null);
    FacilityLastRequisitionTimeDto facilityLastRequisitionTimeDto =
        new FacilityLastRequisitionTimeDto(facilityId, startDate);
    //when
    when(historicalDataRepository.getFacilityLatestRequisitionDate(Sets.newHashSet(facilityId)))
        .thenReturn(Collections.singletonList(facilityLastRequisitionTimeDto));
    service.updateHistoricalDataByFacility(Collections.singletonList(historicalDataRequest));
    //then
    verify(historicalDataRepository).updateFacilityHistoricalData(facilityId,
        startDate, LocalDate.of(2099, 12, 31));
  }

  @Test
  public void shouleUpdateFacilityHistoricalDataByFacilityWithOutStartDate() {
    //given
    HistoricalDataRequest historicalDataRequest = new HistoricalDataRequest(facilityId, null, endDate);
    //when
    service.updateHistoricalDataByFacility(Collections.singletonList(historicalDataRequest));
    //then
    verify(historicalDataRepository).updateFacilityHistoricalData(facilityId,
        LocalDate.of(1970, 1, 1), endDate);
  }
}