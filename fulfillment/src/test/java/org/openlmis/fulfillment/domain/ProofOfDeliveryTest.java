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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.openlmis.fulfillment.i18n.MessageKeys.MUST_CONTAIN_VALUE;
import static org.openlmis.fulfillment.i18n.MessageKeys.PROOF_OF_DELIVERY_LINE_ITEMS_REQUIRED;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.ProofOfDeliveryDataBuilder;
import org.openlmis.fulfillment.ProofOfDeliveryLineItemDataBuilder;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.testutils.OrderableDataBuilder;
import org.openlmis.fulfillment.testutils.ShipmentDataBuilder;
import org.openlmis.fulfillment.testutils.ShipmentLineItemDataBuilder;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;

@SuppressWarnings("PMD.TooManyMethods")
public class ProofOfDeliveryTest {

  private Map<VersionIdentityDto, OrderableDto> orderables = Maps.newHashMap();
  private OrderableDto orderableDto;
  private List<ProofOfDeliveryLineItem> lineItems;

  @Before
  public void setUp() {
    orderableDto = new OrderableDataBuilder()
        .withId(UUID.randomUUID())
        .withVersionNumber(1L)
        .build();
    orderables.put(orderableDto.getIdentity(), orderableDto);

    lineItems = Collections.singletonList(
        new ProofOfDeliveryLineItemDataBuilder()
        .withOrderable(orderableDto.getId(), orderableDto.getVersionNumber())
        .build());
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void shouldNotConfirmIfDeliveredByIsBlank() {
    setException("deliveredBy");
    new ProofOfDeliveryDataBuilder()
        .withoutDeliveredBy()
        .withLineItems(lineItems)
        .build()
        .confirm(orderables);
  }

  @Test
  public void shouldNotConfirmIfReceivedByIsBlank() {
    setException("receivedBy");
    new ProofOfDeliveryDataBuilder()
        .withoutReceivedBy()
        .withLineItems(lineItems)
        .build()
        .confirm(orderables);
  }

  @Test
  public void shouldNotConfirmIfReceivedDateIsNull() {
    setException("receivedDate");
    new ProofOfDeliveryDataBuilder()
        .withoutReceivedDate()
        .withLineItems(lineItems)
        .build()
        .confirm(orderables);
  }

  @Test
  public void shouldConfirm() {
    ProofOfDelivery pod = new ProofOfDeliveryDataBuilder()
        .withLineItems(lineItems)
        .build();
    pod.confirm(orderables);

    assertThat(pod.isConfirmed(), is(true));
  }

  @Test
  public void shouldCreateInstanceBasedOnImporter() {
    ProofOfDelivery expected = new ProofOfDeliveryDataBuilder().build();
    ProofOfDelivery.Importer importer = new DummyProofOfDeliveryDto(expected, orderableDto);

    ProofOfDelivery actual = ProofOfDelivery.newInstance(importer);

    assertThat(expected, new ReflectionEquals(actual));
  }

  @Test
  public void shouldCreateInstanceBasedOnShipment() {
    Shipment shipment = new ShipmentDataBuilder()
        .withLineItems(Lists.newArrayList(
            new ShipmentLineItemDataBuilder().build(),
            new ShipmentLineItemDataBuilder().build(),
            new ShipmentLineItemDataBuilder().withQuantityShipped(0L).build()
        ))
        .build();

    ProofOfDelivery pod = ProofOfDelivery.newInstance(shipment);

    assertThat(pod.getShipment(), is(shipment));
    assertThat(pod.getStatus(), is(ProofOfDeliveryStatus.INITIATED));
    assertThat(pod.getDeliveredBy(), is(nullValue()));
    assertThat(pod.getReceivedBy(), is(nullValue()));
    assertThat(pod.getReceivedDate(), is(nullValue()));
    assertThat(pod.getLineItems().size(), is(shipment.getLineItems().size() - 1));

    for (ProofOfDeliveryLineItem line : pod.getLineItems()) {
      assertThat(
          shipment.getLineItems(),
          hasItem(hasProperty("orderable", is(line.getOrderable())))
      );
      assertThat(
          shipment.getLineItems(),
          hasItem(hasProperty("lotId", is(line.getLotId())))
      );
      assertThat(line.getQuantityAccepted(), is(nullValue()));
      assertThat(line.getVvmStatus(), is(nullValue()));
      assertThat(line.getQuantityRejected(), is(nullValue()));
      assertThat(line.getRejectionReasonId(), is(nullValue()));
      assertThat(line.getNotes(), is(nullValue()));
    }
  }

  @Test
  public void shouldThrowExceptionIfLineItemsAreNotGiven() {
    exception.expect(ValidationException.class);
    exception.expectMessage(PROOF_OF_DELIVERY_LINE_ITEMS_REQUIRED);

    ProofOfDelivery.Importer importer =
        new DummyProofOfDeliveryDto(null, null, null, Collections.emptyList(), null, null, null);

    ProofOfDelivery.newInstance(importer);
  }

  @Test
  public void shouldConfirmIfLineItemIsLotless() {
    Shipment shipment = new ShipmentDataBuilder()
        .withLineItems(Collections.singletonList(new ShipmentLineItemDataBuilder()
            .withoutLotId()
            .withOrderable(orderableDto.getId(), orderableDto.getVersionNumber())
            .build()))
        .build();

    ProofOfDelivery proofOfDelivery = new ProofOfDeliveryDataBuilder()
        .withLineItems(Collections.singletonList(
            new ProofOfDeliveryLineItemDataBuilder()
            .withOrderable(shipment.getLineItems().get(0).getOrderable().getId(),
                shipment.getLineItems().get(0).getOrderable().getVersionNumber())
            .withoutLotId()
            .build()
        ))
        .build();

    proofOfDelivery.confirm(orderables);

    assertTrue(proofOfDelivery.isConfirmed());
  }

  @Test
  public void shouldExportValues() {
    DummyProofOfDeliveryDto exporter = new DummyProofOfDeliveryDto();

    ProofOfDelivery pod = new ProofOfDeliveryDataBuilder().build();
    pod.export(exporter);

    assertEquals(pod.getId(), exporter.getId());
    assertEquals(pod.getShipment(), exporter.getShipment());
    assertEquals(pod.getStatus(), exporter.getStatus());
    assertEquals(pod.getReceivedBy(), exporter.getReceivedBy());
    assertEquals(pod.getDeliveredBy(), exporter.getDeliveredBy());
    assertEquals(pod.getReceivedDate(), exporter.getReceivedDate());
  }

  @Test
  public void shouldReturnProgramId() {
    UUID programId = UUID.randomUUID();
    Order order = new OrderDataBuilder().withProgramId(programId).build();
    Shipment shipment = new ShipmentDataBuilder().withOrder(order).build();
    ProofOfDelivery instance = new ProofOfDeliveryDataBuilder().withShipment(shipment).build();
    assertThat(instance.getProgramId(), is(programId));
  }

  @Test
  public void shouldReturnReceivingFacilityId() {
    UUID receivingFacilityId = UUID.randomUUID();
    Order order = new OrderDataBuilder().withReceivingFacilityId(receivingFacilityId).build();
    Shipment shipment = new ShipmentDataBuilder().withOrder(order).build();
    ProofOfDelivery instance = new ProofOfDeliveryDataBuilder().withShipment(shipment).build();
    assertThat(instance.getReceivingFacilityId(), is(receivingFacilityId));
  }

  @Test
  public void shouldReturnSupplyingFacilityId() {
    UUID supplyingFacilityId = UUID.randomUUID();
    Order order = new OrderDataBuilder().withSupplyingFacilityId(supplyingFacilityId).build();
    Shipment shipment = new ShipmentDataBuilder().withOrder(order).build();
    ProofOfDelivery instance = new ProofOfDeliveryDataBuilder().withShipment(shipment).build();
    assertThat(instance.getSupplyingFacilityId(), is(supplyingFacilityId));
  }

  private void setException(String field) {
    exception.expect(allOf(
        instanceOf(ValidationException.class),
        hasProperty("messageKey", is(MUST_CONTAIN_VALUE)),
        hasProperty("params", hasItemInArray(field))
    ));
  }
}
