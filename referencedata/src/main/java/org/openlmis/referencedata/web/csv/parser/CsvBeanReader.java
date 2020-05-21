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

package org.openlmis.referencedata.web.csv.parser;

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import lombok.Getter;
import org.openlmis.referencedata.dto.BaseDto;
import org.openlmis.referencedata.validate.CsvHeaderValidator;
import org.openlmis.referencedata.web.csv.model.ModelClass;
import org.openlmis.referencedata.web.csv.processor.CsvCellProcessors;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.dozer.CsvDozerBeanReader;
import org.supercsv.prefs.CsvPreference;

/**
 * This class has responsibility to instantiate a dozerBeanReader from given inputStream,
 * and CsvPreferences. Also is responsible for validating headers.
 */
class CsvBeanReader<T extends BaseDto> {

  private ModelClass<T> modelClass;
  private CsvDozerBeanReader dozerBeanReader;
  private CsvHeaderValidator csvHeaderValidator;
  private CellProcessor[] processors;

  @Getter
  private String[] headers;

  CsvBeanReader(ModelClass<T> modelClass,
                InputStream inputStream,
                CsvHeaderValidator csvHeaderValidator) throws IOException {
    this.modelClass = modelClass;
    this.csvHeaderValidator = csvHeaderValidator;
    configureDozerBeanReader(inputStream);
    configureProcessors();
  }

  T readWithCellProcessors() throws IOException {
    return dozerBeanReader.read(modelClass.getClazz(), processors);
  }

  int getRowNumber() {
    return dozerBeanReader.getRowNumber();
  }

  void validateHeaders() {
    csvHeaderValidator.validateHeaders(asList(headers), modelClass, false);
  }

  private void configureDozerBeanReader(InputStream inputStream) throws IOException {
    CsvPreference csvPreference = new CsvPreference.Builder(CsvPreference.STANDARD_PREFERENCE)
        .surroundingSpacesNeedQuotes(true)
        .build();

    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    dozerBeanReader = new CsvDozerBeanReader(bufferedReader, csvPreference);
    headers = readHeaders();
    String[] mappings = modelClass.getFieldNameMappings(headers);
    dozerBeanReader.configureBeanMapping(modelClass.getClazz(), mappings);
  }

  private String[] readHeaders() throws IOException {
    String[] headers = dozerBeanReader.getHeader(true);
    return headers == null ? new String[0] : headers;
  }

  private void configureProcessors() {
    List<CellProcessor> cellProcessors =
        CsvCellProcessors.getParseProcessors(modelClass, asList(headers));
    processors = cellProcessors.toArray(new CellProcessor[cellProcessors.size()]);
  }
}
