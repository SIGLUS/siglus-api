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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreRemove;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;

@Entity
@Table(name = "order_line_items", schema = "fulfillment")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderLineItem extends BaseEntity {

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "orderId", nullable = false)
  @Setter
  private Order order;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(name = "orderableId")),
      @AttributeOverride(name = "versionNumber", column = @Column(name = "orderableVersionNumber"))
  })
  @Getter
  // [SIGLUS change start]
  // [change reason]: support for skip products.
  @Setter
  // [SIGLUS change end]
  private VersionEntityReference orderable;

  @Column(nullable = false)
  @Getter
  @Setter
  private Long orderedQuantity;

  // [SIGLUS change start]
  // [change reason]: synchronized the relationship PERSISTED IN CURRENT SESSION
  @PreRemove
  private void removeFromOrder() {
    order.getOrderLineItems()
        .removeIf(orderLineItem -> orderLineItem.getId().equals(this.id));
  }
  // [SIGLUS change end]

  /**
   * Create new instance of OrderLineItem based on given {@link Importer}.
   * @param importer instance of {@link Importer}
   * @return new instance of OrderLineItem.
   */
  public static OrderLineItem newInstance(Importer importer) {
    VersionIdentityDto orderableDto = importer.getOrderableIdentity();

    VersionEntityReference orderable = Optional
        .ofNullable(orderableDto)
        .map(item -> new VersionEntityReference(orderableDto.getId(),
            orderableDto.getVersionNumber()))
        .orElse(null);

    OrderLineItem orderLineItem = new OrderLineItem(
        null, orderable, importer.getOrderedQuantity()
    );
    orderLineItem.setId(importer.getId());

    return orderLineItem;
  }

  /**
   * Export this object to the specified OrderLineItem exporter (DTO).
   *
   * @param exporter OrderLineItem exporter
   */
  public void export(Exporter exporter) {
    exporter.setId(getId());
    exporter.setOrderedQuantity(getOrderedQuantity());
  }

  public interface Exporter {
    void setId(UUID id);

    void setOrderable(OrderableDto orderable);

    void setOrderedQuantity(Long orderedQuantity);

    void setTotalDispensingUnits(Long totalDispensingUnits);
  }

  public interface Importer {
    UUID getId();

    VersionIdentityDto getOrderableIdentity();

    Long getOrderedQuantity();

    Long getTotalDispensingUnits();

    // [SIGLUS change start]
    // [change reason]: support for skip products.
    boolean isSkipped();
    // [SIGLUS change end]
  }
}
