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

package org.openlmis.referencedata.testbuilder;

import java.util.UUID;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.OrderableDisplayCategory;

public class OrderableDisplayCategoryDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private Code code;

  /**
   * Builds instance of {@link OrderableDisplayCategoryDataBuilder} with sample data.
   */
  public OrderableDisplayCategoryDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = Code.code("Code" + instanceNumber);
  }

  /**
   * Builds instance of {@link OrderableDisplayCategory}.
   */
  public OrderableDisplayCategory build() {
    OrderableDisplayCategory orderableDisplayCategory = buildAsNew();
    orderableDisplayCategory.setId(id);
    return orderableDisplayCategory;
  }

  /**
   * Builds instance of {@link OrderableDisplayCategory} without id.
   */
  public OrderableDisplayCategory buildAsNew() {
    return OrderableDisplayCategory.createNew(code);
  }

  public OrderableDisplayCategoryDataBuilder withCode(Code code) {
    this.code = code;
    return this;
  }

}
