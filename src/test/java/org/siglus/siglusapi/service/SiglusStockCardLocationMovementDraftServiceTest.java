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
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_EXISTS;

import java.time.LocalDate;
import java.util.Arrays;
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
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.constant.LocationConstants;
import org.siglus.siglusapi.domain.StockCardLocationMovementDraft;
import org.siglus.siglusapi.domain.StockCardLocationMovementLineItem;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftDto;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftLineItemDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockCardLocationMovementDraftRepository;
import org.siglus.siglusapi.repository.StockCardLocationMovementLineItemRepository;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.siglus.siglusapi.validator.StockCardLocationMovementDraftValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private StockCardLocationMovementLineItemRepository stockCardLocationMovementLineItemRepository;

  @Mock
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private SiglusLotReferenceDataService siglusLotReferenceDataService;
  private final UUID programId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID movementDraftId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID stockCardLocationMovementDraftId = UUID.randomUUID();
  private final UUID stockCardId1 = UUID.randomUUID();
  private final UUID stockCardId2 = UUID.randomUUID();

  private final StockCardLocationMovementDraftDto movementDraftDto = StockCardLocationMovementDraftDto
      .builder().programId(programId).facilityId(facilityId).build();
  private final StockCardLocationMovementDraft movementDraft = StockCardLocationMovementDraft
      .builder().programId(programId).facilityId(facilityId).build();

  @Before
  public void setup() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    userDto.setId(userId);
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

  @Test
  public void shouldSaveAndReturnVirtualLocationMovementDraftWhenThereIsVirtualLocationDataInFacility() {

    // given
    doNothing().when(draftValidator).validateFacilityId(facilityId);
    doNothing().when(draftValidator).validateProgramId(programId);
    doNothing().when(operatePermissionService).checkPermission(facilityId);

    StockCard stockCard1 = StockCard.builder().orderableId(orderableId).lotId(lotId).build();
    stockCard1.setId(stockCardId1);
    StockCard stockCard2 = new StockCard();
    stockCard2.setId(stockCardId2);
    when(stockCardRepository.findByFacilityIdIn(facilityId)).thenReturn(Arrays.asList(stockCard1, stockCard2));

    int quantity = 100;
    StockCardLocationMovementLineItem movementLineItem1 = StockCardLocationMovementLineItem
        .builder()
        .stockCardId(stockCardId1)
        .srcArea(LocationConstants.VIRTUAL_LOCATION_AREA)
        .srcLocationCode(LocationConstants.VIRTUAL_LOCATION_CODE)
        .destArea(LocationConstants.VIRTUAL_LOCATION_AREA)
        .destLocationCode(LocationConstants.VIRTUAL_LOCATION_CODE)
        .quantity(quantity)
        .build();
    StockCardLocationMovementLineItem movementLineItem2 = StockCardLocationMovementLineItem
        .builder()
        .stockCardId(stockCardId2)
        .srcLocationCode("22A05")
        .destLocationCode("22A06")
        .build();

    when(stockCardLocationMovementLineItemRepository.findPreviousRecordByStockCardId(any()))
        .thenReturn(Arrays.asList(movementLineItem1, movementLineItem2));

    Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);
    PageImpl<StockCard> stockCards = new PageImpl<>(Collections.singletonList(stockCard1), pageable, Integer.MAX_VALUE);
    when(stockCardRepository.findByIdIn(any(), any())).thenReturn(stockCards);

    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    String productName = "productName";
    orderableDto.setFullProductName(productName);
    String productCode = "99999";
    orderableDto.setProductCode(productCode);
    Page<OrderableDto> orderableDtoPage = new PageImpl<>((Collections.singletonList(orderableDto)));
    when(siglusOrderableService.searchOrderables(any(), any(), any())).thenReturn(orderableDtoPage);

    String lotCode = "SEM-LOTE-08L11-062025-25/06/2025";
    LotDto lotDto
        = LotDto.builder().lotCode(lotCode).expirationDate(LocalDate.now()).build();
    lotDto.setId(lotId);
    when(siglusLotReferenceDataService.findByIds(Collections.singletonList(lotId)))
        .thenReturn(Collections.singletonList(lotDto));

    StockCardLocationMovementDraftDto stockCardLocationMovementDraftDtoInputParam = StockCardLocationMovementDraftDto
        .builder()
        .userId(userId)
        .facilityId(facilityId)
        .programId(ALL_PRODUCTS_PROGRAM_ID)
        .id(stockCardLocationMovementDraftId)
        .build();

    StockCardLocationMovementDraftLineItemDto stockCardLocationMovementDraftLineItemDto =
        StockCardLocationMovementDraftLineItemDto
            .builder()
            .orderableId(orderableId)
            .productName(productName)
            .productCode(productCode)
            .lotId(lotId)
            .lotCode(lotCode)
            .expirationDate(LocalDate.now())
            .srcLocationCode(LocationConstants.VIRTUAL_LOCATION_CODE)
            .srcArea(LocationConstants.VIRTUAL_LOCATION_AREA)
            .quantity(quantity)
            .stockOnHand(quantity)
            .build();
    StockCardLocationMovementDraftDto expectedStockCardLocationMovementDraftDto = StockCardLocationMovementDraftDto
        .builder()
        .id(stockCardLocationMovementDraftId)
        .userId(userId)
        .facilityId(facilityId)
        .programId(ALL_PRODUCTS_PROGRAM_ID)
        .lineItems(Collections.singletonList(stockCardLocationMovementDraftLineItemDto))
        .build();

    // when
    StockCardLocationMovementDraftDto stockCardLocationMovementDraftDto = service.updateVirtualLocationMovementDraft(
        stockCardLocationMovementDraftDtoInputParam, stockCardLocationMovementDraftId);

    // then
    assertEquals(expectedStockCardLocationMovementDraftDto, stockCardLocationMovementDraftDto);
  }

}