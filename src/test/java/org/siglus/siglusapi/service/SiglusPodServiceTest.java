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

package org.siglus.siglusapi.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ShipmentObjectReferenceDto;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.service.client.SiglusPodFulfillmentService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusPodServiceTest {

  @InjectMocks
  private SiglusPodService service;

  @Mock
  private SiglusPodFulfillmentService fulfillmentService;

  @Mock
  private SiglusOrderService siglusOrderService;

  @Mock
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Mock
  private OrderExternalRepository orderExternalRepository;

  private final UUID externalId = UUID.randomUUID();

  @Test
  public void shouldGetPartialQualityWhenGetProofOfDelivery() {
    // given
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(UUID.randomUUID());
    OrderLineItemDto lineItemDto = new OrderLineItemDto();
    lineItemDto.setId(UUID.randomUUID());
    orderDto.setOrderLineItems(Arrays.asList(lineItemDto));
    ShipmentObjectReferenceDto shipmentDto = new ShipmentObjectReferenceDto(UUID.randomUUID());
    shipmentDto.setOrder(orderDto);
    ProofOfDeliveryDto dto = new ProofOfDeliveryDto();
    dto.setShipment(shipmentDto);
    when(fulfillmentService.searchProofOfDelivery(any(UUID.class), any())).thenReturn(dto);
    OrderLineItemDto lineItemDtoExtension = new OrderLineItemDto();
    lineItemDtoExtension.setId(UUID.randomUUID());
    lineItemDtoExtension.setPartialFulfilledQuantity(10L);
    OrderObjectReferenceDto orderExtensionDto = new OrderObjectReferenceDto(UUID.randomUUID());
    orderExtensionDto.setOrderLineItems(Arrays.asList(lineItemDtoExtension));
    when(siglusOrderService.getExtensionOrder(any(OrderObjectReferenceDto.class)))
        .thenReturn(orderExtensionDto);

    // when
    ProofOfDeliveryDto proofOfDeliveryDto = service
        .getPodDto(UUID.randomUUID(), Collections.emptySet());

    //then
    OrderLineItemDto lineItem = proofOfDeliveryDto.getShipment().getOrder().getOrderLineItems()
        .get(0);
    assertEquals(Long.valueOf(10), lineItem.getPartialFulfilledQuantity());
  }

  @Test
  public void shouldSetRequisitionNumberWhenGetProofOfDelivery() {
    // given
    OrderObjectReferenceDto orderDto = new OrderObjectReferenceDto(UUID.randomUUID());
    orderDto.setExternalId(externalId);
    ShipmentObjectReferenceDto shipmentDto = new ShipmentObjectReferenceDto(UUID.randomUUID());
    shipmentDto.setOrder(orderDto);
    ProofOfDeliveryDto dto = new ProofOfDeliveryDto();
    dto.setShipment(shipmentDto);
    when(fulfillmentService.searchProofOfDelivery(any(UUID.class), any())).thenReturn(dto);
    when(orderExternalRepository.findOne(externalId)).thenReturn(null);
    when(siglusRequisitionExtensionService.formatRequisitionNumber(externalId))
        .thenReturn("requisitionNumber");

    // when
    ProofOfDeliveryDto proofOfDeliveryDto = service.getPodDto(UUID.randomUUID(),
        Collections.emptySet());

    // then
    assertEquals("requisitionNumber",
        proofOfDeliveryDto.getShipment().getOrder().getRequisitionNumber());
  }
}
