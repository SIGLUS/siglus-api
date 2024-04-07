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

package org.siglus.siglusapi.service;

import static java.util.Collections.emptyList;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_PROGRAM_NOT_SUPPORTED;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.AC;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.CENTRAL;
import static org.siglus.siglusapi.constant.FieldConstants.ALL_PROGRAM;
import static org.siglus.siglusapi.constant.FieldConstants.EXCLUDE_ARCHIVED;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;
import static org.siglus.siglusapi.constant.FieldConstants.PROGRAM_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.FieldConstants.SEPARATOR;
import static org.siglus.siglusapi.constant.FieldConstants.SINGLE_PROGRAM;
import static org.siglus.siglusapi.constant.FieldConstants.STOCK_CARD_ID;
import static org.siglus.siglusapi.constant.FieldConstants.VM_STATUS;
import static org.siglus.siglusapi.constant.LocationConstants.VIRTUAL_LOCATION_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_UUID;
import static org.siglus.siglusapi.constant.ProgramConstants.MMC_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.VIA_PROGRAM_CODE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_INVENTORY_CONFLICT_DRAFT;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_ACCEPTABLE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SPLIT_NUM_TOO_LARGE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.web.PhysicalInventoryController;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.PhysicalInventoryEmptyLocationLineItem;
import org.siglus.siglusapi.domain.PhysicalInventoryExtension;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.siglus.siglusapi.domain.PhysicalInventorySubDraft;
import org.siglus.siglusapi.dto.DraftListDto;
import org.siglus.siglusapi.dto.InitialInventoryFieldDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.PhysicalInventoryLineItemExtensionDto;
import org.siglus.siglusapi.dto.PhysicalInventorySubDraftLineItemsExtensionDto;
import org.siglus.siglusapi.dto.PhysicalInventoryValidationDto;
import org.siglus.siglusapi.dto.SiglusPhysicalInventoryDto;
import org.siglus.siglusapi.dto.SubDraftDto;
import org.siglus.siglusapi.dto.enums.LocationManagementOption;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryEmptyLocationLineItemRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.CustomListSortHelper;
import org.siglus.siglusapi.util.LocalMachineHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.PreserveStackTrace"})
public class SiglusPhysicalInventoryService {

  private final PhysicalInventoriesRepository physicalInventoriesRepository;
  private final StockCardRepository stockCardRepository;
  private final SupportedProgramsHelper supportedProgramsHelper;
  private final PhysicalInventoryLineItemsExtensionRepository lineItemsExtensionRepository;
  private final PhysicalInventoryController inventoryController;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final OrderableRepository orderableRepository;
  private final RequisitionService requisitionService;
  private final PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;
  private final SiglusStockCardSummariesService siglusStockCardSummariesService;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final PhysicalInventoryExtensionRepository physicalInventoryExtensionRepository;
  private final CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;
  private final SiglusStockCardRepository siglusStockCardRepository;
  private final FacilityLocationsRepository facilityLocationsRepository;
  private final PhysicalInventoryEmptyLocationLineItemRepository physicalInventoryEmptyLocationLineItemRepository;
  private final SiglusOrderableService siglusOrderableService;

  private final SiglusProgramService siglusProgramService;

  private final SiglusArchiveProductService archiveProductService;

  private final LocalMachineHelper localMachineHelper;

  @Transactional
  public PhysicalInventoryDto createAndSplitNewDraftForAllPrograms(PhysicalInventoryDto physicalInventoryDto,
      Integer splitNum, boolean initialPhysicalInventory, String optionString, boolean isByLocation) {
    if (!canInitialOrPhysicalInventory(physicalInventoryDto.getFacilityId(), initialPhysicalInventory)) {
      throw new ValidationMessageException(new Message(ERROR_NOT_ACCEPTABLE));
    }
    LocationManagementOption option = null;
    if (StringUtils.isNotEmpty(optionString)) {
      option = LocationManagementOption.fromString(optionString);
    }
    PhysicalInventoryDto physicalInventoryDraft = createNewDraftForAllPrograms(physicalInventoryDto, option);
    buildPhysicalInventoryLineItemsForAllPrograms(physicalInventoryDraft, initialPhysicalInventory, option != null);
    splitPhysicalInventory(physicalInventoryDraft, splitNum, isByLocation);
    return physicalInventoryDraft;
  }

  @Transactional
  public PhysicalInventoryDto createAndSpiltNewDraftForOneProgram(PhysicalInventoryDto physicalInventoryDto,
      Integer splitNum, String optionString, boolean isByLocation) {
    LocationManagementOption option = null;
    if (StringUtils.isNotEmpty(optionString)) {
      option = LocationManagementOption.fromString(optionString);
    }
    if (!canPhysicalInventory(physicalInventoryDto.getFacilityId())) {
      throw new ValidationMessageException(new Message(ERROR_NOT_ACCEPTABLE));
    }
    PhysicalInventoryDto physicalInventory = createNewDraft(physicalInventoryDto);

    PhysicalInventoryExtension physicalInventoryExtension = buildPhysicalInventoryExtension(
        physicalInventory, false, option);
    log.info("physical inventory extension input for one program: {}", physicalInventoryExtension);
    physicalInventoryExtensionRepository.save(physicalInventoryExtension);

    physicalInventory = buildPhysicalInventoryLineItemsForOneProgram(physicalInventory, false, option != null);
    splitPhysicalInventory(physicalInventory, splitNum, isByLocation);
    return physicalInventory;
  }

  public DraftListDto getSubDraftListForAllPrograms(UUID facility, Boolean isDraft) {
    Set<UUID> supportedPrograms = getSupportedPrograms();
    List<PhysicalInventorySubDraft> allProductSubDraftList = new LinkedList<>();
    supportedPrograms.forEach(programId -> {
      List<PhysicalInventoryDto> physicalInventoryDtoList = getPhysicalInventoryDtos(programId, facility, isDraft);
      List<UUID> physicalInventoryIds = physicalInventoryDtoList
          .stream()
          .map(PhysicalInventoryDto::getId)
          .collect(Collectors.toList());
      List<PhysicalInventorySubDraft> subDraftList = physicalInventorySubDraftRepository.findByPhysicalInventoryIdIn(
          physicalInventoryIds);
      allProductSubDraftList.addAll(subDraftList);
    });
    if (CollectionUtils.isEmpty(allProductSubDraftList)) {
      throw new IllegalArgumentException("there is no subDraft for any record");
    }
    return DraftListDto
        .builder()
        .physicalInventoryId(ALL_PRODUCTS_UUID)
        .subDrafts(convertSubDraftToSubDraftDto(allProductSubDraftList))
        .canMergeOrDeleteDrafts(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts())
        .canSubmitDrafts(isTheAllSubDraftIsSubmitted(allProductSubDraftList))
        .build();
  }

