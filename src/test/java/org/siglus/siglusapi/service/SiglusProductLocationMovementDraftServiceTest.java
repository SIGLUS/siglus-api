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
import org.siglus.siglusapi.domain.ProductLocationMovementDraft;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftDto;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftLineItemDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.ProductLocationMovementDraftRepository;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.siglus.siglusapi.validator.ProductLocationMovementDraftValidator;

@RunWith(MockitoJUnitRunner.class)
public class SiglusProductLocationMovementDraftServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusProductLocationMovementDraftService service;

  @Mock
  private OperatePermissionService operatePermissionService;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private ActiveDraftValidator draftValidator;

  @Mock
  private ProductLocationMovementDraftRepository productLocationMovementDraftRepository;

  @Mock
  private ProductLocationMovementDraftValidator productLocationMovementDraftValidator;

  private final UUID programId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID movementDraftId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID lotId = UUID.randomUUID();
  private final ProductLocationMovementDraftDto movementDraftDto = ProductLocationMovementDraftDto
      .builder().programId(programId).facilityId(facilityId).build();
  private final ProductLocationMovementDraft movementDraft = ProductLocationMovementDraft
      .builder().programId(programId).facilityId(facilityId).build();

  @Before
  public void setup() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
  }

  @Test
  public void shouldCreateEmptyProductLocationMovementDraft() {

    doNothing().when(productLocationMovementDraftValidator)
        .validateEmptyMovementDraft(movementDraftDto);
    when(productLocationMovementDraftRepository.findByProgramIdAndFacilityId(programId, facilityId)).thenReturn(
        Collections.emptyList());
    when(productLocationMovementDraftRepository.save(any(ProductLocationMovementDraft.class)))
        .thenReturn(movementDraft);

    ProductLocationMovementDraftDto emptyProductLocationMovementDraft = service
        .createEmptyMovementDraft(movementDraftDto);

    assertThat(emptyProductLocationMovementDraft.getProgramId()).isEqualTo(programId);
    assertThat(emptyProductLocationMovementDraft.getFacilityId()).isEqualTo(facilityId);
  }

  @Test
  public void shouldThrowExceptionWhenProductLocationMovementDraftExists() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_MOVEMENT_DRAFT_EXISTS));

    doNothing().when(productLocationMovementDraftValidator)
        .validateEmptyMovementDraft(movementDraftDto);
    when(productLocationMovementDraftRepository.findByProgramIdAndFacilityId(programId, facilityId))
        .thenReturn(newArrayList(movementDraft));

    service.createEmptyMovementDraft(movementDraftDto);
  }

  @Test
  public void shouldSearchMovementDrafts() {
    doNothing().when(draftValidator).validateFacilityId(facilityId);
    doNothing().when(draftValidator).validateProgramId(programId);
    when(productLocationMovementDraftRepository.findByProgramIdAndFacilityId(programId, facilityId))
        .thenReturn(newArrayList(movementDraft));
    doNothing().when(operatePermissionService).checkPermission(facilityId);

    List<ProductLocationMovementDraftDto> productLocationMovementDraftDtos = service.searchMovementDrafts(programId);

    assertThat(productLocationMovementDraftDtos.size()).isEqualTo(1);
  }

  @Test
  public void shouldSearchMovementDraft() {
    when(productLocationMovementDraftRepository.findOne(movementDraftId)).thenReturn(movementDraft);
    ProductLocationMovementDraftDto productLocationMovementDraftDto = service.searchMovementDraft(movementDraftId);

    assertThat(productLocationMovementDraftDto.getFacilityId()).isEqualTo(facilityId);
    assertThat(productLocationMovementDraftDto.getProgramId()).isEqualTo(programId);
  }

  @Test
  public void shouldUpdateMovementDraft() {
    ProductLocationMovementDraftLineItemDto lineItemDto = ProductLocationMovementDraftLineItemDto.builder()
        .orderableId(orderableId)
        .lotId(lotId)
        .srcArea("A")
        .srcLocationCode("AA25E")
        .quantity(20)
        .stockOnHand(30)
        .build();
    movementDraftDto.setLineItems(newArrayList(lineItemDto));
    ProductLocationMovementDraft movementDraft = ProductLocationMovementDraft
        .createMovementDraft(movementDraftDto);
    doNothing().when(productLocationMovementDraftValidator)
        .validateMovementDraftAndLineItems(movementDraftDto, movementDraftId);
    when(productLocationMovementDraftRepository.save(movementDraft)).thenReturn(movementDraft);

    ProductLocationMovementDraftDto productLocationMovementDraftDto = service
        .updateMovementDraft(movementDraftDto, movementDraftId);

    assertThat(productLocationMovementDraftDto.getProgramId()).isEqualTo(programId);
    assertThat(productLocationMovementDraftDto.getFacilityId()).isEqualTo(facilityId);
  }

  @Test
  public void shouldDeleteMovementDraftById() {
    when(productLocationMovementDraftRepository.findOne(movementDraftId)).thenReturn(movementDraft);
    doNothing().when(productLocationMovementDraftValidator).validateMovementDraft(movementDraft);

    service.deleteMovementDraft(movementDraftId);
    verify(productLocationMovementDraftRepository).delete(movementDraft);
  }

  @Test
  public void shouldThrowExceptionWhenQuantityMoreThanStockOnHand() {
    exception.expect(BusinessDataException.class);
    exception.expectMessage(containsString(ERROR_MOVEMENT_QUANTITY_MORE_THAN_STOCK_ON_HAND));

    ProductLocationMovementDraftLineItemDto lineItemDto = ProductLocationMovementDraftLineItemDto.builder()
        .orderableId(orderableId)
        .lotId(lotId)
        .srcArea("A")
        .srcLocationCode("AA25E")
        .quantity(40)
        .stockOnHand(30)
        .build();
    movementDraftDto.setLineItems(newArrayList(lineItemDto));

    doNothing().when(productLocationMovementDraftValidator)
        .validateMovementDraftAndLineItems(movementDraftDto, movementDraftId);

    service.updateMovementDraft(movementDraftDto, movementDraftId);
  }
}