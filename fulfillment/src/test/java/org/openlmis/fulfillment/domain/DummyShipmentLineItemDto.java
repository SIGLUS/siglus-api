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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DummyShipmentLineItemDto
    implements ShipmentLineItem.Importer, ShipmentLineItem.Exporter {

  private UUID id;
  private OrderableDto orderable;
  private UUID lotId;
  private Long quantityShipped;
  private Map<String, String> extraData;

  @Override
  @JsonIgnore
  public void setOrderable(OrderableDto orderableDto) {
    if (orderableDto != null) {
      this.orderable = orderableDto;
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

}
