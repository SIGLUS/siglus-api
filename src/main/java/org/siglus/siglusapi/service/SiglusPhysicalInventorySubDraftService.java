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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_INVENTORY_CONFLICT_SUB_DRAFT;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.siglusapi.domain.PhysicalInventoryEmptyLocationLineItem;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.siglus.siglusapi.domain.PhysicalInventorySubDraft;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.PhysicalInventoryLineItemExtensionDto;
import org.siglus.siglusapi.dto.PhysicalInventorySubDraftLineItemsExtensionDto;
import org.siglus.siglusapi.dto.ProductSubDraftConflictDto;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.PhysicalInventoryEmptyLocationLineItemRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD"})
public class SiglusPhysicalInventorySubDraftService {

  private final SiglusStockCardSummariesService siglusStockCardSummariesService;
  private final SiglusOrderableService siglusOrderableService;
  private final PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;
  private final SiglusPhysicalInventoryService siglusPhysicalInventoryService;
  private final PhysicalInventoryLineItemsExtensionRepository lineItemsExtensionRepository;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final PhysicalInventoryEmptyLocationLineItemRepository physicalInventoryEmptyLocationLineItemRepository;
  public static final String DRAFT = "Draft ";

  @Transactional
  public void deleteSubDrafts(List<UUID> subDraftIds, boolean initialPhysicalInventory, boolean isByLocation) {
    log.info("deleteSubDrafts, subDraftIds=" + subDraftIds);
    try {
      List<PhysicalInventorySubDraft> physicalInventorySubDrafts = updateSubDraftsStatus(subDraftIds,
          PhysicalInventorySubDraftEnum.NOT_YET_STARTED, true);
      doDelete(physicalInventorySubDrafts, subDraftIds, initialPhysicalInventory);
      if (isByLocation) {
        List<PhysicalInventoryEmptyLocationLineItem> emptyLocationLineItems
            = physicalInventoryEmptyLocationLineItemRepository.findBySubDraftIdIn(subDraftIds);
        emptyLocationLineItems.forEach(e -> {
          e.setHasProduct(false);
          e.setSkipped(false);
        });
        physicalInventoryEmptyLocationLineItemRepository.save(emptyLocationLineItems);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    }
  }

  private List<PhysicalInventorySubDraft> updateSubDraftsStatus(List<UUID> subDraftIds,
      PhysicalInventorySubDraftEnum subDraftStatus, boolean isDelete) {
    UUID currentUserId = authenticationHelper.getCurrentUserId().orElseThrow(IllegalStateException::new);
    log.info("updateSubDraftsStatus, subDraftIds=" + subDraftIds);
    List<PhysicalInventorySubDraft> subDrafts = physicalInventorySubDraftRepository.findAll(subDraftIds);
    if (CollectionUtils.isEmpty(subDrafts)) {
      return new ArrayList<>();
    }
    subDrafts.forEach(item -> {
      if (PhysicalInventorySubDraftEnum.SUBMITTED == item.getStatus()) {
        throw new IllegalStateException("can't operate submitted sub draft. sub draft id = " + item.getId());
      }
      item.setStatus(subDraftStatus);
      item.setOperatorId(isDelete ? null : currentUserId);
    });

    physicalInventorySubDraftRepository.save(subDrafts);

    return subDrafts;
  }


  private void resetInitialLineItems(UUID physicalInventoryId, UUID subDraftId) {
    lineItemsExtensionRepository.deleteByPhysicalInventoryIdIn(Collections.singleton(physicalInventoryId));
    PhysicalInventoryDto physicalInventoryDto = siglusPhysicalInventoryService.getFullPhysicalInventoryDto(
        physicalInventoryId);
    List<PhysicalInventoryLineItemDto> originalInitialInventoryLineItems = siglusPhysicalInventoryService
        .buildInitialInventoryLineItemDtos(Collections.singleton(physicalInventoryDto.getProgramId()),
            physicalInventoryDto.getFacilityId());
    if (CollectionUtils.isNotEmpty(originalInitialInventoryLineItems)) {
      physicalInventoryDto.setLineItems(originalInitialInventoryLineItems);
      List<PhysicalInventorySubDraftLineItemsExtensionDto> newLineItemsExtension = convertToLineItemsExtension(
          originalInitialInventoryLineItems,
          subDraftId,
          physicalInventoryDto.getId());
      PhysicalInventoryLineItemExtensionDto physicalInventoryExtendDto = new PhysicalInventoryLineItemExtensionDto();
      BeanUtils.copyProperties(physicalInventoryDto, physicalInventoryExtendDto);
      physicalInventoryExtendDto.setLineItemsExtensions(newLineItemsExtension);
      siglusPhysicalInventoryService.saveDraftForProductsForOneProgramWithExtension(physicalInventoryExtendDto);
      siglusPhysicalInventoryService.saveDraftForProductsForOneProgram(physicalInventoryDto);
    }
  }

  private void doDelete(List<PhysicalInventorySubDraft> subDrafts,
      List<UUID> subDraftIds,
      boolean initialPhysicalInventory) {
    Map<UUID, UUID> physicalInventoryIdToSubDraftIdMap = subDrafts.stream()
        .collect(Collectors.toMap(PhysicalInventorySubDraft::getPhysicalInventoryId, PhysicalInventorySubDraft::getId));
    List<StockCardSummaryV2Dto> stockSummaries = siglusStockCardSummariesService.findAllProgramStockSummaries();
    Set<CanFulfillForMeEntryDto> canFulfillForMeEntryDtos = new HashSet<>();
    for (StockCardSummaryV2Dto stockCardSummaryV2Dto : stockSummaries) {
      if (CollectionUtils.isNotEmpty(stockCardSummaryV2Dto.getCanFulfillForMe())) {
        canFulfillForMeEntryDtos.addAll(stockCardSummaryV2Dto.getCanFulfillForMe());
      }
    }
    for (final Map.Entry<UUID, UUID> physicalInventoryIdToSubDraftId : physicalInventoryIdToSubDraftIdMap.entrySet()) {
      UUID physicalInventoryId = physicalInventoryIdToSubDraftId.getKey();
      if (initialPhysicalInventory) {
        resetInitialLineItems(physicalInventoryId, physicalInventoryIdToSubDraftId.getValue());
        continue;
      }
      List<PhysicalInventoryLineItemsExtension> oldLineItemsExtension
          = lineItemsExtensionRepository.findByPhysicalInventoryId(physicalInventoryId);
      // TODO orderable is not enough for delete or reset, use unique key instead
      Set<UUID> needResetLineItemIds = new HashSet<>();
      Set<UUID> needDeleteLineItemIds = new HashSet<>();
      for (PhysicalInventoryLineItemsExtension item : oldLineItemsExtension) {
        if (subDraftIds.contains(item.getSubDraftId())) {
          if (Boolean.TRUE.equals(item.getInitial())) {
            needResetLineItemIds.add(item.getPhysicalInventoryLineItemId());
          } else {
            needDeleteLineItemIds.add(item.getPhysicalInventoryLineItemId());
          }
        }
      }

      PhysicalInventoryDto physicalInventoryDto = siglusPhysicalInventoryService.getFullPhysicalInventoryDto(
          physicalInventoryId);

      if (physicalInventoryDto == null) {
        throw new IllegalArgumentException("physical inventory not exists. id = " + physicalInventoryId);
      }
      List<PhysicalInventoryLineItemDto> lineItems = physicalInventoryDto.getLineItems();
      if (CollectionUtils.isEmpty(lineItems)) {
        continue;
      }
      Iterator<PhysicalInventoryLineItemDto> iterator = lineItems.iterator();
      while (iterator.hasNext()) {
        PhysicalInventoryLineItemDto physicalInventoryLineItemDto = iterator.next();
        if (needResetLineItemIds.contains(physicalInventoryLineItemDto.getId())) {
          physicalInventoryLineItemDto.setQuantity(null);
          physicalInventoryLineItemDto.setReasonFreeText(null);
          if (CollectionUtils.isNotEmpty(physicalInventoryLineItemDto.getStockAdjustments())) {
            physicalInventoryLineItemDto.getStockAdjustments().clear();
          }
        } else if (needDeleteLineItemIds.contains(physicalInventoryLineItemDto.getId())) {
          iterator.remove();
        }
      }
      siglusPhysicalInventoryService.saveDraftForProductsForOneProgram(physicalInventoryDto);
    }
  }

  @Transactional
  public void updateSubDrafts(List<UUID> subDraftIds, PhysicalInventoryDto physicalInventoryDto,
      PhysicalInventorySubDraftEnum status, boolean isByLocation) {
    if (isByLocation) {
      updateEmptyLocationLineItem(subDraftIds, physicalInventoryDto);
    }
    physicalInventoryDto.getLineItems().removeIf(e -> Objects.isNull(e.getOrderableId()));

    List<PhysicalInventorySubDraft> subDrafts = physicalInventorySubDraftRepository.findAll(subDraftIds);

    List<UUID> physicalInventoryIds = subDrafts.stream().map(PhysicalInventorySubDraft::getPhysicalInventoryId)
        .distinct().collect(Collectors.toList());

    checkConflictSubDraft(physicalInventoryIds, physicalInventoryDto, subDraftIds);

    updateSubDraftsStatus(subDraftIds, status, false);

    saveSubDraftsLineItems(physicalInventoryDto, physicalInventoryIds, subDrafts);
  }

  private void updateEmptyLocationLineItem(List<UUID> subDraftIds, PhysicalInventoryDto physicalInventoryDto) {
    List<PhysicalInventoryEmptyLocationLineItem> emptyLocations =
        physicalInventoryEmptyLocationLineItemRepository.findBySubDraftIdIn(subDraftIds);
    List<PhysicalInventoryLineItemDto> emptyLocationLineItemList = physicalInventoryDto.getLineItems().stream().filter(
        lineItem -> emptyLocations
            .stream()
            .anyMatch(location -> location.getLocationCode().equals(lineItem.getLocationCode())))
        .collect(Collectors.toList());
    List<PhysicalInventoryEmptyLocationLineItem> needToUpdateInEmptyLocations = new LinkedList<>();
    emptyLocationLineItemList.forEach(lineItem -> {
      PhysicalInventoryEmptyLocationLineItem emptyLocationLineItem = emptyLocations.stream()
          .filter(location -> location.getLocationCode().equals(lineItem.getLocationCode())).findFirst()
          .orElseThrow(NullPointerException::new);
      if (Objects.isNull(lineItem.getOrderableId())) {
        emptyLocationLineItem.setSkipped(lineItem.isSkipped());
        emptyLocationLineItem.setHasProduct(false);
      } else {
        emptyLocationLineItem.setSkipped(false);
        emptyLocationLineItem.setHasProduct(true);
      }
      needToUpdateInEmptyLocations.add(emptyLocationLineItem);
    });
    log.info("save physical inventory empty location lineItem, size: {}", needToUpdateInEmptyLocations.size());
    physicalInventoryEmptyLocationLineItemRepository.save(needToUpdateInEmptyLocations);
  }

  private void saveSubDraftsLineItems(PhysicalInventoryDto dto, List<UUID> physicalInventoryIds,
      List<PhysicalInventorySubDraft> subDrafts) {
    Map<UUID, UUID> physicalInventorySubDraftMap = subDrafts.stream()
        .collect(Collectors.toMap(PhysicalInventorySubDraft::getPhysicalInventoryId, PhysicalInventorySubDraft::getId,
            (e1, e2) -> {
              throw new IllegalArgumentException("not supported operation.");
            }));
    Map<UUID, List<PhysicalInventoryLineItemDto>> programLineItemMap = dto.getLineItems().stream()
        .collect(Collectors.groupingBy(PhysicalInventoryLineItemDto::getProgramId));

    for (UUID physicalInventoryId : physicalInventoryIds) {
      PhysicalInventoryDto physicalInventoryDto = siglusPhysicalInventoryService.getFullPhysicalInventoryDto(
          physicalInventoryId);
      UUID subDraftId = physicalInventorySubDraftMap.get(physicalInventoryId);

      List<PhysicalInventoryLineItemsExtension> subDraftLineItemsExtensions
          = lineItemsExtensionRepository.findBySubDraftIdIn(Lists.newArrayList(subDraftId));

      List<UUID> oldLineItemIds = subDraftLineItemsExtensions
          .stream()
          .map(PhysicalInventoryLineItemsExtension::getPhysicalInventoryLineItemId)
          .collect(Collectors.toList());

      UUID programId = physicalInventoryDto.getProgramId();

      List<PhysicalInventoryLineItemDto> curLineItems = programLineItemMap.getOrDefault(programId, new ArrayList<>());

      List<PhysicalInventoryLineItemDto> oldLineItems = physicalInventoryDto.getLineItems();

      List<PhysicalInventoryLineItemDto> notChangedLineItems = oldLineItems.stream()
          .filter(item -> !oldLineItemIds.contains(item.getId()))
          .collect(Collectors.toList());

      List<PhysicalInventoryLineItemDto> newLineItems = Lists.newArrayList(notChangedLineItems);
      newLineItems.addAll(curLineItems);

      if (CollectionUtils.isEmpty(newLineItems)) {
        continue;
      }

      physicalInventoryDto.setLineItems(newLineItems);

      List<PhysicalInventorySubDraftLineItemsExtensionDto> newLineItemsExtension
          = convertToLineItemsExtension(physicalInventoryDto.getLineItems(), subDraftId, physicalInventoryId);

      PhysicalInventoryLineItemExtensionDto physicalInventoryExtendDto = new PhysicalInventoryLineItemExtensionDto();
      BeanUtils.copyProperties(physicalInventoryDto, physicalInventoryExtendDto);
      physicalInventoryExtendDto.setLineItemsExtensions(newLineItemsExtension);

      siglusPhysicalInventoryService.saveDraftForProductsForOneProgramWithExtension(physicalInventoryExtendDto);
    }
  }

  private List<PhysicalInventorySubDraftLineItemsExtensionDto> convertToLineItemsExtension(
      List<PhysicalInventoryLineItemDto> curLineItems, UUID subDraftId, UUID physicalInventoryId) {
    List<PhysicalInventorySubDraftLineItemsExtensionDto> result = new ArrayList<>();
    curLineItems.forEach(item -> result.add(PhysicalInventorySubDraftLineItemsExtensionDto.builder()
        .subDraftId(subDraftId)
        .lotId(item.getLotId())
        .orderableId(item.getOrderableId())
        .locationCode(item.getLocationCode())
        .area(item.getArea())
        .physicalInventoryId(physicalInventoryId)
        .physicalInventoryLineItemId(item.getId())
        .build()));
    return result;
  }

  private void checkConflictSubDraft(List<UUID> physicalInventoryIds, PhysicalInventoryDto physicalInventoryDto,
      List<UUID> subDraftIds) {
    List<PhysicalInventoryLineItemsExtension> physicalInventoryLineItemsExtensions
        = lineItemsExtensionRepository.findByPhysicalInventoryIdIn(physicalInventoryIds);

    ArrayList<ProductSubDraftConflictDto> conflictDtoList = new ArrayList<>();
    Set<UUID> conflictOrderableIds = new HashSet<>();
    for (PhysicalInventoryLineItemDto lineItem : physicalInventoryDto.getLineItems()) {
      if (conflictOrderableIds.contains(lineItem.getOrderableId())) {
        continue;
      }
      String orderableUniqueKey = lineItem.getOrderableId().toString();
      Optional<PhysicalInventoryLineItemsExtension> oldLineItem = physicalInventoryLineItemsExtensions.stream()
          .filter(item -> item.getOrderableId().toString().equals(orderableUniqueKey)).findFirst();
      // exist old line item and not current operating drafts
      if (oldLineItem.isPresent() && !subDraftIds.contains(oldLineItem.get().getSubDraftId())) {
        ProductSubDraftConflictDto build = ProductSubDraftConflictDto.builder()
            .conflictWithSubDraftId(oldLineItem.get().getSubDraftId())
            .orderableId(lineItem.getOrderableId())
            .build();
        conflictDtoList.add(build);
        conflictOrderableIds.add(lineItem.getOrderableId());
      }
    }
    if (CollectionUtils.isEmpty(conflictDtoList)) {
      return;
    }

    fillConflictDtoList(conflictDtoList, conflictOrderableIds);

    throw new BusinessDataException(new Message(ERROR_INVENTORY_CONFLICT_SUB_DRAFT), conflictDtoList);
  }

  private void fillConflictDtoList(List<ProductSubDraftConflictDto> conflictDtoList, Set<UUID> conflictOrderableIds) {
    List<OrderableDto> orderables = siglusOrderableService.getAllProducts();
    Map<UUID, OrderableDto> orderableMap = orderables.stream()
        .filter(item -> conflictOrderableIds.contains(item.getId()))
        .collect(Collectors.toMap(OrderableDto::getId, Function.identity()));

    List<UUID> existSubDraftIds = conflictDtoList.stream().map(ProductSubDraftConflictDto::getConflictWithSubDraftId)
        .collect(Collectors.toList());
    List<PhysicalInventorySubDraft> subDrafts = physicalInventorySubDraftRepository.findAll(existSubDraftIds);
    Map<UUID, Integer> subDraftsMap = subDrafts.stream()
        .collect(Collectors.toMap(PhysicalInventorySubDraft::getId, PhysicalInventorySubDraft::getNum));

    for (ProductSubDraftConflictDto draftConflictDto : conflictDtoList) {
      OrderableDto orderable = orderableMap.get(draftConflictDto.getOrderableId());
      Integer num = subDraftsMap.get(draftConflictDto.getConflictWithSubDraftId());
      if (orderable != null && num != null) {
        draftConflictDto.setProductCode(orderable.getProductCode());
        draftConflictDto.setProductName(orderable.getFullProductName());
        draftConflictDto.setConflictWith(DRAFT + num);
      }
    }
  }
}