  public PhysicalInventoryValidationDto checkConflictForAllPrograms(UUID facility) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    if (CollectionUtils.isEmpty(supportedPrograms)) {
      throw new PermissionMessageException(new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED));
    }

    List<UUID> conflictProgramIdList = Lists.newArrayList();
    supportedPrograms.forEach(supportedProgramId -> {
      List<PhysicalInventory> programIsDraft = physicalInventoriesRepository
          .findByProgramIdAndFacilityIdAndIsDraft(supportedProgramId, facility, true);
      if (CollectionUtils.isNotEmpty(programIsDraft)) {
        List<PhysicalInventoryExtension> programWithDraftList = physicalInventoryExtensionRepository
            .findByPhysicalInventoryId(programIsDraft.get(0).getId());
        if (CollectionUtils.isNotEmpty(programWithDraftList) && SINGLE_PROGRAM.equals(programWithDraftList
            .get(0).getCategory())) {
          conflictProgramIdList.add(programIsDraft.get(0).getProgramId());
        }
      }
    });

    if (CollectionUtils.isNotEmpty(conflictProgramIdList)) {
      return buildPhysicalInventoryValidationDto(false, conflictProgramIdList);
    }
    return buildPhysicalInventoryValidationDto(true, Lists.newArrayList());
  }

  public List<PhysicalInventoryLineItemDto> buildInitialInventoryLineItems(
      Set<UUID> supportedVirtualProgramIds, UUID facilityId) {
    return supportedVirtualProgramIds.stream()
        .map(programId ->
            requisitionService.getApprovedProductsWithoutAdditional(facilityId, programId)
                .stream()
                .map(ApprovedProductDto::getOrderable)
                .filter(o -> o.getExtraData().containsKey(IS_BASIC))
                .filter(o -> Boolean.parseBoolean(o.getExtraData().get(IS_BASIC)))
                .map(o -> newLineItem(o.getId(), programId))
                .collect(Collectors.toList())
        )
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  public PhysicalInventoryDto getSubPhysicalInventoryDtoBySubDraftId(List<UUID> subDraftIds) {
    if (CollectionUtils.isEmpty(subDraftIds)) {
      throw new IllegalArgumentException("empty subDraftIds");
    }
    List<PhysicalInventoryLineItemDto> subPhysicalInventoryLineItemDtoList =
        getSubPhysicalInventoryLineItemListBySubDraftIds(subDraftIds);
    List<PhysicalInventoryLineItemDto> sortedSubPhysicalInventoryLineItemList = subPhysicalInventoryLineItemDtoList
        .stream().sorted(Comparator.comparing(o -> String.valueOf(
            orderableRepository.findLatestById(o.getOrderableId())
                .orElseThrow(IllegalArgumentException::new)
                .getProductCode()))).collect(Collectors.toList());
    PhysicalInventoryDto physicalInventory = getPhysicalInventoryBySubDraftId(subDraftIds.get(0));

    UUID programId = subDraftIds.size() > 1 ? ALL_PRODUCTS_PROGRAM_ID : physicalInventory.getProgramId();
    UUID physicalInventoryId = subDraftIds.size() > 1 ? ALL_PRODUCTS_UUID : physicalInventory.getId();
    physicalInventory.setId(physicalInventoryId);
    physicalInventory.setProgramId(programId);
    physicalInventory.setLineItems(sortedSubPhysicalInventoryLineItemList);
    return physicalInventory;
  }

  public SiglusPhysicalInventoryDto getSubLocationPhysicalInventoryDtoBySubDraftId(
      List<UUID> subDraftIds, boolean isByLocation) {
    if (CollectionUtils.isEmpty(subDraftIds)) {
      throw new IllegalArgumentException("empty subDraftIds");
    }
    List<PhysicalInventoryLineItemDto> subPhysicalInventoryLineItemDtoList =
        getSubPhysicalInventoryLineItemListBySubDraftIds(subDraftIds);
    List<PhysicalInventoryLineItemDto> sortedSubPhysicalInventoryLineItemList;
    if (isByLocation) {
      subPhysicalInventoryLineItemDtoList.addAll(findEmptyLocationsBySubDraftIds(subDraftIds));
      sortedSubPhysicalInventoryLineItemList = subPhysicalInventoryLineItemDtoList
          .stream()
          .sorted(Comparator.comparing(PhysicalInventoryLineItemDto::getLocationCode))
          .collect(Collectors.toList());
    } else {
      sortedSubPhysicalInventoryLineItemList = subPhysicalInventoryLineItemDtoList
          .stream().sorted(Comparator.comparing(
              o -> String.valueOf(
                  orderableRepository.findLatestById(o.getOrderableId()).orElseThrow(IllegalArgumentException::new)
                      .getProductCode()))).collect(Collectors.toList());
    }

    SiglusPhysicalInventoryDto physicalInventory = fillLocationOption(
        getPhysicalInventoryBySubDraftId(subDraftIds.get(0)));

    UUID programId = subDraftIds.size() > 1 ? ALL_PRODUCTS_PROGRAM_ID : physicalInventory.getProgramId();
    UUID physicalInventoryId = subDraftIds.size() > 1 ? ALL_PRODUCTS_UUID : physicalInventory.getId();
    physicalInventory.setId(physicalInventoryId);
    physicalInventory.setProgramId(programId);
    physicalInventory.setLineItems(sortedSubPhysicalInventoryLineItemList);
    return physicalInventory;
  }

  public PhysicalInventoryValidationDto checkConflictForOneProgram(UUID facility, UUID program, UUID draft) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    if (CollectionUtils.isEmpty(supportedPrograms) || !supportedPrograms.contains(program)) {
      throw new PermissionMessageException(new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED));
    }
    List<PhysicalInventory> programHaveDraft = physicalInventoriesRepository
        .findByProgramIdAndFacilityIdAndIsDraft(program, facility, true);
    if (ObjectUtils.isEmpty(draft)) {
      if (CollectionUtils.isNotEmpty(programHaveDraft)) {
        return buildPhysicalInventoryValidationDto(false, Lists.newArrayList(program));
      }
    } else {
      if (programHaveDraft.size() != 1) {
        return buildPhysicalInventoryValidationDto(false, Lists.newArrayList(program));
      }
      PhysicalInventory physicalInventory = programHaveDraft.get(0);
      if (!physicalInventory.getId().equals(draft)) {
        throw new BusinessDataException(
            Message.createFromMessageKeyStr("facility, program and draft mismatch"));
      }
    }
    return buildPhysicalInventoryValidationDto(true, Lists.newArrayList());
  }

  public List<List<PhysicalInventoryLineItemDto>> groupByProductCode(List<PhysicalInventoryLineItemDto> lineItemDtos) {
    List<List<PhysicalInventoryLineItemDto>> list = new LinkedList<>();
    Map<UUID, String> orderableIdToCode = siglusOrderableService.getAllProducts()
        .stream()
        .collect(Collectors.toMap(OrderableDto::getId, OrderableDto::getProductCode));
    Map<String, List<PhysicalInventoryLineItemDto>> orderableCodeToLineItems = lineItemDtos
        .stream()
        .collect(Collectors.groupingBy(item -> orderableIdToCode.get(item.getOrderableId())));
    orderableCodeToLineItems.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> list.add(entry.getValue()));
    return list;
  }

  public DraftListDto getSubDraftListForOneProgram(UUID program, UUID facility, Boolean isDraft) {
    List<PhysicalInventoryDto> physicalInventoryDtoList = getPhysicalInventoryDtos(program, facility, isDraft);
    if (CollectionUtils.isNotEmpty(physicalInventoryDtoList)) {
      UUID physicalInventoryId = physicalInventoryDtoList.get(0).getId();
      List<PhysicalInventorySubDraft> physicalInventorySubDraftList = physicalInventorySubDraftRepository
          .findByPhysicalInventoryId(physicalInventoryId);
      if (CollectionUtils.isEmpty(physicalInventorySubDraftList)) {
        throw new IllegalArgumentException(
            "there is no matching subDraft for the physicalInventoryId : " + physicalInventoryId);
      }
      return DraftListDto
          .builder()
          .subDrafts(convertSubDraftToSubDraftDto(physicalInventorySubDraftList))
          .physicalInventoryId(physicalInventoryId)
          .canMergeOrDeleteDrafts(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts())
          .canSubmitDrafts(isTheAllSubDraftIsSubmitted(physicalInventorySubDraftList))
          .build();
    }
    return DraftListDto.builder().build();
  }

  public PhysicalInventoryDto saveDraftForProductsForOneProgram(PhysicalInventoryDto dto) {
    saveDraft(dto, dto.getId());
    PhysicalInventoryDto afterSavedDto = getPhysicalInventory(dto.getId());
    fillLineItemIdForInitiate(dto, afterSavedDto);
    List<PhysicalInventoryDto> physicalInventoryDtos = Collections.singletonList(dto);
    if (CollectionUtils.isNotEmpty(physicalInventoryDtos)) {
      return getResultInventory(physicalInventoryDtos, updateExtension(dto, physicalInventoryDtos));
    }
    return null;
  }


  public PhysicalInventoryDto saveDraftForProductsForOneProgramWithExtension(
      PhysicalInventoryLineItemExtensionDto dto) {
    saveDraft(dto, dto.getId());
    PhysicalInventoryDto afterSavedDto = getPhysicalInventory(dto.getId());
    fillLineItemId(dto, afterSavedDto);
    List<PhysicalInventoryDto> physicalInventoryDtos = Collections.singletonList(dto);
    if (CollectionUtils.isNotEmpty(physicalInventoryDtos)) {
      return getResultInventory(physicalInventoryDtos, updateExtensionWithSubDraft(dto, physicalInventoryDtos));
    }
    return null;
  }

  public PhysicalInventoryDto saveDraftForAllPrograms(PhysicalInventoryDto dto) {
    deletePhysicalInventoryDraftForAllPrograms(dto.getFacilityId());
    createNewDraftForAllPrograms(dto, null);
    Set<UUID> programIds = dto.getLineItems()
        .stream()
        .map(PhysicalInventoryLineItemDto::getProgramId)
        .collect(Collectors.toSet());
    List<PhysicalInventoryDto> inventories = fetchPhysicalInventories(programIds, dto.getFacilityId(),
        Boolean.TRUE);
    inventories.forEach(inventory -> inventory.setLineItems(
        dto.getLineItems().stream()
            .filter(lineItem -> lineItem.getProgramId().equals(inventory.getProgramId()))
            .collect(Collectors.toList())));
    inventories = inventories.stream()
        .map(inventory -> saveDraft(inventory, inventory.getId()))
        .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(inventories)) {
      return getResultInventoryForAllPrograms(inventories, updateExtension(dto, inventories));
    }
    return null;
  }

  public void checkDraftIsExist(UUID facilityId) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    List<PhysicalInventoryDto> inventories = supportedPrograms.stream()
        .map(programId -> getPhysicalInventoryDtos(programId, facilityId, Boolean.TRUE))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(inventories)) {
      throw new ValidationMessageException("stockmanagement.error.physicalInventory.isSubmitted");
    }
  }

  public void deletePhysicalInventoryDraft(UUID id) {
    inventoryController.deletePhysicalInventory(id);
    physicalInventorySubDraftRepository.deletePhysicalInventorySubDraftsByPhysicalInventoryId(id);
    List<UUID> subDraftIds = physicalInventorySubDraftRepository.findByPhysicalInventoryId(id)
        .stream()
        .map(BaseEntity::getId)
        .collect(Collectors.toList());
    physicalInventoryEmptyLocationLineItemRepository
        .deletePhysicalInventoryEmptyLocationLineItemsBySubDraftIdIn(subDraftIds);
  }

  public void deletePhysicalInventoryDraftForOneProgram(UUID facilityId, UUID programId) {
    doDeletePhysicalInventoryForProductInOneProgram(facilityId, programId);
  }

  public void deletePhysicalInventoryDraftForAllPrograms(UUID facilityId) {
    doDeletePhysicalInventoryForAllPrograms(facilityId);
  }

  public PhysicalInventoryDto getPhysicalInventory(UUID id) {
    return inventoryController.getPhysicalInventory(id);
  }

  public PhysicalInventoryDto getFullPhysicalInventoryDto(UUID physicalInventoryId) {
    PhysicalInventoryDto physicalInventoryDto = getPhysicalInventory(
        physicalInventoryId);
    List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(Lists.newArrayList(physicalInventoryId));
    return getResultInventory(Lists.newArrayList(physicalInventoryDto), extensions);
  }

  public PhysicalInventoryDto getPhysicalInventoryForAllPrograms(UUID facilityId) {
    List<PhysicalInventoryDto> inventories = getPhysicalInventoryDtosForAllPrograms(facilityId, Boolean.TRUE, false);
    if (CollectionUtils.isNotEmpty(inventories)) {
      return inventories.get(0);
    }
    return null;
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtos(UUID program, UUID facility, Boolean isDraft) {
    return inventoryController.searchPhysicalInventory(program, facility, isDraft).getBody();
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosForProductsForOneProgram(UUID programId, UUID facilityId,
      Boolean isDraft, boolean isByLocation) {
    Set<UUID> supportedPrograms = Collections.singleton(programId);
    List<PhysicalInventoryDto> inventories = fetchPhysicalInventories(supportedPrograms, facilityId, isDraft);
    if (CollectionUtils.isNotEmpty(inventories)) {
      List<UUID> updatePhysicalInventoryIds =
          inventories
              .stream()
              .map(PhysicalInventoryDto::getId)
              .collect(Collectors.toList());
      log.info("find physical inventory extension in one program: {}", updatePhysicalInventoryIds);
      List<PhysicalInventoryLineItemsExtension> extensions =
          lineItemsExtensionRepository.findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
      PhysicalInventoryDto resultInventory = getResultInventory(inventories, extensions);
      if (isByLocation) {
        List<PhysicalInventoryLineItemDto> emptyLocationPhysicalInventoryLineItemDtos = getEmptyLocations(facilityId);
        resultInventory.getLineItems().addAll(emptyLocationPhysicalInventoryLineItemDtos);
      }
      return Collections.singletonList(resultInventory);
    }
    return Collections.emptyList();
  }

  public List<SiglusPhysicalInventoryDto> getLocationPhysicalInventoryDtosForProductsForOneProgram(
      UUID programId, UUID facilityId, Boolean isDraft, boolean isByLocation) {
    List<PhysicalInventoryDto> dtos =
        getPhysicalInventoryDtosForProductsForOneProgram(programId, facilityId, isDraft, isByLocation);
    if (dtos.isEmpty()) {
      return Collections.emptyList();
    }
    PhysicalInventoryDto physicalInventoryDto = dtos.get(0);
    return Collections.singletonList(fillLocationOption(physicalInventoryDto));
  }

  public List<SiglusPhysicalInventoryDto> getLocationPhysicalInventoryDtosForAllPrograms(
      UUID facilityId, Boolean isDraft, boolean isByLocation) {
    List<PhysicalInventoryDto> physicalInventoryDtosForAllPrograms = getPhysicalInventoryDtosForAllPrograms(facilityId,
        isDraft, isByLocation);
    if (physicalInventoryDtosForAllPrograms.isEmpty()) {
      return Collections.emptyList();
    }
    PhysicalInventoryDto physicalInventoryDto = physicalInventoryDtosForAllPrograms.get(0);
    return Collections.singletonList(fillLocationOption(physicalInventoryDto));
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosForAllPrograms(UUID facilityId, Boolean isDraft,
      boolean isByLocation) {
    try {
      Set<UUID> supportedPrograms = getSupportedPrograms();
      if (CollectionUtils.isEmpty(supportedPrograms)) {
        throw new PermissionMessageException(
            new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED, ALL_PRODUCTS_PROGRAM_ID));
      }
      List<PhysicalInventoryDto> inventories = fetchPhysicalInventories(supportedPrograms, facilityId, isDraft);
      if (CollectionUtils.isNotEmpty(inventories)) {
        List<UUID> updatePhysicalInventoryIds =
            inventories
                .stream()
                .map(PhysicalInventoryDto::getId)
                .collect(Collectors.toList());
        log.info("find physical inventory extension: {}", updatePhysicalInventoryIds);
        List<PhysicalInventoryLineItemsExtension> extensions =
            lineItemsExtensionRepository.findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
        PhysicalInventoryDto resultInventory = getResultInventoryForAllPrograms(inventories, extensions);
        if (isByLocation) {
          List<PhysicalInventoryLineItemDto> emptyLocationPhysicalInventoryLineItemDtos = getEmptyLocations(facilityId);
          resultInventory.getLineItems().addAll(emptyLocationPhysicalInventoryLineItemDtos);
        }
        return Collections.singletonList(resultInventory);
      }

      return inventories;
    } catch (PermissionMessageException e) {
      throw new PermissionMessageException(
          new org.openlmis.stockmanagement.util.Message(ERROR_PERMISSION_NOT_SUPPORTED), e);
    }
  }

  public Set<String> findPhysicalInventoryDates(UUID programId, UUID facility,
      String startDate,
      String endDate) {
    List<PhysicalInventory> physicalInventories =
        physicalInventoriesRepository.findByProgramIdAndFacilityIdAndStartDateAndEndDate(programId,
            facility, startDate, endDate);
    return physicalInventories
        .stream()
        .map(physicalInventory -> physicalInventory.getOccurredDate().toString())
        .collect(Collectors.toSet());
  }

  public PhysicalInventoryDto findLatestPhysicalInventory(UUID facilityId, UUID programId) {
    UUID mmcProgramId = siglusProgramService.getProgramByCode(MMC_PROGRAM_CODE)
            .orElseThrow(() -> new NotFoundException("MMC program not found"))
            .getId();
    PhysicalInventory latest = physicalInventoriesRepository
            .findTopByProgramIdAndFacilityIdAndIsDraftOrderByOccurredDateDesc(programId, facilityId, false);
    if (mmcProgramId.equals(programId)) {
      UUID viaProgramId = siglusProgramService.getProgramByCode(VIA_PROGRAM_CODE)
              .orElseThrow(() -> new NotFoundException("VIA program not found"))
              .getId();

      if (latest == null) {
        return findLatestPhysicalInventory(facilityId, viaProgramId);
      } else {
        PhysicalInventory latestForVia = physicalInventoriesRepository
                .findTopByProgramIdAndFacilityIdAndIsDraftOrderByOccurredDateDesc(viaProgramId, facilityId, false);
        if (latestForVia != null && latestForVia.getOccurredDate().isAfter(latest.getOccurredDate())) {
          return PhysicalInventoryDto.builder()
                  .programId(latestForVia.getProgramId())
                  .facilityId(latestForVia.getFacilityId())
                  .isDraft(latestForVia.getIsDraft())
                  .occurredDate(latestForVia.getOccurredDate())
                  .documentNumber(latestForVia.getDocumentNumber())
                  .signature(latestForVia.getSignature())
                  .build();
        }
      }

    }

    if (latest == null) {
      return null;
    }
    return PhysicalInventoryDto.builder()
            .programId(latest.getProgramId())
            .facilityId(latest.getFacilityId())
            .isDraft(latest.getIsDraft())
            .occurredDate(latest.getOccurredDate())
            .documentNumber(latest.getDocumentNumber())
            .signature(latest.getSignature())
            .build();
  }

  public InitialInventoryFieldDto canInitialInventory(UUID facility) {
    return new InitialInventoryFieldDto(!isVirtualFacility(facility) && isStockCardCountEmpty(facility));
  }

  private List<PhysicalInventoryLineItemDto> findEmptyLocationsBySubDraftIds(List<UUID> subDraftIds) {
    List<PhysicalInventoryEmptyLocationLineItem> physicalInventoryEmptyLocationLineItem =
        physicalInventoryEmptyLocationLineItemRepository.findBySubDraftIdIn(subDraftIds);
    List<PhysicalInventoryEmptyLocationLineItem> visualEmptyLocations = physicalInventoryEmptyLocationLineItem.stream()
        .filter(lineItem -> !lineItem.isHasProduct()).collect(Collectors.toList());
    return convertEmptyLocationToPhysicalInventoryLineItemDto(visualEmptyLocations);
  }

  private List<PhysicalInventoryLineItemDto> convertEmptyLocationToPhysicalInventoryLineItemDto(
      List<PhysicalInventoryEmptyLocationLineItem> emptyLocations) {
    return emptyLocations.stream()
        .map(location -> PhysicalInventoryLineItemDto.builder().locationCode(location.getLocationCode())
            .area(location.getArea()).skipped(location.isSkipped()).build()).collect(Collectors.toList());
  }

  private SiglusPhysicalInventoryDto fillLocationOption(PhysicalInventoryDto physicalInventoryDto) {
    SiglusPhysicalInventoryDto dto = new SiglusPhysicalInventoryDto();
    BeanUtils.copyProperties(physicalInventoryDto, dto);

    UUID physicalInventoryId = physicalInventoryDto.getId();
    if (physicalInventoryDto.getProgramId() == ALL_PRODUCTS_PROGRAM_ID) {
      UUID programId = supportedProgramsHelper.findHomeFacilitySupportedProgramIds().iterator().next();
      physicalInventoryId = UUID.fromString(physicalInventoriesRepository
          .findIdByProgramIdAndFacilityIdAndIsDraft(programId, physicalInventoryDto.getFacilityId(), true));
    }
    List<PhysicalInventoryExtension> extensions = physicalInventoryExtensionRepository
        .findByPhysicalInventoryId(physicalInventoryId);
    if (CollectionUtils.isNotEmpty(extensions) && null != extensions.get(0).getLocationOption()) {
      dto.setLocationOption(extensions.get(0).getLocationOption().getValue());
    }
    return dto;
  }

  private PhysicalInventoryDto getPhysicalInventoryBySubDraftId(UUID subDraftId) {
    PhysicalInventorySubDraft subDraft = physicalInventorySubDraftRepository.findFirstById(subDraftId);
    PhysicalInventoryDto physicalInventory = getPhysicalInventory(subDraft.getPhysicalInventoryId());
    return getPhysicalInventoryDtosForProductsForOneProgram(
        physicalInventory.getProgramId(), physicalInventory.getFacilityId(), true, false).get(0);
  }

  private List<PhysicalInventoryLineItemDto> getSubPhysicalInventoryLineItemListBySubDraftIds(List<UUID> subDraftIds) {
    List<PhysicalInventoryLineItemDto> allSubPhysicalInventoryLineItemDtoList = new LinkedList<>();
    subDraftIds.forEach(subDraftId -> {
      PhysicalInventoryDto physicalInventoryDto = getPhysicalInventoryBySubDraftId(subDraftId);
      if (physicalInventoryDto == null) {
        return;
      }
      List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
          .findByPhysicalInventoryId(physicalInventoryDto.getId());
      List<PhysicalInventoryLineItemDto> subPhysicalInventoryLineItemDtoLists = physicalInventoryDto.getLineItems()
          .stream().filter(lineItem -> {
            PhysicalInventoryLineItemsExtension extension = getExtension(extensions, lineItem);
            return extension != null && subDraftId.equals(extension.getSubDraftId());
          }).collect(Collectors.toList());
      allSubPhysicalInventoryLineItemDtoList.addAll(subPhysicalInventoryLineItemDtoLists);
    });
    return allSubPhysicalInventoryLineItemDtoList;
  }

  private void createEmptySubDraft(Integer spiltNum, PhysicalInventoryDto physicalInventoryDto) {
    if (physicalInventoryDto.getProgramId().equals(ALL_PRODUCTS_PROGRAM_ID)) {
      Set<UUID> supportedPrograms = getSupportedPrograms();
      supportedPrograms.forEach(programId -> {
        UUID physicalInventoryId = UUID.fromString(physicalInventoriesRepository
            .findIdByProgramIdAndFacilityIdAndIsDraft(programId, physicalInventoryDto.getFacilityId(), true));
        doCreateEmptySubDraft(spiltNum, physicalInventoryId);
      });
    } else {
      UUID physicalInventoryId = physicalInventoryDto.getId();
      doCreateEmptySubDraft(spiltNum, physicalInventoryId);
    }
  }

  private void doCreateEmptySubDraft(
      Integer spiltNum, UUID physicalInventoryId) {
    if (!physicalInventorySubDraftRepository.findByPhysicalInventoryId(physicalInventoryId).isEmpty()) {
      throw new IllegalArgumentException(
          "Has already begun the physical inventory : " + physicalInventoryId);
    }
    for (int i = 1; i <= spiltNum; i++) {
      physicalInventorySubDraftRepository.save(
          PhysicalInventorySubDraft
              .builder()
              .physicalInventoryId(physicalInventoryId)
              .num(i)
              .status(PhysicalInventorySubDraftEnum.NOT_YET_STARTED)
              .build());
    }
  }

  private List<SubDraftDto> convertSubDraftToSubDraftDto(
      List<PhysicalInventorySubDraft> physicalInventorySubDraftList) {
    List<SubDraftDto> subDraftDtoList = new LinkedList<>();
    Map<Integer, List<PhysicalInventorySubDraft>> groupSubDraftDtoMap = physicalInventorySubDraftList.stream()
        .collect(Collectors.groupingBy(PhysicalInventorySubDraft::getNum));
    groupSubDraftDtoMap.forEach((groupNum, subDraftList) -> subDraftDtoList.add(
        SubDraftDto.builder()
            .groupNum(groupNum)
            .status(subDraftList.get(0).getStatus())
            .subDraftId(subDraftList.stream().map(BaseEntity::getId).collect(Collectors.toList()))
            .saver(authenticationHelper.getUserNameByUserId(subDraftList.get(0).getOperatorId()))
            .build()));
    return subDraftDtoList;
  }

  private boolean isTheAllSubDraftIsSubmitted(List<PhysicalInventorySubDraft> subDraftList) {
    return subDraftList.stream().allMatch(e -> e.getStatus() == PhysicalInventorySubDraftEnum.SUBMITTED);
  }

  @VisibleForTesting
  List<List<PhysicalInventoryLineItemDto>> groupByLocationCode(
      List<PhysicalInventoryLineItemDto> lineItemDtos) {

    List<List<PhysicalInventoryLineItemDto>> list = new LinkedList<>();
    Map<String, List<PhysicalInventoryLineItemDto>> locationCodeToLineItemsMap = lineItemDtos.stream()
        .collect(Collectors.groupingBy(PhysicalInventoryLineItemDto::getLocationCode));
    locationCodeToLineItemsMap.forEach((locationCode, lineItems) -> list.add(lineItems));

    list.sort(Comparator.comparing(o -> o.get(0).getLocationCode()));
    return list;
  }

  private void splitPhysicalInventory(PhysicalInventoryDto dto, Integer splitNum, boolean isByLocation) {
    createEmptySubDraft(splitNum, dto);
    spiltLineItem(dto, splitNum, isByLocation);
  }

  @VisibleForTesting
  void spiltLineItem(PhysicalInventoryDto physicalInventory, Integer splitNum, boolean isByLocation) {
    if (physicalInventory == null
        || physicalInventory.getLineItems() == null
        || (physicalInventory.getLineItems().isEmpty() && !isByLocation)) {
      return;
    }
    UUID facilityId = physicalInventory.getFacilityId();
    List<UUID> updatePhysicalInventoryIds = getPhysicalInventoryIds(physicalInventory, facilityId);
    List<PhysicalInventorySubDraft> subDraftList = physicalInventorySubDraftRepository
        .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);

    List<PhysicalInventoryLineItemDto> lineItems = physicalInventory.getLineItems();

    if (isByLocation) {
      List<PhysicalInventoryLineItemDto> emptyLocationPhysicalInventoryLineItems =
          getPhysicalInventoryEmptyLocationLineItemDtos(facilityId, lineItems);
      lineItems.addAll(emptyLocationPhysicalInventoryLineItems);
    }

    // Aggregate by product and sort by product
    List<List<PhysicalInventoryLineItemDto>> lists = isByLocation
        ? groupByLocationCode(lineItems)
        : groupByProductCode(lineItems);

    if (lists.size() < splitNum) {
      throw new BusinessDataException(new Message(ERROR_SPLIT_NUM_TOO_LARGE));
    }
    // Grouping
    List<List<List<PhysicalInventoryLineItemDto>>> groupList = CustomListSortHelper.averageAssign(lists, splitNum);

    if (isByLocation) {
      associateEmptyLocation(subDraftList, groupList);
    }

    List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> updateExtensions = establishConnectionBetweenSubDraftAndLineItemExtension(
        facilityId, subDraftList, extensions, groupList);
    log.info("save physical inventory extension, size: {}", updateExtensions.size());
    lineItemsExtensionRepository.save(updateExtensions);
  }

  @VisibleForTesting
  List<UUID> getPhysicalInventoryIds(PhysicalInventoryDto physicalInventory, UUID facilityId) {
    return physicalInventory.getProgramId().equals(ALL_PRODUCTS_PROGRAM_ID)
        ? getSupportPhysicalInventoryIds(facilityId)
        : Collections.singletonList(physicalInventory.getId());
  }

  @VisibleForTesting
  List<PhysicalInventoryLineItemDto> getPhysicalInventoryEmptyLocationLineItemDtos(
      UUID facilityId, List<PhysicalInventoryLineItemDto> lineItems) {
    List<FacilityLocations> locations = facilityLocationsRepository.findByFacilityId(facilityId);
    return locations.stream().filter(location -> lineItems.stream()
            .noneMatch(lineItem -> lineItem.getLocationCode().equals(location.getLocationCode())))
        .map(location -> PhysicalInventoryLineItemDto.builder().locationCode(location.getLocationCode())
            .area(location.getArea()).build()).collect(Collectors.toList());
  }

  @VisibleForTesting
  void associateEmptyLocation(List<PhysicalInventorySubDraft> subDraftList,
      List<List<List<PhysicalInventoryLineItemDto>>> groupList) {
    Map<Integer, PhysicalInventorySubDraft> numToSubDraftMap = subDraftList.stream()
        .collect(Collectors.toMap(PhysicalInventorySubDraft::getNum, a -> a, (k1, k2) -> k1));

    List<PhysicalInventoryEmptyLocationLineItem> physicalInventoryEmptyLocationLineItemList = new LinkedList<>();
    AtomicInteger groupNum = new AtomicInteger();
    groupList.forEach(group -> {
      groupNum.getAndIncrement();
      group.forEach(locationList -> {
        if (locationList.stream().anyMatch(lineItem -> Objects.isNull(lineItem.getOrderableId()))) {
          PhysicalInventoryEmptyLocationLineItem emptyLocationLineItem = PhysicalInventoryEmptyLocationLineItem
              .builder()
              .hasProduct(false)
              .locationCode(locationList.get(0).getLocationCode())
              .area(locationList.get(0).getArea())
              .skipped(false)
              .subDraftId(numToSubDraftMap.get(groupNum.get()).getId())
              .build();
          physicalInventoryEmptyLocationLineItemList.add(emptyLocationLineItem);
        }
      });
    });
    log.info("save physical inventory empty location lineItem, size: {}",
        physicalInventoryEmptyLocationLineItemList.size());
    physicalInventoryEmptyLocationLineItemRepository.save(physicalInventoryEmptyLocationLineItemList);
  }

  private List<PhysicalInventoryLineItemsExtension> establishConnectionBetweenSubDraftAndLineItemExtension(
      UUID facilityId,
      List<PhysicalInventorySubDraft> subDraftList,
      List<PhysicalInventoryLineItemsExtension> extensions,
      List<List<List<PhysicalInventoryLineItemDto>>> groupList) {
    AtomicInteger groupNum = new AtomicInteger();
    List<PhysicalInventoryLineItemsExtension> updateExtensions = new LinkedList<>();
    groupList.forEach(productList -> {
      groupNum.getAndIncrement();
      productList.forEach(lineItemList -> lineItemList.forEach(lineItem -> {
        if (Objects.isNull(lineItem.getOrderableId())) {
          return;
        }
        PhysicalInventoryLineItemsExtension extension = getExtension(extensions, lineItem);
        if (extension == null) {
          extension = PhysicalInventoryLineItemsExtension
              .builder()
              .physicalInventoryLineItemId(lineItem.getId())
              .orderableId(lineItem.getOrderableId())
              .lotId(lineItem.getLotId())
              .physicalInventoryId(UUID.fromString(
                  physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(lineItem.getProgramId(),
                      facilityId, true)))
              .build();
        }

        extension.setInitial(true);
        PhysicalInventoryLineItemsExtension copyedExtension = extension;
        extension.setSubDraftId(
            subDraftList.stream().filter(subDraft -> subDraft.getNum() == groupNum.get()
                    && subDraft.getPhysicalInventoryId().equals(copyedExtension.getPhysicalInventoryId()))
                .findFirst().orElseThrow(NullPointerException::new).getId());
        updateExtensions.add(extension);
      }));
    });
    return updateExtensions;
  }

  private List<UUID> getSupportPhysicalInventoryIds(UUID facilityId) {
    Set<UUID> supportedPrograms = getSupportedPrograms();
    return supportedPrograms.stream()
        .map(programId ->
            physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(programId, facilityId, true))
        .filter(Objects::nonNull)
        .map(UUID::fromString)
        .collect(Collectors.toList());
  }

  private List<PhysicalInventoryLineItemDto> convertCalculatedStockOnHandByLocationToLineItems(
      List<CalculatedStockOnHandByLocation> stockOnHandByLocations,
      Map<UUID, StockCard> stockCardIdMap,
      UUID programId) {
    return stockOnHandByLocations.stream()
        .filter(stockOnHandByLocation -> !VIRTUAL_LOCATION_CODE.equals(stockOnHandByLocation.getLocationCode()))
        .map(soh -> {
          Map<String, String> extraData = new HashMap<>();
          extraData.put(VM_STATUS, null);
          extraData.put(STOCK_CARD_ID, String.valueOf(soh.getStockCardId()));
          StockCard stockCard = stockCardIdMap.get(soh.getStockCardId());
          return PhysicalInventoryLineItemDto.builder()
              .orderableId(stockCard.getOrderableId())
              .lotId(stockCard.getLotId())
              .extraData(extraData)
              .stockAdjustments(Collections.emptyList())
              .programId(programId)
              .stockOnHand(soh.getStockOnHand())
              .locationCode(soh.getLocationCode())
              .area(soh.getArea())
              .build();
        }).collect(Collectors.toList());
  }

  private List<PhysicalInventoryLineItemDto> convertSummaryV2DtosToLineItems(List<StockCardSummaryV2Dto> summaryV2Dtos,
      UUID programId) {
    List<PhysicalInventoryLineItemDto> physicalInventoryLineItems = new LinkedList<>();

    summaryV2Dtos
        .stream()
        .map(StockCardSummaryV2Dto::getCanFulfillForMe)
        .filter(CollectionUtils::isNotEmpty)
        .collect(Collectors.toList())
        .forEach(set -> set
            .stream()
            .filter(c -> c.getStockCard() != null && c.getOrderable() != null).collect(Collectors.toList())
            .forEach(dto -> {
              Map<String, String> extraData = new HashMap<>();
              extraData.put(VM_STATUS, null);
              extraData.put(STOCK_CARD_ID, String.valueOf(dto.getStockCard().getId()));
              physicalInventoryLineItems.add(
                  PhysicalInventoryLineItemDto.builder()
                      .orderableId(dto.getOrderable().getId())
                      .lotId(dto.getLot() != null ? dto.getLot().getId() : null)
                      .extraData(extraData)
                      .stockAdjustments(Collections.emptyList())
                      .programId(programId).build());
            }));
    return physicalInventoryLineItems;
  }

  private PhysicalInventoryLineItemDto newLineItem(UUID orderableId, UUID programId) {
    return PhysicalInventoryLineItemDto.builder()
        .orderableId(orderableId)
        .programId(programId)
        .build();
  }

  private List<PhysicalInventoryLineItemDto> buildPhysicalInventoryLineItems(
      PhysicalInventoryDto physicalInventoryDto) {
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    String programCode = siglusProgramService.getProgram(physicalInventoryDto.getProgramId()).getCode();
    if (MMC_PROGRAM_CODE.equals(programCode)) {
      UUID viaProgramId = siglusProgramService.getProgramByCode(VIA_PROGRAM_CODE)
          .orElseThrow(() -> new NotFoundException("VIA program not found"))
          .getId();
      parameters.set(PROGRAM_ID, String.valueOf(viaProgramId));
    } else {
      parameters.set(PROGRAM_ID, String.valueOf(physicalInventoryDto.getProgramId()));
    }
    parameters.set(FACILITY_ID, String.valueOf(physicalInventoryDto.getFacilityId()));
    parameters.set(RIGHT_NAME, STOCK_INVENTORIES_EDIT);
    parameters.set(EXCLUDE_ARCHIVED, Boolean.TRUE.toString());
    Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);

    List<StockCardSummaryV2Dto> summaryV2Dtos = siglusStockCardSummariesService.findSiglusStockCard(
        parameters, Collections.emptyList(), pageable, false).getContent();
    if (MMC_PROGRAM_CODE.equals(programCode)) {
      Set<UUID> approvedMmcProductIds = requisitionService.getAllApprovedProducts(physicalInventoryDto.getFacilityId(),
          physicalInventoryDto.getProgramId()).stream()
          .map(approvedProductDto -> approvedProductDto.getOrderable().getId())
          .collect(Collectors.toSet());
      summaryV2Dtos = summaryV2Dtos.stream()
          .filter(stockCardSummaryV2Dto -> approvedMmcProductIds.contains(stockCardSummaryV2Dto.getOrderable().getId()))
          .collect(Collectors.toList());
    }

    return convertSummaryV2DtosToLineItems(summaryV2Dtos, physicalInventoryDto.getProgramId());
  }

  private List<PhysicalInventoryLineItemDto> buildPhysicalInventoryLineItemsWithLocation(
      PhysicalInventoryDto physicalInventoryDto) {
    UUID programId = physicalInventoryDto.getProgramId();
    UUID facilityId = physicalInventoryDto.getFacilityId();
    List<StockCard> stockCards = siglusStockCardRepository.findByFacilityIdAndProgramId(
        facilityId, programId);
    Map<UUID, StockCard> stockCardIdMap = stockCards.stream()
        .collect(Collectors.toMap(StockCard::getId, Function.identity()));
    if (CollectionUtils.isEmpty(stockCardIdMap.keySet())) {
      return Collections.emptyList();
    }
    List<CalculatedStockOnHandByLocation> sohByLocations = calculatedStockOnHandByLocationRepository
        .findByStockCardIdIn(stockCardIdMap.keySet());

    Set<String> archivedOrderableIds = archiveProductService.searchArchivedProductsByFacilityId(facilityId);
    List<CalculatedStockOnHandByLocation> activeSohByLocations = sohByLocations.stream()
        .filter(stockOnHandByLocation -> {
          String idString = stockCardIdMap.get(stockOnHandByLocation.getStockCardId()).getOrderableId().toString();
          return !archivedOrderableIds.contains(idString);
        }).collect(Collectors.toList());
    return convertCalculatedStockOnHandByLocationToLineItems(activeSohByLocations, stockCardIdMap, programId);
  }

  private PhysicalInventoryDto createNewDraft(PhysicalInventoryDto physicalInventoryDto) {
    List<PhysicalInventoryDto> physicalInventory = getPhysicalInventoryDtosForProductsForOneProgram(
        physicalInventoryDto.getProgramId(), physicalInventoryDto.getFacilityId(), true, false);
    if (CollectionUtils.isNotEmpty(physicalInventory)) {
      throw new BusinessDataException(new Message(ERROR_INVENTORY_CONFLICT_DRAFT), null);
    }
    log.info("create empty physical inventory: {}", physicalInventoryDto);
    return inventoryController.createEmptyPhysicalInventory(physicalInventoryDto);
  }

  // should ignore MMC for ALL, since MMC items already included in VIA
  private Set<UUID> getSupportedPrograms() {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    UUID viaProgramId = siglusProgramService.getProgramByCode(VIA_PROGRAM_CODE)
            .orElseThrow(() -> new NotFoundException("VIA program not found"))
            .getId();
    UUID mmcProgramId = siglusProgramService.getProgramByCode(MMC_PROGRAM_CODE)
            .orElseThrow(() -> new NotFoundException("MMC program not found"))
            .getId();
    if (supportedPrograms.contains(viaProgramId)) {
      supportedPrograms.remove(mmcProgramId);
    }
    return supportedPrograms;
  }

  private void buildPhysicalInventoryLineItemsForAllPrograms(PhysicalInventoryDto allProductPhysicalInventoryDto,
      boolean initialPhysicalInventory, boolean withLocation) {
    Set<UUID> supportedPrograms = getSupportedPrograms();
    List<PhysicalInventoryLineItemDto> allProductLineItemDtoList = new LinkedList<>();

    for (UUID programId : supportedPrograms) {
      List<PhysicalInventoryDto> physicalInventoryDtos = getPhysicalInventoryDtos(programId,
          allProductPhysicalInventoryDto.getFacilityId(), true);
      if (CollectionUtils.isEmpty(physicalInventoryDtos)) {
        throw new IllegalArgumentException("there is not draft exists for program : " + programId);
      }
      PhysicalInventoryDto savedPhysicalInventoryDto = buildPhysicalInventoryLineItemsForOneProgram(
          physicalInventoryDtos.get(0), initialPhysicalInventory, withLocation);
      List<PhysicalInventoryLineItemDto> lineItems = savedPhysicalInventoryDto.getLineItems();
      if (CollectionUtils.isNotEmpty(lineItems)) {
        allProductLineItemDtoList.addAll(lineItems);
      }
    }
    allProductPhysicalInventoryDto.setLineItems(allProductLineItemDtoList);
  }

  private PhysicalInventoryDto buildPhysicalInventoryLineItemsForOneProgram(PhysicalInventoryDto physicalInventoryDto,
      boolean initialPhysicalInventory, boolean withLocation) {
    List<PhysicalInventoryLineItemDto> physicalInventoryLineItems;
    if (initialPhysicalInventory) {
      physicalInventoryLineItems = withLocation || localMachineHelper.isLocalMachine()
          ? Collections.emptyList() :
          buildInitialInventoryLineItems(Collections.singleton(physicalInventoryDto.getProgramId()),
              physicalInventoryDto.getFacilityId());
    } else {
      physicalInventoryLineItems = withLocation ? buildPhysicalInventoryLineItemsWithLocation(physicalInventoryDto)
          : buildPhysicalInventoryLineItems(physicalInventoryDto);
    }

    PhysicalInventoryDto toBeSavedPhysicalInventoryDto = PhysicalInventoryDto
        .builder()
        .programId(physicalInventoryDto.getProgramId())
        .facilityId(physicalInventoryDto.getFacilityId()).lineItems(physicalInventoryLineItems)
        .id(physicalInventoryDto.getId())
        .build();

    if (CollectionUtils.isNotEmpty(physicalInventoryLineItems)) {
      return saveDraftForProductsForOneProgram(toBeSavedPhysicalInventoryDto);
    }
    return toBeSavedPhysicalInventoryDto;
  }

  private PhysicalInventoryDto saveDraft(PhysicalInventoryDto dto, UUID id) {
    inventoryController.savePhysicalInventory(id, dto);
    return dto;
  }

  // fill the beforeSavedDto.lineItem id where id = null, before has locationCode, after doesn't
  private void fillLineItemId(PhysicalInventoryLineItemExtensionDto beforeSavedDto,
      PhysicalInventoryDto afterSavedDto) {
    Set<UUID> previousLineItemIds = beforeSavedDto.getLineItems()
        .stream()
        .map(PhysicalInventoryLineItemDto::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    // no locationCode in new saved lineitem, uniqueKey can be duplicate
    List<PhysicalInventoryLineItemDto> newSavedLineItems = afterSavedDto.getLineItems()
        .stream()
        .filter(lineItem -> !previousLineItemIds.contains(lineItem.getId()))
        .collect(Collectors.toList());

    Set<UUID> usedIds = new HashSet<>();
    beforeSavedDto.getLineItems()
        .stream()
        .filter(lineItemDto -> lineItemDto.getId() == null)
        .forEach(lineItemDto -> {
          UUID id = newSavedLineItems
              .stream()
              .filter(saved -> getUniqueKey(saved).equals(getUniqueKey(lineItemDto)))
              .filter(saved -> !usedIds.contains(saved.getId()))
              .findFirst().orElseThrow(() -> new IllegalArgumentException("can't find saved line item"))
              .getId();
          lineItemDto.setId(id);
          Optional<PhysicalInventorySubDraftLineItemsExtensionDto> lineItemsExtension =
              beforeSavedDto.getLineItemsExtensions()
                  .stream()
                  .filter(lineItemsExtensionDto -> lineItemsExtensionDto.getPhysicalInventoryLineItemId() == null)
                  .filter(lineItemsExtensionDto -> getUniqueKeyWithLocation(lineItemDto)
                      .equals(getUniqueKeyWithLocation(lineItemsExtensionDto)))
                  .findFirst();
          lineItemsExtension.ifPresent(dto -> dto.setPhysicalInventoryLineItemId(id));
          usedIds.add(id);
        });
  }

  private void fillLineItemIdForInitiate(PhysicalInventoryDto beforeSavedDto,
      PhysicalInventoryDto afterSavedDto) {
    Set<UUID> previousLineItemIds = beforeSavedDto.getLineItems()
        .stream()
        .map(PhysicalInventoryLineItemDto::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    // no locationCode in new saved lineitem, uniqueKey can be duplicate
    List<PhysicalInventoryLineItemDto> newSavedLineItems = afterSavedDto.getLineItems()
        .stream()
        .filter(lineItem -> !previousLineItemIds.contains(lineItem.getId()))
        .collect(Collectors.toList());

    Set<UUID> usedIds = new HashSet<>();
    beforeSavedDto.getLineItems()
        .stream()
        .filter(lineItemDto -> lineItemDto.getId() == null)
        .forEach(lineItemDto -> {
          UUID id = newSavedLineItems
              .stream()
              .filter(saved -> getUniqueKey(saved).equals(getUniqueKey(lineItemDto)))
              .filter(saved -> !usedIds.contains(saved.getId()))
              .findFirst().orElseThrow(() -> new IllegalArgumentException("can't find saved line item"))
              .getId();
          lineItemDto.setId(id);
          usedIds.add(id);
        });
  }

  private List<PhysicalInventoryLineItemDto> getEmptyLocations(UUID facilityId) {
    List<UUID> supportPhysicalInventoryIds = getSupportPhysicalInventoryIds(facilityId);
    List<UUID> subDraftIds = physicalInventorySubDraftRepository.findByPhysicalInventoryIdIn(
            supportPhysicalInventoryIds)
        .stream()
        .map(BaseEntity::getId)
        .collect(Collectors.toList());
    List<PhysicalInventoryEmptyLocationLineItem> emptyLocations
        = physicalInventoryEmptyLocationLineItemRepository.findBySubDraftIdIn(subDraftIds);
    List<PhysicalInventoryEmptyLocationLineItem> visualEmptyLocations = emptyLocations
        .stream()
        .filter(lineItem -> !lineItem.isHasProduct())
        .collect(Collectors.toList());
    return convertEmptyLocationToPhysicalInventoryLineItemDto(visualEmptyLocations);
  }

  private List<PhysicalInventoryDto> fetchPhysicalInventories(
      Set<UUID> supportedPrograms, UUID facilityId, Boolean isDraft) {
    return supportedPrograms.stream()
        .map(it -> getPhysicalInventoryDtos(it, facilityId, isDraft))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  PhysicalInventoryDto createNewDraftForAllPrograms(PhysicalInventoryDto dto, LocationManagementOption locationOption) {
    Set<UUID> supportedPrograms = getSupportedPrograms();
    List<PhysicalInventoryDto> inventories = supportedPrograms
        .stream()
        .map(supportedVirtualProgram -> {
          // avoid direct change dto
          PhysicalInventoryDto copy = new PhysicalInventoryDto();
          BeanUtils.copyProperties(dto, copy);
          copy.setProgramId(supportedVirtualProgram);
          return createNewDraft(copy);
        }).collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(inventories)) {
      inventories.forEach(eachInventory -> {
        PhysicalInventoryExtension physicalInventoryExtension = buildPhysicalInventoryExtension(
            eachInventory, true, locationOption);
        log.info("physical inventory extension input for all product: {}", physicalInventoryExtension);
        physicalInventoryExtensionRepository.save(physicalInventoryExtension);
      });
      return getResultInventoryForAllPrograms(inventories, emptyList());
    }
    return null;
  }

  private void doDeletePhysicalInventoryForProductInOneProgram(UUID facilityId, UUID programId) {
    doDeletePhysicalInventoryCore(facilityId, programId);
  }

  private void doDeletePhysicalInventoryCore(UUID facilityId, UUID programId) {
    String physicalInventoryId = physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(
        programId, facilityId, Boolean.TRUE);
    if (physicalInventoryId == null) {
      return;
    }
    List<UUID> physicalInventoryIdList = new ArrayList<>(
        Collections.singleton(UUID.fromString(physicalInventoryId)))
        .stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    physicalInventoryIdList.forEach(this::deletePhysicalInventoryDraft);
    lineItemsExtensionRepository.deleteByPhysicalInventoryIdIn(physicalInventoryIdList);
  }

  private void doDeletePhysicalInventoryForAllPrograms(UUID facilityId) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    supportedPrograms.forEach(programId -> doDeletePhysicalInventoryCore(facilityId, programId));
  }

  private List<PhysicalInventoryLineItemsExtension> updateExtension(PhysicalInventoryDto inventoryDto,
      List<PhysicalInventoryDto> updatedDto) {
    List<UUID> updatePhysicalInventoryIds = updatedDto.stream()
        .map(PhysicalInventoryDto::getId)
        .collect(Collectors.toList());
    log.info("find physical inventory extension: {}", updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> updateExtensions = new ArrayList<>();
    updatedDto.forEach(dto -> {
      if (dto.getLineItems() == null || dto.getLineItems().isEmpty()) {
        return;
      }
      dto.getLineItems().forEach(lineItem -> {
        PhysicalInventoryLineItemsExtension extension = getExtension(extensions, lineItem);
        if (extension == null) {
          extension = PhysicalInventoryLineItemsExtension.builder()
              .physicalInventoryLineItemId(lineItem.getId())
              .orderableId(lineItem.getOrderableId())
              .lotId(lineItem.getLotId())
              .physicalInventoryId(dto.getId())
              .build();
        }
        PhysicalInventoryLineItemDto matched = getMatchedLineItem(inventoryDto, lineItem);
        if (matched != null) {
          extension.setReasonFreeText(matched.getReasonFreeText());
          extension.setLocationCode(matched.getLocationCode());
          extension.setArea(matched.getArea());
        }
        updateExtensions.add(extension);
      });
    });

    log.info("save physical inventory extension, size: {}", updateExtensions.size());
    return lineItemsExtensionRepository.save(updateExtensions);
  }

  private List<PhysicalInventoryLineItemsExtension> updateExtensionWithSubDraft(
      PhysicalInventoryLineItemExtensionDto inventoryDto,
      List<PhysicalInventoryDto> updatedDto) {
    List<PhysicalInventorySubDraftLineItemsExtensionDto> lineItemsExtensions = inventoryDto.getLineItemsExtensions();
    Map<UUID, UUID> lineItemsExtensionMap = lineItemsExtensions.stream()
        .collect(Collectors.toMap(PhysicalInventorySubDraftLineItemsExtensionDto::getPhysicalInventoryLineItemId,
            PhysicalInventorySubDraftLineItemsExtensionDto::getSubDraftId, (item1, item2) -> item1));

    List<UUID> updatePhysicalInventoryIds =
        updatedDto.stream()
            .map(PhysicalInventoryDto::getId)
            .collect(Collectors.toList());
    log.info("find physical inventory extension: {}", updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> updateExtensions = new ArrayList<>();

    Set<UUID> uniqueKeys = new HashSet<>();
    updatedDto.forEach(dto -> {
      if (dto.getLineItems() == null || dto.getLineItems().isEmpty()) {
        return;
      }
      dto.getLineItems().forEach(lineItem -> {
        uniqueKeys.add(lineItem.getId());
        PhysicalInventoryLineItemsExtension extension = getExtension(extensions, lineItem);
        if (extension == null) {
          UUID subDraftId = lineItemsExtensionMap.get(lineItem.getId());
          extension = PhysicalInventoryLineItemsExtension.builder()
              .physicalInventoryLineItemId(lineItem.getId())
              .orderableId(lineItem.getOrderableId())
              .lotId(lineItem.getLotId())
              .physicalInventoryId(dto.getId())
              .initial(false)
              .subDraftId(subDraftId)
              .build();
        }
        PhysicalInventoryLineItemDto matched = getMatchedLineItem(inventoryDto, lineItem);
        if (matched != null) {
          extension.setReasonFreeText(matched.getReasonFreeText());
          extension.setLocationCode(matched.getLocationCode());
          extension.setArea(matched.getArea());
        }
        updateExtensions.add(extension);
      });
    });
    List<PhysicalInventoryLineItemsExtension> needDeleteLineItemsExtensions = extensions.stream()
        .filter(extension -> !uniqueKeys.contains(extension.getPhysicalInventoryLineItemId()))
        .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(needDeleteLineItemsExtensions)) {
      lineItemsExtensionRepository.deleteInBatch(needDeleteLineItemsExtensions);
    }

    log.info("save physical inventory extension, size: {}", updateExtensions.size());
    return lineItemsExtensionRepository.save(updateExtensions);
  }

  private String getString(Object o) {
    return o == null ? "" : o.toString();
  }

  private String getUniqueKey(PhysicalInventoryLineItemDto lineItemDto) {
    return getString(lineItemDto.getOrderableId()) + SEPARATOR + getString(lineItemDto.getLotId());
  }

  private String getUniqueKeyWithLocation(PhysicalInventoryLineItemDto lineItemDto) {
    return getString(lineItemDto.getOrderableId()) + SEPARATOR + getString(lineItemDto.getLotId())
        + SEPARATOR + getString(lineItemDto.getLocationCode());
  }

  private String getUniqueKeyWithLocation(PhysicalInventorySubDraftLineItemsExtensionDto lineItemsExtensionDto) {
    return getString(lineItemsExtensionDto.getOrderableId()) + SEPARATOR + getString(lineItemsExtensionDto.getLotId())
        + SEPARATOR + getString(lineItemsExtensionDto.getLocationCode());
  }

  private PhysicalInventoryLineItemsExtension getExtension(
      List<PhysicalInventoryLineItemsExtension> extensions, PhysicalInventoryLineItemDto lineItem) {
    return extensions
        .stream()
        .filter(extension -> Objects.equals(extension.getPhysicalInventoryLineItemId(), lineItem.getId()))
        .findFirst()
        .orElse(null);
  }

  private PhysicalInventoryLineItemDto getMatchedLineItem(PhysicalInventoryDto inventoryDto,
      PhysicalInventoryLineItemDto lineItem) {
    return inventoryDto.getLineItems()
        .stream()
        .filter(itemDto -> Objects.equals(itemDto.getId(), lineItem.getId()))
        .findFirst()
        .orElse(null);
  }

  private PhysicalInventoryDto getResultInventory(List<PhysicalInventoryDto> inventories,
      List<PhysicalInventoryLineItemsExtension> extensions) {
    PhysicalInventoryDto resultInventory = getResultInventoryForAllPrograms(inventories, extensions);
    resultInventory.setId(inventories.get(0).getId());
    resultInventory.setProgramId(inventories.get(0).getProgramId());
    return resultInventory;
  }

  private PhysicalInventoryDto getResultInventoryForAllPrograms(List<PhysicalInventoryDto> inventories,
      List<PhysicalInventoryLineItemsExtension> extensions) {
    return PhysicalInventoryDto.builder()
        .id(ALL_PRODUCTS_UUID)
        .programId(ALL_PRODUCTS_PROGRAM_ID)
        .facilityId(inventories.get(0).getFacilityId())
        .occurredDate(inventories.get(0).getOccurredDate())
        .signature(inventories.get(0).getSignature())
        .documentNumber(inventories.get(0).getDocumentNumber())
        .isStarter(inventories.get(0).getIsStarter())
        .isDraft(inventories.get(0).getIsDraft())
        .lineItems(getLineItems(inventories, extensions))
        .build();
  }

  private List<PhysicalInventoryLineItemDto> getLineItems(List<PhysicalInventoryDto> inventories,
      List<PhysicalInventoryLineItemsExtension> extensions) {
    return inventories
        .stream()
        .map(inventory -> {
          Optional<List<PhysicalInventoryLineItemDto>> optionalList = Optional
              .ofNullable(inventory.getLineItems());
          optionalList
              .ifPresent(physicalInventoryLineItemDtos -> physicalInventoryLineItemDtos.forEach(
                  physicalInventoryLineItemDto -> {
                    physicalInventoryLineItemDto.setProgramId(inventory.getProgramId());
                    PhysicalInventoryLineItemsExtension extension = getExtension(
                        extensions, physicalInventoryLineItemDto);
                    if (extension != null) {
                      physicalInventoryLineItemDto.setReasonFreeText(extension.getReasonFreeText());
                      physicalInventoryLineItemDto.setArea(extension.getArea());
                      physicalInventoryLineItemDto.setLocationCode(extension.getLocationCode());
                    }
                  }));
          return optionalList.orElse(new ArrayList<>());
        })
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private boolean isVirtualFacility(UUID facility) {
    String code = facilityReferenceDataService.findOne(facility).getType().getCode();
    return code == null || Arrays.asList(AC, CENTRAL).contains(code);
  }

  private boolean isStockCardCountEmpty(UUID facility) {
    return stockCardRepository.countByFacilityId(facility) == 0;
  }

  private boolean canInitialOrPhysicalInventory(UUID facilityId, boolean initialPhysicalInventory) {
    boolean virtualFacility = isVirtualFacility(facilityId);
    boolean emptyStockCardCount = isStockCardCountEmpty(facilityId);
    return (!virtualFacility && emptyStockCardCount && initialPhysicalInventory)
        || (!virtualFacility && !emptyStockCardCount && !initialPhysicalInventory);
  }

  private boolean canPhysicalInventory(UUID facilityId) {
    boolean virtualFacility = isVirtualFacility(facilityId);
    boolean emptyStockCardCount = isStockCardCountEmpty(facilityId);
    return !virtualFacility && !emptyStockCardCount;
  }

  private PhysicalInventoryExtension buildPhysicalInventoryExtension(PhysicalInventoryDto physicalInventoryDto,
      boolean isForAllProgram, LocationManagementOption locationOption) {
    return PhysicalInventoryExtension
        .builder()
        .physicalInventoryId(physicalInventoryDto.getId())
        .category(isForAllProgram ? ALL_PROGRAM : SINGLE_PROGRAM)
        .locationOption(locationOption)
        .build();
  }

  private PhysicalInventoryValidationDto buildPhysicalInventoryValidationDto(
      boolean canStartInventory, List<UUID> conflictProgramIdList) {
    return PhysicalInventoryValidationDto
        .builder()
        .canStartInventory(canStartInventory)
        .containDraftProgramsList(conflictProgramIdList)
        .build();
  }
}
