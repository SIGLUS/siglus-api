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

package org.openlmis.referencedata.domain;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.TypeName;

@Entity
@Table(name = "orderable_children", schema = "referencedata",
    uniqueConstraints = @UniqueConstraint(
        name = "unq_orderable_parent_id",
        columnNames = {"orderableid", "orderableVersionNumber", "parentid", "parentVersionNumber"})
)
@NoArgsConstructor
@AllArgsConstructor
@TypeName("OrderableChild")
@EqualsAndHashCode(callSuper = false, of = {"parent", "orderable"})
public class OrderableChild extends BaseEntity {

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "parentid", referencedColumnName = "id", nullable = false),
      @JoinColumn(name = "parentVersionNumber", referencedColumnName = "versionNumber",
          nullable = false)
  })
  @Getter
  @Setter
  private Orderable parent;

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "orderableId", referencedColumnName = "id", nullable = false),
      @JoinColumn(name = "orderableVersionNumber", referencedColumnName = "versionNumber",
          nullable = false),
  })
  @Getter
  @Setter
  private Orderable orderable;

  @Getter
  @Setter
  private Long quantity;

  /**
   * Create a new instance of OrderableChild to represent what is in a kit.
   *
   * @param parent parent orderable.
   * @param child kit constituent.
   * @param quantity quantity of constituent contained in kit.
   * @return OrderableChild.
   */
  public static OrderableChild newInstance(Orderable parent, Orderable child, Long quantity) {
    OrderableChild orderableChild = new OrderableChild();
    orderableChild.setOrderable(child);
    orderableChild.setParent(parent);
    orderableChild.setQuantity(quantity);
    return orderableChild;
  }

  /**
   * Exports current state of Orderable Child object.
   *
   * @param exporter instance of {@link Exporter}
   */
  public void export(Exporter exporter) {
    exporter.setOrderable(orderable);
    exporter.setQuantity(quantity);
  }

  public interface Exporter {

    void setOrderable(Orderable orderable);

    void setQuantity(Long quantity);
  }

  public interface Importer {

    Long getQuantity();
  }

}
