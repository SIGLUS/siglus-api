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

package org.openlmis.referencedata.dto;

import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.referencedata.domain.OrderableDisplayCategory;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class OrderableDisplayCategoryDto extends BaseDto implements
    OrderableDisplayCategory.Exporter, OrderableDisplayCategory.Importer {

  private String code;

  private String displayName;

  private Integer displayOrder;

  /**
   * Creates new set of OrderableDisplayCategoryDto based on
   * {@link OrderableDisplayCategory} iterable.
   */
  public static Set<OrderableDisplayCategoryDto> newInstance(
      Iterable<OrderableDisplayCategory> iterable) {
    Set<OrderableDisplayCategoryDto> categoryDtos = new HashSet<>();
    iterable.forEach(i -> categoryDtos.add(newInstance(i)));
    return categoryDtos;
  }

  /**
   * Creates new instance of OrderableDisplayCategoryDto based on {@link OrderableDisplayCategory}.
   */
  public static OrderableDisplayCategoryDto newInstance(OrderableDisplayCategory category) {
    OrderableDisplayCategoryDto categoryDto = new OrderableDisplayCategoryDto();
    category.export(categoryDto);
    return categoryDto;
  }
}
