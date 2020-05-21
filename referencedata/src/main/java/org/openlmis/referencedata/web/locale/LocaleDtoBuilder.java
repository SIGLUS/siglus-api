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

package org.openlmis.referencedata.web.locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocaleDtoBuilder {

  @Value("${time.zoneId}")
  private String timeZoneId;

  @Value("${currencyCode}")
  private String currencyCode;

  @Value("${currencySymbol}")
  private String currencySymbol;

  @Value("${currencySymbolSide}")
  private String currencySymbolSide;

  @Value("${currencyDecimalPlaces}")
  private Integer currencyDecimalPlaces;

  @Value("${groupingSeparator}")
  private String groupingSeparator;

  @Value("${groupingSize}")
  private Integer groupingSize;

  @Value("${decimalSeparator}")
  private String decimalSeparator;

  @Value("${dateFormat}")
  private String dateFormat;

  @Value("${dateTimeFormat}")
  private String dateTimeFormat;

  /**
   * Create a new instance of {@link LocaleDto}.
   *
   * @return new instance of {@link LocaleDto}. {@code null}
   *         if timeZoneId is {@code null}.
   */
  public LocaleDto build() {
    return new LocaleDto(timeZoneId, currencyCode, currencySymbol, currencySymbolSide,
        currencyDecimalPlaces, groupingSeparator, groupingSize, decimalSeparator, dateFormat,
        dateTimeFormat);
  }
}
