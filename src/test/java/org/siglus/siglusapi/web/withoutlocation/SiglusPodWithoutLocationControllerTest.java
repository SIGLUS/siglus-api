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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.siglus.siglusapi.domain.PodSubDraftLineItem;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.service.SiglusLotService;
import org.siglus.siglusapi.service.SiglusPodService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.CreatePodSubDraftLineItemRequest;
import org.siglus.siglusapi.web.response.CreatePodSubDraftLineItemResponse;

@RunWith(MockitoJUnitRunner.class)
public class SiglusPodWithoutLocationControllerTest {

  @InjectMocks
  private SiglusPodWithoutLocationController controller;

  @Mock
  private SiglusPodService proofOfDeliveryService;
  @Mock
  private SiglusLotService siglusLotService;
  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Before
  public void prepare() {
    UserDto user = new UserDto();
    user.setId(UUID.randomUUID());
    user.setHomeFacilityId(UUID.randomUUID());
    when(authenticationHelper.getCurrentUser()).thenReturn(user);
  }

  @Test
  public void shouldDeleteSubDrafts() {
    // given
    UUID podId = UUID.randomUUID();

    // when
    controller.deleteSubDrafts(podId);

    // then
    verify(proofOfDeliveryService).resetSubDrafts(podId);
  }

  @Test
  public void shouldCreatePodSubDraftLineItemSuccess() {
    final UUID podId = UUID.randomUUID();
    final UUID subDraftId = UUID.randomUUID();
    CreatePodSubDraftLineItemRequest request = new CreatePodSubDraftLineItemRequest();
    request.setPodLineItemId(UUID.randomUUID());
    when(siglusLotService.getLotsByOrderable(any(), any())).thenReturn(new ArrayList<>());
    PodSubDraftLineItem item = PodSubDraftLineItem.builder()
        .orderable(new VersionEntityReference(UUID.randomUUID(), 1L))
        .build();
    item.setId(UUID.randomUUID());
    when(proofOfDeliveryService.createPodSubDraftLineItem(podId, subDraftId, request.getPodLineItemId()))
        .thenReturn(item);

    CreatePodSubDraftLineItemResponse response = controller.createPodSubDraftLineItem(podId, subDraftId, request);

    assertEquals(item.getId(), response.getId());
    assertEquals(item.getOrderable(), response.getOrderable());
    assertEquals(0, response.getLots().size());
  }

  @Test
  public void shouldDeletePodSubDraftLineItem() {
    UUID podId = UUID.randomUUID();
    UUID subDraftId = UUID.randomUUID();
    UUID lineItemId = UUID.randomUUID();

    controller.deletePodSubDraftLineItem(podId, subDraftId, lineItemId);

    verify(proofOfDeliveryService).deletePodSubDraftLineItem(podId, subDraftId, lineItemId);
  }
}