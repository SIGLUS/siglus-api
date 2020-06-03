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

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class ShipmentMessageErrorHandler {

  @Autowired
  private ShipmentArchiveFileNameGenerator fileNameGenerator;

  /**
   * Given a shipment file and a runtime exception, this method returns two messages that should be
   * sent to the error channel.
   *
   * @param file shipment file.
   * @param exception error that was encountered
   */
  public List<Message<File>> extractLogMessages(File file, RuntimeException exception)
      throws IOException {
    String prefix = fileNameGenerator.generatePrefix();
    String fileName = prefix + file.getName();

    File originalPayload = new File(
        file.getParentFile().getParentFile().getPath().concat(File.separator).concat(fileName));
    Files.move(file.toPath(), originalPayload.toPath());

    Message<File> payload = MessageBuilder.withPayload(originalPayload).build();

    File errorLogFile = writeExceptionToFile(exception, file
        .getParentFile().getPath().concat(File.separator).concat(fileName));
    Message<File> errorLog = MessageBuilder.withPayload(errorLogFile).build();
    return asList(payload, errorLog);
  }

  private File writeExceptionToFile(RuntimeException exception, String fileName)
      throws FileNotFoundException {
    File errorLogFile = new File(fileName + ".log");
    PrintWriter writer = new PrintWriter(errorLogFile);
    writer.println(exception.getMessage());
    writer.println("\n");
    exception.printStackTrace(writer);
    writer.flush();
    writer.close();
    return errorLogFile;
  }

}
