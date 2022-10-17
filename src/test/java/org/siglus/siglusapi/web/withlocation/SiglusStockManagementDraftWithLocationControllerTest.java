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

package org.siglus.siglusapi.web.withlocation;

import static org.mockito.Mockito.verify;

import java.util.UUID;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.dto.StockManagementDraftWithLocationDto;
import org.siglus.siglusapi.dto.StockManagementInitialDraftDto;
import org.siglus.siglusapi.service.SiglusStockManagementDraftService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusStockManagementDraftWithLocationControllerTest extends TestCase {

  @InjectMocks
  private SiglusStockManagementDraftWithLocationController controller;

  @Mock
  private SiglusStockManagementDraftService service;

  private final UUID programId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID draftId = UUID.randomUUID();
  private final UUID initialDraftId = UUID.randomUUID();
  private final String draftType = FieldConstants.ISSUE_WITH_LOCATION;
  private final StockManagementDraftWithLocationDto draftWithLocationDto = StockManagementDraftWithLocationDto.builder()
      .build();
  private final StockManagementInitialDraftDto initialDraftDto = StockManagementInitialDraftDto.builder().build();

  @Test
  public void shouldCallSearchInitialDrafts() {
    controller.searchInitialDrafts(programId, draftType);

    verify(service).findStockManagementInitialDraft(programId, draftType);
  }

  @Test
  public void shouldCallInitialDraft() {
    controller.initialDraft(initialDraftDto);

    verify(service).createInitialDraft(initialDraftDto);
  }

  @Test
  public void shouldCallSearchDrafts() {
    controller.searchDrafts(programId, userId, draftType, true);

    verify(service).findStockManagementDraftWithLocation(programId, draftType, true);
  }

  @Test
  public void shouldCallUpdateDraft() {
    controller.updateDraftWithMutilUser(draftId, draftWithLocationDto);

    verify(service).updateDraftWithLocation(draftWithLocationDto, draftId);
  }

  @Test
  public void shouldCreateEmptyStockManagementDraftWithLocation() {
    controller.createEmptyStockManagementDraftWithLocation(draftWithLocationDto);

    verify(service).createEmptyStockManagementDraftWithLocation(draftWithLocationDto);
  }

  @Test
  public void shouldCallDeleteInitialDraft() {
    controller.deleteDraftWithMutilUser(draftId);

    verify(service).deleteStockManagementDraft(draftId);
  }

  @Test
  public void shouldCallDeleteDraft() {
    controller.deleteInitialDraft(initialDraftId);

    verify(service).deleteInitialDraft(initialDraftId);
  }

  @Test
  public void shouldCallSearchSubDrafts() {
    controller.searchSubDrafts(initialDraftId);

    verify(service).findSubDraftsWithLocation(initialDraftId);
  }
}