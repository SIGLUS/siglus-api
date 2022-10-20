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

package org.siglus.siglusapi.web.withoutlocation;

import static org.apache.commons.lang3.RandomUtils.nextBoolean;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.dto.StockManagementInitialDraftDto;
import org.siglus.siglusapi.service.SiglusStockManagementDraftService;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusStockManagementDraftWithoutLocationControllerTest {

  @InjectMocks
  private SiglusStockManagementDraftWithoutLocationController controller;

  @Mock
  private SiglusStockManagementDraftService service;

  private final UUID draftId = UUID.randomUUID();

  private final UUID initialDraftId = UUID.randomUUID();

  private final StockManagementDraftDto dto = new StockManagementDraftDto();

  @Test
  public void shouldCallServiceWhenSearchDrafts() {
    UUID programId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String draftType = "draft_type";
    boolean isDraft = nextBoolean();
    controller.searchDrafts(programId, userId, draftType, isDraft);

    verify(service).findStockManagementDraft(programId, draftType, isDraft);
  }

  @Test
  public void shouldCallServiceWhenSearchDraftsMulti() {
    UUID initialDraftId = UUID.randomUUID();
    controller.searchMultiUserDrafts(initialDraftId);

    verify(service).findStockManagementDrafts(initialDraftId);
  }

  @Test
  public void shouldCallServiceWhenCreateEmptyStockManagementDraft() {
    controller.createEmptyStockManagementSubDraft(dto);

    verify(service).createNewSubDraft(dto);
  }

  @Test
  public void shouldCallServiceWhenUpdateDraft() {
    UUID id = UUID.randomUUID();
    controller.updateDraft(id, dto);

    verify(service).updateDraft(dto, id);
  }

  @Test
  public void shouldCallServiceWhenInitialDraft() {
    StockManagementInitialDraftDto initialDraftDto = new StockManagementInitialDraftDto();
    controller.initialDraft(initialDraftDto);

    verify(service).createInitialDraft(initialDraftDto);
  }

  @Test
  public void shouldCallServiceWhenSearchInitialDrafts() {
    UUID programId = UUID.randomUUID();
    String draftType = "draft-type";
    controller.searchInitialDrafts(programId, draftType);

    verify(service).findStockManagementInitialDraft(programId, draftType);
  }

  @Test
  public void shouldCallSearchDraft() {
    controller.searchDraft(draftId);
    verify(service).searchDraft(draftId);
  }

  @Test
  public void shouldCallupdatePartOfInfoWithDraft() {
    controller.restoreSubDraftWhenDoDelete(dto);
    verify(service).restoreSubDraftWhenDoDelete(dto);
  }

  @Test
  public void shouldCallCreateEmptyStockManagementSubDraft() {
    controller.createEmptyStockManagementSubDraft(dto);
    verify(service).createNewSubDraft(dto);
  }

  @Test
  public void shouldCallUpdateStatusAfterSubmit() {
    controller.updateStatusAfterSubmit(dto);
    verify(service).updateStatusAfterSubmit(dto);
  }

  @Test
  public void shouldMergeSubDrafts() {
    controller.mergeSubDrafts(initialDraftId);
    verify(service).mergeSubDrafts(initialDraftId);
  }

  @Test
  public void shouldCallDeleteInitialDraft() {
    controller.deleteInitialDraft(initialDraftId);
    verify(service).deleteInitialDraft(initialDraftId);
  }
}