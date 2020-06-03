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

import java.util.Optional;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.javers.core.metamodel.annotation.TypeName;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;

@Entity
@Table(name = "shipment_draft_line_items")
@TypeName("ShipmentDraftLineItem")
@AllArgsConstructor
@ToString
public class ShipmentDraftLineItem extends BaseEntity {

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(name = "orderableId")),
      @AttributeOverride(name = "versionNumber", column = @Column(name = "orderableVersionNumber"))
  })
  @Getter
  private VersionEntityReference orderable;

  @Type(type = UUID_TYPE)
  private UUID lotId;

  private Long quantityShipped;

  // Constructor needed by framework. Use all args constructor to create new instance.
  private ShipmentDraftLineItem() {}

  /**
   * Creates new instance based on data from {@link ShipmentLineItem.Importer}.
   *
   * @param importer instance of {@link ShipmentLineItem.Importer}
   * @return new instance of shipment draft line item.
   */
  protected static ShipmentDraftLineItem newInstance(ShipmentLineItem.Importer importer) {
    VersionIdentityDto orderableDto = importer.getOrderableIdentity();

    VersionEntityReference orderable = Optional
        .ofNullable(orderableDto)
        .map(item -> new VersionEntityReference(orderableDto.getId(),
            orderableDto.getVersionNumber()))
        .orElse(null);

    ShipmentDraftLineItem shipmentLineItem = new ShipmentDraftLineItem(
        orderable, importer.getLotId(), importer.getQuantityShipped());
    shipmentLineItem.setId(importer.getId());
    return shipmentLineItem;
  }


  /**
   * Allows update existing draft line item.
   *
   * @param newItem new item to update from.
   */
  public void updateFrom(ShipmentDraftLineItem newItem) {
    this.orderable = newItem.orderable;
    this.lotId = newItem.lotId;
    this.quantityShipped = newItem.quantityShipped;
  }

  /**
   * Returns a copy of line item.
   */
  public ShipmentDraftLineItem copy() {
    ShipmentDraftLineItem clone = new ShipmentDraftLineItem(orderable, lotId, quantityShipped);
    clone.setId(id);

    return clone;
  }

  /**
   * Exports data from the given shipment draft to the instance that implement
   * {@link ShipmentLineItem.Exporter} interface.
   */
  public void export(ShipmentLineItem.Exporter exporter, OrderableDto orderableDto) {
    exporter.setId(getId());
    exporter.setOrderable(orderableDto);
    exporter.setLotId(lotId);
    exporter.setQuantityShipped(quantityShipped);
  }
}
