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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class ShipmentArchiveFileNameGenerator implements FileNameGenerator {

  private static final String FILE_PREFIX_DATE_PATTERN = "yyyy_MM_dd__HH_mm_ss__";
  private static final String REGEX_PATTERN = "^\\d{4}_\\d{2}_\\d{2}__\\d{2}_\\d{2}_\\d{2}.*";

  /**
   * Generate shipment file archive name prefix.
   *
   * @return String prefix
   */
  public String generatePrefix() {
    DateFormat format = new SimpleDateFormat(FILE_PREFIX_DATE_PATTERN);
    return format.format(new Date());
  }

  /**
   * Generates a file name by prefixing the file name with Date/Time. It only prepends date time,
   * only if the filename does not contain date time.
   *
   * @param message of type File
   * @return String
   */
  @Override
  public String generateFileName(Message<?> message) {
    String fileName = ((File) message.getPayload()).getName();
    if (fileName.matches(REGEX_PATTERN)) {
      return fileName;
    }
    return generatePrefix() + fileName;
  }
}
