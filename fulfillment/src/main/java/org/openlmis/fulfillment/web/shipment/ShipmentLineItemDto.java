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

import static org.openlmis.fulfillment.service.ResourceNames.LOTS;
import static org.openlmis.fulfillment.service.ResourceNames.ORDERABLES;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;
import org.openlmis.fulfillment.web.util.VersionObjectReferenceDto;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"serviceUrl"})
@ToString
public final class ShipmentLineItemDto
    implements ShipmentLineItem.Exporter, ShipmentLineItem.Importer {

  @Setter
  @JsonIgnore
  private String serviceUrl;

  @Getter
  @Setter
  private UUID id;

  @Getter
  private VersionObjectReferenceDto orderable;

  @Getter
  @Setter
  private ObjectReferenceDto lot;

  @Getter
  @Setter
  private Long quantityShipped;

  @Getter
  @Setter
  private Map<String, String> extraData;

  @Override
  @JsonIgnore
  public void setOrderable(OrderableDto orderableDto) {
    if (orderableDto != null) {
      this.orderable = new VersionObjectReferenceDto(orderableDto.getId(), serviceUrl,
          ORDERABLES, orderableDto.getVersionNumber());
    }
  }

  @JsonSetter("orderable")
  public void setOrderable(VersionObjectReferenceDto orderable) {
    this.orderable = orderable;
  }

  @Override
  @JsonIgnore
  public void setLotId(UUID lotId) {
    if (lotId != null) {
      this.lot = ObjectReferenceDto.create(lotId, serviceUrl, LOTS);
    }
  }

  @Override
  @JsonIgnore
  public VersionIdentityDto getOrderableIdentity() {
    return Optional
        .ofNullable(orderable)
        .map(item -> new VersionIdentityDto(orderable.getId(), orderable.getVersionNumber()))
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public UUID getLotId() {
    if (lot == null) {
      return null;
    }
    return lot.getId();
  }
}
