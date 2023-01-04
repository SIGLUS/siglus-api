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

import static org.siglus.siglusapi.constant.FcConstants.FACILITY_TYPE_API;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResult;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.openlmis.referencedata.domain.BaseEntity;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.VersionIdentity;
import org.openlmis.referencedata.repository.FacilityTypeApprovedProductRepository;
import org.openlmis.referencedata.repository.FacilityTypeRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemReasonDto;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.siglus.siglusapi.domain.FcIntegrationChanges;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.fc.FcFacilityTypeDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultBuildDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusStockCardLineItemReasons;
import org.siglus.siglusapi.service.client.ValidReasonAssignmentStockManagementService;
import org.siglus.siglusapi.util.FcUtil;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcFacilityTypeService implements ProcessDataService {

  private final SiglusFacilityTypeReferenceDataService facilityTypeService;
  private final SiglusStockCardLineItemReasons siglusStockCardLineItemReasons;
  private final ProgramReferenceDataService programRefDataService;
  private final ValidReasonAssignmentStockManagementService assignmentService;

  private final FacilityTypeApprovedProductRepository facilityTypeApprovedProductRepository;
  private final ProgramRepository programRepository;
  private final FacilityTypeRepository facilityTypeRepository;

  @Override
  public FcIntegrationResultDto processData(List<? extends ResponseBaseDto> facilityTypes, String startDate,
      ZonedDateTime previousLastUpdatedAt) {
    log.info("[FC facilityType] sync facilityType count: {}", facilityTypes.size());
    if (facilityTypes.isEmpty()) {
      return null;
    }
    boolean finalSuccess = true;
    int createCounter = 0;
    int updateCounter = 0;
    List<FcIntegrationChanges> fcIntegrationChangesList = new ArrayList<>();
    try {
      if (CollectionUtils.isNotEmpty(facilityTypes)) {
        Map<String, FacilityTypeDto> codeToFacilityType = getCodeToFacilityType();
        List<FcFacilityTypeDto> createFacilityTypes = new ArrayList<>();
        List<FcFacilityTypeDto> updateFacilityTypes = new ArrayList<>();
        facilityTypes.forEach(item -> {
          FcFacilityTypeDto facilityType = (FcFacilityTypeDto) item;
          if (codeToFacilityType.containsKey(facilityType.getCode())) {
            FcIntegrationChanges updateChanges = getUpdatedFacilityType(codeToFacilityType, facilityType);
            if (updateChanges != null) {
              updateFacilityTypes.add(facilityType);
              fcIntegrationChangesList.add(updateChanges);
            }
          } else {
            createFacilityTypes.add(facilityType);
            FcIntegrationChanges createChanges = FcUtil
                .buildCreateFcIntegrationChanges(FACILITY_TYPE_API, facilityType.getCode(), facilityType.toString());
            fcIntegrationChangesList.add(createChanges);
          }
        });
        createFacilityTypes(codeToFacilityType, createFacilityTypes);
        updateFacilityTypes(codeToFacilityType, updateFacilityTypes);
        createCounter = createFacilityTypes.size();
        updateCounter = updateFacilityTypes.size();
      }
    } catch (Exception e) {
      log.error("[FC facilityType] sync facilityType error", e);
      finalSuccess = false;
    }
    log.info("[FC facilityType] process product data create: {}, update: {}, same: {}",
        createCounter, updateCounter, facilityTypes.size() - createCounter - updateCounter);
    return buildResult(
        new FcIntegrationResultBuildDto(FACILITY_TYPE_API, facilityTypes, startDate, previousLastUpdatedAt,
            finalSuccess, createCounter, updateCounter, null, fcIntegrationChangesList));
  }

  private Map<String, FacilityTypeDto> getCodeToFacilityType() {
    List<FacilityTypeDto> facilityTypeDtos = facilityTypeService.searchAllFacilityTypes();
    return facilityTypeDtos.stream().collect(Collectors.toMap(
        FacilityTypeDto::getCode,
        Function.identity()));
  }

  private FcIntegrationChanges getUpdatedFacilityType(Map<String, FacilityTypeDto> codeToFacilityType,
      FcFacilityTypeDto typeDto) {
    FacilityTypeDto facilityTypeDto = codeToFacilityType.get(typeDto.getCode());
    boolean isSame = true;
    StringBuilder updateContent = new StringBuilder();
    StringBuilder originContent = new StringBuilder();
    if (!facilityTypeDto.getName().equals(typeDto.getDescription())) {
      updateContent.append("name=").append(typeDto.getDescription()).append("; ");
      originContent.append("name=").append(facilityTypeDto.getName()).append("; ");
      isSame = false;
    }
    if (!facilityTypeDto.getActive().equals(FcUtil.isActive(typeDto.getStatus()))) {
      updateContent.append("status=").append(FcUtil.isActive(typeDto.getStatus())).append("; ");
      originContent.append("status=").append(facilityTypeDto.getActive()).append("; ");
      isSame = false;
    }
    if (isSame) {
      return null;
    }
    return FcUtil.buildUpdateFcIntegrationChanges(FACILITY_TYPE_API, facilityTypeDto.getCode(),
        updateContent.toString(), originContent.toString());
  }

  private void updateFacilityTypes(Map<String, FacilityTypeDto> codeToFacilityType,
      List<FcFacilityTypeDto> currentFacilityTypes) {

    List<UUID> needDeletedFacilityTypeId = new ArrayList<>();
    List<UUID> needCreatedFacilityTypeId = new ArrayList<>();

    currentFacilityTypes.forEach(current -> {
      FacilityTypeDto existed = codeToFacilityType.get(current.getCode());
      log.info("[FC facilityType] update facilityType, existed: {}, current: {}", existed, current);

      if (BooleanUtils.isTrue(existed.getActive()) && !FcUtil.isActive(current.getStatus())) {
        needDeletedFacilityTypeId.add(existed.getId());
      } else if (BooleanUtils.isFalse(existed.getActive()) && FcUtil.isActive(current.getStatus())) {
        needCreatedFacilityTypeId.add(existed.getId());
      }

      existed.setActive(FcUtil.isActive(current.getStatus()));
      existed.setName(current.getDescription());
      facilityTypeService.saveFacilityType(existed);
    });

    if (!needDeletedFacilityTypeId.isEmpty()) {
      facilityTypeApprovedProductRepository.deleteFtapWhenFacilityIsNotActive(needDeletedFacilityTypeId);
    }
    if (!needCreatedFacilityTypeId.isEmpty()) {
      insertIntoFtap(needCreatedFacilityTypeId);
    }
  }

  private void createFacilityTypes(Map<String, FacilityTypeDto> codeToFacilityType,
      List<FcFacilityTypeDto> needAddedFacilityTypes) {
    int originSize = codeToFacilityType.size();
    List<FacilityTypeDto> needUpdateReasonType = new ArrayList<>();

    List<UUID> needAddedFacilityTypesId = new ArrayList<>();

    needAddedFacilityTypes.forEach(typeDto -> {
      log.info("[FC facilityType] create facilityType: {}", typeDto);
      int index = needAddedFacilityTypes.indexOf(typeDto);
      FacilityTypeDto dto = new FacilityTypeDto();
      dto.setCode(typeDto.getCode());
      dto.setActive(FcUtil.isActive(typeDto.getStatus()));
      dto.setName(typeDto.getDescription());
      dto.setDisplayOrder(originSize + index + 1);

      FacilityTypeDto facilityTypeWithId = facilityTypeService.createFacilityType(dto);
      needAddedFacilityTypesId.add(facilityTypeWithId.getId());

      needUpdateReasonType.add(facilityTypeWithId);
    });
    updateReason(needUpdateReasonType);
    insertIntoFtap(needAddedFacilityTypesId);
  }

  private void insertIntoFtap(List<UUID> needAddedFacilityTypesId) {
    List<String> orderableIdAndProgramIdInFtap = facilityTypeApprovedProductRepository
        .findOrderableIdAndProgramIdInFtap();
    List<Program> programs = programRepository.findAll();
    Map<UUID, Program> programIdToProgram = programs.stream()
        .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));
    List<FacilityType> facilityTypes = facilityTypeRepository.findAll();
    Map<UUID, FacilityType> facilityTypeIdToFacilityType = facilityTypes.stream()
        .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

    List<FacilityTypeApprovedProduct> needAddedFtap = new ArrayList<>();
    needAddedFacilityTypesId.forEach(facilityTypeId -> orderableIdAndProgramIdInFtap.forEach(oapId -> {
      String orderableId = oapId.split("\\+")[0];
      String programId = oapId.split("\\+")[1];
      VersionIdentity versionIdentity = new VersionIdentity(UUID.randomUUID(), 1L);
      FacilityTypeApprovedProduct facilityTypeApprovedProduct = new FacilityTypeApprovedProduct(
          versionIdentity,
          UUID.fromString(orderableId),
          programIdToProgram.get(UUID.fromString(programId)),
          facilityTypeIdToFacilityType.get(facilityTypeId),
          3.0,
          null,
          null,
          true,
          ZonedDateTime.now()
      );
      needAddedFtap.add(facilityTypeApprovedProduct);
    }));

    facilityTypeApprovedProductRepository.save(needAddedFtap);
  }

  private void updateReason(List<FacilityTypeDto> facilityTypeDtos) {
    Collection<StockCardLineItemReasonDto> reasonDtos = siglusStockCardLineItemReasons.findAll();
    List<ProgramDto> programs = programRefDataService.findAll();
    List<ValidReasonAssignmentDto> assignmentDtos = new ArrayList<>();
    facilityTypeDtos.forEach(facilityTypeDto ->
        programs.forEach(programDto -> configureProgram(facilityTypeDto, programDto, reasonDtos, assignmentDtos)));
    assignmentDtos.forEach(assignmentService::assignReason);
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
