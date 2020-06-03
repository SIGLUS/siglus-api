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

package org.openlmis.fulfillment.web.shipmentdraft;

import java.util.Collections;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.fulfillment.testutils.ToStringTestUtils;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDtoDataBuilder;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;

public class ShipmentDraftDtoTest {

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(ShipmentDraftDto.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .withPrefabValues(
            OrderObjectReferenceDto.class,
            new OrderObjectReferenceDto(UUID.randomUUID()),
            new OrderObjectReferenceDto(UUID.randomUUID()))
        .withIgnoredFields("serviceUrl")
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    ShipmentDraftDto shipmentDraftDto = new ShipmentDraftDtoDataBuilder().build();

    ShipmentLineItemDto shipmentLineItemDto = new ShipmentLineItemDtoDataBuilder().build();
    shipmentDraftDto.setLineItems(Collections.singletonList(shipmentLineItemDto));

    ToStringTestUtils.verify(ShipmentDraftDto.class, shipmentDraftDto);
  }
}
