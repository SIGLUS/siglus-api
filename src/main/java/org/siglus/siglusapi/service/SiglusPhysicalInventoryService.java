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
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_UUID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_INVENTORY_CONFLICT_DRAFT;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_ACCEPTABLE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;

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
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.web.PhysicalInventoryController;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.BaseEntity;
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
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.CustomListSortHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.beans.BeanUtils;
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
  private SiglusApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Autowired
  private PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;

  @Autowired
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private PhysicalInventoryExtensionRepository physicalInventoryExtensionRepository;

  public List<PhysicalInventoryLineItemDto> buildInitialInventoryLineItemDtos(
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

  private PhysicalInventoryDto getPhysicalInventoryBySubDraftId(UUID subDraftId) {
    PhysicalInventorySubDraft subDraft = physicalInventorySubDraftRepository.findFirstById(subDraftId);
    PhysicalInventoryDto physicalInventory = getPhysicalInventory(subDraft.getPhysicalInventoryId());
    return getPhysicalInventoryDtosForProductsInOneProgram(
        physicalInventory.getProgramId(), physicalInventory.getFacilityId(), true).get(0);
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

    UUID programId = subDraftIds.size() > 1
            ? ALL_PRODUCTS_PROGRAM_ID
            : physicalInventory.getProgramId();
    UUID physicalInventoryId = subDraftIds.size() > 1
            ? ALL_PRODUCTS_UUID
            : physicalInventory.getId();
    physicalInventory.setId(physicalInventoryId);
    physicalInventory.setLineItems(sortedSubPhysicalInventoryLineItemList);
    physicalInventory.setProgramId(programId);
    return physicalInventory;
  }

  public SiglusPhysicalInventoryDto getSubLocationPhysicalInventoryDtoBySubDraftId(
          List<UUID> subDraftIds) {
    if (CollectionUtils.isEmpty(subDraftIds)) {
      throw new IllegalArgumentException("empty subDraftIds");
    }
    List<PhysicalInventoryLineItemDto> subPhysicalInventoryLineItemDtoList =
        getSubPhysicalInventoryLineItemListBySubDraftIds(subDraftIds);
    List<PhysicalInventoryLineItemDto> sortedSubPhysicalInventoryLineItemList = subPhysicalInventoryLineItemDtoList
        .stream().sorted(Comparator.comparing(
            o -> String.valueOf(
                orderableRepository.findLatestById(o.getOrderableId()).orElseThrow(IllegalArgumentException::new)
                    .getProductCode()))).collect(Collectors.toList());
    SiglusPhysicalInventoryDto physicalInventory = fillLocationOption(
            getPhysicalInventoryBySubDraftId(subDraftIds.get(0)));

    UUID programId = subDraftIds.size() > 1
        ? ALL_PRODUCTS_PROGRAM_ID
        : physicalInventory.getProgramId();
    UUID physicalInventoryId = subDraftIds.size() > 1
        ? ALL_PRODUCTS_UUID
        : physicalInventory.getId();
    physicalInventory.setId(physicalInventoryId);
    physicalInventory.setLineItems(sortedSubPhysicalInventoryLineItemList);
    physicalInventory.setProgramId(programId);
    return physicalInventory;
  }

  public SiglusPhysicalInventoryDto fillLocationOption(PhysicalInventoryDto physicalInventoryDto) {
    SiglusPhysicalInventoryDto dto = new SiglusPhysicalInventoryDto();
    BeanUtils.copyProperties(physicalInventoryDto, dto);
    List<PhysicalInventoryExtension> extensions = physicalInventoryExtensionRepository
            .findByPhysicalInventoryId(physicalInventoryDto.getId());
    if (CollectionUtils.isNotEmpty(extensions) && null != extensions.get(0).getLocationOption()) {
      dto.setLocationOption(extensions.get(0).getLocationOption().getValue());
    }
    return dto;
  }

  private void createEmptySubDraft(Integer spiltNum, PhysicalInventoryDto physicalInventoryDto) {

    if (physicalInventoryDto.getProgramId().equals(ALL_PRODUCTS_PROGRAM_ID)) {
      Set<UUID> supportedPrograms = supportedProgramsHelper
          .findHomeFacilitySupportedProgramIds();
      supportedPrograms.forEach(programId -> {
        UUID physicalInventoryId = UUID.fromString(physicalInventoriesRepository
            .findIdByProgramIdAndFacilityIdAndIsDraft(
                programId, physicalInventoryDto.getFacilityId(), true));
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

  public DraftListDto getSubDraftListForAllProduct(UUID facility, Boolean isDraft) {
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds();
    List<PhysicalInventorySubDraft> allProductSubDraftList = new LinkedList<>();
    supportedPrograms.forEach(programId -> {
      List<PhysicalInventoryDto> physicalInventoryDtoList = getPhysicalInventoryDtos(programId, facility, isDraft);
      List<UUID> physicalInventoryIds = physicalInventoryDtoList
          .stream()
          .map(PhysicalInventoryDto::getId)
          .collect(Collectors.toList());
      List<PhysicalInventorySubDraft> subDraftList = physicalInventorySubDraftRepository
          .findByPhysicalInventoryIdIn(physicalInventoryIds);
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

  public PhysicalInventoryValidationDto checkConflictForAllProduct(UUID facility) {
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

  public PhysicalInventoryValidationDto checkConflictForOneProgram(UUID facility) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    if (CollectionUtils.isEmpty(supportedPrograms)) {
      throw new PermissionMessageException(new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED));
    }

    List<Boolean> draftCreateByALlProduct = Lists.newArrayList();
    supportedPrograms.forEach(supportedProgramId -> {
      List<PhysicalInventory> programHaveDraft = physicalInventoriesRepository
          .findByProgramIdAndFacilityIdAndIsDraft(supportedProgramId, facility, true);
      if (CollectionUtils.isNotEmpty(programHaveDraft)) {
        List<PhysicalInventoryExtension> programWithDraftList = physicalInventoryExtensionRepository
            .findByPhysicalInventoryId(programHaveDraft.get(0).getId());
        if (CollectionUtils.isNotEmpty(programWithDraftList) && ALL_PROGRAM.equals(programWithDraftList.get(0)
                .getCategory())) {
          draftCreateByALlProduct.add(true);
        }
      }
    });

    if (CollectionUtils.isNotEmpty(draftCreateByALlProduct) && draftCreateByALlProduct.contains(true)) {
      return buildPhysicalInventoryValidationDto(false, Lists.newArrayList(ALL_PRODUCTS_UUID));
    }
    return buildPhysicalInventoryValidationDto(true, Lists.newArrayList());
  }

  private List<List<PhysicalInventoryLineItemDto>> groupByProductCode(List<PhysicalInventoryLineItemDto> lineItemDtos) {

    List<List<PhysicalInventoryLineItemDto>> list = new LinkedList<>();
    Map<UUID, List<PhysicalInventoryLineItemDto>> orderableIdToLineItemsMap = lineItemDtos.stream()
        .collect(Collectors.groupingBy(PhysicalInventoryLineItemDto::getOrderableId));
    orderableIdToLineItemsMap.forEach((orderablesId, lineItems) -> list.add(lineItems));

    list.sort(Comparator.comparing(o -> String.valueOf(
        orderableRepository.findLatestById(o.get(0).getOrderableId()).orElseThrow(IllegalArgumentException::new)
            .getProductCode())));
    return list;
  }


  private void splitPhysicalInventory(PhysicalInventoryDto dto, Integer splitNum) {
    createEmptySubDraft(splitNum, dto);
    spiltLineItem(dto, splitNum);
  }

  private void spiltLineItem(PhysicalInventoryDto physicalInventory, Integer splitNum) {
    if (physicalInventory == null
        || physicalInventory.getLineItems() == null
        || physicalInventory.getLineItems().isEmpty()) {
      return;
    }
    List<UUID> updatePhysicalInventoryIds;
    if (physicalInventory.getProgramId().equals(ALL_PRODUCTS_PROGRAM_ID)) {
      updatePhysicalInventoryIds = getSupportPhysicalInventoryIds(physicalInventory.getFacilityId());
    } else {
      updatePhysicalInventoryIds = Collections.singletonList(physicalInventory.getId());
    }
    List<PhysicalInventorySubDraft> subDraftList = physicalInventorySubDraftRepository
        .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);

    // Aggregate by product and sort by product
    List<List<PhysicalInventoryLineItemDto>> lists = groupByProductCode(physicalInventory.getLineItems());
    // Grouping
    List<List<List<PhysicalInventoryLineItemDto>>> groupList = CustomListSortHelper.averageAssign(lists, splitNum);

    List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> updateExtensions = establishConnectionBetweenSubDraftAndLineItemExtension(
        physicalInventory.getFacilityId(), subDraftList, extensions, groupList);
    log.info("save physical inventory extension, size: {}", updateExtensions.size());
    lineItemsExtensionRepository.save(updateExtensions);
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
        PhysicalInventoryLineItemsExtension extension = getExtension(extensions, lineItem);
        if (extension == null) {
          extension = PhysicalInventoryLineItemsExtension
              .builder()
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
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds();
    return supportedPrograms.stream().map(programId ->
            UUID.fromString(physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(
                programId, facilityId, true)))
        .collect(Collectors.toList());
  }

  private List<PhysicalInventoryLineItemDto> convertSummaryV2DtosToLineItems(List<StockCardSummaryV2Dto> summaryV2Dtos,
      UUID programId) {
    List<PhysicalInventoryLineItemDto> physicalInventoryLineItems = new LinkedList<>();
    if (CollectionUtils.isNotEmpty(summaryV2Dtos)) {
      summaryV2Dtos.forEach(orderableDto -> {
        if (CollectionUtils.isNotEmpty(orderableDto.getCanFulfillForMe())) {
          orderableDto.getCanFulfillForMe().forEach(dto -> {
            Map<String, String> extraData = new HashMap<>();
            if (dto.getStockCard() != null && dto.getOrderable() != null) {
              extraData.put(VM_STATUS, null);
              extraData.put(STOCK_CARD_ID, String.valueOf(dto.getStockCard().getId()));
              physicalInventoryLineItems.add(
                  PhysicalInventoryLineItemDto.builder()
                      .orderableId(dto.getOrderable().getId())
                      .lotId(dto.getLot() != null ? dto.getLot().getId() : null)
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

  private PhysicalInventoryLineItemDto newLineItem(UUID orderableId, UUID programId) {
    return PhysicalInventoryLineItemDto.builder()
        .orderableId(orderableId)
        .programId(programId)
        .build();
  }

  private List<PhysicalInventoryLineItemDto> buildPhysicalInventoryLineItemDtos(
      PhysicalInventoryDto physicalInventoryDto) {
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.set(FACILITY_ID, String.valueOf(physicalInventoryDto.getFacilityId()));
    parameters.set(PROGRAM_ID, String.valueOf(physicalInventoryDto.getProgramId()));
    parameters.set(RIGHT_NAME, STOCK_INVENTORIES_EDIT);
    parameters.set(EXCLUDE_ARCHIVED, Boolean.TRUE.toString());
    Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);

    List<StockCardSummaryV2Dto> summaryV2Dtos = siglusStockCardSummariesService.findSiglusStockCard(
        parameters, Collections.emptyList(), pageable).getContent();

    return convertSummaryV2DtosToLineItems(summaryV2Dtos, physicalInventoryDto.getProgramId());
  }

  private PhysicalInventoryDto getPhysicalInventoryWithLineItemForOneProgram(PhysicalInventoryDto physicalInventoryDto,
      boolean initialPhysicalInventory) {
    List<PhysicalInventoryLineItemDto> physicalInventoryLineItems;
    if (initialPhysicalInventory) {
      physicalInventoryLineItems = buildInitialInventoryLineItemDtos(
          Collections.singleton(physicalInventoryDto.getProgramId()),
          physicalInventoryDto.getFacilityId());
    } else {
      physicalInventoryLineItems = buildPhysicalInventoryLineItemDtos(physicalInventoryDto);
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

  private PhysicalInventoryDto createNewDraft(PhysicalInventoryDto physicalInventoryDto) {
    List<PhysicalInventoryDto> physicalInventory = getPhysicalInventoryDtosForProductsInOneProgram(
        physicalInventoryDto.getProgramId(), physicalInventoryDto.getFacilityId(), true);
    if (CollectionUtils.isNotEmpty(physicalInventory)) {
      throw new BusinessDataException(new Message(ERROR_INVENTORY_CONFLICT_DRAFT), null);
    }
    return inventoryController.createEmptyPhysicalInventory(physicalInventoryDto);
  }

  public PhysicalInventoryDto createAndSpiltNewDraftForOneProgram(PhysicalInventoryDto physicalInventoryDto,
      Integer splitNum, String optionString) {
    LocationManagementOption option = null;
    if (StringUtils.isNotEmpty(optionString)) {
      option = LocationManagementOption.fromString(optionString);
    }
    if (!canDailyOperateInventory(physicalInventoryDto.getFacilityId(), false)) {
      throw new ValidationMessageException(new Message(ERROR_NOT_ACCEPTABLE));
    }
    PhysicalInventoryDto physicalInventory = createNewDraft(physicalInventoryDto);
    PhysicalInventoryExtension physicalInventoryExtension = buildPhysicalInventoryExtension(
        physicalInventory, false, option);
    log.info("physical inventory extension input for one program: {}", physicalInventoryExtension);
    physicalInventoryExtensionRepository.save(physicalInventoryExtension);

    physicalInventory = getPhysicalInventoryWithLineItemForOneProgram(physicalInventory, false);
    splitPhysicalInventory(physicalInventory, splitNum);
    return physicalInventory;
  }

  private PhysicalInventoryDto createNewDraftDirectly(PhysicalInventoryDto dto) {
    return inventoryController.createEmptyPhysicalInventory(dto);
  }

  private PhysicalInventoryDto getPhysicalInventoryWithLineItemForAllProduct(
      PhysicalInventoryDto allProductPhysicalInventoryDto, boolean initialPhysicalInventory) {
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds();
    List<PhysicalInventoryLineItemDto> allProductLineItemDtoList = new LinkedList<>();
    for (UUID programId : supportedPrograms) {
      List<PhysicalInventoryDto> physicalInventoryDtos = getPhysicalInventoryDtos(programId,
          allProductPhysicalInventoryDto.getFacilityId(), true);
      if (CollectionUtils.isEmpty(physicalInventoryDtos)) {
        throw new IllegalArgumentException("there is not draft exists for program : " + programId);
      }
      PhysicalInventoryDto savedPhysicalInventoryDto = getPhysicalInventoryWithLineItemForOneProgram(
          physicalInventoryDtos.get(0), initialPhysicalInventory);
      List<PhysicalInventoryLineItemDto> lineItems = savedPhysicalInventoryDto.getLineItems();
      if (CollectionUtils.isNotEmpty(lineItems)) {
        allProductLineItemDtoList.addAll(lineItems);
      }
    }
    allProductPhysicalInventoryDto.setLineItems(allProductLineItemDtoList);
    return allProductPhysicalInventoryDto;
  }

  public PhysicalInventoryDto createAndSplitNewDraftForAllProduct(PhysicalInventoryDto physicalInventoryDto,
                                                                        Integer splitNum,
                                                                        boolean initialPhysicalInventory,
                                                                        String optionString) {
    if (!canInitialOrDailyOperateInventory(physicalInventoryDto.getFacilityId(), initialPhysicalInventory)) {
      throw new ValidationMessageException(new Message(ERROR_NOT_ACCEPTABLE));
    }
    LocationManagementOption option = null;
    if (StringUtils.isNotEmpty(optionString)) {
      option = LocationManagementOption.fromString(optionString);
    }
    PhysicalInventoryDto allProductPhysicalInventoryDto = createNewDraftForAllProducts(physicalInventoryDto, option);
    allProductPhysicalInventoryDto = getPhysicalInventoryWithLineItemForAllProduct(allProductPhysicalInventoryDto,
        initialPhysicalInventory);
    splitPhysicalInventory(allProductPhysicalInventoryDto, splitNum);
    return allProductPhysicalInventoryDto;
  }

  public PhysicalInventoryDto createNewDraftForAllProducts(PhysicalInventoryDto dto,
                                                           LocationManagementOption locationOption) {
    return doCreateNewDraftForAllProducts(dto, false, locationOption);
  }

  public void createNewDraftForAllProductsDirectly(PhysicalInventoryDto dto,
                                                   LocationManagementOption locationOption) {
    doCreateNewDraftForAllProducts(dto, true, locationOption);
  }

  private PhysicalInventoryDto saveDraft(PhysicalInventoryDto dto, UUID id) {
    inventoryController.savePhysicalInventory(id, dto);
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

  public PhysicalInventoryDto saveDraftForProductsForOneProgramWithExtension(
      PhysicalInventoryLineItemExtensionDto dto) {
    saveDraft(dto, dto.getId());
    List<PhysicalInventoryDto> physicalInventoryDtos = Collections.singletonList(dto);
    if (CollectionUtils.isNotEmpty(physicalInventoryDtos)) {
      return getResultInventory(physicalInventoryDtos, updateExtensionWithSubDraft(dto, physicalInventoryDtos));
    }
    return null;
  }

  public PhysicalInventoryDto saveDraftForAllProducts(PhysicalInventoryDto dto) {
    deletePhysicalInventoryDraftForAllProgramsWithSubDraft(dto.getFacilityId());
    createNewDraftForAllProducts(dto, null);
    Set<UUID> programIds = dto.getLineItems().stream()
        .map(PhysicalInventoryLineItemDto::getProgramId).collect(Collectors.toSet());
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

  public void deletePhysicalInventoryDraftWithSubDrafts(UUID id) {
    deletePhysicalInventoryDraftById(id);
    physicalInventorySubDraftRepository.deletePhysicalInventorySubDraftsByPhysicalInventoryId(id);
  }

  public void deletePhysicalInventoryDraftById(UUID id) {
    inventoryController.deletePhysicalInventory(id);
  }

  public void deletePhysicalInventoryDraftForOneProgramWithSubDraft(UUID facilityId, UUID programId) {
    doDeletePhysicalInventoryForProductInOneProgram(facilityId, programId, true);
  }

  public void deletePhysicalInventoryDraftForOneProgram(UUID facilityId, UUID programId) {
    doDeletePhysicalInventoryForProductInOneProgram(facilityId, programId, false);
  }

  public void deletePhysicalInventoryDraftForAllProgramsWithSubDraft(UUID facilityId) {
    doDeletePhysicalInventoryForAllProducts(facilityId, true);
  }

  public void deletePhysicalInventoryDraftForAllPrograms(UUID facilityId) {
    doDeletePhysicalInventoryForAllProducts(facilityId, false);
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

  public PhysicalInventoryDto getPhysicalInventoryForAllProducts(UUID facilityId) {
    List<PhysicalInventoryDto> inventories = getPhysicalInventoryDtosForAllProducts(facilityId,
        Boolean.TRUE);
    if (CollectionUtils.isNotEmpty(inventories)) {
      return inventories.get(0);
    }
    return null;
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtos(UUID program, UUID facility,
      Boolean isDraft) {
    return getPhysicalInventoryDtosDirectly(program, facility, isDraft);
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosDirectly(UUID program, UUID facility,
      Boolean isDraft) {
    return inventoryController.searchPhysicalInventory(program, facility, isDraft).getBody();
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosForProductsInOneProgram(
      UUID programId,
      UUID facilityId,
      Boolean isDraft) {
    Set<UUID> supportedPrograms = Collections.singleton(programId);

    List<PhysicalInventoryDto> inventories =
        fetchPhysicalInventories(supportedPrograms, facilityId, isDraft);
    if (CollectionUtils.isNotEmpty(inventories)) {
      List<UUID> updatePhysicalInventoryIds =
          inventories.stream().map(PhysicalInventoryDto::getId).collect(Collectors.toList());
      log.info("find physical inventory extension in one program: {}", updatePhysicalInventoryIds);
      List<PhysicalInventoryLineItemsExtension> extensions =
          lineItemsExtensionRepository.findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
      PhysicalInventoryDto resultInventory = getResultInventory(inventories, extensions);

      return Collections.singletonList(resultInventory);
    }
    return Collections.emptyList();
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosForAllProducts(
      UUID facilityId,
      Boolean isDraft) {
    try {
      Set<UUID> supportedPrograms = supportedProgramsHelper
          .findHomeFacilitySupportedProgramIds();
      if (CollectionUtils.isEmpty(supportedPrograms)) {
        throw new PermissionMessageException(
            new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED,
                ALL_PRODUCTS_PROGRAM_ID));
      }
      List<PhysicalInventoryDto> inventories =
          fetchPhysicalInventories(supportedPrograms, facilityId, isDraft);
      if (CollectionUtils.isNotEmpty(inventories)) {
        List<UUID> updatePhysicalInventoryIds =
            inventories.stream().map(PhysicalInventoryDto::getId).collect(Collectors.toList());
        log.info("find physical inventory extension: {}", updatePhysicalInventoryIds);
        List<PhysicalInventoryLineItemsExtension> extensions =
            lineItemsExtensionRepository.findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
        PhysicalInventoryDto resultInventory =
            getResultInventoryForAllProducts(inventories, extensions);
        return Collections.singletonList(resultInventory);
      }
      return inventories;
    } catch (PermissionMessageException e) {
      throw new PermissionMessageException(
          new org.openlmis.stockmanagement.util.Message(ERROR_PERMISSION_NOT_SUPPORTED), e);
    }
  }

  private List<PhysicalInventoryDto> fetchPhysicalInventories(
      Set<UUID> supportedPrograms, UUID facilityId, Boolean isDraft) {
    return supportedPrograms.stream()
        .map(it -> getPhysicalInventoryDtos(it, facilityId, isDraft))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private PhysicalInventoryDto doCreateNewDraftForAllProducts(PhysicalInventoryDto dto,
                                                              boolean directly,
                                                              LocationManagementOption locationOption) {
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds();
    List<PhysicalInventoryDto> inventories = supportedPrograms.stream().map(
        supportedVirtualProgram -> {
          // avoid direct change dto
          PhysicalInventoryDto copy = new PhysicalInventoryDto();
          BeanUtils.copyProperties(dto, copy);
          copy.setProgramId(supportedVirtualProgram);
          if (directly) {
            return createNewDraftDirectly(copy);
          } else {
            return createNewDraft(copy);
          }
        }).collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(inventories)) {
      inventories.forEach(eachInventory -> {
        PhysicalInventoryExtension physicalInventoryExtension = buildPhysicalInventoryExtension(
            eachInventory, true, locationOption);
        log.info("physical inventory extension input for all product: {}", physicalInventoryExtension);
        physicalInventoryExtensionRepository.save(physicalInventoryExtension);
      });
      return getResultInventoryForAllProducts(inventories, emptyList());
    }
    return null;
  }

  private void doDeletePhysicalInventoryForProductInOneProgram(UUID facilityId, UUID programId, boolean hasSubDraft) {
    doDeletePhysicalInventoryCore(facilityId, programId, hasSubDraft);
  }

  private void doDeletePhysicalInventoryCore(UUID facilityId, UUID programId, boolean hasSubDraft) {
    String physicalInventoryId = physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(
        programId, facilityId, Boolean.TRUE);
    if (physicalInventoryId == null) {
      return;
    }
    List<UUID> physicalInventoryIdList = new ArrayList<>(
        Collections.singleton(UUID.fromString(physicalInventoryId)))
        .stream().filter(Objects::nonNull).collect(Collectors.toList());
    if (hasSubDraft) {
      physicalInventoryIdList.forEach(this::deletePhysicalInventoryDraftWithSubDrafts);
    } else {
      physicalInventoryIdList.forEach(this::deletePhysicalInventoryDraftById);
    }
    lineItemsExtensionRepository.deleteByPhysicalInventoryIdIn(physicalInventoryIdList);
  }

  private void doDeletePhysicalInventoryForAllProducts(UUID facilityId, boolean hasSubDraft) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    supportedPrograms.forEach(programId -> doDeletePhysicalInventoryCore(facilityId, programId, hasSubDraft));
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
    Map<String, UUID> lineItemsExtensionMap = lineItemsExtensions.stream()
        .collect(Collectors.toMap(item -> getUniqueKey(item.getOrderableId(), item.getLotId(), item.getLocationCode()),
            PhysicalInventorySubDraftLineItemsExtensionDto::getSubDraftId, (item1, item2) -> item1));

    List<UUID> updatePhysicalInventoryIds =
        updatedDto.stream().map(PhysicalInventoryDto::getId).collect(Collectors.toList());
    log.info("find physical inventory extension: {}", updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> updateExtensions = new ArrayList<>();

    Set<UUID> usedExtensionIds = new HashSet<>();
    Set<String> uniqueKeys = new HashSet<>();
    updatedDto.forEach(dto -> {
      if (dto.getLineItems() == null || dto.getLineItems().isEmpty()) {
        return;
      }
      dto.getLineItems().forEach(lineItem -> {
        uniqueKeys.add(getUniqueKey(lineItem.getOrderableId(), lineItem.getLotId(), lineItem.getLocationCode()));
        PhysicalInventoryLineItemsExtension extension = getUnusedExtension(extensions, lineItem, usedExtensionIds);
        if (extension == null) {
          UUID subDraftId = lineItemsExtensionMap.get(getUniqueKey(lineItem.getOrderableId(),
                          lineItem.getLotId(), lineItem.getLocationCode()));
          extension = PhysicalInventoryLineItemsExtension.builder()
              .orderableId(lineItem.getOrderableId())
              .lotId(lineItem.getLotId())
              .physicalInventoryId(dto.getId())
              .initial(false)
              .subDraftId(subDraftId)
              .build();
        } else {
          usedExtensionIds.add(extension.getId());
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
        .filter(item -> !uniqueKeys.contains(getUniqueKey(item.getOrderableId(),
                item.getLotId(), item.getLocationCode())))
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

  private String getUniqueKey(UUID orderableId, UUID lotId, String locationCode) {
    return getString(orderableId) + SEPARATOR + getString(lotId) + SEPARATOR + getString(locationCode);
  }

  // TODO lineItem form PhysicalInventoryDto, locationCode is always null, then match multiple extensions
  // Then how to identify which quantity match which one???
  private PhysicalInventoryLineItemsExtension getUnusedExtensionIgnoreLocationCode(
          List<PhysicalInventoryLineItemsExtension> extensions,
          PhysicalInventoryLineItemDto lineItem,
          Set<UUID> usedIds) {
    return extensions.stream()
            .filter(extension -> Objects.equals(extension.getOrderableId(), lineItem.getOrderableId())
                    && Objects.equals(extension.getLotId(), lineItem.getLotId()))
            .filter(extension -> !usedIds.contains(extension.getId()))
            .findFirst()
            .orElse(null);
  }

  private PhysicalInventoryLineItemsExtension getUnusedExtension(
          List<PhysicalInventoryLineItemsExtension> extensions,
          PhysicalInventoryLineItemDto lineItem,
          Set<UUID> usedIds) {
    return extensions.stream()
            .filter(extension -> Objects.equals(extension.getOrderableId(), lineItem.getOrderableId())
                    && Objects.equals(extension.getLotId(), lineItem.getLotId())
                    && Objects.equals(extension.getLocationCode(), lineItem.getLocationCode()))
            .filter(extension -> !usedIds.contains(extension.getId()))
            .findFirst()
            .orElse(null);
  }

  private PhysicalInventoryLineItemsExtension getExtension(
      List<PhysicalInventoryLineItemsExtension> extensions, PhysicalInventoryLineItemDto lineItem) {
    return extensions.stream()
        .filter(extension -> Objects.equals(extension.getOrderableId(), lineItem.getOrderableId())
                                && Objects.equals(extension.getLotId(), lineItem.getLotId()))
        .findFirst()
        .orElse(null);
  }

  private boolean compareTwoId(UUID oneId, UUID secondId) {
    return oneId == null ? oneId == secondId : oneId.equals(secondId);
  }

  private PhysicalInventoryLineItemDto getMatchedLineItem(PhysicalInventoryDto inventoryDto,
                                                          PhysicalInventoryLineItemDto lineItem) {
    return inventoryDto.getLineItems().stream()
            .filter(itemDto -> Objects.equals(itemDto.getLotId(), lineItem.getLotId())
                    && Objects.equals(itemDto.getOrderableId(), lineItem.getOrderableId())
                    && Objects.equals(itemDto.getLocationCode(), lineItem.getLocationCode()))
            .findFirst()
            .orElse(null);
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
        .lineItems(getLineItems(inventories, extensions))
        .build();
  }

  private List<PhysicalInventoryLineItemDto> getLineItems(List<PhysicalInventoryDto> inventories,
                                                          List<PhysicalInventoryLineItemsExtension> extensions) {
    // avoid one extension match on multiple lines, no locationCode in inventories.lineItem
    Set<UUID> usedExtensionIds = new HashSet<>();
    return inventories.stream()
            .map(inventory -> {
              Optional<List<PhysicalInventoryLineItemDto>> optionalList = Optional
                    .ofNullable(inventory.getLineItems());
              optionalList
                    .ifPresent(physicalInventoryLineItemDtos -> physicalInventoryLineItemDtos.forEach(
                        physicalInventoryLineItemDto -> {
                          physicalInventoryLineItemDto.setProgramId(inventory.getProgramId());
                          PhysicalInventoryLineItemsExtension extension = getUnusedExtensionIgnoreLocationCode(
                                  extensions, physicalInventoryLineItemDto, usedExtensionIds);
                          if (extension != null) {
                            physicalInventoryLineItemDto.setReasonFreeText(extension.getReasonFreeText());
                            physicalInventoryLineItemDto.setArea(extension.getArea());
                            physicalInventoryLineItemDto.setLocationCode(extension.getLocationCode());
                            usedExtensionIds.add(extension.getId());
                          }
                        }));
              return optionalList.orElse(new ArrayList<>());
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
  }

  public InitialInventoryFieldDto canInitialInventory(UUID facility) {
    return new InitialInventoryFieldDto(
        isFacilityLegalForInitialInventory(facility)
            && isStockCardCountEmpty(facility));

  }

  private boolean isFacilityLegalForInitialInventory(UUID facility) {
    String code = facilityReferenceDataService.findOne(facility).getType().getCode();
    return code != null && !Arrays.asList(AC, CENTRAL).contains(code);
  }

  private boolean isStockCardCountEmpty(UUID facility) {
    return stockCardRepository.countByFacilityId(facility) == 0;
  }

  private boolean canInitialOrDailyOperateInventory(UUID facilityId, boolean initialPhysicalInventory) {
    boolean leGalFacility = isFacilityLegalForInitialInventory(facilityId);
    boolean emptyStockCardCount = isStockCardCountEmpty(facilityId);
    return (leGalFacility && emptyStockCardCount && initialPhysicalInventory)
        || (leGalFacility && !emptyStockCardCount && !initialPhysicalInventory);
  }

  private boolean canDailyOperateInventory(UUID facilityId, boolean initialPhysicalInventory) {
    boolean leGalFacility = isFacilityLegalForInitialInventory(facilityId);
    boolean emptyStockCardCount = isStockCardCountEmpty(facilityId);
    return leGalFacility && !emptyStockCardCount && !initialPhysicalInventory;
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

  private PhysicalInventoryExtension buildPhysicalInventoryExtension(
      PhysicalInventoryDto physicalInventoryDto,
      boolean isForAllProduct,
      LocationManagementOption locationOption) {
    return PhysicalInventoryExtension
        .builder()
        .physicalInventoryId(physicalInventoryDto.getId())
        .category(isForAllProduct ? ALL_PROGRAM : SINGLE_PROGRAM)
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
