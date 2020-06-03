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

package org.openlmis.fulfillment.service;

import static java.util.Locale.ENGLISH;

import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.TransferProperties;
import org.openlmis.fulfillment.domain.TransferType;
import org.openlmis.fulfillment.repository.TransferPropertiesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderFtpSender implements OrderSender {
  private static final Logger LOGGER = LoggerFactory.getLogger(OrderFtpSender.class);

  private static final String CAMEL_FTP_PATTERN =
      "{0}://{1}@{2}:{3}/{4}?password={5}&passiveMode={6}";

  @Autowired
  private ProducerTemplate producerTemplate;

  @Autowired
  private OrderStorage orderStorage;

  @Autowired
  private TransferPropertiesRepository transferPropertiesRepository;

  @Override
  public boolean send(Order order) {
    Path path = orderStorage.getOrderAsPath(order);
    TransferProperties properties = transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(order.getSupplyingFacilityId(),
            TransferType.ORDER);

    return properties instanceof FtpTransferProperties
        && send(order, path, (FtpTransferProperties) properties);

  }

  private boolean send(Order order, Path path, FtpTransferProperties ftp) {
    try {
      String endpointUri = createEndpointUri(ftp);
      File file = path.toFile();
      producerTemplate.sendBodyAndHeader(endpointUri, file, Exchange.FILE_NAME, file.getName());
    } catch (Exception exp) {
      LOGGER.error(
          "Can't transfer CSV file {} related with order {} to the FTP server",
          path, order.getId(), exp
      );

      return false;
    }

    return true;
  }

  private String createEndpointUri(FtpTransferProperties setting) {
    return MessageFormat.format(CAMEL_FTP_PATTERN,
        setting.getProtocol().name().toLowerCase(ENGLISH),
        setting.getUsername(),
        setting.getServerHost(),
        setting.getServerPort(),
        setting.getRemoteDirectory(),
        setting.getPassword(),
        setting.getPassiveMode());
  }
}
