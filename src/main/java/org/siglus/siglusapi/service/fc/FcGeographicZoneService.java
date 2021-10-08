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

import static org.siglus.siglusapi.constant.FcConstants.GEOGRAPHIC_ZONE_API;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResult;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.GeographicLevelDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneDistrictDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneNationalDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneProvinceDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.service.client.SiglusGeographicLevelReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusGeographicZoneReferenceDataService;
import org.siglus.siglusapi.util.FcUtil;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcGeographicZoneService implements ProcessDataService {

  private static final String LEVEL_NATIONAL = "national";
  private static final String LEVEL_PROVINCE = "province";
  private static final String LEVEL_DISTRICT = "district";

  private final SiglusGeographicZoneReferenceDataService geographicZoneService;
  private final SiglusGeographicLevelReferenceDataService geographicLevelService;

  @Override
  public FcIntegrationResultDto processData(List<? extends ResponseBaseDto> zones, String startDate,
      ZonedDateTime previousLastUpdatedAt) {
    log.info("[FC geographicZone] sync count: {}", zones.size());
    if (zones.isEmpty()) {
      return null;
    }
    boolean finalSuccess = true;
    int createCounter = 0;
    int updateCounter = 0;
    try {
      List<? extends ResponseBaseDto> activeZones = filterInactiveZones(zones);
      Map<String, GeographicLevelDto> levelDtoMap = getLevelDtoMap();
      Map<String, GeographicZoneDto> geographicZoneMaps = getGeographicZoneDtoMap();
      List<GeographicZoneDto> fcGeographicZones = getGeographicZones(activeZones, levelDtoMap);
      List<GeographicZoneDto> needCreateZones = new ArrayList<>();
      List<GeographicZoneDto> needUpdateZones = new ArrayList<>();
      fcGeographicZones.forEach(fcZone -> {
        if (!geographicZoneMaps.containsKey(fcZone.getCode())) {
          needCreateZones.add(fcZone);
        } else {
          GeographicZoneDto originZone = geographicZoneMaps.get(fcZone.getCode());
          if (!fcZone.getName().equals(originZone.getName())
              || !fcZone.getLevel().equals(originZone.getLevel())
              || isDifferentParentZone(fcZone, originZone)) {
            needUpdateZones.add(fcZone);
          }
        }
      });
      createGeographicZones(needCreateZones, zones, startDate, previousLastUpdatedAt);
      updateGeographicZones(needUpdateZones, geographicZoneMaps);
      createCounter = needCreateZones.size();
      updateCounter = needUpdateZones.size();
    } catch (Exception e) {
      log.error("[FC geographicZone] process data error", e);
      finalSuccess = false;
    }
    log.info("[FC geographicZone] process data create: {}, update: {}, same: {}",
        createCounter, updateCounter, zones.size() - createCounter - updateCounter);
    return buildResult(GEOGRAPHIC_ZONE_API, zones, startDate, previousLastUpdatedAt, finalSuccess, createCounter,
        updateCounter);
  }

  private List<? extends ResponseBaseDto> filterInactiveZones(List<? extends ResponseBaseDto> zones) {
    List<? extends ResponseBaseDto> nationals = zones.stream()
        .filter(item -> {
          FcGeographicZoneNationalDto national = (FcGeographicZoneNationalDto) item;
          return FcUtil.isActive(national.getStatus());
        })
        .collect(Collectors.toList());
    nationals.forEach(item -> {
      FcGeographicZoneNationalDto national = (FcGeographicZoneNationalDto) item;
      List<FcGeographicZoneProvinceDto> provinces = national.getProvinces().stream()
          .filter(province -> FcUtil.isActive(province.getStatus()))
          .collect(Collectors.toList());
      provinces.forEach(province -> {
        List<FcGeographicZoneDistrictDto> districts = province.getDistricts().stream()
            .filter(district -> FcUtil.isActive(district.getStatus()))
            .collect(Collectors.toList());
        province.setDistricts(districts);
      });
      national.setProvinces(provinces);
    });
    return nationals;
  }

  private Map<String, GeographicLevelDto> getLevelDtoMap() {
    return geographicLevelService.searchAllGeographicLevel().stream()
        .collect(Collectors.toMap(GeographicLevelDto::getCode, Function.identity()));
  }

  private boolean isDifferentParentZone(GeographicZoneDto fcZone,
      GeographicZoneDto originZone) {
    if (fcZone.getParent() == null) {
      return originZone.getParent() != null;
    }
    return originZone.getParent() == null
        || !fcZone.getParent().getCode().equals(originZone.getParent().getCode());
  }

  private List<GeographicZoneDto> getGeographicZones(List<? extends ResponseBaseDto> zones,
      Map<String, GeographicLevelDto> levelDtoMap) {
    List<GeographicZoneDto> fcGeographicZones = new ArrayList<>();
    zones.forEach(item -> {
      FcGeographicZoneNationalDto nationalDto = (FcGeographicZoneNationalDto) item;
      GeographicZoneDto national = getGeographicZoneSimpleDto(levelDtoMap,
          nationalDto.getCode(), nationalDto.getDescription(), LEVEL_NATIONAL, null);
      fcGeographicZones.add(national);
      nationalDto.getProvinces().forEach(provinceDto -> {
        GeographicZoneDto province = getGeographicZoneSimpleDto(levelDtoMap,
            provinceDto.getCode(), provinceDto.getDescription(), LEVEL_PROVINCE, national);
        fcGeographicZones.add(province);
        provinceDto.getDistricts().forEach(districtDto -> {
          GeographicZoneDto district = getGeographicZoneSimpleDto(levelDtoMap,
              districtDto.getCode(), districtDto.getDescription(), LEVEL_DISTRICT, province);
          fcGeographicZones.add(district);
        });
      });
    });
    return fcGeographicZones;
  }

  private GeographicZoneDto getGeographicZoneSimpleDto(
      Map<String, GeographicLevelDto> levelDtoMap, String code, String description,
      String level, GeographicZoneDto parent) {
    return GeographicZoneDto.builder()
        .code(code)
        .name(description)
        .level(levelDtoMap.get(level))
        .parent(parent)
        .build();
  }

  private void createGeographicZones(List<GeographicZoneDto> needCreateZones,
      List<? extends ResponseBaseDto> fcDtos, String startDate, ZonedDateTime previousLastUpdatedAt) {
    if (needCreateZones.isEmpty()) {
      return;
    }
    needCreateZones.forEach(zone -> {
      if (zone.getParent() != null) {
        zone.setParent(null);
      }
      log.info("[FC geographicZone] create new: {}", zone);
      geographicZoneService.createGeographicZone(zone);
    });
    processData(fcDtos, startDate, previousLastUpdatedAt);
  }

  private void updateGeographicZones(List<GeographicZoneDto> needUpdateZones,
      Map<String, GeographicZoneDto> geographicZoneMaps) {
    needUpdateZones.forEach(zone -> {
      GeographicZoneDto originZone = geographicZoneMaps.get(zone.getCode());
      log.info("[FC] update existed geographic zone: {} to new geographic zone: {}", originZone,
          zone);
      originZone.setName(zone.getName());
      originZone.setLevel(zone.getLevel());
      originZone.setParent(zone.getParent() == null ? null :
          geographicZoneMaps.get(zone.getParent().getCode()));
      geographicZoneService.updateGeographicZone(originZone);
    });
  }

  private Map<String, GeographicZoneDto> getGeographicZoneDtoMap() {
    List<GeographicZoneDto> zoneDtos = geographicZoneService.searchAllGeographicZones();
    return zoneDtos.stream().collect(Collectors.toMap(GeographicZoneDto::getCode,
        Function.identity()));
  }

}
