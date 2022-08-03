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

package org.siglus.siglusapi.service.android.mapper;

import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openlmis.referencedata.dto.OrderableChildDto;
import org.openlmis.referencedata.dto.OrderableDto;
import org.siglus.siglusapi.dto.android.response.ProductChildResponse;

@Mapper(componentModel = "spring")
public interface ProductChildMapper {

  @Mapping(target = "productCode", source = "orderable.id", qualifiedByName = "toProductCode")
  ProductChildResponse toResponse(OrderableChildDto child,
      @Context Map<UUID, OrderableDto> allProducts);

  @Named("toProductCode")
  default String toProgramCode(UUID orderableId, @Context Map<UUID, OrderableDto> allProducts) {
    OrderableDto orderable = allProducts.get(orderableId);
    if (orderable == null) {
      return null;
    }
    return orderable.getProductCode();
  }

}
