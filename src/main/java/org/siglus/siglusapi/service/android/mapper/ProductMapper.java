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

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.siglusapi.dto.android.response.ProductResponse;

@Mapper(componentModel = "spring", uses = ProductChildMapper.class)
public interface ProductMapper {

  @Mapping(target = "programCode", source = ".", qualifiedByName = "getProgramCode")
  @Mapping(target = "active", expression = "java(parseKey(domain, \"active\", true))")
  @Mapping(target = "isBasic", expression = "java(parseKey(domain, \"isBasic\", false))")
  @Mapping(target = "isNos", expression = "java(parseKey(domain, \"isNos\", false))")
  @Mapping(target = "isHiv", expression = "java(parseKey(domain, \"isHiv\", false))")
  @Mapping(target = "category", source = ".", qualifiedByName = "getCategory")
  @Mapping(target = "lastUpdated", source = ".", qualifiedByName = "getLastUpdated")
  ProductResponse toResponse(OrderableDto domain, @Context Map<UUID, OrderableDto> allProducts);

  @Named("getProgramCode")
  default String getProgramCode(OrderableDto domain) {
    return (String) domain.getExtraData().get("programCode");
  }

  default Boolean parseKey(OrderableDto orderable, String key, boolean defaultValue) {
    Object value = orderable.getExtraData().get(key);
    return Optional.ofNullable(value)
        .map(Object::toString)
        .map(Boolean::parseBoolean)
        .orElse(defaultValue);
  }

  @Named("getCategory")
  default String getCategory(OrderableDto domain) {
    return domain.getPrograms().stream().findFirst()
        .orElseThrow(IllegalStateException::new).getOrderableCategoryDisplayName();
  }

  @Named("getLastUpdated")
  default Instant getLastUpdated(OrderableDto domain) {
    return domain.getMeta().getLastUpdated().toInstant();
  }

}
