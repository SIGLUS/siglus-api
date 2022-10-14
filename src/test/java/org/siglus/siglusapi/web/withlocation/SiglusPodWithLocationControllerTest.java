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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.service.SiglusPodService;
import org.siglus.siglusapi.web.request.OperateTypeEnum;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftWithLocationRequest;

@RunWith(MockitoJUnitRunner.class)
public class SiglusPodWithLocationControllerTest {

  @InjectMocks
  private SiglusPodWithLocationController controller;

  @Mock
  private SiglusPodService proofOfDeliveryService;

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
    UpdatePodSubDraftWithLocationRequest request = buildForRequest(OperateTypeEnum.SAVE);

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
    UpdatePodSubDraftWithLocationRequest request = buildForRequest(OperateTypeEnum.SUBMIT);

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
    verify(proofOfDeliveryService).deleteSubDraftsWithLocation(podId);
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

  private UpdatePodSubDraftWithLocationRequest buildForRequest(OperateTypeEnum operateType) {
    UpdatePodSubDraftWithLocationRequest request = new UpdatePodSubDraftWithLocationRequest();
    request.setOperateType(operateType);
    return request;
  }
}
