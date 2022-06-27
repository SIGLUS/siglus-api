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

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.referencedata.Orderable;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.siglus.siglusapi.domain.PhysicalInventorySubDraft;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.ProductSubDraftConflictDto;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings({"PMD"})
public class SiglusPhysicalInventorySubDraftService {

  public static final String DRAFT = "Draft ";
  @Autowired
  private SiglusStockCardSummariesService siglusStockCardSummariesService;
  @Autowired
  private OrderableRepository orderableRepository;

  @Autowired
  private PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;

  @Autowired
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Autowired
  private PhysicalInventoryLineItemsExtensionRepository lineItemsExtensionRepository;

  @Transactional
  public void deleteSubDrafts(List<UUID> subDraftIds) {
    List<PhysicalInventorySubDraft> subDrafts = physicalInventorySubDraftRepository.findAll(subDraftIds);
    if (CollectionUtils.isEmpty(subDrafts)) {
      return;
    }

    subDrafts.forEach(item -> item.setStatus(PhysicalInventorySubDraftEnum.NOT_YET_STARTED));
    // TODO: check right
    physicalInventorySubDraftRepository.save(subDrafts);

    doDelete(subDrafts, subDraftIds);
  }

  private void doDelete(List<PhysicalInventorySubDraft> subDrafts, List<UUID> subDraftIds) {
    List<UUID> physicalInventoryIds = subDrafts.stream().map(PhysicalInventorySubDraft::getPhysicalInventoryId)
        .collect(Collectors.toList());
    List<StockCardSummaryV2Dto> init = siglusStockCardSummariesService.findAllProgramStockSummaries();
    Set<CanFulfillForMeEntryDto> canFulfillForMeEntryDtos = new HashSet<>();
    for (StockCardSummaryV2Dto stockCardSummaryV2Dto : init) {
      canFulfillForMeEntryDtos.addAll(stockCardSummaryV2Dto.getCanFulfillForMe());
    }
    Map<String, CanFulfillForMeEntryDto> canFulfillForMeEntryDtoMap = canFulfillForMeEntryDtos.stream()
        .collect(Collectors.toMap(this::getUniqueKey, Function.identity()));

    for (UUID physicalInventoryId : physicalInventoryIds) {
      List<PhysicalInventoryLineItemsExtension> oldLineItemsExtension
          = lineItemsExtensionRepository.findByPhysicalInventoryId(physicalInventoryId);

      List<UUID> needReplaceOrderableIds = new ArrayList<>();
      List<UUID> needDeleteOrderableIds = new ArrayList<>();
      for (PhysicalInventoryLineItemsExtension item : oldLineItemsExtension) {
        if (subDraftIds.contains(item.getSubDraftId())) {
          if (item.getIsInitial() != 0) {
            needReplaceOrderableIds.add(item.getOrderableId());
          } else {
            needDeleteOrderableIds.add(item.getOrderableId());
          }
        }
      }

      PhysicalInventoryDto physicalInventory = siglusPhysicalInventoryService.getPhysicalInventory(physicalInventoryId);

      Iterator<PhysicalInventoryLineItemDto> iterator = physicalInventory.getLineItems().iterator();
      while (iterator.hasNext()) {
        PhysicalInventoryLineItemDto physicalInventoryLineItemDto = iterator.next();
        if (needReplaceOrderableIds.contains(physicalInventoryLineItemDto.getOrderableId())) {
          CanFulfillForMeEntryDto canFulfillForMeEntryDto = canFulfillForMeEntryDtoMap.get(
              getUniqueKey(physicalInventoryLineItemDto));
          if (canFulfillForMeEntryDto != null) {
            physicalInventoryLineItemDto.setQuantity(canFulfillForMeEntryDto.getStockOnHand());
            // todo 这里设置为空 可以这样吗
            physicalInventoryLineItemDto.setReasonFreeText(null);
          }
        } else if (needDeleteOrderableIds.contains(physicalInventoryLineItemDto.getOrderableId())) {
          iterator.remove();
        }
      }
      siglusPhysicalInventoryService.saveDraftForProductsInOneProgram(physicalInventory);
    }
  }

