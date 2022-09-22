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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionBuilder;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.localmachine.EventPayloadCheckUtils;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.ConvertToOrderRequest;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.OrderFulfillmentSyncedEmitter;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.OrderFulfillmentSyncedEvent;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.RequisitionLineItemRequest;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SiglusStatusChangeRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.android.FileBasedTest;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class OrderFulfillmentSyncedEmitterTest extends FileBasedTest {
  @InjectMocks
  private OrderFulfillmentSyncedEmitter orderFulfillmentSyncedEmitter;

  @Mock
  private EventPublisher eventPublisher;
  @Mock
  private SiglusAuthenticationHelper authHelper;
  @Mock
  private SiglusRequisitionRepository siglusRequisitionRepository;
  @Mock
  private RequisitionRepository requisitionRepository;
  @Mock
  private OrderRepository orderRepository;
  @Mock
  private SiglusStatusChangeRepository siglusStatusChangeRepository;
  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;

  private final UUID requisitionId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();


  @Before
  public void setup() {
    UserDto user = new UserDto();
    user.setHomeFacilityId(facilityId);
    user.setId(userId);
    when(authHelper.getCurrentUser()).thenReturn(user);

    when(requisitionExtensionRepository.findByRequisitionNumber(any())).thenReturn(new RequisitionExtension());
  }

  @Test
  public void shouldSuccessWhenEmitWithJson() throws IOException {
    // given
    String json = readFromFile("shipmentRequest.json");
    ObjectMapper objectMapper = PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER;
    ShipmentDto req = objectMapper.readValue(json, ShipmentDto.class);
    final ShipmentExtensionRequest reqExtension = new ShipmentExtensionRequest();
    reqExtension.setShipment(req);
    reqExtension.setConferredBy("test1");
    reqExtension.setPreparedBy("test2");
    when(siglusRequisitionRepository.countById(any())).thenReturn(1);

    Requisition requisition = RequisitionBuilder.newRequisition(facilityId, programId, true);
    requisition.setId(requisitionId);
    requisition.setTemplate(new RequisitionTemplate());
    requisition.setRequisitionLineItems(new ArrayList<>());
    when(requisitionRepository.findOne(any(UUID.class))).thenReturn(requisition);

    // when
    OrderFulfillmentSyncedEvent emitted = orderFulfillmentSyncedEmitter.emit(true, true, reqExtension);
    int count = EventPayloadCheckUtils.checkEventSerializeChanges(emitted,
        OrderFulfillmentSyncedEvent.class);
    // then
    assertThat(count).isZero();
  }

  @Test
  public void shouldSerializeSuccessWhenEmit() throws IOException {
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
        .orderable(new VersionEntityReference()).build());
    convertToOrderRequest.setRequisitionLineItems(list);

    final ShipmentExtensionRequest reqExtension = new ShipmentExtensionRequest();
    String jsonShipment = readFromFile("shipmentRequest.json");
    ShipmentDto req = objectMapper.readValue(jsonShipment, ShipmentDto.class);
    reqExtension.setShipment(req);
    reqExtension.setConferredBy("test1");
    reqExtension.setPreparedBy("test2");
    final OrderFulfillmentSyncedEvent event = new OrderFulfillmentSyncedEvent(UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), UUID.randomUUID(), true, true, reqExtension, convertToOrderRequest);
    // when
    int count = EventPayloadCheckUtils.checkEventSerializeChanges(event, OrderFulfillmentSyncedEvent.class);
    // then
    assertThat(count).isZero();
  }
}
