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

package org.siglus.common.domain.referencedata;

import java.util.Objects;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import org.siglus.common.domain.BaseEntity;

/**
 * Informative category a Product is in when assigned to a {@link Program}.
 */
@Entity
@Table(name = "orderable_display_categories", schema = "referencedata")
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

}