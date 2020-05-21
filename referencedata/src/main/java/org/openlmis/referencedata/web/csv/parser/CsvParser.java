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

import static org.openlmis.referencedata.util.messagekeys.CsvUploadMessageKeys.ERROR_UPLOAD_RECORD_INVALID;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.NoArgsConstructor;
import org.openlmis.referencedata.domain.BaseEntity;
import org.openlmis.referencedata.dto.BaseDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.validate.CsvHeaderValidator;
import org.openlmis.referencedata.web.csv.model.ModelClass;
import org.openlmis.referencedata.web.csv.recordhandler.RecordProcessor;
import org.openlmis.referencedata.web.csv.recordhandler.RecordWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.supercsv.exception.SuperCsvException;
import org.supercsv.util.CsvContext;

/**
 * This class has logic to invoke corresponding respective record handler to parse data from input
 * stream into the corresponding model. To speed up the process for huge files the stream is divided
 * into smaller chunks. The chunk size is set by {@code csvParser.chunkSize} property. Each chunk is
 * executed asynchronously in the thread pool with size set by {@code csvParser.poolSize}.
 */
@Component
@NoArgsConstructor
public class CsvParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvParser.class);

  @Value("${csvParser.chunkSize}")
  private int chunkSize;

  /**
   * Parses data from input stream into the corresponding model.
   *
   * @return number of uploaded records
   */
  public <D extends BaseDto, E extends BaseEntity> int parse(InputStream inputStream,
                                                             ModelClass<D> modelClass,
                                                             CsvHeaderValidator headerValidator,
                                                             RecordProcessor<D, E> processor,
                                                             RecordWriter<E> writer)
      throws IOException {
    Profiler profiler = new Profiler("PARSE_CSV_FILE");
    profiler.setLogger(LOGGER);

    profiler.start("NEW_CSV_READER");
    CsvBeanReader<D> csvBeanReader = new CsvBeanReader<>(
        modelClass, inputStream, headerValidator
    );

    profiler.start("VALIDATE_HEADERS");
    csvBeanReader.validateHeaders();

    profiler.start("PROCESS_CSV");
    while (true) {
      List<D> imported = doRead(csvBeanReader);

      if (imported.isEmpty()) {
        break;
      }

      doWrite(processor, writer, imported);
    }

    profiler.stop().log();
    return csvBeanReader.getRowNumber() - 1;
  }

  private <D extends BaseDto> List<D> doRead(CsvBeanReader<D> csvBeanReader) throws IOException {
    try {
      List<D> list = Lists.newArrayList();

      for (int i = 0; i < chunkSize; ++i) {
        D imported = csvBeanReader.readWithCellProcessors();

        if (null == imported) {
          break;
        }

        list.add(imported);
      }

      return list;
    } catch (SuperCsvException err) {
      Message message = getCsvRowErrorMessage(err);
      throw new ValidationMessageException(err, message);
    }
  }

  private <D extends BaseDto, E extends BaseEntity> void doWrite(RecordProcessor<D, E> processor,
                                                                 RecordWriter<E> writer,
                                                                 List<D> imported) {
    Profiler profiler = new Profiler("WRITE_CSV_CHUNK");
    profiler.setLogger(LOGGER);

    profiler.start("PROCESS");
    List<E> entities = processor.process(imported);

    profiler.start("WRITE_TO_DB");
    writer.write(entities);

    profiler.stop().log();
  }

  private Message getCsvRowErrorMessage(SuperCsvException err) {
    CsvContext context = err.getCsvContext();
    int row = context.getRowNumber() - 1;
    return new Message(ERROR_UPLOAD_RECORD_INVALID, row, err.getMessage());
  }

}
