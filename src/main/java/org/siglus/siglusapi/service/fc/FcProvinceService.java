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

import static org.siglus.siglusapi.constant.FcConstants.PROVINCE_API;
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
import java.util.stream.StreamSupport;
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
import org.siglus.siglusapi.dto.fc.ProvinceDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.service.client.SiglusGeographicLevelReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusGeographicZoneReferenceDataService;
import org.siglus.siglusapi.util.FcUtil;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcProvinceService implements ProcessDataService  {
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
      log.info("[FC province] sync count: {}", fcDtos.size());
      List<GeographicLevel> allLevels = (List<GeographicLevel>) geographicLevelRepository.findAll();
      GeographicLevel provinceLevel = allLevels.stream()
          .filter(l -> LEVEL_PROVINCE.equals(l.getCode())).findFirst()
          .orElseThrow(() -> new EntityNotFoundException("Province level not found"));

      Map<String, GeographicZone> codeToGeographicZone = StreamSupport
          .stream(geographicZoneRepository.findAll().spliterator(), false)
          .collect(Collectors.toMap(GeographicZone::getCode, Function.identity()));
      Set<GeographicZone> provincesToUpdate = new HashSet<>();
      fcDtos
          .forEach(fcDto -> {
            ProvinceDto current = (ProvinceDto) fcDto;
            GeographicZone existed = codeToGeographicZone.get(current.getCode());
            GeographicZone parent = codeToGeographicZone.getOrDefault(current.getRegionCode(), null);
            if (parent == null) {
              log.info("[FC province] NO regin for: {}", current.getCode());
            }

            if (existed == null) {
              log.info("[FC province] create: {}", current);
              provincesToUpdate.add(ProvinceDto.convertToGeographicZone(current, provinceLevel, parent));
              createCounter.getAndIncrement();
              FcIntegrationChanges createChanges = FcUtil
                  .buildCreateFcIntegrationChanges(PROVINCE_API, current.getCode(), current.toString());
              fcIntegrationChangesList.add(createChanges);
            } else {
              FcIntegrationChanges updateChanges = getUpdatedProvinceChanges(existed, current);
              if (updateChanges != null) {
                log.info("[FC province] update, existed: {}, current: {}", existed, current);
                provincesToUpdate.add(getUpdatedProvince(existed, current, parent));
                updateCounter.getAndIncrement();
                fcIntegrationChangesList.add(updateChanges);
              } else {
                sameCounter.getAndIncrement();
              }
            }
          });
      geographicZoneRepository.save(provincesToUpdate);
    } catch (Exception e) {
      log.error("process fc province error", e);
      finalSuccess = false;
    }
    log.info("[FC province] process data create: {}, update: {}, same: {}",
        createCounter.get(), updateCounter.get(), sameCounter.get());
    return buildResultForNewFc(
        new FcIntegrationResultBuildDto(PROVINCE_API, fcDtos, startDate, previousLastUpdatedAt, finalSuccess,
            createCounter.get(), updateCounter.get(), null, fcIntegrationChangesList));
  }

  private FcIntegrationChanges getUpdatedProvinceChanges(GeographicZone existed, ProvinceDto current) {
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
    if (!existed.getParent().getCode().equals(current.getRegionCode())) {
      log.info("[FC region] regionCode different, existed: {}, current: {}",
          existed.getParent().getCode(), current.getRegionCode());
      updateContent.append("regionCode=").append(current.getRegionCode()).append('\n');
      originContent.append("regionCode=").append(existed.getParent().getCode()).append('\n');
      isSame = false;
    }
    if (isSame) {
      return null;
    }
    return FcUtil.buildUpdateFcIntegrationChanges(PROVINCE_API, current.getCode(), updateContent.toString(),
        originContent.toString());
  }

  private GeographicZone getUpdatedProvince(GeographicZone existed, ProvinceDto current, GeographicZone parent) {
    if (!existed.getName().equals(current.getDescription())) {
      existed.setName(current.getDescription());
    }
    if (!existed.getParent().getCode().equals(current.getRegionCode())) {
      existed.setParent(parent);
    }
    return existed;
  }
}
