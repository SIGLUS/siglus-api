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
import org.siglus.siglusapi.service.SiglusLocalIssueVoucherService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusLocalIssueVoucherControllerTest {

  @InjectMocks
  private SiglusLocalIssueVoucherController controller;

  @Mock
  private SiglusLocalIssueVoucherService siglusLocalIssueVoucherService;

  private static final UUID podId = UUID.randomUUID();

  private static final UUID subDraftId = UUID.randomUUID();

  @Test
  public void shouleDeleteWhenCallByService() {
    controller.deleteSubDraft(podId, subDraftId);

    verify(siglusLocalIssueVoucherService).deleteSubDraft(podId, subDraftId);
  }

  @Test
  public void shouldUpdateWhenCallByService() {
    controller.updateSubDraft(subDraftId, null);

    verify(siglusLocalIssueVoucherService).updateSubDraft(null, subDraftId);
  }

  @Test
  public void shouldGetWhenCallByService() {
    controller.getSubDraftDetail(podId, subDraftId, null);

    verify(siglusLocalIssueVoucherService).getSubDraftDetail(podId, subDraftId, null);
  }
}