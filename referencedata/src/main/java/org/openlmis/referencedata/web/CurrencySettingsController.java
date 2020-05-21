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

package org.openlmis.referencedata.web;

import org.openlmis.referencedata.CurrencyConfig;
import org.openlmis.referencedata.dto.CurrencySettingsDto;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CurrencySettingsController extends BaseController {

  /**
   * Get currency settings.
   *
   * @return {Version} Returns currency settings from properties.
   */
  @RequestMapping(value = "/currencySettings", method = RequestMethod.GET)
  @ResponseBody
  public CurrencySettingsDto getCurrencySettings() {

    return new CurrencySettingsDto(CurrencyConfig.CURRENCY_CODE, CurrencyConfig.CURRENCY_SYMBOL,
        CurrencyConfig.CURRENCY_SYMBOL_SIDE, CurrencyConfig.CURRENCY_DECIMAL_PLACES,
        CurrencyConfig.GROUPING_SEPARATOR, CurrencyConfig.GROUPING_SIZE,
        CurrencyConfig.DECIMAL_SEPARATOR);
  }
}
