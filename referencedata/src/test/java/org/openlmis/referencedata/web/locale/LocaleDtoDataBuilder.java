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

public class LocaleDtoDataBuilder {

  private String timeZoneId = "UTC";
  private String currencyCode = "USD";
  private String currencySymbol = "$";
  private String currencySymbolSide = "left";
  private Integer currencyDecimalPlaces = 2;
  private String groupingSeparator = ",";
  private Integer groupingSize = 3;
  private String decimalSeparator = ".";
  private String dateFormat = "dd/MM/yyyy";
  private String dateTimeFormat = "dd/MM/yyyy HH:mm:ss";

  /**
   * Creates a new instance of {@link LocaleDto} without any field.
   */
  public LocaleDto buildAsNew() {
    return new LocaleDto();
  }

  /**
   * Creates a new instance of {@link LocaleDto}.
   */
  public LocaleDto build() {
    LocaleDto dto = buildAsNew();
    dto.setTimeZoneId(timeZoneId);
    dto.setCurrencyCode(currencyCode);
    dto.setCurrencySymbol(currencySymbol);
    dto.setCurrencySymbolSide(currencySymbolSide);
    dto.setCurrencyDecimalPlaces(currencyDecimalPlaces);
    dto.setGroupingSeparator(groupingSeparator);
    dto.setGroupingSize(groupingSize);
    dto.setDecimalSeparator(decimalSeparator);
    dto.setDateFormat(dateFormat);
    dto.setDateTimeFormat(dateTimeFormat);
    return dto;
  }
}
