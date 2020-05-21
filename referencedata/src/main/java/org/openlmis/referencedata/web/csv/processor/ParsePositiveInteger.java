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

import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.ift.StringCellProcessor;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.util.CsvContext;

/**
 * This is a custom cell processor used to parse amount value.
 * This is used in {@link CsvCellProcessors}.
 */
public class ParsePositiveInteger extends CellProcessorAdaptor implements StringCellProcessor {

  @Override
  public Object execute(Object value, CsvContext context) {
    validateInputNotNull(value, context);

    Integer result;
    if (value instanceof String) {
      String textValue = String.valueOf(value);

      try {
        result = Integer.valueOf(textValue);
      } catch (NumberFormatException ex) {
        throw getSuperCsvCellProcessorException(textValue, context, ex);
      }

      if (result < 0) {
        throw new SuperCsvCellProcessorException(
            String.format("'%s' is less than 0", value), context, this, null);
      }
    } else {
      throw getSuperCsvCellProcessorException(value, context, null);
    }

    return next.execute(result, context);
  }

  private SuperCsvCellProcessorException getSuperCsvCellProcessorException(Object value,
                                                                           CsvContext context,
                                                                           Exception cause) {
    return new SuperCsvCellProcessorException(
        String.format("'%s' could not be parsed to integer amount", value), context, this, cause);
  }
}
