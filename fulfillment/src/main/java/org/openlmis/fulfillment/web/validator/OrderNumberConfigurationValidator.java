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

package org.openlmis.fulfillment.web.validator;

import org.openlmis.fulfillment.web.util.OrderNumberConfigurationDto;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class OrderNumberConfigurationValidator implements Validator {

  private static final String INVALID_PREFIX =
      "Prefix should be alphanumeric and at most 8 characters long";
  private static final String PREFIX_FIELD = "orderNumberPrefix";

  @Override
  public boolean supports(Class<?> clazz) {
    return OrderNumberConfigurationDto.class.equals(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    OrderNumberConfigurationDto orderNumberConfiguration = (OrderNumberConfigurationDto) target;
    if (!validateOrderNumberPrefix(orderNumberConfiguration.getOrderNumberPrefix())) {
      errors.rejectValue(PREFIX_FIELD, INVALID_PREFIX);
    }
  }

  private Boolean validateOrderNumberPrefix(String prefix) {
    return prefix.matches("^[a-zA-Z0-9]{1,8}$");
  }

}
