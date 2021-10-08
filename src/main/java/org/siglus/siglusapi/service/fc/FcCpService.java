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

package org.siglus.siglusapi.service.fc;

import static org.siglus.siglusapi.constant.FcConstants.CP_API;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResult;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.CpDomain;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.repository.CmmRepository;
import org.siglus.siglusapi.repository.CpRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SupervisoryNodeRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcCpService implements ProcessDataService {

  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final CmmRepository cmmRepository;
  private final CpRepository cpRepository;
  private final SiglusOrderableReferenceDataService orderableReferenceDataService;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final SiglusProcessingPeriodReferenceDataService processingPeriodReferenceDataService;
  private final SupervisoryNodeRepository supervisoryNodeRepository;
  private final SiglusRequisitionRepository siglusRequisitionRepository;

  @Override
  public FcIntegrationResultDto processData(List<? extends ResponseBaseDto> cps, String startDate,
      ZonedDateTime previousLastUpdatedAt) {
    log.info("[FC cp] sync count: {}", cps.size());
    if (cps.isEmpty()) {
      return null;
    }
    boolean finalSuccess = true;
    AtomicInteger createCounter = new AtomicInteger();
    AtomicInteger updateCounter = new AtomicInteger();
    try {
      CpDomain.from(cps).forEach(cp -> {
        CpDomain existCp = cpRepository
            .findCpByFacilityCodeAndProductCodeAndQueryDate(cp.getFacilityCode(), cp.getProductCode(), startDate);
        if (null != existCp) {
          cp.setId(existCp.getId());
          createCounter.getAndIncrement();
        } else {
          updateCounter.getAndIncrement();
        }
        cp.setQueryDate(startDate);
        cpRepository.save(cp);
      });
    } catch (Exception e) {
      log.error("[FC cp] process data error", e);
      finalSuccess = false;
    }
    log.info("[FC cp] process data create: {}, update: {}, same: {}", createCounter.get(), updateCounter.get(), 0);
    return buildResult(CP_API, cps, startDate, previousLastUpdatedAt, finalSuccess, createCounter.get(),
        updateCounter.get());
  }
}
