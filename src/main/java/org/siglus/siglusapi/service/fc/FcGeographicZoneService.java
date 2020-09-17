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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.GeographicLevelDto;
import org.siglus.siglusapi.dto.GeographicZoneSimpleDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneNationalDto;
import org.siglus.siglusapi.service.client.SiglusGeographicLevelService;
import org.siglus.siglusapi.service.client.SiglusGeographicZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcGeographicZoneService {

  private static final String LEVEL_NATIONAL = "national";
  private static final String LEVEL_PROVINCE = "province";
  private static final String LEVEL_DISTRICT = "district";
  @Autowired
  private SiglusGeographicZoneService geographicZoneService;

  @Autowired
  private SiglusGeographicLevelService geographicLevelService;

  public boolean processFacilityType(List<FcGeographicZoneNationalDto> dtos) {
    Map<String, GeographicLevelDto> levelDtoMap = getLevelDtoMap();
    Map<String, GeographicZoneSimpleDto> geographicZoneMaps = getGeographicZoneDtoMap();
    List<GeographicZoneSimpleDto> fcGeographicZones = getGeographicZoneSimpleDtos(dtos,
        levelDtoMap);
    List<GeographicZoneSimpleDto> needAddedZones = new ArrayList<>();
    List<GeographicZoneSimpleDto> needUpdatedZones = new ArrayList<>();
    fcGeographicZones.stream().forEach(fcZone -> {
      if (!geographicZoneMaps.containsKey(fcZone.getCode())) {
        needAddedZones.add(fcZone);
      } else {
        GeographicZoneSimpleDto originZone = geographicZoneMaps.get(fcZone.getCode());
        if (fcZone.getName().equals(originZone.getName())
            || fcZone.getLevel().equals(originZone.getLevel())
            || compareFcAndSystemParentZone(fcZone, originZone)) {
          needUpdatedZones.add(fcZone);
        }
      }
    });
    syncAddedZone(needAddedZones);
    syncUpdateZone(needUpdatedZones, geographicZoneMaps);
    return true;
  }

  private Map<String, GeographicLevelDto> getLevelDtoMap() {
    return geographicLevelService.searchAllGeographicLevel()
        .stream()
        .collect(Collectors
            .toMap(geographicLevelDto -> geographicLevelDto.getCode(), Function.identity()));
  }

  private boolean compareFcAndSystemParentZone(GeographicZoneSimpleDto fcZone,
      GeographicZoneSimpleDto originZone) {
    if (fcZone.getParent() == null) {
      return originZone == null ? true : false;
    }
    return originZone != null
        && fcZone.getParent().getId().equals(originZone.getParent().getId());
  }

  private List<GeographicZoneSimpleDto> getGeographicZoneSimpleDtos(
      List<FcGeographicZoneNationalDto> dtos, Map<String, GeographicLevelDto> levelDtoMap) {
    List<GeographicZoneSimpleDto> fcGeographicZones = new ArrayList<>();
    dtos.stream().forEach(nationalDto -> {
      GeographicZoneSimpleDto national = getGeographicZoneSimpleDto(levelDtoMap,
          nationalDto.getCode(), nationalDto.getDescription(), LEVEL_NATIONAL, null);
      fcGeographicZones.add(national);
      nationalDto.getProvinces().stream().forEach(provinceDto -> {
        GeographicZoneSimpleDto province = getGeographicZoneSimpleDto(levelDtoMap,
            provinceDto.getCode(), provinceDto.getDescription(), LEVEL_PROVINCE, national);
        fcGeographicZones.add(province);
        provinceDto.getDistricts().stream().forEach(districtDto -> {
          GeographicZoneSimpleDto district = getGeographicZoneSimpleDto(levelDtoMap,
              districtDto.getCode(), districtDto.getDescription(), LEVEL_DISTRICT, province);
          fcGeographicZones.add(district);
        });
      });
    });
    return fcGeographicZones;
  }

  private GeographicZoneSimpleDto getGeographicZoneSimpleDto(
      Map<String, GeographicLevelDto> levelDtoMap, String code, String description,
      String level, GeographicZoneSimpleDto parent) {
    return GeographicZoneSimpleDto.builder()
        .code(code)
        .name(description)
        .level(levelDtoMap.get(level))
        .parent(parent)
        .build();
  }

  private void syncAddedZone(List<GeographicZoneSimpleDto> zoneSimpleDtos) {
    zoneSimpleDtos.forEach(zoneSimpleDto -> createGeographicZone(zoneSimpleDto));
  }

  private void syncUpdateZone(List<GeographicZoneSimpleDto> zoneSimpleDtos,
      Map<String, GeographicZoneSimpleDto> geographicZoneMaps) {
    zoneSimpleDtos.forEach(zoneSimpleDto -> {
      GeographicZoneSimpleDto originZone = geographicZoneMaps.get(zoneSimpleDto.getCode());
      zoneSimpleDto.setId(originZone.getId());
      saveGeographicZone(zoneSimpleDto);
    });
  }

  @Async
  public void createGeographicZone(GeographicZoneSimpleDto dto) {
    geographicZoneService.createGeographicZone(dto);
  }

  @Async
  public void saveGeographicZone(GeographicZoneSimpleDto dto) {
    geographicZoneService.saveGeographicZone(dto);
  }

  private Map<String, GeographicZoneSimpleDto> getGeographicZoneDtoMap() {
    List<GeographicZoneSimpleDto> zoneSimpleDtos =
        geographicZoneService.searchAllGeographicZones();
    return zoneSimpleDtos.stream().collect(Collectors.toMap(
        dto -> dto.getCode(), Function.identity()));
  }

}
