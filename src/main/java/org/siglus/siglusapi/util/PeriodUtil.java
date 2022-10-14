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

package org.siglus.siglusapi.util;

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_PERIOD_MATCH;

import java.time.LocalDate;
import java.util.List;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;

public class PeriodUtil {

  public static ProcessingPeriod getPeriodDateIn(List<ProcessingPeriod> processingPeriods, LocalDate localDate) {
    return processingPeriods.stream().filter(period -> isDateInPeriod(period, localDate)).findFirst()
        .orElseThrow(() -> new BusinessDataException(new Message(ERROR_NO_PERIOD_MATCH)));
  }

  public static ProcessingPeriod getPeriodDateInDefaultNull(List<ProcessingPeriod> processingPeriods,
      LocalDate localDate) {
    return processingPeriods.stream().filter(period -> isDateInPeriod(period, localDate)).findFirst().orElse(null);
  }

  public static boolean isDateInPeriod(ProcessingPeriod period, LocalDate localDate) {
    return !isDateNotInPeriod(period, localDate);
  }

  private static boolean isDateNotInPeriod(ProcessingPeriod period, LocalDate localDate) {
    return localDate.isBefore(period.getStartDate()) || localDate.isAfter(period.getEndDate());
  }

}
