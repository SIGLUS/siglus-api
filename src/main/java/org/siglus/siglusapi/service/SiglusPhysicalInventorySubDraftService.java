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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.siglus.siglusapi.domain.PhysicalInventorySubDraft;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.PhysicalInventoryLineItemExtensionDto;
import org.siglus.siglusapi.dto.PhysicalInventorySubDraftLineItemsExtensionDto;
import org.siglus.siglusapi.dto.ProductSubDraftConflictDto;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@SuppressWarnings({"PMD"})
public class SiglusPhysicalInventorySubDraftService {

  public static final String DRAFT = "Draft ";
  @Autowired
  private SiglusStockCardSummariesService siglusStockCardSummariesService;
  @Autowired
  private SiglusOrderableService siglusOrderableService;

  @Autowired
  private PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;

  @Autowired
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Autowired
  private PhysicalInventoryLineItemsExtensionRepository lineItemsExtensionRepository;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Transactional
  public void deleteSubDrafts(List<UUID> subDraftIds) {
    log.info("deleteSubDrafts, subDraftIds=" + subDraftIds);
    try {
      List<PhysicalInventorySubDraft> physicalInventorySubDrafts = updateSubDraftsStatus(subDraftIds,
          PhysicalInventorySubDraftEnum.NOT_YET_STARTED, true);

      doDelete(physicalInventorySubDrafts, subDraftIds);
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
      item.setStatus(subDraftStatus);
      item.setOperatorId(isDelete ? null : currentUserId);
    });

    physicalInventorySubDraftRepository.save(subDrafts);

