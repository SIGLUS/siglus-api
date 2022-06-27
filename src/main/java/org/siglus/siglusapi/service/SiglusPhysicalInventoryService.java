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
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_UUID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_ACCEPTABLE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.PhysicalInventoryService;
import org.openlmis.stockmanagement.web.PhysicalInventoryController;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.siglus.siglusapi.domain.PhysicalInventorySubDraft;
import org.siglus.siglusapi.dto.DraftListDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.InitialInventoryFieldDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.SubDraftDto;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.siglus.siglusapi.service.client.PhysicalInventoryStockManagementService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.CustomListSortHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.PreserveStackTrace"})
public class SiglusPhysicalInventoryService {

  @Autowired
  private PhysicalInventoryStockManagementService physicalInventoryStockManagementService;

  @Autowired
  private PhysicalInventoriesRepository physicalInventoriesRepository;

  @Autowired
  private StockCardRepository stockCardRepository;

  @Autowired
  private SiglusApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Autowired
  private PhysicalInventoryService physicalInventoryService;

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
  private PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;

  @Autowired
  private SiglusStockCardSummariesService siglusStockCardSummariesService;


  private PhysicalInventoryDto getPhysicalInventoryBySubDraftId(UUID subDraftId) {
    PhysicalInventorySubDraft subDraft = physicalInventorySubDraftRepository.findFirstById(subDraftId);
    PhysicalInventoryDto physicalInventory = getPhysicalInventory(subDraft.getPhysicalInventoryId());
    return getPhysicalInventoryDtosForProductsInOneProgram(
        physicalInventory.getProgramId(), physicalInventory.getFacilityId(), true).get(0);
  }

