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

import static org.siglus.siglusapi.constant.FcConstants.REGION_API;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResultForNewFc;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.GeographicLevel;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.repository.GeographicLevelRepository;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.siglus.siglusapi.domain.FcIntegrationChanges;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultBuildDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.RegionDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.service.client.SiglusGeographicLevelReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusGeographicZoneReferenceDataService;
import org.siglus.siglusapi.util.FcUtil;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcRegionService implements ProcessDataService  {
  private static final String LEVEL_NATIONAL = "national";
  private static final String LEVEL_PROVINCE = "province";
  private static final String LEVEL_DISTRICT = "district";

  private final SiglusGeographicZoneReferenceDataService geographicZoneService;
  private final SiglusGeographicLevelReferenceDataService geographicLevelService;

  private final GeographicLevelRepository geographicLevelRepository;
  private final GeographicZoneRepository geographicZoneRepository;


  @Override
  public FcIntegrationResultDto processData(List<? extends ResponseBaseDto> fcDtos,
                                            String startDate, ZonedDateTime previousLastUpdatedAt) {
    boolean finalSuccess = true;
    AtomicInteger createCounter = new AtomicInteger();
    AtomicInteger updateCounter = new AtomicInteger();
    AtomicInteger sameCounter = new AtomicInteger();
    List<FcIntegrationChanges> fcIntegrationChangesList = new ArrayList<>();
    try {
      log.info("[FC regions] sync count: {}", fcDtos.size());
      List<GeographicLevel> allLevels = (List<GeographicLevel>) geographicLevelRepository.findAll();
      GeographicLevel nationLevel = allLevels.stream()
          .filter(l -> LEVEL_NATIONAL.equals(l.getCode())).findFirst()
          .orElseThrow(() -> new EntityNotFoundException("National level not found"));
      // TODO if zone level changes, then it will NOT work!, but the fc api NOT contains it!
      List<GeographicZone> nations = geographicZoneRepository.findByLevel(nationLevel);
      Map<String, GeographicZone> codeToGeographicZone = nations
          .stream().collect(Collectors.toMap(GeographicZone::getCode, Function.identity()));
      Set<GeographicZone> zonesToUpdate = new HashSet<>();
      fcDtos
          .forEach(fcDto -> {
            RegionDto current = (RegionDto) fcDto;
            GeographicZone existed = codeToGeographicZone.get(current.getCode());
            if (existed == null) {
              log.info("[FC regions] create: {}", current);
              zonesToUpdate.add(RegionDto.convertToGeographicZone(current, nationLevel));
              createCounter.getAndIncrement();
              FcIntegrationChanges createChanges = FcUtil
                  .buildCreateFcIntegrationChanges(REGION_API, current.getCode(), current.toString());
              fcIntegrationChangesList.add(createChanges);
            } else {
              FcIntegrationChanges updateChanges = getUpdatedZoneChanges(existed, current);
              if (updateChanges != null) {
                log.info("[FC program] update, existed: {}, current: {}", existed, current);
                zonesToUpdate.add(getUpdatedZone(existed, current));
                updateCounter.getAndIncrement();
                fcIntegrationChangesList.add(updateChanges);
              } else {
                sameCounter.getAndIncrement();
              }
            }
          });
      geographicZoneRepository.save(zonesToUpdate);
    } catch (Exception e) {
      log.error("process fc regions error", e);
      finalSuccess = false;
    }
    log.info("[FC regions] process data create: {}, update: {}, same: {}",
        createCounter.get(), updateCounter.get(), sameCounter.get());
    return buildResultForNewFc(
        new FcIntegrationResultBuildDto(REGION_API, fcDtos, startDate, previousLastUpdatedAt, finalSuccess,
            createCounter.get(), updateCounter.get(), null, fcIntegrationChangesList));
  }

  private FcIntegrationChanges getUpdatedZoneChanges(GeographicZone existed, RegionDto current) {
    boolean isSame = true;
    StringBuilder updateContent = new StringBuilder();
    StringBuilder originContent = new StringBuilder();
    if (!existed.getName().equals(current.getDescription())) {
      log.info("[FC region] name different, existed: {}, current: {}", existed.getName(),
          current.getDescription());
      updateContent.append("name=").append(current.getDescription()).append('\n');
      originContent.append("name=").append(existed.getName()).append('\n');
      isSame = false;
    }
    if (isSame) {
      return null;
    }
    return FcUtil.buildUpdateFcIntegrationChanges(REGION_API, current.getCode(), updateContent.toString(),
        originContent.toString());
  }

  private GeographicZone getUpdatedZone(GeographicZone existed, RegionDto current) {
    if (!existed.getName().equals(current.getDescription())) {
      existed.setName(current.getDescription());
    }
    return existed;
  }
}
