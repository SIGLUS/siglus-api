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

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DummyProofOfDeliveryDto
    implements ProofOfDelivery.Importer, ProofOfDelivery.Exporter {

  private UUID id;
  private Identifiable shipment;
  private ProofOfDeliveryStatus status;
  private List<ProofOfDeliveryLineItem.Importer> lineItems;
  private String receivedBy;
  private String deliveredBy;
  private LocalDate receivedDate;

  DummyProofOfDeliveryDto(ProofOfDelivery pod, OrderableDto orderableDto) {
    this(
        pod.getId(), pod.getShipment(), pod.getStatus(),
        pod.getLineItems()
            .stream()
            .map(item -> new DummyProofOfDeliveryLineItemDto(item, orderableDto)).collect(toList()),
        pod.getReceivedBy(), pod.getDeliveredBy(), pod.getReceivedDate()
    );
  }

  @Override
  public void setShipment(Shipment shipment) {
    this.shipment = shipment;
  }

}
