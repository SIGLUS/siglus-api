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

package org.openlmis.fulfillment.service.shipment;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.NoArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openlmis.fulfillment.domain.FileColumn;
import org.openlmis.fulfillment.domain.FileTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
public class ShipmentCsvFileParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentCsvFileParser.class);

  /**
   * Parse CSV shipment files and return List of Object Array.
   *
   * @param file csv file
   * @throws IOException Exception.
   */
  public List<CSVRecord> parse(File file, FileTemplate template)
      throws IOException {
    LOGGER.info("Parse shipment file: {}", file.getName());

    try (Reader targetReader = new FileReader(file)) {
      CSVParser parser = getCsvFormat(template).parse(targetReader);
      // read data rows
      List<CSVRecord> lines = new ArrayList<>();
      for (CSVRecord row : parser.getRecords()) {
        if (!row.isConsistent()) {
          throw new IllegalArgumentException(
              String.format("Shipment record inconsistent: %s", row));
        }
        lines.add(row);
      }
      LOGGER.info("Finished parsing shipment file: {}", file.getName());
      return lines;
    }
  }

  private CSVFormat getCsvFormat(FileTemplate template) {
    String[] columns = template
        .getFileColumns()
        .stream()
        .sorted(Comparator.comparingLong(FileColumn::getPosition))
        .map(column -> String.format("%s_%s", column.getNested(), column.getKeyPath()))
        .toArray(String[]::new);

    return CSVFormat.DEFAULT.withTrim(true)
        .withHeader(columns)
        .withNullString("").withSkipHeaderRecord(template.getHeaderInFile());
  }
}
