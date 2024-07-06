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

package org.siglus.siglusapi.util;

import java.util.Map;
import org.openlmis.referencedata.domain.Dispensable;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.stockmanagement.dto.referencedata.DispensableDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;

public class OrderableUtil {

  private OrderableUtil() {
  }

  public static OrderableDto convert(Orderable orderable) {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderable.getId());
    orderableDto.getMeta().setVersionNumber(orderable.getVersionNumber());
    Dispensable dispensable = orderable.getDispensable();
    if (dispensable != null) {
      Map<String, String> attributes = dispensable.getAttributes();
      orderableDto.setDispensable(new DispensableDto(attributes.get("dispensingUnit"), attributes.get("displayUnit")));
    }
    orderableDto.setProductCode(orderable.getProductCode().toString());
    orderableDto.setFullProductName(orderable.getFullProductName());
    return orderableDto;
  }
}
