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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.repository.HistoricalDataRepository;
import org.siglus.siglusapi.repository.dto.FacilityLastRequisitionTimeDto;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Slf4j
@RequiredArgsConstructor
@Service
public class HistoricalDataPersistentService {

  private final HistoricalDataRepository historicalDataRepository;

  @Transactional
  @Async
  public void refreshHistoricalDataReport() {
    log.info("historical data persistentData refresh. start at {} ", LocalDateTime.now());
    historicalDataRepository.deleteAll();
    updateHistoricalData(new HashMap<>());
    long count = historicalDataRepository.count();
    log.info("historical data persistentData refresh. end at {}, data quanrity is {}", LocalDateTime.now(), count);
  }

  @Transactional
  @Async
  public void updateAllFacilityHistoricalData() {
    updateHistoricalData(null);
  }

  @Transactional
  @Async
  public void updateHistoricalDataByFacility(Map<UUID, LocalDate> facilityIdEndDateMap) {
    updateHistoricalData(facilityIdEndDateMap);
  }

  public void updateHistoricalData(Map<UUID, LocalDate> requestMap) {
    List<FacilityLastRequisitionTimeDto> facilityLatestRequisitionDateList;
    if (ObjectUtils.isEmpty(requestMap) || requestMap.size() == 0) {
      facilityLatestRequisitionDateList = historicalDataRepository.getFacilityLatestRequisitionDate();
    } else {
      Set<UUID> facilityIds = requestMap.keySet();
      facilityLatestRequisitionDateList = historicalDataRepository.getFacilityLatestRequisitionDate(facilityIds);
    }
    for (FacilityLastRequisitionTimeDto facilityLastRequisitionTimeDto : facilityLatestRequisitionDateList) {
      LocalDate startDate = facilityLastRequisitionTimeDto.getLastRequisitionDate();
      LocalDate endDate =
          ObjectUtils.isEmpty(requestMap) ? null : requestMap.get(facilityLastRequisitionTimeDto.getFacilityId());
      log.info("update facility {} historical data start at {}", facilityLastRequisitionTimeDto.getFacilityId(),
          LocalDateTime.now());
      historicalDataRepository.updateFacilityHistoricalData(facilityLastRequisitionTimeDto.getFacilityId(),
          startDate == null ? LocalDate.of(1970, 1, 1) : startDate,
          endDate == null ? LocalDate.of(2099, 12, 31) : endDate);
      log.info("finish update facility {} historical data start at {}", facilityLastRequisitionTimeDto.getFacilityId(),
          LocalDateTime.now());
    }
  }
}
