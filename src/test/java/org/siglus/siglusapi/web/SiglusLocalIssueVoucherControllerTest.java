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
import org.openlmis.fulfillment.domain.OrderStatus;
import org.siglus.siglusapi.dto.LocalIssueVoucherDto;
import org.siglus.siglusapi.service.SiglusLocalIssueVoucherService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusLocalIssueVoucherControllerTest {

  @InjectMocks
  private SiglusLocalIssueVoucherController controller;

  @Mock
  private SiglusLocalIssueVoucherService siglusLocalIssueVoucherService;

  private static final UUID podId = UUID.randomUUID();

  private static final UUID subDraftId = UUID.randomUUID();

  private static final UUID programId = UUID.randomUUID();

  private static final UUID localIssueVoucherId = UUID.randomUUID();

  private static final UUID requestingFacilityId = UUID.randomUUID();

  private static final UUID supplyingFacilityId = UUID.randomUUID();

  private final LocalIssueVoucherDto localIssueVoucherDto = LocalIssueVoucherDto.builder()
      .orderCode("code-1")
      .status(OrderStatus.SHIPPED)
      .programId(programId)
      .requestingFacilityId(requestingFacilityId)
      .supplyingFacilityId(supplyingFacilityId)
      .build();

  @Test
  public void shouleDeleteWhenCallByService() {
    controller.clearFillingPage(podId, subDraftId);

    verify(siglusLocalIssueVoucherService).clearFillingPage(subDraftId);
  }

  @Test
  public void shouldUpdateWhenCallByService() {
    controller.updateSubDraft(subDraftId, null);

    verify(siglusLocalIssueVoucherService).updateSubDraft(null, subDraftId);
  }

  @Test
  public void shouldGetWhenCallByService() {
    controller.getSubDraftDetail(subDraftId);

    verify(siglusLocalIssueVoucherService).getSubDraftDetail(subDraftId);
  }

  @Test
  public void shouldCreateLocalIssueVoucher() {
    controller.createLocalIssueVoucher(localIssueVoucherDto);

    verify(siglusLocalIssueVoucherService).createLocalIssueVoucher(localIssueVoucherDto);
  }

  @Test
  public void shouldCreateLocalIssueVoucherSubDraft() {
    controller.createLocalIssueVoucherSubDraft(localIssueVoucherId);

    verify(siglusLocalIssueVoucherService).createLocalIssueVoucherSubDraft(localIssueVoucherId);
  }

  @Test
  public void shouldSearchLocalIssueVoucherSubDrafts() {
    controller.searchLocalIssueVoucherSubDrafts(localIssueVoucherId);

    verify(siglusLocalIssueVoucherService).searchLocalIssueVoucherSubDrafts(localIssueVoucherId);
  }

  @Test
  public void shouldDeleteLocalIssueVoucher() {
    controller.deleteLocalIssueVoucher(localIssueVoucherId);

    verify(siglusLocalIssueVoucherService).deleteLocalIssueVoucher(localIssueVoucherId);
  }

  @Test
  public void shouldGetAvailableProductWhenCallByService() {
    controller.availableProduct(podId);

    verify(siglusLocalIssueVoucherService).getAvailableOrderables(podId);
  }
}