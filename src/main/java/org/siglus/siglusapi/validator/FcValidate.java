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
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.common.util.Message;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.springframework.stereotype.Component;

@Slf4j
@Component("FcValidate")
public class FcValidate {

  public static final String DATA_ERROR = "dataError";

  public void validateEmptyRequisitionNumber(String requisitionNumber) {
    if (requisitionNumber == null || requisitionNumber.isEmpty()) {
      log.error("[FC] FcIntegrationError: requisitionNumber should be not empty");
      throw new ValidationMessageException(new Message(DATA_ERROR, ""));
    }
  }

  public void validateExistRequisitionNumber(RequisitionExtension extension) {
    if (extension == null || extension.getRequisitionNumber() == null) {
      log.error("[FC] FcIntegrationError: requisitionNumber not exist in our database");
      throw new ValidationMessageException(new Message(DATA_ERROR));
    }
  }

  public void validateEmptyFacilityCode(String code) {
    if (code == null || code.isEmpty()) {
      log.error("[FC] FcIntegrationError: requisitionNumber should be not null");
      throw new ValidationMessageException(new Message(DATA_ERROR));
    }
  }

  public void validateExistFacility(List<FacilityDto> facilityDtos) {
    if (CollectionUtils.isEmpty(facilityDtos)) {
      log.error("[FC] FcIntegrationError: facility should be not null");
      throw new ValidationMessageException(new Message(DATA_ERROR));
    }
  }

  public void validateExistUser(List<UserDto> userDtos) {
    if (CollectionUtils.isEmpty(userDtos)) {
      log.error("[FC] FcIntegrationError: user should be not null");
      throw new ValidationMessageException(new Message(DATA_ERROR));
    }
  }

  public void validateFcSource(ValidSourceDestinationDto source) {
    if (source == null) {
      log.error("[FC] FcIntegrationError: user should be not null");
      throw new ValidationMessageException(new Message(DATA_ERROR));
    }
  }

}
