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

package org.openlmis.fulfillment.web.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.domain.Shipment;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"serviceUrl"})
public final class ProofOfDeliveryDto
    extends BaseDto
    implements ProofOfDelivery.Exporter, ProofOfDelivery.Importer {

  @Setter
  private String serviceUrl;

  @Getter
  private ShipmentObjectReferenceDto shipment;

  @Getter
  @Setter
  private ProofOfDeliveryStatus status;

  @Setter
  private List<ProofOfDeliveryLineItemDto> lineItems;

  @Getter
  @Setter
  private String receivedBy;

  @Getter
  @Setter
  private String deliveredBy;

  @Getter
  @Setter
  private LocalDate receivedDate;

  @JsonProperty
  public void setShipment(ShipmentObjectReferenceDto shipment) {
    this.shipment = shipment;
  }

  @Override
  @JsonIgnore
  public void setShipment(Shipment shipment) {
    if (null != shipment) {
      this.shipment = new ShipmentObjectReferenceDto(shipment.getId(), serviceUrl);
    }
  }

  @Override
  public List<ProofOfDeliveryLineItem.Importer> getLineItems() {
    return null == lineItems ? null : new ArrayList<>(lineItems);
  }

}
