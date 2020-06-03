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

package org.openlmis.fulfillment.domain;

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import org.junit.Test;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.testutils.OrderableDataBuilder;
import org.openlmis.fulfillment.testutils.ShipmentDraftLineItemDataBuilder;
import org.openlmis.fulfillment.testutils.ToStringTestUtils;

public class ShipmentDraftLineItemTest {

  private UUID lineItemId = UUID.randomUUID();
  private UUID lotId = UUID.randomUUID();
  private Long quantityShipped = 15L;
  OrderableDto orderableDto = new OrderableDataBuilder()
      .withId(UUID.randomUUID())
      .withVersionNumber(1L)
      .build();

  @Test
  public void shouldExportValues() {
    DummyShipmentLineItemDto exporter = new DummyShipmentLineItemDto();

    ShipmentDraftLineItem shipmentLineItem = createShipmentLineItem();
    OrderableDto orderableDto = new OrderableDataBuilder()
        .withId(shipmentLineItem.getOrderable().getId())
        .withVersionNumber(shipmentLineItem.getOrderable().getVersionNumber())
        .build();
    shipmentLineItem.export(exporter, orderableDto);

    assertEquals(lineItemId, exporter.getId());
    assertEquals(orderableDto, exporter.getOrderable());
    assertEquals(lotId, exporter.getLotId());
    assertEquals(quantityShipped, exporter.getQuantityShipped());
  }

  private ShipmentDraftLineItem createShipmentLineItem() {
    return new ShipmentDraftLineItemDataBuilder()
        .withId(lineItemId)
        .withLotId(lotId)
        .withOrderable(orderableDto.getId(), orderableDto.getVersionNumber())
        .withQuantityShipped(quantityShipped)
        .build();
  }

  @Test
  public void shouldImplementToString() {
    ShipmentDraftLineItem shipmentLineItem = new ShipmentDraftLineItemDataBuilder().build();
    ToStringTestUtils.verify(ShipmentDraftLineItem.class, shipmentLineItem);
  }

}