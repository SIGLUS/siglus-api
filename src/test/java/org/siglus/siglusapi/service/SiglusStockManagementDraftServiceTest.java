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
import static org.apache.commons.lang3.RandomUtils.nextBoolean;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.common.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS;
import static org.siglus.common.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND;

import java.util.ArrayList;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.siglus.siglusapi.validator.StockManagementDraftValidator;

@RunWith(MockitoJUnitRunner.class)
public class SiglusStockManagementDraftServiceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusStockManagementDraftService siglusStockManagementDraftService;

  @Mock
  private ActiveDraftValidator draftValidator;

  @Mock
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Mock
  StockManagementDraftValidator stockManagementDraftValidator;

  private final UUID id = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID userId = UUID.randomUUID();

  private final Boolean isDraft = nextBoolean();

  private final String draftType = "draftType";

  @Test
  public void shouldThrowExceptionWhenCreateNewDraftIfDraftExists() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MANAGEMENT_DRAFT_DRAFT_EXISTS));

    StockManagementDraft draft = StockManagementDraft.builder().build();
    when(stockManagementDraftRepository.findByProgramIdAndUserIdAndIsDraftAndDraftType(programId,
        userId, true, draftType)).thenReturn(newArrayList(draft));
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .programId(programId).userId(userId).draftType(draftType).build();

    siglusStockManagementDraftService.createNewDraft(draftDto);
  }

  @Test
  public void shouldIsDraftBeTrueWhenCreateNewDraft() {
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .programId(programId).userId(userId).draftType(draftType).build();
    StockManagementDraft draft = StockManagementDraft.createEmptyDraft(draftDto);
    when(stockManagementDraftRepository.save(draft)).thenReturn(draft);

    draftDto = siglusStockManagementDraftService.createNewDraft(draftDto);

    assertTrue(draftDto.getIsDraft());
  }

  @Test
  public void shouldIsDraftBeTrueWhenSaveDraft() {
    StockManagementDraftDto draftDto = StockManagementDraftDto.builder()
        .programId(programId).userId(userId).draftType(draftType).build();
    StockManagementDraft draft = StockManagementDraft.createStockManagementDraft(draftDto, true);
    when(stockManagementDraftRepository.save(draft)).thenReturn(draft);

    draftDto = siglusStockManagementDraftService.saveDraft(draftDto, id);

    assertTrue(draftDto.getIsDraft());
  }

  @Test
  public void shouldCallRepositoryWhenFindStockManagementDraft() {
    siglusStockManagementDraftService.findStockManagementDraft(programId, userId, draftType,
        isDraft);

    verify(draftValidator).validateIsDraft(isDraft);
    verify(stockManagementDraftRepository).findByProgramIdAndUserIdAndIsDraftAndDraftType(programId,
        userId, isDraft, draftType);
  }

  @Test
  public void shouldThrowExceptionWhenDeleteNotExistsDraft() {
    exception.expect(ResourceNotFoundException.class);
    exception.expectMessage(containsString(ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND));

    siglusStockManagementDraftService.deleteStockManagementDraft(id);
  }

  @Test
  public void shouldCallRepositoryWhenDeleteStockManagementDraftById() {
    StockManagementDraft draft = StockManagementDraft.builder().build();
    when(stockManagementDraftRepository.findOne(id)).thenReturn(draft);

    siglusStockManagementDraftService.deleteStockManagementDraft(id);

    verify(stockManagementDraftRepository).delete(draft);
  }

  @Test
  public void shouldCallRepositoryWhenDeleteStockManagementDraftByStockEventDto() {
    StockManagementDraft draft = StockManagementDraft.builder().build();
    final ArrayList<StockManagementDraft> drafts = newArrayList(draft);
    when(stockManagementDraftRepository.findByProgramIdAndUserIdAndIsDraftAndDraftType(programId,
        userId, true, draftType)).thenReturn(drafts);
    StockEventDto stockEventDto = StockEventDto.builder().programId(programId).userId(userId)
        .type(draftType).build();

    siglusStockManagementDraftService.deleteStockManagementDraft(stockEventDto);

    verify(stockManagementDraftRepository).delete(drafts);
  }
}
