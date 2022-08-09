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

package org.siglus.siglusapi.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

@Data
@EqualsAndHashCode(callSuper = true)
public class FacilitySearchResultDto extends FacilityDto {

  private Boolean isAndroidDevice;

  private Boolean enableLocationManagement;

  private Boolean isNewFacility;

  public static FacilitySearchResultDto from(FacilityDto facilityDto) {
    FacilitySearchResultDto searchResultDto = new FacilitySearchResultDto();
    BeanUtils.copyProperties(facilityDto, searchResultDto);
    return searchResultDto;
  }

  public static List<FacilitySearchResultDto> from(List<FacilityDto> facilityDto) {
    List<FacilitySearchResultDto> facilitySearchResultDtoList = new ArrayList<>();
    facilityDto.forEach(eachFacilityDto -> {
      FacilitySearchResultDto facilitySearchResultDto = new FacilitySearchResultDto();
      BeanUtils.copyProperties(eachFacilityDto, facilitySearchResultDto);
      facilitySearchResultDtoList.add(facilitySearchResultDto);
    });
    return facilitySearchResultDtoList;
  }
}
