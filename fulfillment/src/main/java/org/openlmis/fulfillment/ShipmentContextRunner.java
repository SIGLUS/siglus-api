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

package org.openlmis.fulfillment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.fulfillment.domain.TransferProperties;
import org.openlmis.fulfillment.domain.TransferType;
import org.openlmis.fulfillment.repository.TransferPropertiesRepository;
import org.openlmis.fulfillment.util.ShipmentChannelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(30)
@Component
public class ShipmentContextRunner implements CommandLineRunner {

  @Autowired
  private TransferPropertiesRepository transferPropertiesService;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private ShipmentChannelHelper channelHelper;

  private final Map<UUID, ConfigurableApplicationContext> contexts = new HashMap<>();

  /**
   * Creates Shipment File Polling Contexts.
   */
  public void run(String... args) {
    createAllChannels();
  }

  /**
   * Create/re-create the application context for the transfer property that was updated/created.
   *
   * @param transferProperty transfer property that was created or updated.
   */
  public void reCreateShipmentChannel(TransferProperties transferProperty) {
    if (contexts.containsKey(transferProperty.getId())) {
      contexts.get(transferProperty.getId()).close();
      contexts.remove(transferProperty.getId());
    }
    contexts.put(transferProperty.getId(),
        channelHelper.createChannel(transferProperty, applicationContext));
  }


  private void createAllChannels() {
    List<TransferProperties> propertiesList = transferPropertiesService
        .findByTransferType(TransferType.SHIPMENT);
    propertiesList
        .forEach(properties ->
            contexts.put(properties.getId(),
                channelHelper.createChannel(properties, applicationContext)));
  }

}
