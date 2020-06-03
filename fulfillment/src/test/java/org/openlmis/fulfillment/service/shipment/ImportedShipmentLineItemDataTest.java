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

package org.openlmis.fulfillment.service.shipment;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.testutils.ShipmentLineItemDataBuilder;

public class ImportedShipmentLineItemDataTest {

  @Test
  public void shouldAddLineItem() {
    ImportedShipmentLineItemData data = new ImportedShipmentLineItemData();
    ShipmentLineItem lineItem = new ShipmentLineItemDataBuilder().withId(UUID.randomUUID()).build();

    data.addLineItem(lineItem);

    assertThat(data.getLineItems(), contains(lineItem));
  }

  @Test
  public void shouldAddUnresolvedRowData() {
    Map<String, String> row = new HashMap<>();
    row.put("orderId", "O1");

    ImportedShipmentLineItemData data = new ImportedShipmentLineItemData();
    data.addUnresolvedRowData(row);

    assertThat(data.getRowsWithUnresolvedOrderable(), contains(row));
  }
}