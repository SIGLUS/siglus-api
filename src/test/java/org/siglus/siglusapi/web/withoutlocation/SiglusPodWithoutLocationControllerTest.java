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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ShipmentObjectReferenceDto;
import org.siglus.siglusapi.domain.PodSubDraftLineItem;
import org.siglus.siglusapi.dto.ProofOfDeliverySubDraftDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.service.SiglusLotService;
import org.siglus.siglusapi.service.SiglusPodService;
import org.siglus.siglusapi.util.MovementDateValidator;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.CreatePodSubDraftLineItemRequest;
import org.siglus.siglusapi.web.request.SubmitPodSubDraftsRequest;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.CreatePodSubDraftLineItemResponse;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

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
  @Mock
  private MovementDateValidator movementDateValidator;

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

  @Test
  public void shouldMergeSubDrafts() {
    UUID podId = UUID.randomUUID();
    Set<String> expand = new HashSet<>();

    controller.mergeSubDrafts(podId, expand);

    verify(proofOfDeliveryService).mergeSubDrafts(podId, expand);
  }

  @Test
  public void shouldSubmitSubDrafts() {
    doNothing().when(movementDateValidator).validateMovementDate(any(), any());
    ProofOfDeliverySubDraftDto draftDto = new ProofOfDeliverySubDraftDto();
    draftDto.setReceivedDate(LocalDate.now());
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(UUID.randomUUID());
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto();
    orderDto.setReceivingFacility(facilityDto);
    ShipmentObjectReferenceDto shipmentDto = new ShipmentObjectReferenceDto(orderDto, null, null, null, null, null);
    draftDto.setShipment(shipmentDto);
    SubmitPodSubDraftsRequest request = new SubmitPodSubDraftsRequest();
    request.setPodDto(draftDto);
    UUID podId = UUID.randomUUID();
    OAuth2Authentication authentication = mock(OAuth2Authentication.class);

    controller.submitSubDrafts(podId, request, authentication);

    verify(proofOfDeliveryService).submitSubDrafts(podId, request, authentication, false);
  }

  @Test
  public void shouldGetSubDraftDetail() {
    UUID podId = UUID.randomUUID();
    UUID subDraftId = UUID.randomUUID();
    Set<String> expand = new HashSet<>();

    controller.getSubDraftDetail(podId, subDraftId, expand);

    verify(proofOfDeliveryService).getSubDraftDetail(podId, subDraftId, expand);
  }

  @Test
  public void shouldUpdateSubDraft() {
    UUID podId = UUID.randomUUID();
    UUID subDraftId = UUID.randomUUID();
    UpdatePodSubDraftRequest request = new UpdatePodSubDraftRequest();

    controller.updateSubDraft(podId, subDraftId, request);

    verify(proofOfDeliveryService).updateSubDraft(request, subDraftId);
  }

  @Test
  public void shouldDeleteSubDraft() {
    UUID podId = UUID.randomUUID();
    UUID subDraftId = UUID.randomUUID();

    controller.deleteSubDraft(podId, subDraftId);

    verify(proofOfDeliveryService).deleteSubDraft(podId, subDraftId);
  }
}