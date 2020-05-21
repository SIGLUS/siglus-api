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

import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.openlmis.referencedata.domain.CommodityType;

public class CommodityTypeDataBuilder {
  private UUID id = UUID.randomUUID();
  private String name = RandomStringUtils.randomAlphanumeric(5);
  private String classificationSystem = RandomStringUtils.randomAlphanumeric(5);
  private String classificationId = RandomStringUtils.randomAlphanumeric(5);
  private CommodityType parent = null;
  private List<CommodityType> children = Lists.newArrayList();

  /**
   * Creates a new instance of {@link CommodityType} without id field.
   */
  public CommodityType buildAsNew() {
    return new CommodityType(name, classificationSystem, classificationId, parent, children);
  }

  /**
   * Creates a new instance of {@link CommodityType}.
   */
  public CommodityType build() {
    CommodityType commodityType = buildAsNew();
    commodityType.setId(id);

    return commodityType;
  }

  public CommodityTypeDataBuilder withClassificationSystem(String classificationSystem) {
    this.classificationSystem = classificationSystem;
    return this;
  }

  public CommodityTypeDataBuilder withClassificationId(String classificationId) {
    this.classificationId = classificationId;
    return this;
  }
}
