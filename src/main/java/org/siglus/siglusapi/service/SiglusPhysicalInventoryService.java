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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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
import static org.siglus.siglusapi.util.ComparorUtil.distinctByKey;

import com.google.common.annotations.VisibleForTesting;
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
import java.util.stream.IntStream;
import javax.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.referencedata.domain.Orderable;
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
import org.siglus.siglusapi.dto.LotDto;
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
import org.siglus.siglusapi.repository.SiglusPhysicalInventoryRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.dto.SiglusPhysicalInventoryBriefDto;
import org.siglus.siglusapi.repository.dto.StockCardStockDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.CustomListSortHelper;
import org.siglus.siglusapi.util.FacilityConfigHelper;
import org.siglus.siglusapi.util.LocalMachineHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.PreserveStackTrace", "PMD.CyclomaticComplexity"})
public class SiglusPhysicalInventoryService {

  @Autowired
  private PhysicalInventoriesRepository physicalInventoriesRepository;
  @Autowired
  private StockCardRepository stockCardRepository;
  @Autowired
  private SupportedProgramsHelper supportedProgramsHelper;
  @Autowired
  private PhysicalInventoryLineItemsExtensionRepository lineItemsExtensionRepository;
  @Autowired
  private PhysicalInventoryController inventoryController;
  @Autowired
  private SiglusFacilityReferenceDataService facilityReferenceDataService;
  @Autowired
  private OrderableRepository orderableRepository;
  @Autowired
  private RequisitionService requisitionService;
  @Autowired
  private PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;
  @Autowired
  private SiglusStockCardSummariesService siglusStockCardSummariesService;
  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;
  @Autowired
  private PhysicalInventoryExtensionRepository physicalInventoryExtensionRepository;
  @Autowired
  private CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;
  @Autowired
  private SiglusStockCardRepository siglusStockCardRepository;
  @Autowired
  private FacilityLocationsRepository facilityLocationsRepository;
  @Autowired
  private PhysicalInventoryEmptyLocationLineItemRepository physicalInventoryEmptyLocationLineItemRepository;
  @Autowired
  private SiglusOrderableService siglusOrderableService;
  @Autowired
  private SiglusLotService siglusLotService;
  @Autowired
  private SiglusProgramService siglusProgramService;
  @Autowired
  private SiglusArchiveProductService archiveProductService;
  @Autowired
  private LocalMachineHelper localMachineHelper;
  @Autowired
  private SiglusPhysicalInventoryRepository siglusPhysicalInventoryRepository;
  @Autowired
  private FacilityConfigHelper facilityConfigHelper;

