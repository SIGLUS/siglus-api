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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_CARD_NOT_FOUND;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.siglus.siglusapi.domain.ProductLocationMovementDraft;
import org.siglus.siglusapi.domain.ProductLocationMovementLineItem;
import org.siglus.siglusapi.dto.ProductLocationMovementDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.ProductLocationMovementDraftRepository;
import org.siglus.siglusapi.repository.ProductLocationMovementLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SiglusProductLocationMovementService {

  @Autowired
  private ProductLocationMovementLineItemRepository movementLineItemRepository;

  @Autowired
  private ProductLocationMovementDraftRepository movementDraftRepository;

  @Autowired
  private SiglusStockCardRepository stockCardRepository;

  @Transactional
  public void createMovementLineItems(ProductLocationMovementDto movementDto) {
    List<ProductLocationMovementLineItem> movementLineItems = convertMovementDtoToMovementItems(movementDto);
    movementLineItemRepository.save(movementLineItems);
    deleteMovementDraft(movementDto);
  }

  private void deleteMovementDraft(ProductLocationMovementDto movementDto) {
    List<ProductLocationMovementDraft> movementDrafts = movementDraftRepository
        .findByProgramIdAndFacilityId(movementDto.getProgramId(), movementDto.getFacilityId());
    if (movementDrafts.isEmpty()) {
      throw new NotFoundException(ERROR_MOVEMENT_DRAFT_NOT_FOUND);
    }
    movementDraftRepository.delete(movementDrafts.get(0));
  }

  private List<ProductLocationMovementLineItem> convertMovementDtoToMovementItems(
      ProductLocationMovementDto movementDto) {
    List<ProductLocationMovementLineItem> movementLineItems = new ArrayList<>();
    movementDto.getMovementLineItems().forEach(lineItemDto -> {
      List<StockCard> stockCards = stockCardRepository
          .findByFacilityIdAndProgramIdAndOrderableIdAndLotId(movementDto.getFacilityId(), movementDto.getProgramId(),
              lineItemDto.getOrderableId(), lineItemDto.getLotId());
      if (stockCards.isEmpty()) {
        throw new NotFoundException(ERROR_STOCK_CARD_NOT_FOUND);
      }
      ProductLocationMovementLineItem movementLineItem = ProductLocationMovementLineItem.builder()
          .stcokCardId(stockCards.get(0).getId())
          .srcArea(lineItemDto.getSrcArea())
          .srcLocationCode(lineItemDto.getSrcLocationCode())
          .destArea(lineItemDto.getDestArea())
          .destLocationCode(lineItemDto.getDestLocationCode())
          .quantity(lineItemDto.getQuantity())
          .createdDate(movementDto.getCreatedDate())
          .userId(movementDto.getUseId())
          .signature(movementDto.getSignature())
          .build();
      movementLineItems.add(movementLineItem);
    });
    return movementLineItems;
  }
}
