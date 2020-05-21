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

import static org.apache.commons.lang3.BooleanUtils.isFalse;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import org.javers.core.metamodel.annotation.TypeName;

/**
 * Informative category a Product is in when assigned to a {@link Program}.
 */
@Entity
@Table(name = "orderable_display_categories", schema = "referencedata")
@TypeName("OrderableDisplayCategory")
public class OrderableDisplayCategory extends BaseEntity {

  @Embedded
  @Getter
  private Code code;

  @Embedded
  @Getter
  private OrderedDisplayValue orderedDisplayValue;

  private OrderableDisplayCategory() {
  }

  /**
   * Creates a new OrderableDisplayCategory with given id.
   */
  public OrderableDisplayCategory(UUID id) {
    this.id = id;
  }

  /**
   * Creates a new OrderableDisplayCategory.
   *
   * @param code         this OrderableDisplayCategory's unique implementation code
   *                     (never {@code null})
   * @param displayValue the display values of this OrderableDisplayCategory (never {@code null})
   */
  protected OrderableDisplayCategory(Code code, OrderedDisplayValue displayValue) {
    Objects.requireNonNull(code);
    Objects.requireNonNull(displayValue);
    this.code = code;
    this.orderedDisplayValue = displayValue;
  }

  /**
   * Update this from another. Copies display values from the other OrderableDisplayCategory
   * into this one.
   *
   * @param orderableDisplayCategory OrderableDisplayCategory to update from.
   */
  public void updateFrom(OrderableDisplayCategory orderableDisplayCategory) {
    this.orderedDisplayValue = orderableDisplayCategory.orderedDisplayValue;
  }

  /**
   * Creates a new OrderableDisplayCategory.
   *
   * @param orderableDisplayCategoryCode this OrderableDisplayCategory's unique implementation code
   *                                     (never {@code null})
   * @return a new OrderableDisplayCategory using default display value and order
   */
  public static OrderableDisplayCategory createNew(Code orderableDisplayCategoryCode) {
    return OrderableDisplayCategory.createNew(orderableDisplayCategoryCode,
        new OrderedDisplayValue(orderableDisplayCategoryCode.toString(), 1));
  }

  /**
   * Creates a new OrderableDisplayCategory.
   *
   * @param orderableDisplayCategoryCode this OrderableDisplayCategory's unique implementation code
   *                                     (never {@code null})
   * @param displayValue        the display values of this OrderableDisplayCategory
   *                            (never {@code null})
   * @return a new OrderableDisplayCategory.
   */
  public static OrderableDisplayCategory createNew(Code orderableDisplayCategoryCode,
                                                   OrderedDisplayValue displayValue) {
    return new OrderableDisplayCategory(orderableDisplayCategoryCode, displayValue);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof OrderableDisplayCategory)) {
      return false;
    }
    OrderableDisplayCategory that = (OrderableDisplayCategory) obj;
    return Objects.equals(code, that.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }

  /**
   * Creates new instance of OrderableDisplayCategory.
   */
  public static OrderableDisplayCategory newInstance(Importer importer) {
    OrderableDisplayCategory category = OrderableDisplayCategory.createNew(
        Code.code(importer.getCode()),
        new OrderedDisplayValue(importer.getDisplayName(), importer.getDisplayOrder()));
    category.setId(importer.getId());
    return category;
  }

  /**
   * Exports domain object to dto.
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    String codeString = code.toString();
    if (isFalse(codeString.isEmpty())) {
      exporter.setCode(codeString);
    }
    exporter.setDisplayName(orderedDisplayValue.getDisplayName());
    exporter.setDisplayOrder(orderedDisplayValue.getDisplayOrder());
  }

  public interface Exporter {
    void setId(UUID id);

    void setCode(String code);

    void setDisplayName(String name);

    void setDisplayOrder(Integer displayOrder);
  }

  public interface Importer {
    UUID getId();

    String getCode();

    String getDisplayName();

    Integer getDisplayOrder();
  }

}