  @Transactional
  public PhysicalInventoryDto createAndSplitNewDraftForAllPrograms(PhysicalInventoryDto physicalInventoryDto,
      Integer splitNum, boolean initialPhysicalInventory, String optionString, boolean isByLocation) {
    if (!canInitialOrPhysicalInventory(physicalInventoryDto.getFacilityId(), initialPhysicalInventory)) {
      throw new ValidationMessageException(new Message(ERROR_NOT_ACCEPTABLE));
    }
    PhysicalInventoryValidationDto validationDto =
        checkConflictForAllPrograms(physicalInventoryDto.getFacilityId(), null);
    if (!validationDto.isCanStartInventory()) {
      throw new ValidationMessageException(new Message(ERROR_INVENTORY_CONFLICT_DRAFT));
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
    if (ObjectUtils.isEmpty(physicalInventoryDto.getFacilityId())
        || ObjectUtils.isEmpty(physicalInventoryDto.getProgramId())) {
      throw new ValidationException("Incorrect facility id or program id");
    }
    LocationManagementOption option = null;
    if (StringUtils.isNotEmpty(optionString)) {
      option = LocationManagementOption.fromString(optionString);
    }
    if (!canPhysicalInventory(physicalInventoryDto.getFacilityId())) {
      throw new ValidationMessageException(new Message(ERROR_NOT_ACCEPTABLE));
    }
    PhysicalInventoryValidationDto validationDto =
        checkConflictForOneProgram(physicalInventoryDto.getFacilityId(), physicalInventoryDto.getProgramId(), null);
    if (!validationDto.isCanStartInventory()) {
      throw new ValidationMessageException(new Message(ERROR_INVENTORY_CONFLICT_DRAFT));
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
    List<UUID> physicalInventoryIds =
        siglusPhysicalInventoryRepository.queryForAllProgram(facility, isDraft)
            .stream().map(SiglusPhysicalInventoryBriefDto::getId).collect(Collectors.toList());
    if (CollectionUtils.isEmpty(physicalInventoryIds)) {
      throw new IllegalArgumentException("there is no subDraft for any record");
    }
    List<PhysicalInventorySubDraft> allProductSubDraftList =
        physicalInventorySubDraftRepository.findByPhysicalInventoryIdIn(physicalInventoryIds);
    return DraftListDto
        .builder()
        .physicalInventoryId(ALL_PRODUCTS_UUID)
        .subDrafts(convertSubDraftToSubDraftDto(allProductSubDraftList))
        .canMergeOrDeleteDrafts(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts())
        .canSubmitDrafts(isTheAllSubDraftIsSubmitted(allProductSubDraftList))
        .build();
  }

  public PhysicalInventoryValidationDto checkConflictForAllPrograms(UUID facility, UUID draft) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    if (CollectionUtils.isEmpty(supportedPrograms)) {
      throw new PermissionMessageException(new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED));
    }
    List<SiglusPhysicalInventoryBriefDto> draftDtos =
        siglusPhysicalInventoryRepository.queryAllDraftByFacility(facility);
    Set<UUID> conflictProgramIdList = new HashSet<>();
    if (CollectionUtils.isNotEmpty(draftDtos)) {
      draftDtos.forEach(dto -> {
        if (ALL_PROGRAM.equals(dto.getCategory())) {
          if (ObjectUtils.isEmpty(draft)) {
            conflictProgramIdList.add(ALL_PRODUCTS_PROGRAM_ID);
          }
        } else {
          conflictProgramIdList.add(dto.getProgramId());
        }
      });
    }
    if (ObjectUtils.isEmpty(conflictProgramIdList)) {
      return buildPhysicalInventoryValidationDto(true, newArrayList());
    } else {
      return buildPhysicalInventoryValidationDto(false, new ArrayList<>(conflictProgramIdList));
    }
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

  public SiglusPhysicalInventoryDto getPhysicalInventoryDtoBySubDraftIds(List<UUID> subDraftIds) {
    if (CollectionUtils.isEmpty(subDraftIds)) {
      throw new IllegalArgumentException("empty subDraftIds");
    }
    List<PhysicalInventoryDto> physicalInventoryDtos = getPhysicalInventoryBySubDraftIds(subDraftIds);
    SiglusPhysicalInventoryDto siglusPhysicalInventoryDto = new SiglusPhysicalInventoryDto();
    BeanUtils.copyProperties(physicalInventoryDtos.get(0), siglusPhysicalInventoryDto);
    List<PhysicalInventoryExtension> extensions = physicalInventoryExtensionRepository
        .findByPhysicalInventoryId(physicalInventoryDtos.get(0).getId());
    boolean isAllProgram = false;
    if (CollectionUtils.isNotEmpty(extensions)) {
      PhysicalInventoryExtension extension = extensions.get(0);
      if (!ObjectUtils.isEmpty(extension.getLocationOption())) {
        siglusPhysicalInventoryDto.setLocationOption(extension.getLocationOption().getValue());
      }
      isAllProgram = ALL_PROGRAM.equals(extension.getCategory());
    }
    List<PhysicalInventoryLineItemDto> lineItemDtos = physicalInventoryDtos.stream()
        .map(PhysicalInventoryDto::getLineItems).flatMap(List::stream)
        .filter(distinctByKey(this::getUniqueKeyWithLocation))
        .collect(Collectors.toList());

    boolean withLocation = facilityConfigHelper.isLocationManagementEnabled(siglusPhysicalInventoryDto.getFacilityId());
    fillPhysicalInventoryLineItemDto(lineItemDtos, withLocation);

    UUID programId = isAllProgram ? ALL_PRODUCTS_PROGRAM_ID : siglusPhysicalInventoryDto.getProgramId();
    UUID physicalInventoryId = isAllProgram ? ALL_PRODUCTS_UUID : siglusPhysicalInventoryDto.getId();
    siglusPhysicalInventoryDto.setId(physicalInventoryId);
    siglusPhysicalInventoryDto.setProgramId(programId);
    siglusPhysicalInventoryDto.setLineItems(lineItemDtos);
    return siglusPhysicalInventoryDto;
  }

  private void fillPhysicalInventoryLineItemDto(List<PhysicalInventoryLineItemDto> lineItemDtos, boolean withLocation) {
    fillPhysicalInventoryLineItemDtoLot(lineItemDtos);
    fillPhysicalInventoryLineItemDtoOrderable(lineItemDtos);
    fillPhysicalInventoryLineItemDtoSoh(lineItemDtos, withLocation);
  }

  private void fillPhysicalInventoryLineItemDtoLot(List<PhysicalInventoryLineItemDto> lineItemDtos) {
    List<UUID> lotIds = lineItemDtos.stream()
        .map(PhysicalInventoryLineItemDto::getLotId).distinct().collect(Collectors.toList());
    if (!ObjectUtils.isEmpty(lotIds)) {
      Map<UUID, LotDto> lotDtoMap = siglusLotService.getLotList(lotIds)
          .stream().collect(Collectors.toMap(LotDto::getId, lotDto -> lotDto));
      lineItemDtos.forEach(lineItemDto -> {
        LotDto lotDto = lotDtoMap.get(lineItemDto.getLotId());
        if (!ObjectUtils.isEmpty(lotDto)) {
          lineItemDto.setLotCode(lotDto.getLotCode());
          lineItemDto.setExpirationDate(lotDto.getExpirationDate());
        }
      });
    }
  }

  private void fillPhysicalInventoryLineItemDtoOrderable(List<PhysicalInventoryLineItemDto> lineItemDtos) {
    List<UUID> orderrableIds = lineItemDtos.stream()
        .map(PhysicalInventoryLineItemDto::getOrderableId).distinct().collect(Collectors.toList());
    if (!ObjectUtils.isEmpty(orderrableIds)) {
      Map<UUID, Orderable> orderableMap = orderableRepository.findLatestByIds(orderrableIds)
          .stream().collect(Collectors.toMap(Orderable::getId, orderable -> orderable));
      lineItemDtos.forEach(lineItemDto -> {
        Orderable orderable = orderableMap.get(lineItemDto.getOrderableId());
        if (!ObjectUtils.isEmpty(orderable)) {
          lineItemDto.setProductCode(orderable.getProductCode().toString());
          lineItemDto.setProductName(orderable.getFullProductName());
        }
      });
    }
  }

  private void fillPhysicalInventoryLineItemDtoSoh(List<PhysicalInventoryLineItemDto> lineItemDtos,
                                                   boolean withLocation) {
    Set<UUID> stockCardIds = lineItemDtos.stream().map(PhysicalInventoryLineItemDto::getExtraData)
        .filter(Objects::nonNull)
        .map(extraMap -> extraMap.get(STOCK_CARD_ID))
        .filter(Objects::nonNull)
        .map(UUID::fromString)
        .collect(Collectors.toSet());
    Map<String, StockCardStockDto> sohMap =
        siglusStockCardSummariesService.getLatestStockOnHandByIds(stockCardIds, withLocation)
            .stream().collect(Collectors.toMap(soh ->
                getUniqueStockCardKeyWithLocation(soh.getStockCardId(), soh.getLocationCode()), soh -> soh));
    lineItemDtos.forEach(lineItemDto -> {
      String key = getUniqueStockCardKeyWithLocation(lineItemDto);
      if (ObjectUtils.isEmpty(key)) {
        return;
      }
      StockCardStockDto stock = sohMap.get(key);
      if (!ObjectUtils.isEmpty(stock)) {
        lineItemDto.setStockOnHand(stock.getStockOnHand());
      }
    });
  }

  public PhysicalInventoryValidationDto checkConflictForOneProgram(UUID facility, UUID program, UUID draft) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    if (CollectionUtils.isEmpty(supportedPrograms) || !supportedPrograms.contains(program)) {
      throw new PermissionMessageException(new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED));
    }

