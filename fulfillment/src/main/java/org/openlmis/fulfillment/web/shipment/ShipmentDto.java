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

import static org.openlmis.fulfillment.domain.Shipment.ROWS_WITH_UNRESOLVED_ORDERABLE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.fulfillment.domain.CreationDetails;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.UserObjectReferenceDto;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"serviceUrl"})
@ToString
public final class ShipmentDto implements CreationDetails.Exporter,
    Shipment.Exporter, Shipment.Importer {

  @Setter
  private String serviceUrl;

  @Getter
  @Setter
  private UUID id;

  @Getter
  private OrderObjectReferenceDto order;

  @Getter
  @Setter
  private UserObjectReferenceDto shippedBy;

  @Getter
  @Setter
  private ZonedDateTime shippedDate;

  @Getter
  @Setter
  private String notes;

  @Setter
  private List<ShipmentLineItemDto> lineItems;

  @Setter
  private Map<String, String> extraData;

  @Override
  @JsonIgnore
  public void setUserId(UUID updaterId) {
    if (updaterId != null) {
      shippedBy = UserObjectReferenceDto.create(updaterId, serviceUrl);
    }
  }

  @Override
  @JsonIgnore
  public void setDate(ZonedDateTime updatedDate) {
    shippedDate = updatedDate;
  }

  @JsonProperty
  public void setOrder(OrderObjectReferenceDto order) {
    this.order = order;
  }

  @Override
  @JsonIgnore
  public void setOrder(Order order) {
    if (order != null) {
      this.order = new OrderObjectReferenceDto(order.getId(), serviceUrl);
    }
  }

  @Override
  @JsonIgnore
  public void setShipDetails(CreationDetails creationDetails) {
    creationDetails.export(this);
  }

  @Override
  @JsonIgnore
  public CreationDetails getShipDetails() {
    return new CreationDetails(shippedBy.getId(), shippedDate);
  }

  @Override
  public Map<String, String> getExtraData() {
    return (this.extraData == null) ? null : this.extraData
        .entrySet().stream()
        .filter(m -> !(ROWS_WITH_UNRESOLVED_ORDERABLE.equals(m.getKey())))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  @JsonRawValue
  @JsonProperty(access = Access.READ_ONLY)
  public String getRowsWithUnresolvedOrderable() {
    return (this.extraData == null) ? null : this.extraData.get(ROWS_WITH_UNRESOLVED_ORDERABLE);
  }

  @Override
  public List<ShipmentLineItem.Importer> getLineItems() {
    return lineItems != null ? new ArrayList<>(lineItems) : null;
  }

  public List<ShipmentLineItemDto> lineItems() {
    return lineItems;
  }

}
