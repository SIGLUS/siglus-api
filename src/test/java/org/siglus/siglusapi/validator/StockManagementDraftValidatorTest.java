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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ORDERABLE_DISABLED_VVM;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.validators.VvmValidator;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.StockManagementDraftLineItemDto;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class StockManagementDraftValidatorTest {

  @Mock
  private VvmValidator vvmValidator;

  @Mock
  private StockManagementDraftRepository stockManagementDraftRepository;

  @InjectMocks
  private StockManagementDraftValidator draftValidator;

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyDraftIfDraftDtoIdIsNotNull() {
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .id(UUID.randomUUID())
        .build();

    draftValidator.validateEmptyDraft(draftDto);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyDraftIfProgramIdIsNull() {
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .id(null)
        .programId(null)
        .build();

    draftValidator.validateEmptyDraft(draftDto);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyDraftIfUserIdIsNull() {
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .id(null)
        .programId(UUID.randomUUID())
        .userId(null)
        .build();

    draftValidator.validateEmptyDraft(draftDto);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateEmptyDraftIfFacilityIdIsNull() {
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .id(null)
        .programId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .facilityId(null)
        .build();

    draftValidator.validateEmptyDraft(draftDto);
  }

  @Test
  public void shouldValidWhenValidateEmptyDraftIfAllIdExisted() {
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .id(null)
        .programId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .facilityId(UUID.randomUUID())
        .build();

    draftValidator.validateEmptyDraft(draftDto);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateDraftIfDraftDtoIdIsDifferent() {
    StockManagementDraftDto draftDto = new StockManagementDraftDto();
    draftDto.setId(UUID.randomUUID());

    draftValidator.validateDraft(draftDto, UUID.randomUUID());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateDraftIfFoundDraftIsNull() {
    UUID uuid = UUID.randomUUID();
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .id(uuid)
        .programId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .facilityId(UUID.randomUUID())
        .build();

    when(stockManagementDraftRepository.findOne(uuid)).thenReturn(null);

    draftValidator.validateDraft(draftDto, uuid);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateDraftIfFoundDraftIsNotDraft() {
    UUID uuid = UUID.randomUUID();
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .id(uuid)
        .programId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .facilityId(UUID.randomUUID())
        .build();
    StockManagementDraft draft = StockManagementDraft.builder()
        .isDraft(false)
        .build();

    when(stockManagementDraftRepository.findOne(uuid)).thenReturn(draft);

    draftValidator.validateDraft(draftDto, uuid);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateDraftIfValidateLineItemIsEmpty() {
    UUID uuid = UUID.randomUUID();
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .id(uuid)
        .programId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .facilityId(UUID.randomUUID())
        .lineItems(Collections.emptyList())
        .build();
    StockManagementDraft draft = StockManagementDraft.builder()
        .isDraft(true)
        .build();

    when(stockManagementDraftRepository.findOne(uuid)).thenReturn(draft);

    draftValidator.validateDraft(draftDto, uuid);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenValidateDraftIfOrderableIdMissing() {
    UUID uuid = UUID.randomUUID();
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .id(uuid)
        .programId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .facilityId(UUID.randomUUID())
        .lineItems(
            Arrays.asList(
                StockManagementDraftLineItemDto.builder().orderableId(null).build(),
                StockManagementDraftLineItemDto.builder().orderableId(UUID.randomUUID()).build()
            )
        )
        .build();
    StockManagementDraft draft = StockManagementDraft.builder()
        .isDraft(true)
        .build();

    when(stockManagementDraftRepository.findOne(uuid)).thenReturn(draft);

    draftValidator.validateDraft(draftDto, uuid);
  }

  @Test
  public void shouldValidWhenValidateDraftIfOrderableIdExisted() {
    UUID uuid = UUID.randomUUID();
    List<StockManagementDraftLineItemDto> lineItemDtos = Arrays.asList(
        StockManagementDraftLineItemDto.builder().orderableId(UUID.randomUUID()).build(),
        StockManagementDraftLineItemDto.builder().orderableId(UUID.randomUUID()).build()
    );
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .id(uuid)
        .programId(UUID.randomUUID())
        .userId(UUID.randomUUID())
        .facilityId(UUID.randomUUID())
        .lineItems(lineItemDtos)
        .build();
    StockManagementDraft draft = StockManagementDraft.builder()
        .isDraft(true)
        .build();

    when(stockManagementDraftRepository.findOne(uuid)).thenReturn(draft);

    draftValidator.validateDraft(draftDto, uuid);
    verify(vvmValidator).validate(lineItemDtos, ERROR_STOCK_MANAGEMENT_DRAFT_ORDERABLE_DISABLED_VVM,
        false);
  }

}