  public void updateSubDrafts(List<UUID> subDraftIds, PhysicalInventoryDto dto) {
    List<PhysicalInventorySubDraft> subDrafts = physicalInventorySubDraftRepository.findAll(subDraftIds);

    List<UUID> physicalInventoryIds = subDrafts.stream().map(PhysicalInventorySubDraft::getPhysicalInventoryId)
        .distinct().collect(Collectors.toList());

    checkConflictSubDraft(physicalInventoryIds, dto, subDraftIds);

    Map<UUID, List<PhysicalInventoryLineItemDto>> programLineItemMap = dto.getLineItems().stream()
        .collect(Collectors.groupingBy(PhysicalInventoryLineItemDto::getProgramId));

    for (UUID physicalInventoryId : physicalInventoryIds) {
      PhysicalInventoryDto physicalInventory = siglusPhysicalInventoryService.getPhysicalInventory(physicalInventoryId);
      UUID programId = physicalInventory.getProgramId();

      List<PhysicalInventoryLineItemDto> curLineItems = programLineItemMap.get(programId);

      List<String> uniqueKeyList = curLineItems.stream().map(this::getUniqueKey).collect(Collectors.toList());
      List<PhysicalInventoryLineItemDto> oldLineItems = physicalInventory.getLineItems();

      List<PhysicalInventoryLineItemDto> notChangedLineItems = oldLineItems.stream()
          .filter(item -> !uniqueKeyList.contains(getUniqueKey(item))).collect(
              Collectors.toList());

      List<PhysicalInventoryLineItemDto> newLineItems = Lists.newArrayList(notChangedLineItems);
      newLineItems.addAll(curLineItems);

      physicalInventory.setLineItems(newLineItems);

      siglusPhysicalInventoryService.saveDraftForProductsInOneProgram(physicalInventory);
    }
  }

  private void checkConflictSubDraft(List<UUID> physicalInventoryIds, PhysicalInventoryDto dto,
      List<UUID> subDraftIds) {
    List<PhysicalInventoryLineItemsExtension> physicalInventories
        = lineItemsExtensionRepository.findByPhysicalInventoryIdIn(physicalInventoryIds);
    List<ProductSubDraftConflictDto> result = new ArrayList<>();
    for (PhysicalInventoryLineItemDto lineItem : dto.getLineItems()) {
      String uniqueKey = getUniqueKey(lineItem);
      Optional<PhysicalInventoryLineItemsExtension> oldLineItem = physicalInventories.stream()
          .filter(item -> getUniqueKey(item).equals(uniqueKey)).findFirst();
      // 存在且不是当前填写的draft
      if (oldLineItem.isPresent() && !subDraftIds.contains(oldLineItem.get().getSubDraftId())) {

        ProductSubDraftConflictDto build = ProductSubDraftConflictDto.builder()
            .conflictWithSubDraftId(oldLineItem.get().getSubDraftId())
            .orderableId(lineItem.getOrderableId())
            .build();
        result.add(build);
      }
    }
    if (CollectionUtils.isEmpty(result)) {
      return;
    }

    List<UUID> conflictOrderableIds = result.stream().map(ProductSubDraftConflictDto::getOrderableId)
        .collect(Collectors.toList());
    Page<Orderable> orderables = orderableRepository.findAllLatestByIds(conflictOrderableIds, null);
    Map<UUID, Orderable> orderableMap = orderables.getContent().stream()
        .collect(Collectors.toMap(Orderable::getId, Function.identity()));

    List<UUID> existSubDraftIds = result.stream().map(ProductSubDraftConflictDto::getConflictWithSubDraftId)
        .collect(Collectors.toList());
    List<PhysicalInventorySubDraft> subDrafts = physicalInventorySubDraftRepository.findAll(existSubDraftIds);
    Map<UUID, Integer> subDraftsMap = subDrafts.stream()
        .collect(Collectors.toMap(PhysicalInventorySubDraft::getId, PhysicalInventorySubDraft::getNum));

    for (ProductSubDraftConflictDto item : result) {
      Orderable orderable = orderableMap.get(item.getOrderableId());
      Integer num = subDraftsMap.get(item.getConflictWithSubDraftId());
      if (orderable != null && num != null) {
        item.setProductCode(orderable.getProductCode().toString());
        item.setProductName(orderable.getFullProductName());
        item.setConflictWith(DRAFT + num);
      }
    }
    throw new ValidationMessageException(new Message(ERROR_INVENTORY_CONFLICT_SUB_DRAFT, JSON.toJSONString(result)));
  }

  private String getUniqueKey(PhysicalInventoryLineItemDto item) {
    return item.getOrderableId().toString() + "&" + item.getLotId().toString();
  }

  private String getUniqueKey(PhysicalInventoryLineItemsExtension item) {
    return item.getOrderableId().toString() + "&" + item.getLotId().toString();
  }

  private String getUniqueKey(CanFulfillForMeEntryDto item) {
    return item.getOrderable().getId().toString() + "&" + item.getLot().getId().toString();
  }

}
