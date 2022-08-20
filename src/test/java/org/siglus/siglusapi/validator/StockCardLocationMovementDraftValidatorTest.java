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

package org.siglus.siglusapi.validator;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.StockCardLocationMovementDraft;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftDto;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftLineItemDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockCardLocationMovementDraftRepository;

@RunWith(MockitoJUnitRunner.class)
public class StockCardLocationMovementDraftValidatorTest {

  @InjectMocks
  StockCardLocationMovementDraftValidator stockCardLocationMovementDraftValidator;

  @Mock
  StockCardLocationMovementDraftRepository stockCardLocationMovementDraftRepository;

  private final UUID movementDraftId = UUID.randomUUID();
  private final UUID id = UUID.randomUUID();
  private final StockCardLocationMovementDraftDto movementDraftDto = StockCardLocationMovementDraftDto
      .builder()
      .id(movementDraftId)
      .build();
  private final StockCardLocationMovementDraft movementDraft = StockCardLocationMovementDraft.builder().build();


  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyMovementDraftIfDraftIdIsNull() {
    StockCardLocationMovementDraftDto draftDto = StockCardLocationMovementDraftDto.builder()
        .id(null)
        .build();

    stockCardLocationMovementDraftValidator.validateEmptyMovementDraft(draftDto);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyMovementDraftIfProgramIdIsNull() {
    StockCardLocationMovementDraftDto draftDto = StockCardLocationMovementDraftDto.builder()
        .id(movementDraftId)
        .programId(null)
        .build();

    stockCardLocationMovementDraftValidator.validateEmptyMovementDraft(draftDto);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyMovementDraftIfUserIdIsNull() {
    StockCardLocationMovementDraftDto draftDto = StockCardLocationMovementDraftDto.builder()
        .id(movementDraftId)
        .userId(null)
        .build();

    stockCardLocationMovementDraftValidator.validateEmptyMovementDraft(draftDto);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyMovementDraftIfFacilityIdIsNull() {
    StockCardLocationMovementDraftDto draftDto = StockCardLocationMovementDraftDto.builder()
        .id(movementDraftId)
        .facilityId(null)
        .build();

    stockCardLocationMovementDraftValidator.validateEmptyMovementDraft(draftDto);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowExceptionWhenMovementDraftNotFound() {
    stockCardLocationMovementDraftValidator.validateMovementDraft(null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenMovementDraftDtoIdNotEqualsId() {
    stockCardLocationMovementDraftValidator.validateMovementDraftAndLineItems(movementDraftDto, id);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenMovementDraftLineItemsIsEmpty() {
    when(stockCardLocationMovementDraftRepository.findOne(movementDraftId)).thenReturn(movementDraft);
    stockCardLocationMovementDraftValidator.validateMovementDraftAndLineItems(movementDraftDto, movementDraftId);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenMovementDraftLineItemsOrderablesIdHasNull() {
    StockCardLocationMovementDraftLineItemDto lineItemDto = StockCardLocationMovementDraftLineItemDto
        .builder()
        .orderableId(null)
        .build();
    movementDraftDto.setLineItems(newArrayList(lineItemDto));
    when(stockCardLocationMovementDraftRepository.findOne(movementDraftId)).thenReturn(movementDraft);
    stockCardLocationMovementDraftValidator.validateMovementDraftAndLineItems(movementDraftDto, movementDraftId);
  }
}