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

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.siglus.siglusapi.constant.FcConstants.STATUS_ACTIVE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.GeographicLevelDto;
import org.siglus.siglusapi.dto.OpenLmisGeographicZoneDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneDistrictDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneNationalDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneProvinceDto;
import org.siglus.siglusapi.service.client.SiglusGeographicLevelService;
import org.siglus.siglusapi.service.client.SiglusGeographicZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  @Transactional
  public boolean processGeographicZones(List<FcGeographicZoneNationalDto> fcDtos) {
    List<FcGeographicZoneNationalDto> dtos = filterInactiveZones(fcDtos);
    if (isEmpty(dtos)) {
      return false;
    }
    Map<String, GeographicLevelDto> levelDtoMap = getLevelDtoMap();
    Map<String, OpenLmisGeographicZoneDto> geographicZoneMaps = getGeographicZoneDtoMap();
    List<OpenLmisGeographicZoneDto> fcGeographicZones = getOpenLmisGeographicZones(dtos,
        levelDtoMap);
    List<OpenLmisGeographicZoneDto> needCreateZones = new ArrayList<>();
    List<OpenLmisGeographicZoneDto> needUpdateZones = new ArrayList<>();
    fcGeographicZones.forEach(fcZone -> {
      if (!geographicZoneMaps.containsKey(fcZone.getCode())) {
        needCreateZones.add(fcZone);
      } else {
        OpenLmisGeographicZoneDto originZone = geographicZoneMaps.get(fcZone.getCode());
        if (!fcZone.getName().equals(originZone.getName())
            || !fcZone.getLevel().equals(originZone.getLevel())
            || isDifferentParentZone(fcZone, originZone)) {
          needUpdateZones.add(fcZone);
        }
      }
    });
    createGeographicZones(needCreateZones);
    updateGeographicZones(needUpdateZones, geographicZoneMaps);
    return true;
  }

  private List<FcGeographicZoneNationalDto> filterInactiveZones(
      List<FcGeographicZoneNationalDto> fcDtos) {
    List<FcGeographicZoneNationalDto> nationals = fcDtos.stream()
        .filter(national -> STATUS_ACTIVE.equalsIgnoreCase(national.getStatus()))
        .collect(Collectors.toList());
    nationals.forEach(national -> {
      List<FcGeographicZoneProvinceDto> provinces = national.getProvinces().stream()
          .filter(province -> STATUS_ACTIVE.equalsIgnoreCase(province.getStatus()))
          .collect(Collectors.toList());
      provinces.forEach(province -> {
        List<FcGeographicZoneDistrictDto> districts = province.getDistricts().stream()
            .filter(district -> STATUS_ACTIVE.equalsIgnoreCase(district.getStatus()))
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

  private boolean isDifferentParentZone(OpenLmisGeographicZoneDto fcZone,
      OpenLmisGeographicZoneDto originZone) {
    if (fcZone.getParent() == null) {
      return originZone.getParent() != null;
    }
    return originZone.getParent() == null
        || !fcZone.getParent().getCode().equals(originZone.getParent().getCode());
  }

  private List<OpenLmisGeographicZoneDto> getOpenLmisGeographicZones(
      List<FcGeographicZoneNationalDto> dtos, Map<String, GeographicLevelDto> levelDtoMap) {
    List<OpenLmisGeographicZoneDto> fcGeographicZones = new ArrayList<>();
    dtos.forEach(nationalDto -> {
      OpenLmisGeographicZoneDto national = getGeographicZoneSimpleDto(levelDtoMap,
          nationalDto.getCode(), nationalDto.getDescription(), LEVEL_NATIONAL, null);
      fcGeographicZones.add(national);
      nationalDto.getProvinces().forEach(provinceDto -> {
        OpenLmisGeographicZoneDto province = getGeographicZoneSimpleDto(levelDtoMap,
            provinceDto.getCode(), provinceDto.getDescription(), LEVEL_PROVINCE, national);
        fcGeographicZones.add(province);
        provinceDto.getDistricts().forEach(districtDto -> {
          OpenLmisGeographicZoneDto district = getGeographicZoneSimpleDto(levelDtoMap,
              districtDto.getCode(), districtDto.getDescription(), LEVEL_DISTRICT, province);
          fcGeographicZones.add(district);
        });
      });
    });
    return fcGeographicZones;
  }

  private OpenLmisGeographicZoneDto getGeographicZoneSimpleDto(
      Map<String, GeographicLevelDto> levelDtoMap, String code, String description,
      String level, OpenLmisGeographicZoneDto parent) {
    return OpenLmisGeographicZoneDto.builder()
        .code(code)
        .name(description)
        .level(levelDtoMap.get(level))
        .parent(parent)
        .build();
  }

  private void createGeographicZones(List<OpenLmisGeographicZoneDto> needCreateZones) {
    needCreateZones.forEach(zone -> {
      log.info("create geographic zone: {}", zone);
      geographicZoneService.createGeographicZone(zone);
    });
  }

  private void updateGeographicZones(List<OpenLmisGeographicZoneDto> needUpdateZones,
      Map<String, OpenLmisGeographicZoneDto> geographicZoneMaps) {
    needUpdateZones.forEach(zone -> {
      OpenLmisGeographicZoneDto originZone = geographicZoneMaps.get(zone.getCode());
      originZone.setName(zone.getName());
      originZone.setLevel(zone.getLevel());
      originZone.setParent(zone.getParent() == null ? null :
          geographicZoneMaps.get(zone.getParent().getCode()));
      log.info("update geographic zone: {}", originZone);
      geographicZoneService.updateGeographicZone(originZone);
    });
  }

  private Map<String, OpenLmisGeographicZoneDto> getGeographicZoneDtoMap() {
    List<OpenLmisGeographicZoneDto> zoneDtos = geographicZoneService.searchAllGeographicZones();
    return zoneDtos.stream().collect(Collectors.toMap(OpenLmisGeographicZoneDto::getCode,
        Function.identity()));
  }

}
