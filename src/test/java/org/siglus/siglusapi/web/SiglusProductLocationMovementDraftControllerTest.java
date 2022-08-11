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

import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.ProductLocationMovementDraftDto;
import org.siglus.siglusapi.service.SiglusProductLocationMovementDraftService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusProductLocationMovementDraftControllerTest {

  @InjectMocks
  private SiglusProductLocationMovementDraftController controller;

  @Mock
  private SiglusProductLocationMovementDraftService service;

  private final ProductLocationMovementDraftDto productLocationMovementDraftDto = new ProductLocationMovementDraftDto();
  private final UUID programId = UUID.randomUUID();
  private final UUID movementDraftId = UUID.randomUUID();

  @Test
  public void shouldCallCreateEmptyProductLocationMovementDraft() {
    controller.createEmptyProductLocationMovementDraft(productLocationMovementDraftDto);

    verify(service).createEmptyMovementDraft(productLocationMovementDraftDto);
  }

  @Test
  public void shouldCallSearchMovementDrafts() {
    controller.searchMovementDrafts(programId);

    verify(service).searchMovementDrafts(programId);
  }

  @Test
  public void shouldCallSearchMovementDraft() {
    controller.searchMovementDraft(programId);

    verify(service).searchMovementDraft(programId);
  }

  @Test
  public void shouldCallUpdateDraft() {
    controller.updateDraft(movementDraftId, productLocationMovementDraftDto);

    verify(service).updateMovementDraft(productLocationMovementDraftDto, movementDraftId);
  }

  @Test
  public void shouldCallDeleteDraft() {
    controller.deleteDraft(movementDraftId);

    verify(service).deleteMovementDraft(movementDraftId);
  }

}