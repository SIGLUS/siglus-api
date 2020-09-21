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

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.FacilityTypeDto;
import org.siglus.common.dto.referencedata.OpenLmisGeographicZoneDto;
import org.siglus.common.dto.referencedata.SupportedProgramDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.fc.FcFacilityDto;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeService;
import org.siglus.siglusapi.service.client.SiglusGeographicZoneService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcFacilityService {

  @Autowired
  private SiglusFacilityReferenceDataService facilityService;

  @Autowired
  private SiglusGeographicZoneService geographicZoneService;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private ProgramRealProgramRepository programRealProgramRepository;

  @Autowired
  private SiglusFacilityTypeService facilityTypeService;

  public boolean processFacility(List<FcFacilityDto> dtos) {
    Map<String, ProgramDto> codeToProgramMap = getCodeToProgramMap();
    Map<String, ProgramRealProgram> codeToRealProgramMap = getCodeToRealProgramMap();
    Map<String, FacilityDto> codeToFacilityMap = getCodeToFacilityDtoMap();
    Map<String, OpenLmisGeographicZoneDto> codeToGeographicZoneDtoMap =
        getCodeToGeographicZoneDtoMap();
    Map<String, FacilityTypeDto> codeToFacilityType = getCodeToFacilityTypeDtoMap();
    List<FcFacilityDto> needCreateFacilities = new ArrayList<>();
    List<FcFacilityDto> needUpdateFacilities = new ArrayList<>();
    dtos.forEach(dto -> {
      Set<String> codes = getFacilitySupportedProgramCode(dto, codeToRealProgramMap);
      if (isValidateFcFacilityData(dto, codeToGeographicZoneDtoMap, codes,
          codeToFacilityType)) {
        if (codeToFacilityMap.containsKey(dto.getCode())) {
          FacilityDto originDto = codeToFacilityMap.get(dto.getCode());
          if (isDifferenceFacilityDto(dto, codes, originDto)) {
            needUpdateFacilities.add(dto);
          }
        } else {
          needCreateFacilities.add(dto);
        }
      }
    });
    createFacilityDto(needCreateFacilities, codeToFacilityType, codeToProgramMap,
        codeToRealProgramMap, codeToGeographicZoneDtoMap);
    updateFacilityDto(needUpdateFacilities, codeToFacilityMap, codeToFacilityType,
        codeToProgramMap, codeToRealProgramMap, codeToGeographicZoneDtoMap);
    return true;
  }

  private Map<String, FacilityDto> getCodeToFacilityDtoMap() {
    return facilityService.findAll().stream()
        .collect(Collectors.toMap(facilityDto -> facilityDto.getCode(), Function.identity()));
  }

  private Map<String, ProgramRealProgram> getCodeToRealProgramMap() {
    return programRealProgramRepository.findAll().stream()
        .collect(Collectors.toMap(ProgramRealProgram::getRealProgramCode, Function.identity()));
  }

  private Map<String, ProgramDto> getCodeToProgramMap() {
    return programReferenceDataService.findAll().stream()
        .collect(Collectors.toMap(programDto -> programDto.getCode(), Function.identity()));
  }

  private Map<String, OpenLmisGeographicZoneDto> getCodeToGeographicZoneDtoMap() {
    return geographicZoneService.searchAllGeographicZones().stream()
        .collect(Collectors.toMap(dto -> dto.getCode(), Function.identity()));
  }

  private Map<String, FacilityTypeDto> getCodeToFacilityTypeDtoMap() {
    return facilityTypeService.searchAllFacilityTypes().stream()
        .collect(Collectors.toMap(facilityType -> facilityType.getCode(),
            Function.identity()));
  }

  private boolean isActive(String status) {
    return STATUS_ACTIVE.equals(status);
  }

  public boolean isValidateFcFacilityData(FcFacilityDto fcFacilityDto,
      Map<String, OpenLmisGeographicZoneDto> codeToGeographicZoneDtoMap, Set<String> codes,
      Map<String, FacilityTypeDto> codeToFacilityType) {
    if (!codeToGeographicZoneDtoMap.containsKey(fcFacilityDto.getDistrictCode())
        && !codeToFacilityType.containsKey(fcFacilityDto.getClientTypeCode())) {
      log.info("[fc facility error] GeographicZone or facilityType not exist in our system: {}",
          fcFacilityDto);
      return false;
    }
    if (CollectionUtils.isEmpty(codes)) {
      log.info("[fc facility error] program code not exsit: {}", fcFacilityDto);
      return false;
    }
    return true;
  }

  private Set<String> getFacilitySupportedProgramCode(FcFacilityDto fcFacilityDto,
      Map<String, ProgramRealProgram> codeToRealProgramMap) {
    return fcFacilityDto.getAreas()
        .stream().map(fcAreaDto -> {
          String code = fcAreaDto.getAreaCode();
          return codeToRealProgramMap.containsKey(code) ? codeToRealProgramMap.get(code)
              .getProgramCode() : null;
        }).filter(code -> code != null)
        .collect(Collectors.toSet());
  }

  private List<SupportedProgramDto> getSupportedProgramDtos(FcFacilityDto fcFacilityDto,
      Map<String, ProgramDto> codeToProgramMap,
      Map<String, ProgramRealProgram> codeToRealProgramMap) {
    return getFacilitySupportedProgramCode(fcFacilityDto, codeToRealProgramMap)
        .stream()
        .map(code -> getSupportedProgramDto(codeToProgramMap, code)
        ).collect(Collectors.toList());
  }

  private SupportedProgramDto getSupportedProgramDto(Map<String, ProgramDto> codeToProgramMap,
      String code) {
    ProgramDto originProgramDto = codeToProgramMap.get(code);
    SupportedProgramDto programDto = new SupportedProgramDto();
    BeanUtils.copyProperties(originProgramDto, programDto);
    programDto.setProgramActive(originProgramDto.getActive());
    programDto.setSupportActive(true);
    return programDto;
  }

  private boolean isDifferenceFacilityDto(FcFacilityDto fcFacilityDto, Set<String> codes,
      FacilityDto originDto) {
    Set<String> originCodes = originDto.getSupportedPrograms().stream()
        .map(supportedProgramDto -> supportedProgramDto.getCode())
        .collect(Collectors.toSet());
    return fcFacilityDto.getDistrictCode().equals(originDto.getGeographicZone().getCode())
        || fcFacilityDto.getClientTypeCode().equals(originDto.getType().getCode())
        || isActive(fcFacilityDto.getStatus()) == originDto.getActive()
        || Sets.difference(codes, originCodes).isEmpty();
  }

  private void createFacilityDto(List<FcFacilityDto> needCreateFacilities,
      Map<String, FacilityTypeDto> codeToFacilityType,
      Map<String, ProgramDto> codeToProgramMap,
      Map<String, ProgramRealProgram> codeToRealProgramMap,
      Map<String, OpenLmisGeographicZoneDto> codeToGeographicZoneDtoMap) {
    needCreateFacilities.forEach(facilityDto -> {
      log.info("create facility: {}", facilityDto);
      List<SupportedProgramDto> supportedProgramDtos = getSupportedProgramDtos(facilityDto,
          codeToProgramMap, codeToRealProgramMap);
      facilityService.createFacility(getFacilityDto(facilityDto, codeToGeographicZoneDtoMap,
          codeToFacilityType, supportedProgramDtos));
    });
  }

  private FacilityDto getFacilityDto(FcFacilityDto fcFacilityDto,
      Map<String, OpenLmisGeographicZoneDto> codeToGeographicZoneDtoMap,
      Map<String, FacilityTypeDto> codeToFacilityType,
      List<SupportedProgramDto> supportedProgramDtos) {
    return FacilityDto.builder()
        .code(fcFacilityDto.getCode())
        .name(fcFacilityDto.getName())
        .description(fcFacilityDto.getDescription())
        .active(isActive(fcFacilityDto.getStatus()))
        .enabled(isActive(fcFacilityDto.getStatus()))
        .type(codeToFacilityType.get(fcFacilityDto.getClientTypeCode()))
        .geographicZone(codeToGeographicZoneDtoMap.get(fcFacilityDto.getDistrictCode()))
        .supportedPrograms(supportedProgramDtos)
        .build();
  }

  private void updateFacilityDto(List<FcFacilityDto> needUpdateFacilities,
      Map<String, FacilityDto> codeToFacilityMap,
      Map<String, FacilityTypeDto> codeToFacilityType,
      Map<String, ProgramDto> codeToProgramMap,
      Map<String, ProgramRealProgram> codeToRealProgramMap,
      Map<String, OpenLmisGeographicZoneDto> codeToGeographicZoneDtoMap) {
    needUpdateFacilities.forEach(fcFacilityDto -> {
      FacilityDto needSaveFacility = codeToFacilityMap.get(fcFacilityDto.getCode());
      List<SupportedProgramDto> supportedProgramDtos = getUpdateProgramDto(fcFacilityDto,
          needSaveFacility, codeToRealProgramMap, codeToProgramMap);
      needSaveFacility.setName(fcFacilityDto.getName());
      needSaveFacility.setDescription(fcFacilityDto.getDescription());
      needSaveFacility.setActive(isActive(fcFacilityDto.getStatus()));
      needSaveFacility.setEnabled(isActive(fcFacilityDto.getStatus()));
      needSaveFacility.setType(codeToFacilityType.get(fcFacilityDto.getClientTypeCode()));
      needSaveFacility.setGeographicZone(
          codeToGeographicZoneDtoMap.get(fcFacilityDto.getDistrictCode()));
      needSaveFacility.setSupportedPrograms(supportedProgramDtos);
      log.info("save facility: {}", fcFacilityDto);
      facilityService.saveFacility(needSaveFacility);
    });
  }

  private List<SupportedProgramDto> getUpdateProgramDto(FcFacilityDto fcFacilityDto,
      FacilityDto needSaveFacility, Map<String, ProgramRealProgram> codeToRealProgramMap,
      Map<String, ProgramDto> codeToProgramMap) {
    List<SupportedProgramDto> supportedProgramDtos = new ArrayList<>();
    Map<String, SupportedProgramDto> codeToSupportProgram = needSaveFacility.getSupportedPrograms()
        .stream()
        .collect(Collectors
            .toMap(supportedProgramDto -> supportedProgramDto.getCode(), Function.identity()));
    getFacilitySupportedProgramCode(fcFacilityDto, codeToRealProgramMap)
        .forEach(programDto -> {
          if (codeToSupportProgram.containsKey(programDto)) {
            supportedProgramDtos.add(codeToSupportProgram.get(programDto));
          } else {
            supportedProgramDtos.add(getSupportedProgramDto(codeToProgramMap, programDto));
          }
        });
    return supportedProgramDtos;
  }
}
