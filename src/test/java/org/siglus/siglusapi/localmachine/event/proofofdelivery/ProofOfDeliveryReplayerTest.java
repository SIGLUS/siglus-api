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
import static org.openlmis.fulfillment.domain.OrderStatus.SHIPPED;

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
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.PodLineItemsByLocation;
import org.siglus.siglusapi.localmachine.event.NotificationService;
import org.siglus.siglusapi.localmachine.event.proofofdelivery.web.ProofOfDeliveryEvent;
import org.siglus.siglusapi.localmachine.event.proofofdelivery.web.ProofOfDeliveryReplayer;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsByLocationRepository;
import org.siglus.siglusapi.repository.SiglusOrdersRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusShipmentRepository;

@RunWith(MockitoJUnitRunner.class)
public class ProofOfDeliveryReplayerTest {

  @InjectMocks
  private ProofOfDeliveryReplayer replayer;

  @Mock
  private ProofOfDeliveryRepository proofOfDeliveryRepository;

  @Mock
  private SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;

  @Mock
  private PodExtensionRepository podExtensionRepository;

  @Mock
  private SiglusOrdersRepository siglusOrdersRepository;

  @Mock
  private PodLineItemsByLocationRepository podLineItemsByLocationRepository;

  @Mock
  private NotificationService notificationService;

  @Mock
  private SiglusShipmentRepository siglusShipmentRepository;

  @Test
  public void shouldReplayerSuccessfully() {
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
    UUID orderId = UUID.randomUUID();
    String orderCode = "orderCode";
    Order order = new Order();
    order.setOrderCode(orderCode);
    order.setId(orderId);
    order.setStatus(SHIPPED);
    UUID shipmentId = UUID.randomUUID();
    Shipment shipment = new Shipment(order, null, null, null, null);
    shipment.setId(shipmentId);
    proofOfDeliveryDto.setShipment(shipment);
    ProofOfDelivery proofOfDelivery = ProofOfDelivery.newInstance(proofOfDeliveryDto);
    proofOfDelivery.setShipment(shipment);
    PodExtension podExtension = new PodExtension();
    List<PodLineItemsByLocation> podLineItemsByLocations = Collections.singletonList(new PodLineItemsByLocation());
    UUID userId = UUID.randomUUID();
    ProofOfDeliveryEvent event = new ProofOfDeliveryEvent();
    event.setProofOfDelivery(proofOfDelivery);
    event.setPodExtension(podExtension);
    event.setPodLineItemsByLocation(podLineItemsByLocations);
    event.setUserId(userId);
    when(siglusOrdersRepository.findByOrderCode(orderCode)).thenReturn(order);
    when(siglusShipmentRepository.findShipmentByOrderId(orderId)).thenReturn(shipment);
    when(siglusProofOfDeliveryRepository.findByShipmentId(shipmentId)).thenReturn(proofOfDelivery);

    // when
    replayer.replay(event);

    // then
    verify(proofOfDeliveryRepository, times(1)).saveAndFlush(proofOfDelivery);
    verify(podExtensionRepository, times(1)).saveAndFlush(podExtension);
    verify(podLineItemsByLocationRepository, times(1)).save(podLineItemsByLocations);
    verify(notificationService, times(1)).postConfirmPod(userId, podId, order);
  }

}
