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

import java.util.regex.Pattern;
import org.openlmis.referencedata.dto.ProcessingPeriodDto;
import org.openlmis.referencedata.dto.ProcessingScheduleDto;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.util.CsvContext;

/**
 * This is a custom cell processor used to parse schedule_code|period_name to processing period dto.
 * This is used in CsvCellProcessors.
 */
public class ParseProcessingPeriod extends CellProcessorAdaptor implements StringCellProcessor {

  public static String SEPARATOR;

  @Override
  public Object execute(Object value, CsvContext context) {
    validateInputNotNull(value, context);

    ProcessingPeriodDto result;
    if (value instanceof String) {
      String[] parts = ((String) value).split(Pattern.quote(SEPARATOR));

      if (parts.length != 2) {
        throw getSuperCsvCellProcessorException(value, context, null);
      }

      result = new ProcessingPeriodDto();
      result.setName(parts[1].trim());
      ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
      processingScheduleDto.setCode(parts[0].trim());
      result.setProcessingSchedule(processingScheduleDto);
    } else {
      throw getSuperCsvCellProcessorException(value, context, null);
    }

    return next.execute(result, context);
  }

  private SuperCsvCellProcessorException getSuperCsvCellProcessorException(Object value,
                                                                           CsvContext context,
                                                                           Exception cause) {
    return new SuperCsvCellProcessorException(String.format(
        "'%s' could not be parsed to Processing Period", value), context, this, cause);
  }
}
