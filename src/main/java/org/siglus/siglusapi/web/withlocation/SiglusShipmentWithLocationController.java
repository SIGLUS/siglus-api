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

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.OrderFulfillmentSyncedEmitter;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusShipmentService;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/siglusapi/shipmentsWithLocation")
public class SiglusShipmentWithLocationController {

  private final SiglusShipmentService siglusShipmentService;
  private final SiglusNotificationService notificationService;
  private final OrderFulfillmentSyncedEmitter orderFulfillmentSyncedEmitter;

  @SneakyThrows
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public void confirmShipmentByLocation(@RequestParam(name = "isSubOrder", required = false, defaultValue = "false")
      boolean isSubOrder, @RequestBody ShipmentExtensionRequest shipmentExtensionRequest) {
    byte[] reqBytes = PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER.writeValueAsBytes(shipmentExtensionRequest);
    siglusShipmentService.checkFulfillOrderExpired(shipmentExtensionRequest);
    ShipmentDto shipmentByLocation = siglusShipmentService.createOrderAndShipmentByLocation(isSubOrder,
        shipmentExtensionRequest);
    notificationService.postConfirmShipment(shipmentByLocation);
    /*
    shipmentExtensionRequest changed in method 'createOrderAndShipment',
    but emitter need the old request, so write the old request to bytes and load it back when emitting.
     */
    ShipmentExtensionRequest request =
        PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER.readValue(reqBytes, ShipmentExtensionRequest.class);
    orderFulfillmentSyncedEmitter.emit(false, isSubOrder, request);
  }
}
