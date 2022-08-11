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
import org.siglus.siglusapi.domain.ProductLocationMovementDraft;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftDto;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftLineItemDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.ProductLocationMovementDraftRepository;

@RunWith(MockitoJUnitRunner.class)
public class ProductLocationMovementDraftValidatorTest {

  @InjectMocks
  ProductLocationMovementDraftValidator productLocationMovementDraftValidator;

  @Mock
  ProductLocationMovementDraftRepository productLocationMovementDraftRepository;

  private final UUID movementDraftId = UUID.randomUUID();
  private final UUID id = UUID.randomUUID();
  private final ProductLocationMovementDraftDto movementDraftDto = ProductLocationMovementDraftDto
      .builder()
      .id(movementDraftId)
      .build();
  private final ProductLocationMovementDraft movementDraft = ProductLocationMovementDraft.builder().build();


  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyMovementDraftIfDraftIdIsNull() {
    ProductLocationMovementDraftDto draftDto = ProductLocationMovementDraftDto.builder()
        .id(null)
        .build();

    productLocationMovementDraftValidator.validateEmptyStockMovementDraft(draftDto);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyMovementDraftIfProgramIdIsNull() {
    ProductLocationMovementDraftDto draftDto = ProductLocationMovementDraftDto.builder()
        .id(movementDraftId)
        .programId(null)
        .build();

    productLocationMovementDraftValidator.validateEmptyStockMovementDraft(draftDto);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyMovementDraftIfUserIdIsNull() {
    ProductLocationMovementDraftDto draftDto = ProductLocationMovementDraftDto.builder()
        .id(movementDraftId)
        .userId(null)
        .build();

    productLocationMovementDraftValidator.validateEmptyStockMovementDraft(draftDto);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyMovementDraftIfFacilityIdIsNull() {
    ProductLocationMovementDraftDto draftDto = ProductLocationMovementDraftDto.builder()
        .id(movementDraftId)
        .facilityId(null)
        .build();

    productLocationMovementDraftValidator.validateEmptyStockMovementDraft(draftDto);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowExceptionWhenMovementDraftNotFound() {
    productLocationMovementDraftValidator.validateMovementDraft(null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenMovementDraftDtoIdNotEqualsId() {
    productLocationMovementDraftValidator.validateMovementDraftAndLineItems(movementDraftDto, id);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenMovementDraftLineItemsIsEmpty() {
    when(productLocationMovementDraftRepository.findOne(movementDraftId)).thenReturn(movementDraft);
    productLocationMovementDraftValidator.validateMovementDraftAndLineItems(movementDraftDto, movementDraftId);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenMovementDraftLineItemsOrderablesIdHasNull() {
    ProductLocationMovementDraftLineItemDto lineItemDto = ProductLocationMovementDraftLineItemDto
        .builder()
        .orderableId(null)
        .build();
    movementDraftDto.setLineItems(newArrayList(lineItemDto));
    when(productLocationMovementDraftRepository.findOne(movementDraftId)).thenReturn(movementDraft);
    productLocationMovementDraftValidator.validateMovementDraftAndLineItems(movementDraftDto, movementDraftId);
  }
}