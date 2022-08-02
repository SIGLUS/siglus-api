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

package org.siglus.siglusapi.util;

import static java.util.stream.Collectors.toList;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_EVENT_ORDERABLE_INVALID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ISSUE_CONFLICT_SUB_DRAFT;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_SUB_DRAFT_SAME_ORDERABLE_ID_WITH_LOT_CODE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.ProductSubDraftConflictDto;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.StockManagementDraftLineItemDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConflictOrderableInSubDraftsService {

  @Autowired
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Autowired
  private SiglusOrderableReferenceDataService siglusOrderableReferenceDataService;

  public void checkConflictOrderableBetweenSubDrafts(StockManagementDraftDto dto) {
    StockManagementDraft currentSubDraft = stockManagementDraftRepository.findOne(dto.getId());
    List<StockManagementDraft> subDrafts = stockManagementDraftRepository
        .findByInitialDraftId(currentSubDraft.getInitialDraftId());

    List<UUID> preSavedDraftOrderableIds = dto.getLineItems().stream()
        .map(StockManagementDraftLineItemDto::getOrderableId)
        .collect(toList());

    subDrafts.remove(currentSubDraft);

    List<ProductSubDraftConflictDto> subDraftConflictDtos = new ArrayList<>();
    subDrafts.forEach(subDraft -> {
      List<UUID> conflictOrderableIds = subDraft.getLineItems().stream()
          .map(StockManagementDraftLineItem::getOrderableId)
          .filter(preSavedDraftOrderableIds::contains).distinct().collect(toList());
      if (CollectionUtils.isNotEmpty(conflictOrderableIds)) {
        conflictOrderableIds.forEach(id -> {
          ProductSubDraftConflictDto subDraftConflictDto = ProductSubDraftConflictDto.builder()
              .conflictWithSubDraftId(subDraft.getId())
              .orderableId(id)
              .conflictWith(subDraft.getDraftNumber().toString())
              .build();
          subDraftConflictDtos.add(subDraftConflictDto);
        });
      }
    });

    fillConflictDtos(subDraftConflictDtos);

    if (CollectionUtils.isNotEmpty(subDraftConflictDtos)) {
      throw new BusinessDataException(new Message(ERROR_ISSUE_CONFLICT_SUB_DRAFT),
          subDraftConflictDtos);
    }
  }

  public void checkConflictOrderableAndLotInSubDraft(StockManagementDraftDto stockManagementDraftDto) {
    if (stockManagementDraftDto.getDraftType().equals(FieldConstants.RECEIVE)) {
      List<StockManagementDraftLineItemDto> lineItemDtos = stockManagementDraftDto.getLineItems();
      Map<String, StockManagementDraftLineItemDto> orderableIdLotCodeMap = lineItemDtos.stream().collect(Collectors
          .toMap(stockManagementDraftLineItemDto -> stockManagementDraftLineItemDto.getOrderableId()
                  + stockManagementDraftLineItemDto.getLotCode(),
              Function.identity(), (k1, k2) -> k1));
      if (lineItemDtos.size() != orderableIdLotCodeMap.size()) {
        throw new ValidationMessageException(
            new Message(ERROR_STOCK_MANAGEMENT_SUB_DRAFT_SAME_ORDERABLE_ID_WITH_LOT_CODE));
      }
    }
  }

  private void fillConflictDtos(List<ProductSubDraftConflictDto> subDraftConflictDtos) {
    List<OrderableDto> orderableDtos = siglusOrderableReferenceDataService.findByIds(
        subDraftConflictDtos.stream().map(ProductSubDraftConflictDto::getOrderableId)
            .collect(toList()));
    subDraftConflictDtos.forEach(subDraftConflictDto -> {
      OrderableDto targetOrderableDto = orderableDtos.stream()
          .filter(orderableDto -> orderableDto.getId().equals(subDraftConflictDto.getOrderableId()))
          .findFirst()
          .orElseThrow(() -> new ResourceNotFoundException(ERROR_EVENT_ORDERABLE_INVALID));
      subDraftConflictDto.setProductCode(targetOrderableDto.getProductCode());
      subDraftConflictDto.setProductName(targetOrderableDto.getFullProductName());
    });
  }

}
