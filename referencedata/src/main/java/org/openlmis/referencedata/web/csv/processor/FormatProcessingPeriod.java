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

package org.openlmis.referencedata.web.csv.processor;

import org.apache.commons.lang3.StringUtils;
import org.openlmis.referencedata.dto.ProcessingPeriodDto;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.util.CsvContext;

public class FormatProcessingPeriod extends CellProcessorAdaptor implements StringCellProcessor {

  public static String SEPARATOR;

  @SuppressWarnings("unchecked")
  @Override
  public Object execute(Object value, CsvContext context) {
    validateInputNotNull(value, context);

    String result;
    if (value instanceof ProcessingPeriodDto) {
      ProcessingPeriodDto period = (ProcessingPeriodDto) value;

      if (period.getName() == null || period.getProcessingSchedule() == null
          || period.getProcessingSchedule().getCode() == null) {
        throw getSuperCsvCellProcessorException(period, context);
      }

      result = StringUtils.joinWith(SEPARATOR, period.getProcessingSchedule().getCode(),
          period.getName());
    } else  {
      throw getSuperCsvCellProcessorException(value, context);
    }

    return next.execute(result, context);
  }

  private SuperCsvCellProcessorException getSuperCsvCellProcessorException(Object value,
                                                                           CsvContext context) {
    return new SuperCsvCellProcessorException(
        String.format("Cannot format '%s' name or processing schedule.", value.toString()),
        context, this);
  }
}
