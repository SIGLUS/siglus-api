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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenCategory;
import org.siglus.siglusapi.dto.android.response.RegimenCategoryResponse;

@Mapper(componentModel = "spring")
public interface RegimentCategoryMapper {

  @Mapping(target = ".", source = ".", qualifiedByName = "toRegimenCategoryResponse")
  RegimenCategoryResponse toResponse(RegimenCategory domain);

  @Named("toRegimenCategoryResponse")
  default RegimenCategoryResponse toRegimenCategoryResponse(RegimenCategory regimenCategory) {

    switch (regimenCategory.getName()) {
      case "Adulto":
        return RegimenCategoryResponse.builder()
            .name(regimenCategory.getName())
            .code("Adults")
            .displayOrder(regimenCategory.getDisplayOrder())
            .build();
      case "Criança":
        return RegimenCategoryResponse.builder()
            .name(regimenCategory.getName())
            .code("Paediatrics")
            .displayOrder(regimenCategory.getDisplayOrder())
            .build();
      default:
        return RegimenCategoryResponse.builder()
            .name(regimenCategory.getName())
            .code("Default")
            .displayOrder(regimenCategory.getDisplayOrder())
            .build();
    }
  }

}
