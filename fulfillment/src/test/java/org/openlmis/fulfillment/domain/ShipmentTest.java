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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_LINE_ITEMS_REQUIRED;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.testutils.CreationDetailsDataBuilder;
import org.openlmis.fulfillment.testutils.OrderableDataBuilder;
import org.openlmis.fulfillment.testutils.ShipmentDataBuilder;
import org.openlmis.fulfillment.testutils.ShipmentLineItemDataBuilder;
import org.openlmis.fulfillment.testutils.ToStringTestUtils;
import org.openlmis.fulfillment.web.ValidationException;

public class ShipmentTest {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  private UUID id = UUID.randomUUID();
  private Order order = new Order(UUID.randomUUID());
  private CreationDetails shipDetails = new CreationDetailsDataBuilder().build();
  private String notes = "some notes";

  private UUID lineItemId = UUID.randomUUID();
  private UUID orderableId = UUID.randomUUID();
  private UUID lotId = UUID.randomUUID();
  private Long quantityShipped = 15L;
  private OrderableDto orderableDto = new OrderableDataBuilder()
      .withId(orderableId)
      .withVersionNumber(1L)
      .build();
  private List<ShipmentLineItem> lineItems =
      Collections.singletonList(new ShipmentLineItemDataBuilder()
          .withId(lineItemId)
          .withOrderable(orderableDto.getId(), orderableDto.getVersionNumber())
          .withLotId(lotId)
          .withQuantityShipped(quantityShipped)
          .build());

  @Test
  public void shouldCreateInstanceBasedOnImporter() {
    Shipment expected = createShipment();
    DummyShipmentDto shipmentDto =
        new DummyShipmentDto(id, order, shipDetails, notes, Collections.singletonList(
            new DummyShipmentLineItemDto(lineItemId, orderableDto, lotId, quantityShipped,
                null)), null);

    Shipment actual = Shipment.newInstance(shipmentDto, order);

    assertThat(expected, new ReflectionEquals(actual));
  }

  @Test
  public void shouldThrowExceptionIfLineItemsAreNotGiven() {
    expected.expect(ValidationException.class);
    expected.expectMessage(SHIPMENT_LINE_ITEMS_REQUIRED);
    DummyShipmentDto shipmentDto =
        new DummyShipmentDto(id, order, shipDetails, notes, Collections.emptyList(), null);

    Shipment.newInstance(shipmentDto, order);
  }

  @Test
  public void shouldExportValues() {
    DummyShipmentDto shipmentDto = new DummyShipmentDto();

    Shipment shipment = createShipment();
    shipment.export(shipmentDto);

    assertEquals(id, shipmentDto.getId());
    assertEquals(order, shipmentDto.getOrder());
    assertEquals(shipDetails, shipmentDto.getShipDetails());
    assertEquals(notes, shipmentDto.getNotes());
  }

  @Test
  public void shouldImplementToString() {
    Shipment shipment = new ShipmentDataBuilder().build();
    ToStringTestUtils.verify(Shipment.class, shipment);
  }

  @Test
  public void shouldGetCopyOfLineItems() {
    Shipment shipment = createShipment();

    UUID newId = UUID.randomUUID();
    ShipmentLineItem unchangedView = shipment.getLineItems().get(0);
    ShipmentLineItem changedView = shipment.getLineItems().get(0);
    changedView.setId(newId);

    assertEquals(lineItems.get(0), unchangedView);
    assertNotEquals(lineItems.get(0).getId(), changedView.getId());
  }

  @Test
  public void shouldReturnProgramId() {
    UUID programId = UUID.randomUUID();
    Order order = new OrderDataBuilder().withProgramId(programId).build();
    Shipment shipment = new ShipmentDataBuilder().withOrder(order).build();
    assertThat(shipment.getProgramId(), is(programId));
  }

  @Test
  public void shouldReturnReceivingFacilityId() {
    UUID receivingFacilityId = UUID.randomUUID();
    Order order = new OrderDataBuilder().withReceivingFacilityId(receivingFacilityId).build();
    Shipment shipment = new ShipmentDataBuilder().withOrder(order).build();
    assertThat(shipment.getReceivingFacilityId(), is(receivingFacilityId));
  }

  @Test
  public void shouldReturnSupplyingFacilityId() {
    UUID supplyingFacilityId = UUID.randomUUID();
    Order order = new OrderDataBuilder().withSupplyingFacilityId(supplyingFacilityId).build();
    Shipment shipment = new ShipmentDataBuilder().withOrder(order).build();
    assertThat(shipment.getSupplyingFacilityId(), is(supplyingFacilityId));
  }

  private Shipment createShipment() {
    return new ShipmentDataBuilder()
        .withId(id)
        .withOrder(order)
        .withShipDetails(shipDetails)
        .withNotes(notes)
        .withLineItems(lineItems)
        .build();
  }

}
