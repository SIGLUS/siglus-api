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

package org.openlmis.fulfillment.web.shipment;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.service.ResourceNames;
import org.openlmis.fulfillment.testutils.ToStringTestUtils;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;

public class ShipmentDtoTest {

  public static final String SERVICE_URL = "localhost";

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(ShipmentDto.class)
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
    ShipmentDto shipmentDto = new ShipmentDtoDataBuilder().build();

    ToStringTestUtils.verify(ShipmentDto.class, shipmentDto);
  }

  @Test
  public void shouldGetRowsWithUnknownColumns() {
    Map<String, String> extraData = new HashMap<>();
    extraData.put(Shipment.ROWS_WITH_UNRESOLVED_ORDERABLE, "[]");
    ShipmentDto shipmentDto = new ShipmentDtoDataBuilder()
        .withExtraData(extraData)
        .build();

    assertThat(shipmentDto.getRowsWithUnresolvedOrderable(), is("[]"));
    assertThat(shipmentDto.getExtraData().isEmpty(), is(true));
  }

  @Test
  public void shouldSetReferenceDtoForShippedBy() {
    // No use of data builders because we actually tests setter here and builder hides what
    // it really does.
    ShipmentDto shipmentDto = new ShipmentDto();
    shipmentDto.setServiceUrl(SERVICE_URL);

    UUID userId = UUID.randomUUID();
    shipmentDto.setUserId(userId);

    assertEquals(userId, shipmentDto.getShippedBy().getId());
    assertEquals(
        SERVICE_URL + ResourceNames.getUsersPath() + userId,
        shipmentDto.getShippedBy().getHref());
  }
}
