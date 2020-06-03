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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.javers.core.metamodel.annotation.TypeName;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;

@Entity
@Table(name = "shipment_line_items")
@TypeName("ShipmentLineItem")
@AllArgsConstructor
@ToString
public class ShipmentLineItem extends BaseEntity {

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(name = "orderableId")),
      @AttributeOverride(name = "versionNumber", column = @Column(name = "orderableVersionNumber"))
  })
  @Getter
  private VersionEntityReference orderable;

  @Type(type = UUID_TYPE)
  @Getter
  private UUID lotId;

  @Column(nullable = false)
  @Getter(AccessLevel.PACKAGE)
  private Long quantityShipped;

  @Column(name = "extradata", columnDefinition = "jsonb")
  @Convert(converter = ExtraDataConverter.class)
  @Getter
  private Map<String, String> extraData;

  // Constructor needed by framework. Use all args constructor to create new instance.
  private ShipmentLineItem() {
  }

  public ShipmentLineItem(VersionEntityReference orderable, Long quantityShipped) {
    this(orderable, null, quantityShipped, null);
  }

  public ShipmentLineItem(VersionEntityReference orderable, Long quantityShipped,
      Map<String, String> extraData) {
    this(orderable, null, quantityShipped, extraData);
  }

  public UUID getOrderableId() {
    return this.orderable.getId();
  }

  /**
   * Creates new instance based on data from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance of shipment line item.
   */
  protected static ShipmentLineItem newInstance(Importer importer) {
    VersionIdentityDto orderableDto = importer.getOrderableIdentity();

    VersionEntityReference orderable = Optional
        .ofNullable(orderableDto)
        .map(item -> new VersionEntityReference(orderableDto.getId(),
            orderableDto.getVersionNumber()))
        .orElse(null);

    ShipmentLineItem shipmentLineItem = new ShipmentLineItem(
        orderable, importer.getLotId(), importer.getQuantityShipped(),
        importer.getExtraData());
    shipmentLineItem.setId(importer.getId());
    return shipmentLineItem;
  }

  /**
   * Verifies if the given line item has something to be shipped.
   */
  public boolean isShipped() {
    return null != quantityShipped && quantityShipped > 0;
  }

  /**
   * Exports data from the given shipment to the instance that implement {@link Exporter}
   * interface.
   */
  public void export(Exporter exporter, OrderableDto orderableDto) {
    exporter.setId(getId());
    exporter.setOrderable(orderableDto);
    exporter.setLotId(lotId);
    exporter.setQuantityShipped(quantityShipped);
    exporter.setExtraData(extraData);
  }

  /**
   * Returns a copy of line item.
   */
  public ShipmentLineItem copy() {
    ShipmentLineItem clone = new ShipmentLineItem(orderable, lotId, quantityShipped, extraData);
    clone.setId(id);

    return clone;
  }

  public interface Exporter {

    void setId(UUID id);

    void setOrderable(OrderableDto orderable);

    void setLotId(UUID lotId);

    void setQuantityShipped(Long quantityShipped);

    void setExtraData(Map<String, String> extraData);
  }

  public interface Importer {

    UUID getId();

    VersionIdentityDto getOrderableIdentity();

    UUID getLotId();

    Long getQuantityShipped();

    Map<String, String> getExtraData();
  }
}
