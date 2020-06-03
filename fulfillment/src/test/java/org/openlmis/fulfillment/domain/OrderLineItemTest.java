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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.OrderLineItemDataBuilder;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.testutils.OrderableDataBuilder;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;

public class OrderLineItemTest {

  private UUID orderableId = UUID.randomUUID();
  private Long orderableVersionNumber = 1L;

  @Test
  public void shouldExportToDto() {
    OrderLineItemDto dto = new OrderLineItemDto();

    OrderLineItem lineItem = new OrderLineItemDataBuilder().build();
    lineItem.export(dto);

    assertEquals(lineItem.getId(), dto.getId());
    assertEquals(lineItem.getOrderedQuantity(), dto.getOrderedQuantity());
  }

  @Test
  public void shouldCreateNewInstance() {
    OrderableDto orderable = new OrderableDataBuilder()
        .withId(orderableId)
        .withVersionNumber(orderableVersionNumber)
        .build();
    OrderLineItem expected = new OrderLineItemDataBuilder()
        .withOrderable(orderable.getId(), orderable.getVersionNumber())
        .build();

    OrderLineItemDto dto = new OrderLineItemDto();
    expected.export(dto);
    dto.setOrderable(orderable);

    OrderLineItem actual = OrderLineItem.newInstance(dto);

    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getOrderedQuantity(), actual.getOrderedQuantity());
    assertEquals(expected.getOrderable(), actual.getOrderable());
  }

  @Test
  public void shouldCreateNewInstanceWithoutOrderable() {
    OrderLineItem expected = new OrderLineItemDataBuilder()
        .withoutOrderable()
        .build();

    OrderLineItemDto dto = new OrderLineItemDto();
    expected.export(dto);

    OrderLineItem actual = OrderLineItem.newInstance(dto);

    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getOrderedQuantity(), actual.getOrderedQuantity());
    assertNull(actual.getOrderable());
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(OrderLineItem.class)
        .withRedefinedSuperclass()
        .withPrefabValues(Order.class,
            new OrderDataBuilder().build(),
            new OrderDataBuilder().build())
        .verify();
  }
}
