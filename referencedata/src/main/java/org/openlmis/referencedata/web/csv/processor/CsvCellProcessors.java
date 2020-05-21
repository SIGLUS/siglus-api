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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openlmis.referencedata.web.csv.model.ModelClass;
import org.openlmis.referencedata.web.csv.model.ModelField;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.Trim;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;

/**
 * This class has mappings from type to cell processors used for parsing value in a cell
 * to corresponding data type.
 */
public class CsvCellProcessors {

  public static final String FACILITY_TYPE = "Facility";
  public static final String COMMODITY_TYPE = "CommodityType";
  public static final String PROCESSING_PERIOD_TYPE = "ProcessingPeriod";
  public static final String POSITIVE_INT = "int";

  private static final Map<String, CellProcessor> typeParseMappings = new HashMap<>();
  private static final Map<String, CellProcessor> typeExportMappings = new HashMap<>();

  static {
    typeExportMappings.put(FACILITY_TYPE, new FormatFacility());
    typeExportMappings.put(COMMODITY_TYPE, new FormatCommodityType());
    typeExportMappings.put(PROCESSING_PERIOD_TYPE, new FormatProcessingPeriod());

    typeParseMappings.put(FACILITY_TYPE, new ParseFacility());
    typeParseMappings.put(COMMODITY_TYPE, new ParseCommodityType());
    typeParseMappings.put(PROCESSING_PERIOD_TYPE, new ParseProcessingPeriod());
    typeParseMappings.put(POSITIVE_INT, new ParsePositiveInteger());
  }

  /**
   * Get all parse processors for given headers.
   */
  public static List<CellProcessor> getParseProcessors(ModelClass modelClass,
                                                        List<String> headers) {
    return getProcessors(modelClass, headers, true);
  }

  /**
   * Get all format processors for given headers.
   */
  public static List<CellProcessor> getFormatProcessors(ModelClass modelClass,
                                                   List<String> headers) {
    return getProcessors(modelClass, headers, false);
  }

  private static List<CellProcessor> getProcessors(ModelClass modelClass,
                                                   List<String> headers,
                                                   boolean forParsing) {
    List<CellProcessor> processors = new ArrayList<>();
    for (String header : headers) {
      ModelField field = modelClass.findImportFieldWithName(header);
      CellProcessor processor = null;
      if (field != null) {
        processor = chainTypeProcessor(field, forParsing);
      }
      processors.add(processor);
    }
    return processors;
  }

  private static CellProcessor chainTypeProcessor(ModelField field, boolean forParsing) {
    CellProcessor mappedProcessor;
    if (forParsing && typeParseMappings.containsKey(field.getType())) {
      mappedProcessor = typeParseMappings.get(field.getType());
    } else if (!forParsing && typeExportMappings.containsKey(field.getType())) {
      mappedProcessor = typeExportMappings.get(field.getType());
    } else {
      mappedProcessor = new Trim();
    }

    return field.isMandatory() ? new NotNull(mappedProcessor) : new Optional(mappedProcessor);
  }
}
