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

package org.openlmis.referencedata.web.csv.format;

import static java.util.Arrays.asList;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import lombok.Getter;
import org.openlmis.referencedata.dto.BaseDto;
import org.openlmis.referencedata.web.csv.model.ModelClass;
import org.openlmis.referencedata.web.csv.model.ModelField;
import org.openlmis.referencedata.web.csv.processor.CsvCellProcessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.dozer.CsvDozerBeanWriter;
import org.supercsv.prefs.CsvPreference;

/**
 * This class has responsibility to instantiate a csvDozerBeanWriter from given inputStream.
 */
class CsvBeanWriter<T extends BaseDto> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvBeanWriter.class);

  private ModelClass<T> modelClass;
  private CsvDozerBeanWriter csvDozerBeanWriter;
  private CellProcessor[] processors;

  @Getter
  private String[] headers;

  CsvBeanWriter(ModelClass<T> modelClass,
                OutputStream outputStream) throws IOException {
    Profiler profiler = new Profiler("CREATE_CSV_WRITER");
    profiler.setLogger(LOGGER);

    profiler.start("MODEL_CLASS");
    this.modelClass = modelClass;

    profiler.start("CONFIGURE_WRITER");
    configureDozerBeanWriter(outputStream);

    profiler.start("CONFIGURE_PROCESSORS");
    configureProcessors();

    profiler.stop().log();
  }

  void writeWithCellProcessors(List<? extends BaseDto> dtos) throws IOException {
    Profiler profiler = new Profiler("CSV_WRITE_CELLS");
    profiler.setLogger(LOGGER);

    profiler.start("WRITE_HEADERS");
    csvDozerBeanWriter.writeHeader(headers);

    profiler.start("WRITE_LINE_ITEMS");
    for (Object dto : dtos) {
      csvDozerBeanWriter.write(dto, processors);
    }

    profiler.start("CLOSE_STREAM");
    csvDozerBeanWriter.close();

    profiler.stop().log();
  }

  private void configureDozerBeanWriter(OutputStream outputStream) throws IOException {
    Profiler profiler = new Profiler("CONFIGURE_DOZER_WRITER");
    profiler.setLogger(LOGGER);

    profiler.start("CSV_PREFERENCE");
    CsvPreference csvPreference = new CsvPreference.Builder(CsvPreference.STANDARD_PREFERENCE)
        .surroundingSpacesNeedQuotes(true)
        .build();

    profiler.start("CREATE_STREAM");
    BufferedWriter bufferedReader = new BufferedWriter(
        new OutputStreamWriter(outputStream, "UTF-8"));

    profiler.start("NEW_READER");
    csvDozerBeanWriter = new CsvDozerBeanWriter(bufferedReader, csvPreference);

    profiler.start("READ_HEADERS");
    headers = readHeaders();

    profiler.start("GET_FIELD_MAPPINGS");
    String[] mappings = modelClass.getFieldNameMappings(headers);

    profiler.start("CONFIGURE_BEAN_MAPPING");
    csvDozerBeanWriter.configureBeanMapping(modelClass.getClazz(), mappings);

    profiler.stop().log();
  }

  private String[] readHeaders() {
    return modelClass
        .getImportFields()
        .stream()
        .map(ModelField::getName)
        .toArray(String[]::new);
  }

  private void configureProcessors() {
    List<CellProcessor> cellProcessors =
        CsvCellProcessors.getFormatProcessors(modelClass, asList(headers));
    processors = cellProcessors.toArray(new CellProcessor[cellProcessors.size()]);
  }
}
