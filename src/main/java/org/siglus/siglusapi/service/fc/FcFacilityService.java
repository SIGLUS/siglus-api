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

import static com.google.common.collect.Lists.newArrayList;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_API;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResult;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.web.FacilityController;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.repository.NodeRepository;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.domain.FcIntegrationChanges;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.dto.fc.FcFacilityDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultBuildDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusGeographicZoneReferenceDataService;
import org.siglus.siglusapi.util.FcUtil;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcFacilityService implements ProcessDataService {

  private final FacilityController facilityController;
  private final SiglusFacilityReferenceDataService facilityService;
  private final SiglusGeographicZoneReferenceDataService geographicZoneService;
  private final ProgramReferenceDataService programReferenceDataService;
  private final SiglusFacilityTypeReferenceDataService facilityTypeService;
  private final ProgramRealProgramRepository programRealProgramRepository;
  private final NodeRepository nodeRepository;

  @Override
  public FcIntegrationResultDto processData(List<? extends ResponseBaseDto> facilities, String startDate,
      ZonedDateTime previousLastUpdatedAt) {
    log.info("[FC facility] sync count: {}", facilities.size());
    if (facilities.isEmpty()) {
      return null;
    }
    boolean finalSuccess = true;
    int createCounter = 0;
    int updateCounter = 0;
    int errorCounter = 0;
    List<FcIntegrationChanges> fcIntegrationChangesList = new ArrayList<>();
    try {
      Map<String, ProgramDto> codeToProgramMap = getCodeToProgramMap();
      Map<String, ProgramRealProgram> codeToRealProgramMap = getCodeToRealProgramMap();
      Map<String, FacilityDto> codeToFacilityMap = getCodeToFacilityDtoMap();
      Map<String, GeographicZoneDto> codeToGeographicZoneDtoMap = getCodeToGeographicZoneDtoMap();
      Map<String, FacilityTypeDto> codeToFacilityType = getCodeToFacilityTypeDtoMap();
      List<FcFacilityDto> createFacilities = new ArrayList<>();
      List<FcFacilityDto> updateFacilities = new ArrayList<>();
      List<String> errorCodes = new ArrayList<>();
      facilities.forEach(item -> {
        FcFacilityDto facility = (FcFacilityDto) item;
        Set<String> codes = getFacilitySupportedProgramCode(facility, codeToRealProgramMap);
        if (!isValid(facility, codeToGeographicZoneDtoMap, codes, codeToFacilityType)) {
          return;
        }
        if (codeToFacilityMap.containsKey(facility.getCode())) {
          FacilityDto originDto = codeToFacilityMap.get(facility.getCode());
          FcIntegrationChanges updateChanges = getUpdatedFacilityDto(facility, codes, originDto);
          if (updateChanges != null) {
            updateFacilities.add(facility);
            fcIntegrationChangesList.add(updateChanges);
          }
        } else if (FcUtil.isNotMatchedCode(facility.getCode())) {
          errorCodes.add(facility.getCode());
        } else {
          createFacilities.add(facility);
          FcIntegrationChanges createChanges = FcUtil
              .buildCreateFcIntegrationChanges(FACILITY_API, facility.getCode(), facility.toString());
          fcIntegrationChangesList.add(createChanges);
        }
      });
      createFacilities(createFacilities, codeToFacilityType, codeToProgramMap, codeToRealProgramMap,
          codeToGeographicZoneDtoMap);
      updateFacilities(updateFacilities, codeToFacilityMap, codeToFacilityType, codeToProgramMap, codeToRealProgramMap,
          codeToGeographicZoneDtoMap);
      createCounter = createFacilities.size();
      updateCounter = updateFacilities.size();
      errorCounter = errorCodes.size();
      log.info("[FC facility] process data error code: {}", errorCodes);
    } catch (Exception e) {
      log.error("[FC facility] process data error", e);
      finalSuccess = false;
    }
    log.info("[FC facility] process data create: {}, update: {}, error: {}, same: {}",
        createCounter, updateCounter, errorCounter, facilities.size() - createCounter - updateCounter - errorCounter);
    return buildResult(
        new FcIntegrationResultBuildDto(FACILITY_API, facilities, startDate, previousLastUpdatedAt, finalSuccess,
            createCounter, updateCounter, null, fcIntegrationChangesList));
  }

  private Map<String, FacilityDto> getCodeToFacilityDtoMap() {
    return Maps.uniqueIndex(facilityService.findAll(), FacilityDto::getCode);
  }

  private Map<String, ProgramRealProgram> getCodeToRealProgramMap() {
    return Maps.uniqueIndex(programRealProgramRepository.findAll(), ProgramRealProgram::getRealProgramCode);
  }

  private Map<String, ProgramDto> getCodeToProgramMap() {
    return Maps.uniqueIndex(programReferenceDataService.findAll(), BasicProgramDto::getCode);
  }

  private Map<String, GeographicZoneDto> getCodeToGeographicZoneDtoMap() {
    return Maps.uniqueIndex(geographicZoneService.searchAllGeographicZones(), GeographicZoneDto::getCode);
  }

  private Map<String, FacilityTypeDto> getCodeToFacilityTypeDtoMap() {
    return Maps.uniqueIndex(facilityTypeService.searchAllFacilityTypes(), FacilityTypeDto::getCode);
  }

  public boolean isValid(FcFacilityDto fcFacilityDto,
      Map<String, GeographicZoneDto> codeToGeographicZoneDtoMap, Set<String> codes,
      Map<String, FacilityTypeDto> codeToFacilityType) {
    if (!codeToGeographicZoneDtoMap.containsKey(fcFacilityDto.getDistrictCode())) {
      log.info("[FC facility] geographic zone not exist in db: {}", fcFacilityDto);
      return false;
    }
    if (!codeToFacilityType.containsKey(fcFacilityDto.getClientTypeCode())) {
      log.info("[FC facility] facility type not exist in db: {}", fcFacilityDto);
      return false;
    }
    if (CollectionUtils.isEmpty(codes)) {
      log.info("[FC facility] program code is empty: {}", fcFacilityDto);
      return false;
    }
    return true;
  }

  private Set<String> getFacilitySupportedProgramCode(FcFacilityDto fcFacilityDto,
      Map<String, ProgramRealProgram> codeToRealProgramMap) {
    Set<String> codes = fcFacilityDto.getAreas()
        .stream()
        .map(fcAreaDto -> {
          String code = fcAreaDto.getAreaCode();
          if (code.equalsIgnoreCase(ProgramConstants.MALARIA_PROGRAM_CODE)) {
            return code;
          }
          return codeToRealProgramMap.containsKey(code) ? codeToRealProgramMap.get(code).getProgramCode() : null;
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (codes.contains(ProgramConstants.MALARIA_PROGRAM_CODE)) {
      codes.add(ProgramConstants.VIA_PROGRAM_CODE);
    }
    return codes;
  }

  private List<SupportedProgramDto> getSupportedProgramDtos(FcFacilityDto fcFacilityDto,
      Map<String, ProgramDto> codeToProgramMap,
      Map<String, ProgramRealProgram> codeToRealProgramMap) {
    return getFacilitySupportedProgramCode(fcFacilityDto, codeToRealProgramMap)
        .stream()
        .map(code -> getSupportedProgramDto(codeToProgramMap, code))
        .collect(Collectors.toList());
  }

  private SupportedProgramDto getSupportedProgramDto(Map<String, ProgramDto> codeToProgramMap, String code) {
    ProgramDto originProgramDto = codeToProgramMap.get(code);
    SupportedProgramDto programDto = new SupportedProgramDto();
    programDto.setCode(originProgramDto.getCode());
    programDto.setName(originProgramDto.getName());
    programDto.setDescription(originProgramDto.getDescription());
    programDto.setProgramActive(originProgramDto.getActive());
    programDto.setSupportActive(true);
    programDto.setSupportLocallyFulfilled(true);
    programDto.setSupportStartDate(LocalDate.of(2019, 9, 24));
    return programDto;
  }

  private FcIntegrationChanges getUpdatedFacilityDto(FcFacilityDto fcFacilityDto, Set<String> codes,
      FacilityDto originDto) {
    FacilityDto origin = facilityService.findOne(originDto.getId());
    originDto.setSupportedPrograms(origin.getSupportedPrograms());
    originDto.setDescription(origin.getDescription());
    boolean isSame = true;
    StringBuilder updateContent = new StringBuilder();
    StringBuilder originContent = new StringBuilder();
    if (!fcFacilityDto.getName().equals(originDto.getName())) {
      updateContent.append("name=").append(fcFacilityDto.getName()).append("; ");
      originContent.append("name=").append(originDto.getName()).append("; ");
      isSame = false;
    }

    if (!fcFacilityDto.getDescription().equals(originDto.getDescription())) {
      updateContent.append("description=").append(fcFacilityDto.getDescription()).append("; ");
      originContent.append("description=").append(originDto.getDescription()).append("; ");
      isSame = false;
    }

    if (!fcFacilityDto.getDistrictCode().equals(originDto.getGeographicZone().getCode())) {
      updateContent.append("districtCode=").append(fcFacilityDto.getDistrictCode()).append("; ");
      originContent.append("districtCode=").append(originDto.getGeographicZone().getCode()).append("; ");
      isSame = false;
    }

    if (!fcFacilityDto.getClientTypeCode().equals(originDto.getType().getCode())) {
      updateContent.append("clientTypeCode=").append(fcFacilityDto.getClientTypeCode()).append("; ");
      originContent.append("clientTypeCode=").append(originDto.getType().getCode()).append("; ");
      isSame = false;
    }

    if (!originDto.getActive().equals(FcUtil.isActive(fcFacilityDto.getStatus()))) {
      updateContent.append("status=").append(fcFacilityDto.getStatus()).append("; ");
      originContent.append("status=").append(originDto.getActive()).append("; ");
      isSame = false;
    }
    Set<String> originCodes = originDto.getSupportedPrograms().stream()
        .map(SupportedProgramDto::getCode)
        .collect(Collectors.toSet());
    if (!Sets.difference(codes, originCodes).isEmpty()) {
      updateContent.append("programCodes=").append(codes).append("; ");
      originContent.append("programCodes=").append(originCodes).append("; ");
      isSame = false;
    }
    if (isSame) {
      return null;
    }
    return FcUtil.buildUpdateFcIntegrationChanges(FACILITY_API, originDto.getCode(), updateContent.toString(),
        originContent.toString());
  }

  private void createFacilities(List<FcFacilityDto> needCreateFacilities,
      Map<String, FacilityTypeDto> codeToFacilityType,
      Map<String, ProgramDto> codeToProgramMap,
      Map<String, ProgramRealProgram> codeToRealProgramMap,
      Map<String, GeographicZoneDto> codeToGeographicZoneDtoMap) {
    List<FacilityDto> createdFacilities = newArrayList();
    needCreateFacilities.forEach(facilityDto -> {
      log.info("[FC facility] create: {}", facilityDto);
      List<SupportedProgramDto> supportedProgramDtos = getSupportedProgramDtos(facilityDto,
          codeToProgramMap, codeToRealProgramMap);
      FacilityDto createdFacility = facilityService.createFacility(getFacilityDto(facilityDto,
          codeToGeographicZoneDtoMap, codeToFacilityType, supportedProgramDtos));
      createdFacilities.add(createdFacility);
    });
    createNode(createdFacilities);
  }

  private void createNode(List<FacilityDto> facilities) {
    facilities.forEach(facility -> {
      Node node = new Node();
      node.setRefDataFacility(true);
      node.setReferenceId(facility.getId());
      nodeRepository.save(node);
    });
  }

  private FacilityDto getFacilityDto(FcFacilityDto fcFacilityDto,
      Map<String, GeographicZoneDto> codeToGeographicZoneDtoMap,
      Map<String, FacilityTypeDto> codeToFacilityType,
      List<SupportedProgramDto> supportedProgramDtos) {
    return FacilityDto.builder()
        .code(fcFacilityDto.getCode())
        .name(fcFacilityDto.getName())
        .description(fcFacilityDto.getDescription())
        .active(FcUtil.isActive(fcFacilityDto.getStatus()))
        .enabled(FcUtil.isActive(fcFacilityDto.getStatus()))
        .type(codeToFacilityType.get(fcFacilityDto.getClientTypeCode()))
        .geographicZone(codeToGeographicZoneDtoMap.get(fcFacilityDto.getDistrictCode()))
        .supportedPrograms(supportedProgramDtos)
        .build();
  }

  private void updateFacilities(List<FcFacilityDto> needUpdateFacilities,
      Map<String, FacilityDto> codeToFacilityMap,
      Map<String, FacilityTypeDto> codeToFacilityType,
      Map<String, ProgramDto> codeToProgramMap,
      Map<String, ProgramRealProgram> codeToRealProgramMap,
      Map<String, GeographicZoneDto> codeToGeographicZoneDtoMap) {
    needUpdateFacilities.forEach(current -> {
      FacilityDto existed = codeToFacilityMap.get(current.getCode());
      log.info("[FC facility] update, existed: {}, current: {}", existed, current);
      List<SupportedProgramDto> supportedProgramDtos = getUpdateProgramDto(current,
          existed, codeToRealProgramMap, codeToProgramMap);
      existed.setName(current.getName());
      existed.setDescription(current.getDescription());
      existed.setActive(FcUtil.isActive(current.getStatus()));
      existed.setEnabled(FcUtil.isActive(current.getStatus()));
      existed.setType(codeToFacilityType.get(current.getClientTypeCode()));
      existed.setGeographicZone(codeToGeographicZoneDtoMap.get(current.getDistrictCode()));
      existed.setSupportedPrograms(supportedProgramDtos);
      org.openlmis.referencedata.dto.FacilityDto saveFacility = FacilityDto.convert(existed);
      BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(saveFacility, "facilityDto");
      facilityController.saveFacility(saveFacility, existed.getId(), bindingResult);
    });
  }

  private List<SupportedProgramDto> getUpdateProgramDto(FcFacilityDto fcFacilityDto,
      FacilityDto needSaveFacility, Map<String, ProgramRealProgram> codeToRealProgramMap,
      Map<String, ProgramDto> codeToProgramMap) {
    List<SupportedProgramDto> supportedProgramDtos = new ArrayList<>();
    Map<String, SupportedProgramDto> codeToSupportProgram = needSaveFacility.getSupportedPrograms()
        .stream()
        .collect(Collectors.toMap(SupportedProgramDto::getCode, Function.identity()));
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
