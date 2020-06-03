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
import java.io.IOException;
import java.util.List;
import javax.transaction.Transactional;
import lombok.NoArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.openlmis.fulfillment.domain.FileTemplate;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.TemplateType;
import org.openlmis.fulfillment.service.FileTemplateService;
import org.openlmis.fulfillment.service.ShipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
public class ShipmentMessageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShipmentMessageHandler.class);

  @Autowired
  private FileTemplateService templateService;

  @Autowired
  private ShipmentCsvFileParser shipmentParser;

  @Autowired
  private ShipmentService shipmentService;

  @Autowired
  private ShipmentBuilder shipmentBuilder;

  @Autowired
  private ApplicationContext context;

  @Autowired
  private ShipmentMessageErrorHandler errorHandler;

  /**
   * A message handler endpoint that processes incoming shipment files.
   *
   * @param message a file message.
   */
  @Transactional
  public void process(Message<File> message) throws IOException {
    FileTemplate template = templateService.getFileTemplate(TemplateType.SHIPMENT);
    LOGGER.info("A shipment file received. {}", message.getHeaders().getId());
    File file = message.getPayload();
    try {
      // parse file
      List<CSVRecord> records = shipmentParser.parse(file, template);
      Shipment shipment = shipmentBuilder.build(template, records);
      shipmentService.save(shipment);
      archiveFile(message, "outboundShipmentFileArchiveChannel");
    } catch (RuntimeException exception) {
      List<Message<File>> messages = errorHandler.extractLogMessages(file, exception);
      messages.forEach(m -> archiveFile(m, "errorChannel"));
    }
  }

  private void archiveFile(Message<File> message, String archiveFtpChannel) {
    MessageChannel archiveChannel = (MessageChannel) context.getBean(archiveFtpChannel);
    Message<File> archiveMessage = MessageBuilder
        .withPayload(message.getPayload()).build();
    archiveChannel.send(archiveMessage);
  }


}
