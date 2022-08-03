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

package org.siglus.siglusapi.validator;

import java.io.IOException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.exception.BusinessDataException;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class CsvValidatorTest {

  @InjectMocks
  CsvValidator csvValidator;

  private static final String csvCorrectInput =
      "Location Code,Area,Zone,Rack,Barcode,Bin,Level\n"
          + "AA25Aq,Armazems Principals,Ad,Aw,AA25%q,253,Aa\n";

  private static final String csvNullHeaderInput =
      "Location Code,,Zone,Rack,Barcode,Bin,Level\n"
          + "AA25Ac,Armazema Principals,Aq,Ar,AA25%t,257,Ap\n";

  private static final String csvInvalidHeaderInput =
      "Location Code,Areassssss,Zone,Rack,Barcode,Bin,Level\n"
          + "AA25Ab,Armazemf Principald,Aw,Aq,wAA25%,25,Ab\n";

  private static final String csvDuplicateLocationCodeInput =
      "Location Code,Area,Zone,Rack,Barcode,Bin,Level,dsada\n"
          + "AA25Af,Armazeme Principalew,Agg,Ae,AA25%rt,250,Ater\n"
          + "AA25Af,Armazemg Principalrwe,Arr,Ar,gAA25%,251,Aert\n";

  private static final String csvLocationCodeMissingInput =
      "Location Code,Area,Zone,Rack,Barcode,Bin,Level\n"
          + "AA25Afgd,Armazems Principalgf,Asfgd,Agdf,AA25%vbc,253, \n";

  @Test
  public void shouldNotThrowBusinessDataExceptionWhenCorrectCsvFileUpload() throws IOException {
    CSVParser parse = CSVParser.parse(csvCorrectInput, CSVFormat.EXCEL.withFirstRecordAsHeader());
    csvValidator.validateCsvHeaders(parse);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowBusinessDataExceptionWhenContainNullHeader() throws IOException {
    CSVParser parse = CSVParser.parse(csvNullHeaderInput, CSVFormat.EXCEL.withFirstRecordAsHeader());
    csvValidator.validateCsvHeaders(parse);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowBusinessDataExceptionWhenInvalidHeader() throws IOException {
    CSVParser parse = CSVParser.parse(csvInvalidHeaderInput, CSVFormat.EXCEL.withFirstRecordAsHeader());
    csvValidator.validateCsvHeaders(parse);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowBusinessDataExceptionWhenDuplicateLocationCode() throws IOException {
    CSVParser parse = CSVParser.parse(csvDuplicateLocationCodeInput, CSVFormat.EXCEL.withFirstRecordAsHeader());
    csvValidator.validateDuplicateLocationCode(parse);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowBusinessDataExceptionWhenLocationCodeMissing() throws IOException {
    CSVParser parse = CSVParser.parse(csvLocationCodeMissingInput, CSVFormat.EXCEL.withFirstRecordAsHeader());
    csvValidator.validateNullRow(parse.getRecords().get(0));
  }
}