    List<SiglusPhysicalInventoryBriefDto> allDraftDtos
        = siglusPhysicalInventoryRepository.queryAllDraftByFacility(facility);
    if (allDraftDtos.stream().anyMatch(draftDto -> ALL_PROGRAM.equals(draftDto.getCategory()))) {
      return buildPhysicalInventoryValidationDto(false, newArrayList(ALL_PRODUCTS_PROGRAM_ID));
    }
    if (allDraftDtos.stream().anyMatch(draftDto ->
        program.equals(draftDto.getProgramId()) && !draftDto.getId().equals(draft))) {
      return buildPhysicalInventoryValidationDto(false, newArrayList(program));
    }
    return buildPhysicalInventoryValidationDto(true, newArrayList(program));
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
    List<UUID> physicalInventoryIds =
        siglusPhysicalInventoryRepository.queryForOneProgram(facility, program, isDraft)
            .stream().map(SiglusPhysicalInventoryBriefDto::getId).collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(physicalInventoryIds)) {
      UUID physicalInventoryId = physicalInventoryIds.get(0);
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
        .findByPhysicalInventoryIdIn(newArrayList(physicalInventoryId));
    return getResultInventory(newArrayList(physicalInventoryDto), extensions);
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
    inventories = filterPhysicalInventories(inventories, false);
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

  public List<SiglusPhysicalInventoryBriefDto> getPhysicalInventoryBriefDtos(
      UUID facilityId, UUID programId, Boolean isDraft) {
    List<SiglusPhysicalInventoryBriefDto> physicalInventories;
    if (ALL_PRODUCTS_PROGRAM_ID.equals(programId)) {
      physicalInventories = siglusPhysicalInventoryRepository.queryForAllProgram(facilityId, isDraft);
      if (!ObjectUtils.isEmpty(physicalInventories)) {
        SiglusPhysicalInventoryBriefDto briefDto = physicalInventories.get(0);
        briefDto.setProgramId(ALL_PRODUCTS_PROGRAM_ID);
        briefDto.setId(ALL_PRODUCTS_PROGRAM_ID);
        physicalInventories = singletonList(briefDto);
      }
    } else {
      physicalInventories = siglusPhysicalInventoryRepository.queryForOneProgram(facilityId, programId, isDraft);
    }
    return physicalInventories;
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
      inventories = filterPhysicalInventories(inventories, true);
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
    List<SiglusPhysicalInventoryBriefDto> physicalInventories =
        siglusPhysicalInventoryRepository.findByProgramIdAndFacilityIdAndStartDateAndEndDate(programId,
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

  private PhysicalInventoryDto getPhysicalInventoryBySubDraftId(UUID subDraftId) {
    PhysicalInventorySubDraft subDraft = physicalInventorySubDraftRepository.findFirstById(subDraftId);
    PhysicalInventoryDto physicalInventory = getPhysicalInventory(subDraft.getPhysicalInventoryId());
    List<PhysicalInventoryLineItemsExtension> extensions =
        lineItemsExtensionRepository.findByPhysicalInventoryIdAndSubDraftId(physicalInventory.getId(), subDraftId);
    Map<UUID, PhysicalInventoryLineItemDto> lineItemDtoMap =
        physicalInventory.getLineItems().stream()
            .collect(Collectors.toMap(PhysicalInventoryLineItemDto::getId, item -> item));
    List<PhysicalInventoryLineItemDto> itemDtos = extensions.stream().map(
        extension -> {
          PhysicalInventoryLineItemDto dto =
              lineItemDtoMap.getOrDefault(extension.getPhysicalInventoryLineItemId(), null);
          return dto == null
              ? null
              : fullFillPhysicalInventoryLineItemDtoExtension(dto, physicalInventory.getProgramId(), extension);
        }
    ).filter(Objects::nonNull).collect(Collectors.toList());
    physicalInventory.setLineItems(itemDtos);
    return physicalInventory;
  }

  private List<PhysicalInventoryDto> getPhysicalInventoryBySubDraftIds(List<UUID> subDraftIds) {
    return subDraftIds.stream().map(this::getPhysicalInventoryBySubDraftId).collect(Collectors.toList());
  }

  private List<PhysicalInventorySubDraft> createEmptySubDraft(Integer spiltNum,
                                                              PhysicalInventoryDto physicalInventoryDto) {
    List<UUID> physicalInventoryIds = newArrayList(physicalInventoryDto.getId());
    if (physicalInventoryDto.getProgramId().equals(ALL_PRODUCTS_PROGRAM_ID)) {
      List<SiglusPhysicalInventoryBriefDto> briefDtos =
          siglusPhysicalInventoryRepository.queryForAllProgram(physicalInventoryDto.getFacilityId(), true);
      physicalInventoryIds = briefDtos.stream()
          .map(SiglusPhysicalInventoryBriefDto::getId).collect(Collectors.toList());
    }

    if (!ObjectUtils.isEmpty(physicalInventorySubDraftRepository.findByPhysicalInventoryIdIn(physicalInventoryIds))) {
      throw new IllegalArgumentException(
          "Has already begun the physical inventory : " + physicalInventoryIds);
    }

    List<PhysicalInventorySubDraft> subDrafts = physicalInventoryIds.stream().map(
        physicalInventoryId -> IntStream.rangeClosed(1, spiltNum).mapToObj(
            splitIndex -> PhysicalInventorySubDraft.builder()
                .physicalInventoryId(physicalInventoryId)
                .num(splitIndex)
                .status(PhysicalInventorySubDraftEnum.NOT_YET_STARTED)
                .build())
            .collect(Collectors.toList()))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    return physicalInventorySubDraftRepository.save(subDrafts);
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
    List<PhysicalInventorySubDraft> subDrafts = createEmptySubDraft(splitNum, dto);
    spiltLineItem(dto, subDrafts, isByLocation);
  }

  @VisibleForTesting
  void spiltLineItem(PhysicalInventoryDto physicalInventory,
                     List<PhysicalInventorySubDraft> subDrafts, boolean isByLocation) {
    if (physicalInventory == null
        || physicalInventory.getLineItems() == null
        || (physicalInventory.getLineItems().isEmpty() && !isByLocation)) {
      return;
    }
    UUID facilityId = physicalInventory.getFacilityId();
    final List<UUID> updatePhysicalInventoryIds = getPhysicalInventoryIds(physicalInventory, facilityId);

    List<PhysicalInventoryLineItemDto> lineItems = physicalInventory.getLineItems();

    if (isByLocation && physicalInventory.getProgramId().equals(ALL_PRODUCTS_PROGRAM_ID)) {
      List<PhysicalInventoryLineItemDto> emptyLocationPhysicalInventoryLineItems =
          getPhysicalInventoryEmptyLocationLineItemDtos(facilityId, lineItems);
      lineItems.addAll(emptyLocationPhysicalInventoryLineItems);
    }

    // Aggregate by product and sort by product
    List<List<PhysicalInventoryLineItemDto>> lists = isByLocation
        ? groupByLocationCode(lineItems)
        : groupByProductCode(lineItems);

    if (lists.size() < subDrafts.size()) {
      throw new BusinessDataException(new Message(ERROR_SPLIT_NUM_TOO_LARGE));
    }
    // Grouping
    List<List<List<PhysicalInventoryLineItemDto>>> groupList =
        CustomListSortHelper.averageAssign(lists, subDrafts.size());

    if (isByLocation && physicalInventory.getProgramId().equals(ALL_PRODUCTS_PROGRAM_ID)) {
      associateEmptyLocation(subDrafts, groupList);
    }

    List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> updateExtensions = establishConnectionBetweenSubDraftAndLineItemExtension(
        facilityId, subDrafts, extensions, groupList);
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
    Set<String> existLocationSet = lineItems.stream()
        .map(PhysicalInventoryLineItemDto::getLocationCode).collect(Collectors.toSet());
    List<FacilityLocations> locations = facilityLocationsRepository.findByFacilityId(facilityId);
    return locations.stream().filter(location -> !existLocationSet.contains(location.getLocationCode()))
        .map(location -> PhysicalInventoryLineItemDto.builder()
            .locationCode(location.getLocationCode())
            .area(location.getArea())
            .build())
        .collect(Collectors.toList());
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
        .filter(soh -> soh.getStockOnHand() > 0)
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
        .facilityId(physicalInventoryDto.getFacilityId())
        .lineItems(physicalInventoryLineItems)
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

  private List<PhysicalInventoryDto> filterPhysicalInventories(
      List<PhysicalInventoryDto> physicalInventoryDtos, Boolean isAllProgram) {
    String category = isAllProgram ? ALL_PROGRAM : SINGLE_PROGRAM;
    return physicalInventoryDtos.stream().filter(dto -> {
      List<PhysicalInventoryExtension> extensionList =
          physicalInventoryExtensionRepository.findByPhysicalInventoryId(dto.getId());
      if (!ObjectUtils.isEmpty(extensionList)) {
        return extensionList.get(0).getCategory().equals(category);
      }
      return false;
    }).collect(Collectors.toList());
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
      lineItemsExtensionRepository.deleteByPhysicalInventoryLineItemIdIn(
          needDeleteLineItemsExtensions.stream()
              .map(PhysicalInventoryLineItemsExtension::getPhysicalInventoryLineItemId)
              .collect(Collectors.toList()));
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

  private String getUniqueStockCardKeyWithLocation(UUID stockCardId, String location) {
    return getString(stockCardId) + SEPARATOR + getString(location);
  }

  private String getUniqueStockCardKeyWithLocation(PhysicalInventoryLineItemDto dto) {
    Map<String, String> extraData = dto.getExtraData();
    if (ObjectUtils.isEmpty(extraData)) {
      return null;
    }
    String stockCardId = extraData.get(STOCK_CARD_ID);
    if (ObjectUtils.isEmpty(stockCardId)) {
      return null;
    }
    return getUniqueStockCardKeyWithLocation(UUID.fromString(stockCardId), dto.getLocationCode());
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

  private PhysicalInventoryLineItemDto fullFillPhysicalInventoryLineItemDtoExtension(
      PhysicalInventoryLineItemDto lineItemDto,
      UUID programId,
      PhysicalInventoryLineItemsExtension extension) {
    lineItemDto.setProgramId(programId);
    if (extension != null) {
      lineItemDto.setReasonFreeText(extension.getReasonFreeText());
      lineItemDto.setArea(extension.getArea());
      lineItemDto.setLocationCode(extension.getLocationCode());
    }
    return lineItemDto;
  }
}
