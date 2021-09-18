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

package org.siglus.siglusapi.web;

import static org.apache.commons.lang3.RandomUtils.nextBoolean;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.StockManagementDraftDto;
import org.siglus.siglusapi.service.SiglusStockManagementDraftService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusStockManagementDraftControllerTest {

  @InjectMocks
  private SiglusStockManagementDraftController controller;

  @Mock
  private SiglusStockManagementDraftService service;

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
  public void shouldCallServiceWhenCreateEmptyStockManagementDraft() {
    StockManagementDraftDto dto = new StockManagementDraftDto();
    controller.createEmptyStockManagementDraft(dto);

    verify(service).createNewDraft(dto);
  }

  @Test
  public void shouldCallServiceWhenDeleteDraft() {
    UUID id = UUID.randomUUID();
    controller.deleteDraft(id);

    verify(service).deleteStockManagementDraft(id);
  }

  @Test
  public void shouldCallServiceWhenUpdateDraft() {
    UUID id = UUID.randomUUID();
    StockManagementDraftDto dto = new StockManagementDraftDto();
    controller.updateDraft(id, dto);

    verify(service).saveDraft(dto, id);
  }

}