  public PhysicalInventoryDto getSubPhysicalInventoryDtoBySubDraftId(List<UUID> subDraftIds) {
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
            return extension.getSubDraftId().equals(subDraftId);
          }).collect(Collectors.toList());
      allSubPhysicalInventoryLineItemDtoList.addAll(subPhysicalInventoryLineItemDtoLists);
    });
    List<PhysicalInventoryLineItemDto> sortedSubPhysialInventoryLineItemList = allSubPhysicalInventoryLineItemDtoList
        .stream().sorted(Comparator.comparing(
            o -> String.valueOf(
                orderableRepository.findLatestById(o.getOrderableId()).orElseThrow(IllegalArgumentException::new)
                    .getProductCode()))).collect(Collectors.toList());
    PhysicalInventoryDto physicalInventory = getPhysicalInventoryBySubDraftId(subDraftIds.get(0));
    UUID programId = subDraftIds.size() > 1
        ? ALL_PRODUCTS_PROGRAM_ID
        : physicalInventory.getProgramId();
    UUID physicalInventoryId = subDraftIds.size() > 1
        ? ALL_PRODUCTS_UUID
        : physicalInventory.getId();
    physicalInventory.setId(physicalInventoryId);
    physicalInventory.setLineItems(sortedSubPhysialInventoryLineItemList);
    physicalInventory.setProgramId(programId);
    return physicalInventory;
  }

  private void createEmptySubDraft(Integer spiltNum, PhysicalInventoryDto physicalInventoryDto) {

    if (physicalInventoryDto.getId().equals(ALL_PRODUCTS_PROGRAM_ID)) {
      Set<UUID> supportedPrograms = supportedProgramsHelper
          .findHomeFacilitySupportedProgramIds();
      supportedPrograms.forEach(programId -> {
        UUID physicalInventoryId = UUID.fromString(physicalInventoriesRepository
            .findIdByProgramIdAndFacilityIdAndIsDraft(
                programId, physicalInventoryDto.getFacilityId(), true));
        if (!physicalInventorySubDraftRepository.findByPhysicalInventoryId(physicalInventoryId).isEmpty()) {
          throw new IllegalArgumentException("Has already begun the physical inventory program : " + programId);
        }
        for (int i = 1; i <= spiltNum; i++) {
          physicalInventorySubDraftRepository.save(
              PhysicalInventorySubDraft.builder().physicalInventoryId(physicalInventoryId)
                  .num(i).status(PhysicalInventorySubDraftEnum.NOT_YET_STARTED)
                  .build());
        }
      });
    } else {
      UUID physicalInventoryDtoId = physicalInventoryDto.getId();
      if (!physicalInventorySubDraftRepository.findByPhysicalInventoryId(physicalInventoryDtoId).isEmpty()) {
        throw new IllegalArgumentException(
            "Has already begun the physical inventory program : " + physicalInventoryDto.getProgramId());
      }
      for (int i = 1; i <= spiltNum; i++) {
        physicalInventorySubDraftRepository.save(
            PhysicalInventorySubDraft.builder().physicalInventoryId(physicalInventoryDtoId).num(i)
                .status(PhysicalInventorySubDraftEnum.NOT_YET_STARTED)
                .build());
      }
    }
  }

  public DraftListDto getSubDraftListForAllProduct() {
    List<PhysicalInventorySubDraft> subDraftList = physicalInventorySubDraftRepository.findAll();
    List<SubDraftDto> subDraftDtos = new LinkedList<>();
    // Aggregation based on num
    Map<Integer, List<PhysicalInventorySubDraft>> groupSubDraftDtoMap = subDraftList.stream()
        .collect(Collectors.groupingBy(PhysicalInventorySubDraft::getNum));
    groupSubDraftDtoMap.forEach((groupNum, subDraftDtoList) -> {
      subDraftDtos.add(
          SubDraftDto.builder().groupNum(groupNum).status(subDraftDtoList.get(0).getStatus())
              .subDraftId(subDraftDtoList.stream()
                  .map(BaseEntity::getId).collect(Collectors.toList()))
              .build());
    });
    return DraftListDto
        .builder()
        .physicalInventoryId(ALL_PRODUCTS_UUID)
        .subDrafts(subDraftDtos)
        .build();
  }


  public DraftListDto getSubDraftListInOneProgram(UUID program, UUID facility,
      Boolean isDraft) {
    List<PhysicalInventoryDto> physicalInventoryDtos = getPhysicalInventoryDtos(program, facility, isDraft);
    if (CollectionUtils.isNotEmpty(physicalInventoryDtos)) {
      UUID physicalInventoryId = physicalInventoryDtos.get(0).getId();
      List<PhysicalInventorySubDraft> physicalInventorySubDrafts = physicalInventorySubDraftRepository
          .findByPhysicalInventoryId(
              physicalInventoryId);
      // todo：通过saverid找savername 填充
      List<SubDraftDto> subDraftDtos = new LinkedList<>();
      physicalInventorySubDrafts.forEach(subDraft -> {
        subDraftDtos.add(
            SubDraftDto.builder().groupNum(subDraft.getNum()).status(subDraft.getStatus())
                .subDraftId(Collections.singletonList(subDraft.getId()))
                .build());
      });
      return DraftListDto
          .builder()
          .subDrafts(subDraftDtos)
          .physicalInventoryId(physicalInventoryDtos.get(0).getId())
          .build();
    }
    return DraftListDto.builder().build();
  }

  private List<List<PhysicalInventoryLineItemDto>> groupByProductCode(List<PhysicalInventoryLineItemDto> lineItemDtos) {

    List<List<PhysicalInventoryLineItemDto>> list = new LinkedList<>();
    Map<UUID, List<PhysicalInventoryLineItemDto>> orderableIdToLineItemsMap = lineItemDtos.stream()
        .collect(Collectors.groupingBy(PhysicalInventoryLineItemDto::getOrderableId));
    orderableIdToLineItemsMap.forEach((orderablesId, lineItems) -> {
      list.add(lineItems);
    });

    list.sort(Comparator.comparing(o -> String.valueOf(
        orderableRepository.findLatestById(o.get(0).getOrderableId()).orElseThrow(IllegalArgumentException::new)
            .getProductCode())));
    return list;
  }


  public void splitPhysicalInventory(PhysicalInventoryDto dto, Integer splitNum) {
    createEmptySubDraft(splitNum, dto);
    spiltLineItem(dto, splitNum);
  }

  private void spiltLineItem(PhysicalInventoryDto physicalInventory, Integer splitNum) {
    if (physicalInventory == null || physicalInventory.getLineItems() == null || physicalInventory.getLineItems()
        .isEmpty()) {
      return;
    }
    List<UUID> updatePhysicalInventoryIds = null;
    List<PhysicalInventorySubDraft> subDraftList = null;
    if (physicalInventory.getId().equals(ALL_PRODUCTS_PROGRAM_ID)) {
      Set<UUID> supportedPrograms = supportedProgramsHelper
          .findHomeFacilitySupportedProgramIds();
      updatePhysicalInventoryIds = supportedPrograms.stream().map(programId ->
              UUID.fromString(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(
                  programId, physicalInventory.getFacilityId(), true)))
          .collect(Collectors.toList());
      subDraftList = physicalInventorySubDraftRepository.findAll();
    } else {
      updatePhysicalInventoryIds = Collections.singletonList(physicalInventory.getId());
      subDraftList = physicalInventorySubDraftRepository.findByPhysicalInventoryId(
          physicalInventory.getId());
    }

    List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> updateExtensions = new ArrayList<>();
    AtomicInteger groupNum = new AtomicInteger();
    // Aggregate by product and sort by product
    List<List<PhysicalInventoryLineItemDto>> lists = groupByProductCode(physicalInventory.getLineItems());
    // Grouping
    List<List<List<PhysicalInventoryLineItemDto>>> groupList = CustomListSortHelper.averageAssign(lists, splitNum);

    List<PhysicalInventorySubDraft> finalSubDraftList = subDraftList;
    groupList.forEach(productList -> {
      groupNum.getAndIncrement();
      productList.forEach(lineItemList -> {
        lineItemList.forEach(lineItem -> {
          PhysicalInventoryLineItemsExtension extension = getExtension(extensions, lineItem);
          if (extension == null) {
            extension = PhysicalInventoryLineItemsExtension.builder()
                .orderableId(lineItem.getOrderableId())
                .lotId(lineItem.getLotId())
                .physicalInventoryId(UUID.fromString(
                    physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(lineItem.getProgramId(),
                        physicalInventory.getFacilityId(), true)))
                .build();
          }
          extension.setInitial(true);
          PhysicalInventoryLineItemsExtension finalExtension = extension;
          extension.setSubDraftId(
              finalSubDraftList.stream().filter(subDraft -> subDraft.getNum() == groupNum.get()
                      && subDraft.getPhysicalInventoryId().equals(finalExtension.getPhysicalInventoryId()))
                  .findFirst().orElseThrow(NullPointerException::new).getId());
          updateExtensions.add(extension);
        });
      });
    });
    log.info("save physical inventory extension, size: {}", updateExtensions.size());
    lineItemsExtensionRepository.save(updateExtensions);
  }

  private List<PhysicalInventoryLineItemDto> convertSummaryV2DtosToLineItems(List<StockCardSummaryV2Dto> summaryV2Dtos,
      UUID programId) {
    List<PhysicalInventoryLineItemDto> physicalInventoryLineItems = new LinkedList<>();
    if (CollectionUtils.isNotEmpty(summaryV2Dtos)) {
      summaryV2Dtos.forEach(orderableDto -> {
        if (CollectionUtils.isNotEmpty(orderableDto.getCanFulfillForMe())) {
          orderableDto.getCanFulfillForMe().forEach(dto -> {
            Map<String, String> extraData = new HashMap<>();
            if (dto.getLot() != null && dto.getStockCard() != null && dto.getOrderable() != null) {
              extraData.put("vvmStatus", null);
              extraData.put("stockCardId", String.valueOf(dto.getStockCard().getId()));
              physicalInventoryLineItems.add(
                  PhysicalInventoryLineItemDto.builder()
                      .orderableId(dto.getOrderable().getId())
                      .lotId(dto.getLot().getId())
                      .extraData(extraData)
                      .stockAdjustments(Collections.emptyList())
                      .programId(programId).build());
            }
          });
        }
      });
    }
    return physicalInventoryLineItems;
  }

  private PhysicalInventoryDto saveLineItemFromStockCardSummariesData(PhysicalInventoryDto physicalInventoryDto) {
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.set("facilityId", String.valueOf(physicalInventoryDto.getFacilityId()));
    parameters.set("programId", String.valueOf(physicalInventoryDto.getProgramId()));
    parameters.set("rightName", STOCK_INVENTORIES_EDIT);
    Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);

    List<StockCardSummaryV2Dto> summaryV2Dtos = siglusStockCardSummariesService.findSiglusStockCard(
        parameters, Collections.emptyList(), pageable).getContent();

    List<PhysicalInventoryLineItemDto> physicalInventoryLineItems = convertSummaryV2DtosToLineItems(
        summaryV2Dtos, physicalInventoryDto.getProgramId());

    PhysicalInventoryDto toBeSavedPhysicalInventoryDto = PhysicalInventoryDto.builder()
        .programId(physicalInventoryDto.getProgramId())
        .facilityId(physicalInventoryDto.getFacilityId()).lineItems(physicalInventoryLineItems)
        .id(physicalInventoryDto.getId()).build();

    if (CollectionUtils.isNotEmpty(physicalInventoryLineItems)) {
      return saveDraftForProductsForOneProgram(toBeSavedPhysicalInventoryDto);
    }
    return toBeSavedPhysicalInventoryDto;

  }

  private PhysicalInventoryDto createNewDraft(PhysicalInventoryDto physicalInventoryDto) {
    return physicalInventoryStockManagementService.createEmptyPhysicalInventory(physicalInventoryDto);
  }

  public PhysicalInventoryDto createAndSpiltNewDraftForOneProgram(PhysicalInventoryDto physicalInventoryDto,
      Integer splitNum) {
    PhysicalInventoryDto physicalInventory = createNewDraft(physicalInventoryDto);
    saveLineItemFromStockCardSummariesData(physicalInventory);
    splitPhysicalInventory(physicalInventory, splitNum);
    return physicalInventory;
  }

  public PhysicalInventoryDto createNewDraftDirectly(PhysicalInventoryDto dto) {
    return inventoryController.createEmptyPhysicalInventory(dto);
  }

  private void saveLineItemFromStockCardSummariesDataForAllProduct(
      PhysicalInventoryDto allProductPhysicalInventoryDto) {
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds();
    List<PhysicalInventoryLineItemDto> allProductLineItemDtoList = new LinkedList<>();
    for (UUID programId : supportedPrograms) {
      List<PhysicalInventoryDto> physicalInventoryDtos = getPhysicalInventoryDtos(programId,
          allProductPhysicalInventoryDto.getFacilityId(), true);
      if (CollectionUtils.isEmpty(physicalInventoryDtos)) {
        throw new IllegalArgumentException("there is not draft exists for program : " + programId);
      }
      PhysicalInventoryDto savedPhysicalInventoryDto = saveLineItemFromStockCardSummariesData(
          physicalInventoryDtos.get(0));
      if (savedPhysicalInventoryDto.getLineItems() != null && savedPhysicalInventoryDto.getLineItems().size() > 0) {
        allProductLineItemDtoList.addAll(savedPhysicalInventoryDto.getLineItems());
      }
    }
    allProductPhysicalInventoryDto.setLineItems(allProductLineItemDtoList);
  }

  public PhysicalInventoryDto createAndSplitNewDraftForAllProduct(PhysicalInventoryDto dto, Integer splitNum) {
    PhysicalInventoryDto allProductPhysicalInventoryDto = createNewDraftForAllProducts(dto);
    saveLineItemFromStockCardSummariesDataForAllProduct(allProductPhysicalInventoryDto);
    splitPhysicalInventory(allProductPhysicalInventoryDto, splitNum);
    return allProductPhysicalInventoryDto;
  }

  public PhysicalInventoryDto createNewDraftForAllProducts(PhysicalInventoryDto dto) {
    return doCreateNewDraftForAllProducts(dto, false);
  }

  public void createNewDraftForAllProductsDirectly(PhysicalInventoryDto dto) {
    doCreateNewDraftForAllProducts(dto, true);
  }

  public PhysicalInventoryDto saveDraft(PhysicalInventoryDto dto, UUID id) {
    physicalInventoryStockManagementService.savePhysicalInventory(id, dto);
    return dto;
  }

  public PhysicalInventoryDto saveDraftForProductsForOneProgram(PhysicalInventoryDto dto) {
    saveDraft(dto, dto.getId());
    List<PhysicalInventoryDto> physicalInventoryDtos = Collections.singletonList(dto);
    if (CollectionUtils.isNotEmpty(physicalInventoryDtos)) {
      return getResultInventory(physicalInventoryDtos, updateExtension(dto, physicalInventoryDtos));
    }
    return null;
  }

  public PhysicalInventoryDto saveDraftForAllProducts(PhysicalInventoryDto dto) {
    deletePhysicalInventoryForAllProducts(dto.getFacilityId());
    createNewDraftForAllProducts(dto);
    Set<UUID> programIds = dto.getLineItems().stream()
        .map(PhysicalInventoryLineItemDto::getProgramId).collect(Collectors.toSet());
    List<PhysicalInventoryDto> inventories = programIds.stream()
        .map(programId -> getPhysicalInventoryDtos(programId, dto.getFacilityId(), Boolean.TRUE))
        .flatMap(Collection::stream).collect(Collectors.toList());
    inventories.forEach(inventory -> inventory.setLineItems(
        dto.getLineItems().stream()
            .filter(lineItem -> lineItem.getProgramId().equals(inventory.getProgramId()))
            .collect(Collectors.toList())));
    inventories = inventories.stream()
        .map(inventory -> saveDraft(inventory, inventory.getId()))
        .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(inventories)) {
      return getResultInventoryForAllProducts(inventories, updateExtension(dto, inventories));
    }
    return null;
  }

  public void checkDraftIsExist(UUID facilityId) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    List<PhysicalInventoryDto> inventories = supportedPrograms.stream()
        .map(programId -> getPhysicalInventoryDtosDirectly(programId, facilityId, Boolean.TRUE))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(inventories)) {
      throw new ValidationMessageException("stockmanagement.error.physicalInventory.isSubmitted");
    }
  }

  public void deletePhysicalInventory(UUID id) {
    physicalInventoryStockManagementService.deletePhysicalInventory(id);
    physicalInventorySubDraftRepository.deletePhysicalInventorySubDraftsByPhysicalInventoryId(id);
  }

  public void deletePhysicalInventoryDirectly(UUID id) {
    inventoryController.deletePhysicalInventory(id);
  }

  public void deletePhysicalInventoryForProductInOneProgram(UUID facilityId, UUID programId) {
    doDeletePhysicalInventoryForProductInOneProgram(facilityId, programId, false);
  }

  public void deletePhysicalInventoryForProductInOneProgramDirectly(UUID facilityId, UUID programId) {
    doDeletePhysicalInventoryForProductInOneProgram(facilityId, programId, true);
  }

  public void deletePhysicalInventoryForAllProducts(UUID facilityId) {
    doDeletePhysicalInventoryForAllProducts(facilityId, false);
  }

  public void deletePhysicalInventoryForAllProductsDirectly(UUID facilityId) {
    doDeletePhysicalInventoryForAllProducts(facilityId, true);
  }

  public PhysicalInventoryDto getPhysicalInventory(UUID id) {
    return physicalInventoryStockManagementService.getPhysicalInventory(id);
  }

  public PhysicalInventoryDto getFullPhysicalInventoryDto(UUID physicalInventoryId) {
    PhysicalInventoryDto physicalInventoryDto = getPhysicalInventory(
        physicalInventoryId);
    List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(Lists.newArrayList(physicalInventoryId));
    return getResultInventory(Lists.newArrayList(physicalInventoryDto), extensions);
  }

  public PhysicalInventoryDto getPhysicalInventoryForAllProducts(UUID facilityId) {
    List<PhysicalInventoryDto> inventories = getPhysicalInventoryDtosForAllProducts(facilityId,
        Boolean.TRUE, Boolean.FALSE);
    if (CollectionUtils.isNotEmpty(inventories)) {
      return inventories.get(0);
    }
    return null;
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtos(UUID program, UUID facility,
      Boolean isDraft) {
    physicalInventoryService.checkPermission(program, facility);
    return physicalInventoryStockManagementService
        .searchPhysicalInventory(program, facility, isDraft);
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosDirectly(UUID program, UUID facility,
      Boolean isDraft) {
    return inventoryController.searchPhysicalInventory(program, facility, isDraft).getBody();
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosForProductsInOneProgram(
      UUID programId,
      UUID facilityId,
      Boolean isDraft) {
    physicalInventoryService.checkPermission(programId, facilityId);
    Set<UUID> supportedProgram = Collections.singleton(programId);

    List<PhysicalInventoryDto> inventories = supportedProgram.stream().map(
        supportedVirtualProgram -> getPhysicalInventoryDtos(supportedVirtualProgram, facilityId,
            isDraft)).flatMap(Collection::stream).collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(inventories)) {
      List<UUID> updatePhysicalInventoryIds =
          inventories.stream().map(PhysicalInventoryDto::getId).collect(Collectors.toList());
      log.info("find physical inventory extension in one program: {}", updatePhysicalInventoryIds);
      List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
          .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
      PhysicalInventoryDto resultInventory = getResultInventory(inventories, extensions);
      return Collections.singletonList(resultInventory);
    }
    return Collections.emptyList();
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosForAllProducts(
      UUID facilityId,
      Boolean isDraft,
      boolean canInitialInventory) {
    try {
      Set<UUID> supportedPrograms = supportedProgramsHelper
          .findHomeFacilitySupportedProgramIds();
      if (CollectionUtils.isEmpty(supportedPrograms)) {
        throw new PermissionMessageException(
            new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED,
                ALL_PRODUCTS_PROGRAM_ID));
      }
      List<PhysicalInventoryDto> inventories = supportedPrograms.stream().map(
          supportedVirtualProgram -> getPhysicalInventoryDtos(supportedVirtualProgram, facilityId,
              isDraft)).flatMap(Collection::stream).collect(Collectors.toList());

      if (CollectionUtils.isNotEmpty(inventories)) {
        List<UUID> updatePhysicalInventoryIds =
            inventories.stream().map(PhysicalInventoryDto::getId).collect(Collectors.toList());
        log.info("find physical inventory extension: {}", updatePhysicalInventoryIds);
        List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
            .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
        PhysicalInventoryDto resultInventory = getResultInventoryForAllProducts(inventories, extensions);
        return Collections.singletonList(resultInventory);
      }
      PhysicalInventoryDto dto = createInitialInventoryDraftForAllProducts(supportedPrograms,
          facilityId, canInitialInventory);
      if (dto != null) {
        inventories.add(dto);
      }
      return inventories;
    } catch (PermissionMessageException e) {
      throw new PermissionMessageException(
          new org.openlmis.stockmanagement.util.Message(ERROR_PERMISSION_NOT_SUPPORTED), e);
    }
  }

  private PhysicalInventoryDto doCreateNewDraftForAllProducts(PhysicalInventoryDto dto, boolean directly) {
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds();
    List<PhysicalInventoryDto> inventories = supportedPrograms.stream().map(
        supportedVirtualProgram -> {
          dto.setProgramId(supportedVirtualProgram);
          if (directly) {
            return createNewDraftDirectly(dto);
          } else {
            return createNewDraft(dto);
          }
        }).collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(inventories)) {
      return getResultInventoryForAllProducts(inventories, emptyList());
    }
    return null;
  }

  private void doDeletePhysicalInventoryForProductInOneProgram(UUID facilityId, UUID programId, boolean directly) {
    doDeletePhysicalInventoryCore(facilityId, programId, directly);
  }

  private void doDeletePhysicalInventoryCore(UUID facilityId, UUID programId, boolean directly) {
    String physicalInventoryId = physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(
        programId, facilityId, Boolean.TRUE);
    if (physicalInventoryId == null) {
      return;
    }
    List<UUID> physicalInventoryIdList = new ArrayList<>(
        Collections.singleton(UUID.fromString(physicalInventoryId)))
        .stream().filter(Objects::nonNull).collect(Collectors.toList());
    if (directly) {
      physicalInventoryIdList.forEach(this::deletePhysicalInventoryDirectly);
    } else {
      physicalInventoryIdList.forEach(this::deletePhysicalInventory);
    }
    lineItemsExtensionRepository.deleteByPhysicalInventoryIdIn(physicalInventoryIdList);
  }

  private void doDeletePhysicalInventoryForAllProducts(UUID facilityId, boolean directly) {
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds();
    supportedPrograms.forEach(programId -> doDeletePhysicalInventoryCore(facilityId, programId, directly));
  }

  private List<PhysicalInventoryLineItemsExtension> updateExtension(
      PhysicalInventoryDto inventoryDto,
      List<PhysicalInventoryDto> updatedDto) {
    List<UUID> updatePhysicalInventoryIds =
        updatedDto.stream().map(PhysicalInventoryDto::getId).collect(Collectors.toList());
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
              .orderableId(lineItem.getOrderableId())
              .lotId(lineItem.getLotId())
              .physicalInventoryId(dto.getId())
              .build();
        }
        extension.setReasonFreeText(getFreeTextByInput(inventoryDto, lineItem));
        updateExtensions.add(extension);
      });
    });

    log.info("save physical inventory extension, size: {}", updateExtensions.size());
    return lineItemsExtensionRepository.save(updateExtensions);
  }

  private PhysicalInventoryLineItemsExtension getExtension(
      List<PhysicalInventoryLineItemsExtension> extensions, PhysicalInventoryLineItemDto lineItem) {
    return extensions.stream()
        .filter(extension -> compareTwoId(extension.getLotId(), lineItem.getLotId())
            && compareTwoId(extension.getOrderableId(), lineItem.getOrderableId()))
        .findFirst()
        .orElse(null);
  }

  private boolean compareTwoId(UUID oneId, UUID secondId) {
    return oneId == null ? oneId == secondId : oneId.equals(secondId);
  }

  private String getFreeTextByInput(PhysicalInventoryDto inventoryDto,
      PhysicalInventoryLineItemDto lineItem) {
    PhysicalInventoryLineItemDto lineItemDto = inventoryDto.getLineItems().stream()
        .filter(itemDto -> itemDto.getLotId() == lineItem.getLotId()
            && itemDto.getOrderableId() == lineItem.getOrderableId())
        .findFirst()
        .orElse(null);
    return lineItemDto == null ? null : lineItemDto.getReasonFreeText();
  }

  private String getFreeTextByExtension(List<PhysicalInventoryLineItemsExtension> extensions,
      PhysicalInventoryLineItemDto lineItem) {
    if (extensions.isEmpty()) {
      return null;
    }
    PhysicalInventoryLineItemsExtension extension = getExtension(extensions, lineItem);
    return extension == null ? null : extension.getReasonFreeText();
  }


  private PhysicalInventoryDto createInitialInventoryDraftForAllProducts(
      Set<UUID> supportedPrograms, UUID facilityId, boolean canInitialInventory) {
    if (canInitialInventory(facilityId).isCanInitialInventory()) {
      List<PhysicalInventoryLineItemDto> physicalInventoryLineItemDtos =
          buildInitialInventoryLineItemDtos(supportedPrograms, facilityId);
      PhysicalInventoryDto dto = PhysicalInventoryDto.builder()
          .facilityId(facilityId)
          .programId(ALL_PRODUCTS_PROGRAM_ID)
          .lineItems(physicalInventoryLineItemDtos)
          .build();
      return saveDraftForAllProducts(dto);
    }
    if (canInitialInventory) {
      throw new ValidationMessageException(new Message(ERROR_NOT_ACCEPTABLE));
    }
    return null;
  }

  private List<PhysicalInventoryLineItemDto> buildInitialInventoryLineItemDtos(
      Set<UUID> supportedVirtualProgramIds, UUID facilityId) {
    return supportedVirtualProgramIds.stream()
        .map(programId ->
            approvedProductReferenceDataService
                .getApprovedProducts(facilityId, programId, emptyList()).stream()
                .map(ApprovedProductDto::getOrderable)
                .filter(o -> o.getExtraData().containsKey(IS_BASIC))
                .filter(o -> Boolean.parseBoolean(o.getExtraData().get(IS_BASIC)))
                .map(o -> newLineItem(o.getId(), programId))
                .collect(Collectors.toList())
        )
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private PhysicalInventoryLineItemDto newLineItem(UUID orderableId, UUID programId) {
    return PhysicalInventoryLineItemDto.builder()
        .orderableId(orderableId)
        .programId(programId)
        .build();
  }

  private PhysicalInventoryDto getResultInventory(List<PhysicalInventoryDto> inventories,
      List<PhysicalInventoryLineItemsExtension> extensions) {
    PhysicalInventoryDto resultInventory = getResultInventoryForAllProducts(inventories, extensions);
    resultInventory.setId(inventories.get(0).getId());
    resultInventory.setProgramId(inventories.get(0).getProgramId());
    return resultInventory;
  }

  private PhysicalInventoryDto getResultInventoryForAllProducts(List<PhysicalInventoryDto> inventories,
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
        .lineItems(inventories.stream()
            .map(inventory -> {
              Optional<List<PhysicalInventoryLineItemDto>> optionalList = Optional
                  .ofNullable(inventory.getLineItems());
              optionalList
                  .ifPresent(physicalInventoryLineItemDtos -> physicalInventoryLineItemDtos.forEach(
                      physicalInventoryLineItemDto -> {
                        physicalInventoryLineItemDto.setProgramId(inventory.getProgramId());
                        physicalInventoryLineItemDto.setReasonFreeText(
                            getFreeTextByExtension(extensions, physicalInventoryLineItemDto));
                      }));
              return optionalList.orElse(new ArrayList<>());
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toList()))
        .build();
  }

  public InitialInventoryFieldDto canInitialInventory(UUID facility) {
    FacilityDto homeFacility = facilityReferenceDataService.findOne(facility);
    String code = homeFacility.getType().getCode();
    if (code != null && Arrays.asList(AC, CENTRAL).contains(code)) {
      return new InitialInventoryFieldDto(false);
    }
    int stockCardCount = stockCardRepository.countByFacilityId(facility);
    return new InitialInventoryFieldDto(0 == stockCardCount);
  }

  public Set<String> findPhysicalInventoryDates(UUID programId, UUID facility,
      String startDate,
      String endDate) {
    List<PhysicalInventory> physicalInventories =
        physicalInventoriesRepository.findByProgramIdAndFacilityIdAndStartDateAndEndDate(programId,
            facility, startDate, endDate);
    return physicalInventories.stream()
        .map(physicalInventory -> physicalInventory.getOccurredDate().toString())
        .collect(Collectors.toSet());
  }

  public PhysicalInventoryDto findLatestPhysicalInventory(UUID facilityId, UUID programId) {
    PhysicalInventory latest = physicalInventoriesRepository
        .findTopByProgramIdAndFacilityIdAndIsDraftOrderByOccurredDateDesc(programId, facilityId, false);
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
}
