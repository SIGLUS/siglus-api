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
import org.siglus.siglusapi.web.request.OperateTypeEnum;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.CreatePodSubDraftLineItemResponse;

@RunWith(MockitoJUnitRunner.class)
public class SiglusPodWithLocationControllerTest {

  @InjectMocks
  private SiglusPodWithLocationController controller;

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
  public void shouldGetPodSubDraftWithLocation() {
    // given
    UUID podId = UUID.randomUUID();
    UUID subDraftId = UUID.randomUUID();

    // when
    controller.getPodSubDraftWithLocation(podId, subDraftId);

    // then
    verify(proofOfDeliveryService).getPodSubDraftWithLocation(podId, subDraftId);
  }

  @Test
  public void shouldDeleteSubDraftWithLocation() {
    // given
    UUID podId = UUID.randomUUID();
    UUID subDraftId = UUID.randomUUID();

    // when
    controller.deleteSubDraftWithLocation(podId, subDraftId);

    // then
    verify(proofOfDeliveryService).deleteSubDraftWithLocation(podId, subDraftId);
  }

  @Test
  public void shouldUpdateSubDraftWithLocation() {
    // given
    UUID subDraftId = UUID.randomUUID();
    UUID podId = UUID.randomUUID();
    UpdatePodSubDraftRequest request = buildForRequest(OperateTypeEnum.SAVE);

    // when
    controller.updateSubDraftWithLocation(podId, subDraftId, request);

    // then
    verify(proofOfDeliveryService).updateSubDraftWithLocation(request, subDraftId);
  }

  @Test
  public void shouldSubmitSubDraftWithLocation() {
    // given
    UUID subDraftId = UUID.randomUUID();
    UUID podId = UUID.randomUUID();
    UpdatePodSubDraftRequest request = buildForRequest(OperateTypeEnum.SUBMIT);

    // when
    controller.updateSubDraftWithLocation(podId, subDraftId, request);

    // then
    verify(proofOfDeliveryService).updateSubDraftWithLocation(request, subDraftId);
  }

  @Test
  public void shouldDeleteAllSubDraftsWithLocation() {
    // given
    UUID podId = UUID.randomUUID();

    // when
    controller.deleteSubDraftsWithLocation(podId);

    // then
    verify(proofOfDeliveryService).resetSubDraftsWithLocation(podId);
  }

  @Test
  public void shouldGetMergedSubDraftWithLocation() {
    // given
    UUID podId = UUID.randomUUID();

    // when
    controller.getMergedSubDraftWithLocation(podId);

    // then
    verify(proofOfDeliveryService).getMergedSubDraftWithLocation(podId);
  }

  @Test
  public void shouldGetPodWithLocation() {
    // given
    UUID podId = UUID.randomUUID();

    // when
    controller.viewPodWithLocation(podId);

    // then
    verify(proofOfDeliveryService).getPodExtensionResponseWithLocation(podId);
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

  private UpdatePodSubDraftRequest buildForRequest(OperateTypeEnum operateType) {
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();
    request.setOperateType(operateType);
    return request;
  }
}
