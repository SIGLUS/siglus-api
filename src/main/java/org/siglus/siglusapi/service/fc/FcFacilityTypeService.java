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

import static org.siglus.siglusapi.constant.FcConstants.STATUS_ACTIVE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.fc.FcFacilityTypeDto;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcFacilityTypeService {

  @Autowired
  private SiglusFacilityTypeService facilityTypeService;

  public boolean processFacilityType(List<FcFacilityTypeDto> dtos) {
    if (CollectionUtils.isNotEmpty(dtos)) {
      Map<String, FacilityTypeDto> facilityTypeMap = getStringFacilityTypeDtoMap();
      List<FcFacilityTypeDto> needAddedFacilityTypes = new ArrayList<>();
      List<FcFacilityTypeDto> needUpdatedFacilityTypes = new ArrayList<>();
      dtos.stream().forEach(typeDto -> {
        if (!facilityTypeMap.containsKey(typeDto.getCode())) {
          needAddedFacilityTypes.add(typeDto);
        } else {
          getUpdatedFacility(facilityTypeMap, needUpdatedFacilityTypes, typeDto);
        }
      });
      updateAddedFacility(facilityTypeMap, needAddedFacilityTypes);
      updateModifiedFacility(facilityTypeMap, needUpdatedFacilityTypes);
    }

    return true;
  }

  @Async
  public void createFacility(FacilityTypeDto dto) {
    facilityTypeService.createFacilityType(dto);
  }

  @Async
  public void saveFacilityType(FacilityTypeDto dto) {
    facilityTypeService.saveFacilityType(dto);
  }


  private Map<String, FacilityTypeDto> getStringFacilityTypeDtoMap() {
    List<FacilityTypeDto> facilityTypeDtos = facilityTypeService.searchAllFacilityTypes();
    return facilityTypeDtos.stream().collect(Collectors.toMap(
        facilityType -> facilityType.getCode(),
        Function.identity()));
  }

  private void getUpdatedFacility(Map<String, FacilityTypeDto> facilityTypeMap,
      List<FcFacilityTypeDto> needUpdatedFacilityTypes, FcFacilityTypeDto typeDto) {
    FacilityTypeDto facilityTypeDto = facilityTypeMap.get(typeDto.getCode());
    if (!facilityTypeDto.getName().equals(typeDto.getDescription())
        || !facilityTypeDto.getActive().equals(isActive(typeDto.getStatus()))) {
      needUpdatedFacilityTypes.add(typeDto);
    }
  }

  private void updateModifiedFacility(Map<String, FacilityTypeDto> facilityTypeMap,
      List<FcFacilityTypeDto> needUpdatedFacilityTypes) {
    needUpdatedFacilityTypes.stream().forEach(typeDto -> {
      FacilityTypeDto dto = facilityTypeMap.get(typeDto.getCode());
      dto.setActive(isActive(typeDto.getStatus()));
      dto.setName(typeDto.getDescription());
      saveFacilityType(dto);
    });
  }

  private void updateAddedFacility(Map<String, FacilityTypeDto> facilityTypeMap,
      List<FcFacilityTypeDto> needAddedFacilityTypes) {
    int originSize = facilityTypeMap.size();
    needAddedFacilityTypes.stream().forEach(typeDto -> {
      int index = needAddedFacilityTypes.indexOf(typeDto);
      FacilityTypeDto dto = new FacilityTypeDto();
      dto.setCode(typeDto.getCode());
      dto.setActive(isActive(typeDto.getStatus()));
      dto.setName(typeDto.getDescription());
      dto.setDisplayOrder(originSize + index + 1);
      createFacility(dto);
    });
  }

  private static boolean isActive(String status) {
    return STATUS_ACTIVE.equals(status);
  }

}
