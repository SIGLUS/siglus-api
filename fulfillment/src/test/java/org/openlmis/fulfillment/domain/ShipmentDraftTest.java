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

import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.testutils.OrderableDataBuilder;
import org.openlmis.fulfillment.testutils.ShipmentDraftDataBuilder;
import org.openlmis.fulfillment.testutils.ShipmentDraftLineItemDataBuilder;
import org.openlmis.fulfillment.testutils.ToStringTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class ShipmentDraftTest {

  private UUID id = UUID.randomUUID();
  private Order order = new Order(UUID.randomUUID());
  private String notes = "some notes";

  private UUID lineItemId = UUID.randomUUID();
  private UUID lotId = UUID.randomUUID();
  private Long quantityShipped = 15L;
  private Map<String, String> extraData = new HashMap<>();
  private OrderableDto orderableDto = new OrderableDataBuilder()
      .withId(UUID.randomUUID())
      .withVersionNumber(1L)
      .build();
  private List<ShipmentDraftLineItem> lineItems =
      Collections.singletonList(new ShipmentDraftLineItemDataBuilder()
          .withId(lineItemId)
          .withOrderable(orderableDto.getId(), orderableDto.getVersionNumber())
          .withLotId(lotId)
          .withQuantityShipped(quantityShipped)
          .build());

  @Test
  public void shouldCreateInstanceBasedOnImporter() {
    ShipmentDraft expected = createShipment();
    DummyShipmentDraftDto shipmentDraftDto = new DummyShipmentDraftDto(id, order, notes,
        Collections.singletonList(
            new DummyShipmentLineItemDto(lineItemId, orderableDto, lotId, quantityShipped,
                extraData)));

    ShipmentDraft actual = ShipmentDraft.newInstance(shipmentDraftDto);

    assertThat(expected, new ReflectionEquals(actual));
  }

  @Test
  public void shouldGetCopyOfLineItems() {
    ShipmentDraft shipment = createShipment();

    UUID newId = UUID.randomUUID();
    ShipmentDraftLineItem unchangedView = shipment.viewLineItems().get(0);
    ShipmentDraftLineItem changedView = shipment.viewLineItems().get(0);
    changedView.setId(newId);

    assertEquals(lineItems.get(0), unchangedView);
    assertNotEquals(lineItems.get(0).getId(), changedView.getId());
  }

  @Test
  public void shouldUpdateLineItems() {
    ShipmentDraftLineItemDataBuilder forUpdateItemBuilder = new ShipmentDraftLineItemDataBuilder();
    ShipmentDraftLineItem itemToUpdate = forUpdateItemBuilder.build();
    ShipmentDraftLineItem itemUpdating = forUpdateItemBuilder
        .withQuantityShipped(new Random().nextLong())
        .build();
    ShipmentDraft existingDraft = new ShipmentDraftDataBuilder()
        .withLineItems(Lists.newArrayList(
            itemToUpdate,
            new ShipmentDraftLineItemDataBuilder().build()))
        .build();
    ShipmentDraftLineItem newItem = new ShipmentDraftLineItemDataBuilder().build();
    ShipmentDraft newDraft = new ShipmentDraftDataBuilder()
        .withLineItems(Lists.newArrayList(
            itemUpdating,
            newItem))
        .build();

    existingDraft.updateFrom(newDraft);

    List<ShipmentDraftLineItem> updatedItems =
        (List<ShipmentDraftLineItem>)ReflectionTestUtils.getField(existingDraft, "lineItems");
    //First line item reference should not change
    assertTrue(itemToUpdate == updatedItems.get(0));
    assertTrue(reflectionEquals(itemToUpdate, updatedItems.get(0)));
    assertTrue(newItem != updatedItems.get(1));
    newItem.setId(null);
    assertTrue(reflectionEquals(newItem, updatedItems.get(1)));
  }

  @Test
  public void shouldExportValues() {
    DummyShipmentDraftDto shipmentDraftDto = new DummyShipmentDraftDto();

    ShipmentDraft shipment = createShipment();
    shipment.export(shipmentDraftDto);

    assertEquals(id, shipmentDraftDto.getId());
    assertEquals(order, shipmentDraftDto.getOrder());
    assertEquals(notes, shipmentDraftDto.getNotes());
  }

  @Test
  public void shouldImplementToString() {
    ShipmentDraft shipment = new ShipmentDraftDataBuilder().build();
    ToStringTestUtils.verify(ShipmentDraft.class, shipment);
  }

  private ShipmentDraft createShipment() {
    return new ShipmentDraftDataBuilder()
        .withId(id)
        .withOrder(order)
        .withNotes(notes)
        .withLineItems(lineItems)
        .build();
  }

}