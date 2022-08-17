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
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_CARD_NOT_FOUND;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.siglus.siglusapi.domain.ProductLocationMovementDraft;
import org.siglus.siglusapi.domain.ProductLocationMovementLineItem;
import org.siglus.siglusapi.dto.ProductLocationMovementDto;
import org.siglus.siglusapi.dto.ProductLocationMovementLineItemDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.ProductLocationMovementDraftRepository;
import org.siglus.siglusapi.repository.ProductLocationMovementLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;

@RunWith(MockitoJUnitRunner.class)
public class SiglusProductLocationMovementServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusProductLocationMovementService service;

  @Mock
  private SiglusStockCardRepository stockCardRepository;

  @Mock
  private ProductLocationMovementLineItemRepository movementLineItemRepository;

  @Mock
  private ProductLocationMovementDraftRepository movementDraftRepository;

  private final UUID allProgramId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();
  private final UUID stockCardId = UUID.randomUUID();
  private final UUID movementDraftId = UUID.randomUUID();
  private final LocalDate localDate = LocalDate.now();

  private final StockCard stockCard = new StockCard();
  private final ProductLocationMovementDraft movementDraft = new ProductLocationMovementDraft();
  private final ProductLocationMovementLineItem lineItem = ProductLocationMovementLineItem.builder()
      .stockCardId(stockCardId)
      .srcArea("A")
      .srcLocationCode("AA20B")
      .destArea("B")
      .destLocationCode("BB30F")
      .userId(userId)
      .signature("Jimmy")
      .createdDate(localDate)
      .quantity(10)
      .build();
  private final ProductLocationMovementLineItemDto movementLineItemDto1 = ProductLocationMovementLineItemDto.builder()
      .programId(programId)
      .orderableId(orderableId)
      .lotId(lotId)
      .srcArea("A")
      .srcLocationCode("AA20B")
      .destArea("B")
      .destLocationCode("BB30F")
      .quantity(10)
      .stockOnHand(20)
      .build();
  private final ProductLocationMovementLineItemDto movementLineItemDto2 = ProductLocationMovementLineItemDto.builder()
      .programId(programId)
      .orderableId(orderableId)
      .lotId(lotId)
      .srcArea("A")
      .srcLocationCode("AA20B")
      .destArea("B")
      .destLocationCode("BB30F")
      .quantity(30)
      .stockOnHand(20)
      .build();
  private final ProductLocationMovementDto movementDto1 = ProductLocationMovementDto.builder()
      .programId(allProgramId)
      .facilityId(facilityId)
      .createdDate(localDate)
      .signature("Jimmy")
      .userId(userId)
      .movementLineItems(newArrayList(movementLineItemDto1))
      .build();
  private final ProductLocationMovementDto movementDto2 = ProductLocationMovementDto.builder()
      .programId(allProgramId)
      .facilityId(facilityId)
      .createdDate(localDate)
      .signature("Jimmy")
      .userId(userId)
      .movementLineItems(newArrayList(movementLineItemDto2))
      .build();

  @Test
  public void shouldCreateMovementLineItems() {
    stockCard.setId(stockCardId);
    movementDraft.setId(movementDraftId);

    when(stockCardRepository
        .findByFacilityIdAndProgramIdAndOrderableIdAndLotId(facilityId, programId, orderableId, lotId))
        .thenReturn(newArrayList(stockCard));
    when(movementLineItemRepository.save(newArrayList(lineItem))).thenReturn(newArrayList(lineItem));
    when(movementDraftRepository.findByProgramIdAndFacilityId(allProgramId, facilityId))
        .thenReturn(newArrayList(movementDraft));

    service.createMovementLineItems(movementDto1);

    verify(movementDraftRepository).delete(movementDraft);
  }

  @Test
  public void shouldThrowExceptionWhenQuantityMoreThanStockOnHand() {
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND));

    service.createMovementLineItems(movementDto2);
  }

  @Test
  public void shouldThrowExceptionWhenCannotFindStockCard() {
    exception.expect(NotFoundException.class);
    exception.expectMessage(containsString(ERROR_STOCK_CARD_NOT_FOUND));

    when(stockCardRepository
        .findByFacilityIdAndProgramIdAndOrderableIdAndLotId(facilityId, programId, orderableId, lotId))
        .thenReturn(Collections.emptyList());

    service.createMovementLineItems(movementDto1);
  }

  @Test
  public void shouldThrowExceptionWhenCannotFindMovementDraft() {
    exception.expect(NotFoundException.class);
    exception.expectMessage(containsString(ERROR_MOVEMENT_DRAFT_NOT_FOUND));

    when(stockCardRepository
        .findByFacilityIdAndProgramIdAndOrderableIdAndLotId(facilityId, programId, orderableId, lotId))
        .thenReturn(newArrayList(stockCard));
    when(movementLineItemRepository.save(newArrayList(lineItem))).thenReturn(newArrayList(lineItem));
    when(movementDraftRepository.findByProgramIdAndFacilityId(programId, facilityId))
        .thenReturn(Collections.emptyList());

    service.createMovementLineItems(movementDto1);
  }
}