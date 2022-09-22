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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.localmachine.event.order.fulfillment.OrderFulfillmentSyncedEmitter;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusShipmentService;
import org.siglus.siglusapi.web.request.ShipmentExtensionRequest;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.UnusedPrivateField")
public class SiglusShipmentControllerTest {

  @InjectMocks
  private SiglusShipmentController siglusShipmentController;

  @Mock
  private SiglusShipmentService siglusShipmentService;

  @Mock
  private SiglusNotificationService notificationService;
  @Mock
  private OrderFulfillmentSyncedEmitter orderFulfillmentSyncedEmitter;

  @Test
  public void shouldCallServiceWhenCreateShipment() {
    // given
    ShipmentExtensionRequest shipmentDto = new ShipmentExtensionRequest();

    // when
    siglusShipmentController.createShipment(false, shipmentDto);

    // then
    verify(siglusShipmentService).createOrderAndShipment(false, shipmentDto);
    verify(notificationService).postConfirmShipment(any());
  }
}
