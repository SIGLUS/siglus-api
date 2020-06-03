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

import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_IO;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.openlmis.fulfillment.domain.FileTemplate;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.TransferProperties;
import org.openlmis.fulfillment.domain.TransferType;
import org.openlmis.fulfillment.repository.TransferPropertiesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderFileStorage implements OrderStorage {
  private static final Logger LOGGER = LoggerFactory.getLogger(OrderFileStorage.class);

  @Autowired
  private OrderCsvHelper csvHelper;

  @Autowired
  private FileTemplateService fileTemplateService;

  @Autowired
  private TransferPropertiesRepository transferPropertiesRepository;

  @Override
  public void store(Order order) {
    TransferProperties properties = transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(order.getSupplyingFacilityId(),
            TransferType.ORDER);

    if (null == properties) {
      LOGGER.warn(
          "Can't store the order {} because there is no transfer properties",
          order.getId()
      );
      return;
    }

    // retrieve order file template
    FileTemplate template = fileTemplateService.getOrderFileTemplate();
    String fileName = template.getFilePrefix() + order.getOrderCode() + ".csv";
    Path path;

    try {
      String dir = properties.getPath();
      Files.createDirectories(Paths.get(dir));
      path = Paths.get(dir, fileName);
    } catch (IOException exp) {
      throw new OrderStorageException(exp, ERROR_IO, exp.getMessage());
    }

    try (Writer writer = Files.newBufferedWriter(path)) {
      // 1. generate CSV file using order file template
      // 2. save generated CSV file in local directory
      csvHelper.writeCsvFile(order, template, writer);
    } catch (IOException exp) {
      throw new OrderStorageException(exp, ERROR_IO, exp.getMessage());
    }
  }

  @Override
  public void delete(Order order) {
    try {
      Files.deleteIfExists(getOrderAsPath(order));
    } catch (IOException exp) {
      throw new OrderStorageException(exp, ERROR_IO, exp.getMessage());
    }
  }

  @Override
  public Path getOrderAsPath(Order order) {
    FileTemplate template = fileTemplateService.getOrderFileTemplate();
    TransferProperties properties = transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(order.getSupplyingFacilityId(),
            TransferType.ORDER);

    String fileName = template.getFilePrefix() + order.getOrderCode() + ".csv";

    return Paths.get(properties.getPath(), fileName);
  }

}
