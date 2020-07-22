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

package org.siglus.siglusapi.web;

import javax.transaction.Transactional;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/shipments")
public class SiglusShipmentController {

  @Autowired
  private SiglusShipmentService siglusShipmentService;

  @Autowired
  private SiglusNotificationService notificationService;

  @PostMapping
  @Transactional
  @ResponseStatus(HttpStatus.CREATED)
  public ShipmentDto createShipment(
      @RequestParam(name = "isSubOrder", required = false, defaultValue = "false")
          boolean isSubOrder, @RequestBody ShipmentDto shipmentDto) {
    ShipmentDto shipment;
    if (isSubOrder) {
      siglusShipmentService.createSubOrder(shipmentDto);
      shipment = siglusShipmentService.createShipment(shipmentDto);
    } else {
      shipment = siglusShipmentService.createShipment(shipmentDto);
      notificationService.postConfirmShipment(shipment);
    }
    return shipment;
  }

}
