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

package org.siglus.siglusapi.localmachine.event.order;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderDtoBuilder;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionBuilder;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.ConvertToOrderRequest;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.OrderFulfillmentSyncedEvent;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.OrderFulfillmentSyncedReplayer;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.RequisitionLineItemRequest;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.siglus.siglusapi.repository.OrdersRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusShipmentService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.siglus.siglusapi.web.android.FileBasedTest;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class OrderFulfillmentSyncedReplayerTest extends FileBasedTest {
  @InjectMocks
  private OrderFulfillmentSyncedReplayer orderFulfillmentSyncedReplayer;
  @Mock
  private SiglusSimulateUserAuthHelper simulateUserAuthHelper;
  @Mock
  private SiglusShipmentService siglusShipmentService;
  @Mock
  private SiglusNotificationService notificationService;
  @Mock
  private OrdersRepository ordersRepository;
  @Mock
  private OrderDtoBuilder orderDtoBuilder;
  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;
  @Mock
  private RequisitionRepository requisitionRepository;
  @Mock
  private OrderableReferenceDataService orderableReferenceDataService;
  private final UUID requisitionId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @Test
  public void shouldDoReplaySuccess() throws IOException {
    // given
    ObjectMapper objectMapper = PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER;
    String jsonOrder = readFromFile("order.json");
    Order order = objectMapper.readValue(jsonOrder, Order.class);
    ConvertToOrderRequest convertToOrderRequest = ConvertToOrderRequest.builder().build();
    convertToOrderRequest.setFirstOrder(order);
    convertToOrderRequest.setRequisitionNumber("RNR-1");
    final List<RequisitionLineItemRequest> list = new ArrayList<>();
    list.add(RequisitionLineItemRequest.builder()
        .approvedQuantity(100)
        .orderable(new VersionEntityReference(orderableId, 1L)).build());
    convertToOrderRequest.setRequisitionLineItems(list);
    String jsonShipment = readFromFile("shipmentRequest.json");
    ShipmentDto req = objectMapper.readValue(jsonShipment, ShipmentDto.class);
    final ShipmentExtensionRequest reqExtension = new ShipmentExtensionRequest();
    reqExtension.setShipment(req);
    reqExtension.setConferredBy("test1");
    reqExtension.setPreparedBy("test2");
    final OrderFulfillmentSyncedEvent event = new OrderFulfillmentSyncedEvent(UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), true, true, reqExtension, convertToOrderRequest);
    RequisitionExtension requisitionExtension = new RequisitionExtension();
    requisitionExtension.setRequisitionId(requisitionId);
    when(requisitionExtensionRepository.findByRequisitionNumber(any())).thenReturn(requisitionExtension);
    Requisition requisition = RequisitionBuilder.newRequisition(facilityId, programId, true);
    requisition.setId(requisitionId);
    requisition.setTemplate(new RequisitionTemplate());
    requisition.setRequisitionLineItems(new ArrayList<>());
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);
    when(ordersRepository.saveAndFlush(any())).thenReturn(order);
    final OrderDto orderDto = new OrderDto();
    orderDto.setId(orderableId);
    when(orderDtoBuilder.build(any())).thenReturn(orderDto);
    // when
    orderFulfillmentSyncedReplayer.replay(event);
  }
}
