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

package org.siglus.siglusapi.service.fc.mapper;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.siglus.siglusapi.constant.FcConstants.STATUS_ACTIVE;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openlmis.referencedata.dto.ProgramOrderableDto;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.domain.CustomProductsRegimens;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.OrderableDisplayCategoryDto;
import org.siglus.siglusapi.dto.fc.AreaDto;
import org.siglus.siglusapi.dto.fc.ProductInfoDto;
import org.siglus.siglusapi.service.fc.FcProductService;
import org.siglus.siglusapi.util.FcUtil;

public class FcProductMapper {

  private final Map<String, ProgramRealProgram> realProgramCodeToEntityMap;

  private final Map<String, UUID> programCodeToIdMap;

  private final Map<String, OrderableDisplayCategoryDto> categoryCodeToEntityMap;

  private final Map<String, String> categoryDisplayNameToCodeMap;

  public FcProductMapper(
      Map<String, ProgramRealProgram> realProgramCodeToEntityMap,
      Map<String, UUID> programCodeToIdMap,
      Map<String, OrderableDisplayCategoryDto> categoryCodeToEntityMap,
      Map<String, String> categoryDisplayNameToCodeMap) {
    this.realProgramCodeToEntityMap = realProgramCodeToEntityMap;
    this.programCodeToIdMap = programCodeToIdMap;
    this.categoryCodeToEntityMap = categoryCodeToEntityMap;
    this.categoryDisplayNameToCodeMap = categoryDisplayNameToCodeMap;
  }

  OrderableDisplayCategoryDto getOrderableDisplayCategoryDtoFromCustomProductsRegimens(
      ProductInfoDto product,
      Map<String, OrderableDisplayCategoryDto> categoryCodeToEntityMap,
      Map<String, CustomProductsRegimens> codeToCustomProductsRegimens) {
    CustomProductsRegimens customProductsRegimens = codeToCustomProductsRegimens.get(product.getFnm());
    if (customProductsRegimens != null) {
      String categoryType = customProductsRegimens.getCategoryType();
      return categoryCodeToEntityMap.get(categoryDisplayNameToCodeMap.get(categoryType));
    }
    return getOrderableDisplayCategoryDto(product, categoryCodeToEntityMap);
  }

  static OrderableDisplayCategoryDto getOrderableDisplayCategoryDto(
      ProductInfoDto product, Map<String, OrderableDisplayCategoryDto> categoryCodeToEntityMap) {
    OrderableDisplayCategoryDto categoryDto =
        categoryCodeToEntityMap.get(product.getCategoryCode());
    if (null == categoryDto) {
      categoryDto = categoryCodeToEntityMap.get("DEFAULT");
    }
    return categoryDto;
  }

  static String getProgramCode(Set<String> programCodes) {
    String programCode;
    if (programCodes.contains(ProgramConstants.RAPIDTEST_PROGRAM_CODE)) {
      programCode = ProgramConstants.RAPIDTEST_PROGRAM_CODE;
    } else if (programCodes.contains(ProgramConstants.TARV_PROGRAM_CODE)) {
      programCode = ProgramConstants.TARV_PROGRAM_CODE;
    } else if (programCodes.contains(ProgramConstants.MTB_PROGRAM_CODE)) {
      programCode = ProgramConstants.MTB_PROGRAM_CODE;
    } else {
      programCode = ProgramConstants.VIA_PROGRAM_CODE;
    }
    return programCode;
  }

  public Set<ProgramOrderableDto> getProgramOrderablesFrom(ProductInfoDto product,
      Map<String, CustomProductsRegimens> codeToCustomProductsRegimens) {
    if (product.getAreas() == null || product.getAreas().isEmpty()) {
      return emptySet();
    }
    Set<String> programCodes = getProgramCodes(product);
    if (isEmpty(programCodes)) {
      return emptySet();
    }
    ProgramOrderableDto programOrderableDto = getProgramOrderableDto(product, programCodes,
        codeToCustomProductsRegimens);
    return singleton(programOrderableDto);
  }

  private ProgramOrderableDto getProgramOrderableDto(
      ProductInfoDto product, Set<String> programCodes,
      Map<String, CustomProductsRegimens> codeToCustomProductsRegimens) {
    String programCode = getProgramCode(programCodes);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setProgramId(programCodeToIdMap.get(programCode));
    programOrderableDto.setActive(true);
    programOrderableDto.setFullSupply(true);
    OrderableDisplayCategoryDto categoryDto =
        getOrderableDisplayCategoryDtoFromCustomProductsRegimens(product,
            categoryCodeToEntityMap, codeToCustomProductsRegimens);
    programOrderableDto.setOrderableDisplayCategoryId(categoryDto.getId());
    programOrderableDto.setOrderableCategoryDisplayName(categoryDto.getDisplayName());
    programOrderableDto.setOrderableCategoryDisplayOrder(categoryDto.getDisplayOrder());
    programOrderableDto.setPricePerPack(FcProductService.getCurrentProductPrice(product.getPrice()));
    return programOrderableDto;
  }

  Set<String> getProgramCodes(ProductInfoDto product) {
    List<AreaDto> areaDtos = product.getAreas();
    areaDtos.sort(Comparator.comparing(AreaDto::getLastUpdatedAt));
    Map<String, AreaDto> timeMapAreaDto = areaDtos.stream()
        .filter(areaDto -> STATUS_ACTIVE.equals(areaDto.getStatus()))
        .collect(Collectors.toMap(AreaDto::getAreaCode, Function.identity(),
            (value1, value2) -> shouldNotUpdate(value1, value2) ? value1 : value2));
    return timeMapAreaDto.values().stream()
        .filter(areaDto -> Objects.nonNull(realProgramCodeToEntityMap.get(areaDto.getAreaCode())))
        .map(areaDto -> realProgramCodeToEntityMap.get(areaDto.getAreaCode()).getProgramCode())
        .collect(Collectors.toSet());
  }

  private boolean shouldNotUpdate(AreaDto value1, AreaDto value2) {
    return value1.getLastUpdatedAt().equals(value2.getLastUpdatedAt()) && FcUtil.isActive(
        value1.getStatus())
        && !FcUtil.isActive(value2.getStatus());
  }
}