    return subDrafts;
  }

  private boolean isSameUuid(UUID id1, UUID id2) {
    if (id1 == null) {
      return id2 == null;
    }
    return id1.equals(id2);
  }

  private boolean isSameLineItem(PhysicalInventoryLineItemDto item1,
                                  PhysicalInventoryLineItemDto item2) {
    return isSameUuid(item1.getOrderableId(), item2.getOrderableId())
            && isSameUuid(item1.getLotId(), item2.getLotId());
  }

  private boolean contains(List<PhysicalInventoryLineItemDto> list, PhysicalInventoryLineItemDto item) {
    return list.stream().anyMatch(lineItem -> isSameLineItem(lineItem, item));
  }

  private List<PhysicalInventoryLineItemDto> getDeletedInitialLineItems(List<PhysicalInventoryLineItemDto> current,
                                                                        List<PhysicalInventoryLineItemDto> original) {
    return original.stream()
            .filter(lineItem -> !contains(current, lineItem)).collect(Collectors.toList());
  }

  private void doDelete(List<PhysicalInventorySubDraft> subDrafts, List<UUID> subDraftIds) {
    Map<UUID, UUID> physicalInventoryIdToSubDraftIdMap = subDrafts.stream()
        .collect(Collectors.toMap(PhysicalInventorySubDraft::getPhysicalInventoryId, PhysicalInventorySubDraft::getId));
    List<StockCardSummaryV2Dto> stockSummaries = siglusStockCardSummariesService.findAllProgramStockSummaries();
    Set<CanFulfillForMeEntryDto> canFulfillForMeEntryDtos = new HashSet<>();
    for (StockCardSummaryV2Dto stockCardSummaryV2Dto : stockSummaries) {
      if (CollectionUtils.isNotEmpty(stockCardSummaryV2Dto.getCanFulfillForMe())) {
        canFulfillForMeEntryDtos.addAll(stockCardSummaryV2Dto.getCanFulfillForMe());
      }
    }
    Map<String, CanFulfillForMeEntryDto> canFulfillForMeEntryDtoMap = canFulfillForMeEntryDtos.stream()
        .collect(Collectors.toMap(this::getUniqueKey, Function.identity()));

    for (UUID physicalInventoryId : physicalInventoryIdToSubDraftIdMap.keySet()) {
      List<PhysicalInventoryLineItemsExtension> oldLineItemsExtension
          = lineItemsExtensionRepository.findByPhysicalInventoryId(physicalInventoryId);

      List<UUID> needResetOrderableIds = new ArrayList<>();
      List<UUID> needDeleteOrderableIds = new ArrayList<>();
      for (PhysicalInventoryLineItemsExtension item : oldLineItemsExtension) {
        if (subDraftIds.contains(item.getSubDraftId())) {
          if (Boolean.TRUE.equals(item.getInitial())) {
            needResetOrderableIds.add(item.getOrderableId());
          } else {
            needDeleteOrderableIds.add(item.getOrderableId());
          }
        }
      }

      PhysicalInventoryDto physicalInventoryDto = siglusPhysicalInventoryService.getFullPhysicalInventoryDto(
          physicalInventoryId);

      List<PhysicalInventoryLineItemDto> originalInitialInventoryLineItems = siglusPhysicalInventoryService
              .buildInitialInventoryLineItemDtos(Collections.singleton(physicalInventoryDto.getProgramId()),
                      physicalInventoryDto.getFacilityId());

      if (physicalInventoryDto == null) {
        throw new IllegalArgumentException("physical inventory not exists. id = " + physicalInventoryId);
      }
      List<PhysicalInventoryLineItemDto> lineItems = physicalInventoryDto.getLineItems();
      List<PhysicalInventoryLineItemDto> deletedInitialLineItems = getDeletedInitialLineItems(lineItems,
              originalInitialInventoryLineItems);
      if (CollectionUtils.isNotEmpty(deletedInitialLineItems)) {
        lineItems.addAll(deletedInitialLineItems);

        List<PhysicalInventorySubDraftLineItemsExtensionDto> newLineItemsExtension = convertToLineItemsExtension(
                deletedInitialLineItems,
                physicalInventoryIdToSubDraftIdMap.get(physicalInventoryId),
                physicalInventoryId);

        PhysicalInventoryLineItemExtensionDto physicalInventoryExtendDto = new PhysicalInventoryLineItemExtensionDto();
        BeanUtils.copyProperties(physicalInventoryDto, physicalInventoryExtendDto);
        physicalInventoryExtendDto.setLineItemsExtensions(newLineItemsExtension);

        siglusPhysicalInventoryService.saveDraftForProductsForOneProgramWithExtension(physicalInventoryExtendDto);
      }

      if (CollectionUtils.isEmpty(lineItems)) {
        continue;
      }
      Iterator<PhysicalInventoryLineItemDto> iterator = lineItems.iterator();
      while (iterator.hasNext()) {
        PhysicalInventoryLineItemDto physicalInventoryLineItemDto = iterator.next();
        if (needResetOrderableIds.contains(physicalInventoryLineItemDto.getOrderableId())) {
          CanFulfillForMeEntryDto canFulfillForMeEntryDto = canFulfillForMeEntryDtoMap.get(
              getUniqueKey(physicalInventoryLineItemDto));
          if (canFulfillForMeEntryDto != null) {
            physicalInventoryLineItemDto.setQuantity(null);
            physicalInventoryLineItemDto.setReasonFreeText(null);
          }
        } else if (needDeleteOrderableIds.contains(physicalInventoryLineItemDto.getOrderableId())) {
          iterator.remove();
        }
      }
      siglusPhysicalInventoryService.saveDraftForProductsForOneProgram(physicalInventoryDto);
    }
  }

  @Transactional
  public void updateSubDrafts(List<UUID> subDraftIds, PhysicalInventoryDto physicalInventoryDto,
      PhysicalInventorySubDraftEnum status) {
    List<PhysicalInventorySubDraft> subDrafts = physicalInventorySubDraftRepository.findAll(subDraftIds);

    List<UUID> physicalInventoryIds = subDrafts.stream().map(PhysicalInventorySubDraft::getPhysicalInventoryId)
        .distinct().collect(Collectors.toList());

    checkConflictSubDraft(physicalInventoryIds, physicalInventoryDto, subDraftIds);

    updateSubDraftsStatus(subDraftIds, status, false);

    saveSubDraftsLineItems(physicalInventoryDto, physicalInventoryIds, subDrafts);
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

      List<String> oldUniqueKeyList = subDraftLineItemsExtensions
          .stream().map(this::getUniqueKey).collect(Collectors.toList());

      UUID programId = physicalInventoryDto.getProgramId();

      List<PhysicalInventoryLineItemDto> curLineItems = programLineItemMap.getOrDefault(programId, new ArrayList<>());

      List<PhysicalInventoryLineItemDto> oldLineItems = physicalInventoryDto.getLineItems();

      List<PhysicalInventoryLineItemDto> notChangedLineItems = oldLineItems.stream()
          .filter(item -> !oldUniqueKeyList.contains(getUniqueKey(item)))
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
        .physicalInventoryId(physicalInventoryId)
        .build()));
    return result;
  }

  private void checkConflictSubDraft(List<UUID> physicalInventoryIds, PhysicalInventoryDto physicalInventoryDto,
      List<UUID> subDraftIds) {
    List<PhysicalInventoryLineItemsExtension> physicalInventoryLineItemsExtensions
        = lineItemsExtensionRepository.findByPhysicalInventoryIdIn(physicalInventoryIds);

    List<ProductSubDraftConflictDto> conflictDtoList = new ArrayList<>();
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

  private String getUniqueKey(PhysicalInventoryLineItemDto item) {
    if (item.getLotId() == null) {
      return item.getOrderableId().toString();
    }
    return item.getOrderableId().toString() + "&" + item.getLotId().toString();
  }

  private String getUniqueKey(PhysicalInventoryLineItemsExtension item) {
    if (item.getLotId() == null) {
      return item.getOrderableId().toString();
    }
    return item.getOrderableId().toString() + "&" + item.getLotId().toString();
  }

  private String getUniqueKey(CanFulfillForMeEntryDto item) {
    if (item.getLot() == null || item.getLot().getId() == null) {
      return item.getOrderable().getId().toString();
    }
    return item.getOrderable().getId().toString() + "&" + item.getLot().getId().toString();
  }

}
