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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.siglus.siglusapi.constant.ProgramConstants.MTB_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.RAPIDTEST_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.TARV_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.VIA_PROGRAM_CODE;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import org.openlmis.referencedata.dto.ProgramOrderableDto;
import org.siglus.siglusapi.dto.OrderableDisplayCategoryDto;
import org.siglus.siglusapi.dto.fc.AreaDto;
import org.siglus.siglusapi.dto.fc.ProductInfoDto;

public class FcProductMapperTest {

  private static final String CATEGORY_CODE = "category-code";

  @Test
  public void shouldReturnDefaultCategoryWhenProductCategoryCodeIsUnknown() {
    ProductInfoDto productInfo = ProductInfoDto.builder().categoryCode("unknown-code").build();

    OrderableDisplayCategoryDto displayCategoryDto = mock(OrderableDisplayCategoryDto.class);
    OrderableDisplayCategoryDto category =
        FcProductMapper.getOrderableDisplayCategoryDto(
            productInfo, Collections.singletonMap("DEFAULT", displayCategoryDto));

    assertThat(category).isEqualTo(displayCategoryDto);
  }

  @Test
  public void shouldReturnNonDefaultCategoryWhenProductCategoryCodeIsFoundOtherThanDefault() {
    ProductInfoDto productInfo = ProductInfoDto.builder().categoryCode(CATEGORY_CODE).build();

    OrderableDisplayCategoryDto displayCategoryDto = mock(OrderableDisplayCategoryDto.class);
    OrderableDisplayCategoryDto category =
        FcProductMapper.getOrderableDisplayCategoryDto(
            productInfo, Collections.singletonMap(CATEGORY_CODE, displayCategoryDto));

    assertThat(category).isEqualTo(displayCategoryDto);
  }

  @Test
  public void shouldReturnProgramCodeAsMappingWhenGetProgramCode() {
    ImmutableMap.of(
            "TR",
            RAPIDTEST_PROGRAM_CODE,
            "T",
            TARV_PROGRAM_CODE,
            "TB",
            MTB_PROGRAM_CODE,
            "any-others",
            VIA_PROGRAM_CODE)
        .forEach(
            (key, value) -> {
              String code = FcProductMapper.getProgramCode(singleton(key));
              assertThat(code).isEqualTo(value);
            });
  }

  @Test
  public void shouldReturnEmptyWhenGetProgramOrderablesGivenEmptyAreas() {
    ProductInfoDto productInfo = ProductInfoDto.builder().areas(emptyList()).build();

    Set<ProgramOrderableDto> programOrderableDtos =
        new FcProductMapper(null, null,
            null).getProgramOrderablesFrom(productInfo);

    assertThat(programOrderableDtos).isEmpty();
  }

  @Test
  public void shouldReturnEmptyWhenGetProgramOrderablesGivenEmptyProgramCodes() {
    ProductInfoDto productInfo = ProductInfoDto.builder().areas(singletonList(mock(AreaDto.class))).build();
    FcProductMapper fcProductMapper = mock(FcProductMapper.class);
    given(fcProductMapper.getProgramCodes(productInfo)).willReturn(emptySet());
    given(fcProductMapper.getProgramOrderablesFrom(productInfo)).willCallRealMethod();

    Set<ProgramOrderableDto> programOrderableDtos = fcProductMapper.getProgramOrderablesFrom(productInfo);

    assertThat(programOrderableDtos).isEmpty();
  }
}
