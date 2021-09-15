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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemReasonDto;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.fc.FcFacilityTypeDto;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusStockCardLineItemReasons;
import org.siglus.siglusapi.service.client.ValidReasonAssignmentStockManagementService;
import org.siglus.siglusapi.util.FcUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcFacilityTypeService {

  @Autowired
  private SiglusFacilityTypeReferenceDataService facilityTypeService;

  @Autowired
  private SiglusStockCardLineItemReasons siglusStockCardLineItemReasons;

  @Autowired
  private ProgramReferenceDataService programRefDataService;

  @Autowired
  private ValidReasonAssignmentStockManagementService assignmentService;

  public boolean processFacilityTypes(List<FcFacilityTypeDto> dtos) {
    if (isEmpty(dtos)) {
      return false;
    }
    if (CollectionUtils.isNotEmpty(dtos)) {
      Map<String, FacilityTypeDto> facilityTypeMap = getStringFacilityTypeDtoMap();
      List<FcFacilityTypeDto> createFacilityTypes = new ArrayList<>();
      List<FcFacilityTypeDto> updateFacilityTypes = new ArrayList<>();
      dtos.forEach(typeDto -> {
        if (facilityTypeMap.containsKey(typeDto.getCode())) {
          getUpdatedFacilityType(facilityTypeMap, updateFacilityTypes, typeDto);
        } else {
          createFacilityTypes.add(typeDto);
        }
      });
      createFacilityTypes(facilityTypeMap, createFacilityTypes);
      updateFacilityTypes(facilityTypeMap, updateFacilityTypes);
    }

    return true;
  }

  private Map<String, FacilityTypeDto> getStringFacilityTypeDtoMap() {
    List<FacilityTypeDto> facilityTypeDtos = facilityTypeService.searchAllFacilityTypes();
    return facilityTypeDtos.stream().collect(Collectors.toMap(
        FacilityTypeDto::getCode,
        Function.identity()));
  }

  private void getUpdatedFacilityType(Map<String, FacilityTypeDto> facilityTypeMap,
      List<FcFacilityTypeDto> needUpdatedFacilityTypes, FcFacilityTypeDto typeDto) {
    FacilityTypeDto facilityTypeDto = facilityTypeMap.get(typeDto.getCode());
    if (!facilityTypeDto.getName().equals(typeDto.getDescription())
        || !facilityTypeDto.getActive().equals(FcUtil.isActive(typeDto.getStatus()))) {
      needUpdatedFacilityTypes.add(typeDto);
    }
  }

  private void updateFacilityTypes(Map<String, FacilityTypeDto> facilityTypeMap,
      List<FcFacilityTypeDto> needUpdatedFacilityTypes) {
    needUpdatedFacilityTypes.forEach(typeDto -> {
      FacilityTypeDto dto = facilityTypeMap.get(typeDto.getCode());
      log.info("[FC] update existed facility type: {} to new facility type: {}", dto, typeDto);
      dto.setActive(FcUtil.isActive(typeDto.getStatus()));
      dto.setName(typeDto.getDescription());
      facilityTypeService.saveFacilityType(dto);
    });
  }

  private void createFacilityTypes(Map<String, FacilityTypeDto> facilityTypeMap,
      List<FcFacilityTypeDto> needAddedFacilityTypes) {
    int originSize = facilityTypeMap.size();
    List<FacilityTypeDto> needUpdateReasonType = new ArrayList<>();
    needAddedFacilityTypes.forEach(typeDto -> {
      log.info("[FC] create facility type: {}", typeDto);
      int index = needAddedFacilityTypes.indexOf(typeDto);
      FacilityTypeDto dto = new FacilityTypeDto();
      dto.setCode(typeDto.getCode());
      dto.setActive(FcUtil.isActive(typeDto.getStatus()));
      dto.setName(typeDto.getDescription());
      dto.setDisplayOrder(originSize + index + 1);
      needUpdateReasonType.add(facilityTypeService.createFacilityType(dto));
    });
    updateReason(needUpdateReasonType);
  }

  private void updateReason(List<FacilityTypeDto> facilityTypeDtos) {
    Collection<StockCardLineItemReasonDto> reasonDtos = siglusStockCardLineItemReasons.findAll();
    List<ProgramDto> programs = programRefDataService.findAll();
    List<ValidReasonAssignmentDto> assignmentDtos = new ArrayList<>();
    facilityTypeDtos.forEach(facilityTypeDto ->
        programs.forEach(programDto ->
            configureProgram(facilityTypeDto, programDto, reasonDtos, assignmentDtos)));
    assignmentDtos.forEach(validReasonAssignmentDto ->
        assignmentService.assignReason(validReasonAssignmentDto));
  }

  private void configureProgram(FacilityTypeDto typeDto, ProgramDto programDto,
      Collection<StockCardLineItemReasonDto> reasonDtos,
      List<ValidReasonAssignmentDto> assignmentDtos) {
    reasonDtos.forEach(reasonDto -> {
      ValidReasonAssignmentDto assignmentDto = new ValidReasonAssignmentDto();
      assignmentDto.setFacilityType(new ObjectReferenceDto("", "", typeDto.getId()));
      assignmentDto.setProgram(new ObjectReferenceDto("", "", programDto.getId()));
      assignmentDto.setReason(getLineItemReason(reasonDto));
      assignmentDto.setHidden(false);
      assignmentDtos.add(assignmentDto);
    });
  }

  private StockCardLineItemReason getLineItemReason(StockCardLineItemReasonDto dto) {
    StockCardLineItemReason lineItemReason = new StockCardLineItemReason();
    lineItemReason.setId(dto.getId());
    return lineItemReason;
  }

}
