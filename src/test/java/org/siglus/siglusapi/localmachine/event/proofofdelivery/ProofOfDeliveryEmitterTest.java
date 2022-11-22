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

package org.siglus.siglusapi.localmachine.event.proofofdelivery;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.PodLineItemsByLocation;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.event.proofofdelivery.web.ProofOfDeliveryEmitter;
import org.siglus.siglusapi.localmachine.event.proofofdelivery.web.ProofOfDeliveryEvent;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsByLocationRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;

@RunWith(MockitoJUnitRunner.class)
public class ProofOfDeliveryEmitterTest {

  @InjectMocks
  private ProofOfDeliveryEmitter emitter;

  @Mock
  private ProofOfDeliveryRepository proofOfDeliveryRepository;

  @Mock
  private PodExtensionRepository podExtensionRepository;

  @Mock
  private OrderExternalRepository orderExternalRepository;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;

  @Mock
  private PodLineItemsByLocationRepository podLineItemsByLocationRepository;

  @Mock
  private EventPublisher eventPublisher;

  @Test
  public void shouldEmitSuccessfully() {
    // given
    UUID podId = UUID.randomUUID();
    ProofOfDeliveryDto proofOfDeliveryDto = new ProofOfDeliveryDto();
    proofOfDeliveryDto.setId(podId);
    ProofOfDeliveryLineItemDto proofOfDeliveryLineItemDto = new ProofOfDeliveryLineItemDto();
    UUID podLineItemDtoId = UUID.randomUUID();
    proofOfDeliveryLineItemDto.setId(podLineItemDtoId);
    VersionObjectReferenceDto orderableDto = new VersionObjectReferenceDto(UUID.randomUUID(), null, null, 1L);
    proofOfDeliveryLineItemDto.setOrderable(orderableDto);
    proofOfDeliveryLineItemDto.setLotId(UUID.randomUUID());
    proofOfDeliveryLineItemDto.setQuantityAccepted(1);
    proofOfDeliveryLineItemDto.setQuantityRejected(2);
    proofOfDeliveryLineItemDto.setRejectionReasonId(UUID.randomUUID());
    proofOfDeliveryLineItemDto.setNotes("notes");
    List<ProofOfDeliveryLineItemDto> lineItems = Collections.singletonList(proofOfDeliveryLineItemDto);
    proofOfDeliveryDto.setLineItems(lineItems);
    UUID externalId = UUID.randomUUID();
    UUID facilityId = UUID.randomUUID();
    Order order = new Order();
    order.setExternalId(externalId);
    order.setSupplyingFacilityId(facilityId);
    Shipment shipment = new Shipment(order, null, null, null, null);
    proofOfDeliveryDto.setShipment(shipment);
    ProofOfDelivery proofOfDelivery = ProofOfDelivery.newInstance(proofOfDeliveryDto);
    proofOfDelivery.setShipment(shipment);
    when(proofOfDeliveryRepository.findOne(podId)).thenReturn(proofOfDelivery);
    List<PodLineItemsByLocation> podLineItemsByLocations = Collections.singletonList(new PodLineItemsByLocation());
    when(podLineItemsByLocationRepository.findByPodLineItemIdIn(Collections.singletonList(podLineItemDtoId)))
        .thenReturn(podLineItemsByLocations);
    PodExtension podExtension = new PodExtension();
    when(podExtensionRepository.findByPodId(podId)).thenReturn(podExtension);
    UserDto userDto = new UserDto();
    userDto.setId(UUID.randomUUID());
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(orderExternalRepository.findByIdIn(Collections.singleton(externalId))).thenReturn(Collections.emptyList());
    String requisitionNumberPrefix = "MTB.01020409.2203.";
    Integer requisitionNumber = 4;
    RequisitionExtension requisitionExtension = new RequisitionExtension();
    requisitionExtension.setRequisitionNumberPrefix(requisitionNumberPrefix);
    requisitionExtension.setRequisitionNumber(requisitionNumber);
    when(requisitionExtensionRepository.findByRequisitionId(externalId)).thenReturn(requisitionExtension);

    ProofOfDeliveryEvent event = new ProofOfDeliveryEvent();
    event.setPodLineItemsByLocation(podLineItemsByLocations);
    event.setProofOfDelivery(proofOfDelivery);
    event.setPodExtension(podExtension);
    event.setUserId(userDto.getId());

    // when
    emitter.emit(podId);

    // then
    verify(eventPublisher, times(1)).emitGroupEvent("MTB.01020409.2203.04", facilityId, event);
  }


}
