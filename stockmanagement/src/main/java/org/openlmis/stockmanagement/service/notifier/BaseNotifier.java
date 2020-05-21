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

package org.openlmis.stockmanagement.service.notifier;

import java.text.MessageFormat;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.UUID;
import org.openlmis.stockmanagement.service.referencedata.FacilityReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.OrderableReferenceDataService;
import org.openlmis.stockmanagement.service.referencedata.ProgramReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;

public class BaseNotifier {

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private OrderableReferenceDataService orderableReferenceDataService;

  @Value("${email.urlToViewBinCard}")
  private String urlToViewBinCard;

  protected DateTimeFormatter getDateFormatter() {
    Locale locale = LocaleContextHolder.getLocale();

    String datePattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        FormatStyle.MEDIUM, null, Chronology.ofLocale(locale), locale);
    return DateTimeFormatter.ofPattern(datePattern);
  }


  String getFacilityName(UUID facilityId) {
    return facilityReferenceDataService.findOne(facilityId).getName();
  }

  String getProgramName(UUID programId) {
    return programReferenceDataService.findOne(programId).getName();
  }

  String getOrderableName(UUID orderableId) {
    return orderableReferenceDataService.findOne(orderableId).getFullProductName();
  }

  String getUrlToViewBinCard(UUID stockCardId) {
    return MessageFormat.format(urlToViewBinCard, stockCardId);
  }
}
