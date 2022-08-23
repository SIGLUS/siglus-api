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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_EXISTS;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.StockCardLocationMovementDraft;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftDto;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftLineItemDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockCardLocationMovementDraftRepository;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.siglus.siglusapi.validator.StockCardLocationMovementDraftValidator;

@RunWith(MockitoJUnitRunner.class)
public class SiglusStockCardLocationMovementDraftServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusStockCardLocationMovementDraftService service;

  @Mock
  private OperatePermissionService operatePermissionService;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private ActiveDraftValidator draftValidator;

  @Mock
  private StockCardLocationMovementDraftRepository stockCardLocationMovementDraftRepository;

  @Mock
  private StockCardLocationMovementDraftValidator stockCardLocationMovementDraftValidator;

  private final UUID programId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID movementDraftId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();
  private final StockCardLocationMovementDraftDto movementDraftDto = StockCardLocationMovementDraftDto
      .builder().programId(programId).facilityId(facilityId).build();
  private final StockCardLocationMovementDraft movementDraft = StockCardLocationMovementDraft
      .builder().programId(programId).facilityId(facilityId).build();

  @Before
  public void setup() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
  }

  @Test
  public void shouldCreateEmptyProductLocationMovementDraft() {

    doNothing().when(stockCardLocationMovementDraftValidator)
        .validateEmptyMovementDraft(movementDraftDto);
    when(stockCardLocationMovementDraftRepository.findByProgramIdAndFacilityId(programId, facilityId)).thenReturn(
        Collections.emptyList());
    when(stockCardLocationMovementDraftRepository.save(any(StockCardLocationMovementDraft.class)))
        .thenReturn(movementDraft);

    StockCardLocationMovementDraftDto emptyProductLocationMovementDraft = service
        .createEmptyMovementDraft(movementDraftDto);

    assertThat(emptyProductLocationMovementDraft.getProgramId()).isEqualTo(programId);
    assertThat(emptyProductLocationMovementDraft.getFacilityId()).isEqualTo(facilityId);
  }

  @Test
  public void shouldThrowExceptionWhenProductLocationMovementDraftExists() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_MOVEMENT_DRAFT_EXISTS));

    doNothing().when(stockCardLocationMovementDraftValidator)
        .validateEmptyMovementDraft(movementDraftDto);
    when(stockCardLocationMovementDraftRepository.findByProgramIdAndFacilityId(programId, facilityId))
        .thenReturn(newArrayList(movementDraft));

    service.createEmptyMovementDraft(movementDraftDto);
  }

  @Test
  public void shouldSearchMovementDrafts() {
    doNothing().when(draftValidator).validateFacilityId(facilityId);
    doNothing().when(draftValidator).validateProgramId(programId);
    when(stockCardLocationMovementDraftRepository.findByProgramIdAndFacilityId(programId, facilityId))
        .thenReturn(newArrayList(movementDraft));
    doNothing().when(operatePermissionService).checkPermission(facilityId);

    List<StockCardLocationMovementDraftDto> stockCardLocationMovementDraftDtos =
        service.searchMovementDrafts(programId);

    assertThat(stockCardLocationMovementDraftDtos.size()).isEqualTo(1);
  }

  @Test
  public void shouldSearchMovementDraft() {
    when(stockCardLocationMovementDraftRepository.findOne(movementDraftId)).thenReturn(movementDraft);
    StockCardLocationMovementDraftDto stockCardLocationMovementDraftDto = service.searchMovementDraft(movementDraftId);

    assertThat(stockCardLocationMovementDraftDto.getFacilityId()).isEqualTo(facilityId);
    assertThat(stockCardLocationMovementDraftDto.getProgramId()).isEqualTo(programId);
  }

  @Test
  public void shouldUpdateMovementDraft() {
    StockCardLocationMovementDraftLineItemDto lineItemDto = StockCardLocationMovementDraftLineItemDto.builder()
        .orderableId(orderableId)
        .lotId(lotId)
        .srcArea("A")
        .srcLocationCode("AA25E")
        .quantity(20)
        .stockOnHand(30)
        .build();
    movementDraftDto.setLineItems(newArrayList(lineItemDto));
    StockCardLocationMovementDraft movementDraft = StockCardLocationMovementDraft
        .createMovementDraft(movementDraftDto);
    doNothing().when(stockCardLocationMovementDraftValidator)
        .validateMovementDraftAndLineItems(movementDraftDto, movementDraftId);
    when(stockCardLocationMovementDraftRepository.save(movementDraft)).thenReturn(movementDraft);

    StockCardLocationMovementDraftDto stockCardLocationMovementDraftDto = service
        .updateMovementDraft(movementDraftDto, movementDraftId);

    assertThat(stockCardLocationMovementDraftDto.getProgramId()).isEqualTo(programId);
    assertThat(stockCardLocationMovementDraftDto.getFacilityId()).isEqualTo(facilityId);
  }

  @Test
  public void shouldDeleteMovementDraftById() {
    when(stockCardLocationMovementDraftRepository.findOne(movementDraftId)).thenReturn(movementDraft);
    doNothing().when(stockCardLocationMovementDraftValidator).validateMovementDraft(movementDraft);

    service.deleteMovementDraft(movementDraftId);
    verify(stockCardLocationMovementDraftRepository).delete(movementDraft);
  }
}