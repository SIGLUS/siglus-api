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

package org.siglus.siglusapi.validator;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.fc.ProductDto;
import org.siglus.siglusapi.service.fc.FcDataException;
import org.springframework.stereotype.Component;

@Slf4j
@Component("FcValidate")
public class FcValidate {

  public void validateEmptyFacilityCode(String code) {
    if (code == null || code.isEmpty()) {
      throw new FcDataException(new Message("facility code should be not null"));
    }
  }

  public void validateExistFacility(List<FacilityDto> facilityDtos) {
    if (CollectionUtils.isEmpty(facilityDtos)) {
      throw new FcDataException(new Message("facility should be not null"));
    }
  }

  public void validateExistUser(List<UserDto> userDtos) {
    if (CollectionUtils.isEmpty(userDtos)) {
      throw new FcDataException(new Message("user should be not null"));
    }
  }

  public void validateFcSource(ValidSourceDestinationDto source) {
    if (source == null) {
      throw new FcDataException(new Message("source should be not null"));
    }
  }

  public void validateFcProduct(List<ProductDto> products) {
    if (CollectionUtils.isEmpty(products)) {
      throw new FcDataException(new Message("all products not exist in master data"));
    }
  }

  public void validateFacility(Facility facility) {
    if (facility == null) {
      throw new FcDataException(new Message("facility not exist in master data"));
    }
  }

}
