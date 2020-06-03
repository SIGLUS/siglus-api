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

package org.openlmis.fulfillment;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.testutils.ShipmentDataBuilder;

public class ProofOfDeliveryDataBuilder {
  private UUID id = UUID.randomUUID();
  private Shipment shipment = new ShipmentDataBuilder().build();
  private ProofOfDeliveryStatus status = ProofOfDeliveryStatus.INITIATED;
  private List<ProofOfDeliveryLineItem> lineItems = Lists.newArrayList(
      new ProofOfDeliveryLineItemDataBuilder().build()
  );
  private String receivedBy = RandomStringUtils.randomAlphanumeric(5);
  private String deliveredBy = RandomStringUtils.randomAlphanumeric(5);
  private LocalDate receivedDate = LocalDate.now();

  public ProofOfDeliveryDataBuilder withoutDeliveredBy() {
    deliveredBy = null;
    return this;
  }

  public ProofOfDeliveryDataBuilder withoutReceivedBy() {
    receivedBy = null;
    return this;
  }

  public ProofOfDeliveryDataBuilder withoutReceivedDate() {
    receivedDate = null;
    return this;
  }

  public ProofOfDeliveryDataBuilder withLineItems(List<ProofOfDeliveryLineItem> lineItems) {
    this.lineItems = lineItems;
    return this;
  }

  public ProofOfDeliveryDataBuilder withShipment(Shipment shipment) {
    this.shipment = shipment;
    return this;
  }

  /**
   * Builds new instance of {@link ProofOfDelivery}.
   */
  public ProofOfDelivery build() {
    if (null == id) {
      lineItems.forEach(line -> line.setId(null));
    }

    ProofOfDelivery pod = new ProofOfDelivery(
        shipment, status, lineItems,
        receivedBy, deliveredBy, receivedDate
    );
    pod.setId(id);

    return pod;
  }

  /**
   * Builds new instance of {@link ProofOfDelivery} without id.
   */
  public ProofOfDelivery buildAsNew() {
    id = null;
    return build();
  }

  /**
   * Builds new instance of {@link ProofOfDelivery} with
   * {@link ProofOfDeliveryStatus#CONFIRMED} status.
   */
  public ProofOfDelivery buildAsConfirmed() {
    status = ProofOfDeliveryStatus.CONFIRMED;
    return build();
  }
}
