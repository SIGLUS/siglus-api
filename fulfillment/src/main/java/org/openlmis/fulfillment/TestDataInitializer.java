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

import java.io.IOException;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("demo-data")
@Order(5)
public class TestDataInitializer implements CommandLineRunner {
  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(TestDataInitializer.class);

  private static final String DEMO_DATA_PATH = "classpath:db/demo-data/";
  private static final String FILE_EXTENSION = ".csv";

  // table names
  private static final String ORDERS = "orders";
  private static final String ORDER_LINE_ITEMS = "order_line_items";
  private static final String SHIPMENTS = "shipments";
  private static final String SHIPMENT_LINE_ITEMS = "shipment_line_items";
  private static final String PROOFS_OF_DELIVERY = "proofs_of_delivery";
  private static final String PROOF_OF_DELIVERY_LINE_ITEMS = "proof_of_delivery_line_items";
  private static final String STATUS_CHANGES = "status_changes";
  private static final String TRANSFER_PROPERTIES = "transfer_properties";

  // database path
  private static final String DB_SCHEMA = "fulfillment.";
  static final String ORDERS_TABLE = DB_SCHEMA + ORDERS;
  static final String ORDER_LINE_ITEMS_TABLE = DB_SCHEMA + ORDER_LINE_ITEMS;
  static final String SHIPMENTS_TABLE = DB_SCHEMA + SHIPMENTS;
  static final String SHIPMENT_LINE_ITEMS_TABLE = DB_SCHEMA + SHIPMENT_LINE_ITEMS;
  static final String PROOFS_OF_DELIVERY_TABLE = DB_SCHEMA + PROOFS_OF_DELIVERY;
  static final String PROOF_OF_DELIVERY_LINE_ITEMS_TABLE = DB_SCHEMA + PROOF_OF_DELIVERY_LINE_ITEMS;
  static final String STATUS_CHANGES_TABLE = DB_SCHEMA + STATUS_CHANGES;
  static final String TRANSFER_PROPERTIES_TABLE = DB_SCHEMA + TRANSFER_PROPERTIES;

  @Value(value = DEMO_DATA_PATH + ORDERS_TABLE + FILE_EXTENSION)
  private Resource ordersResource;

  @Value(value = DEMO_DATA_PATH + ORDER_LINE_ITEMS_TABLE + FILE_EXTENSION)
  private Resource orderLineItemsResource;

  @Value(value = DEMO_DATA_PATH + SHIPMENTS_TABLE + FILE_EXTENSION)
  private Resource shipmentsResource;

  @Value(value = DEMO_DATA_PATH + SHIPMENT_LINE_ITEMS_TABLE + FILE_EXTENSION)
  private Resource shipmentLineItemsResource;

  @Value(value = DEMO_DATA_PATH + PROOFS_OF_DELIVERY_TABLE + FILE_EXTENSION)
  private Resource proofsOfDeliveryResource;

  @Value(value = DEMO_DATA_PATH + PROOF_OF_DELIVERY_LINE_ITEMS_TABLE + FILE_EXTENSION)
  private Resource proofOfDeliveryLineItemsResource;

  @Value(value = DEMO_DATA_PATH + STATUS_CHANGES_TABLE + FILE_EXTENSION)
  private Resource statusChangesResource;

  @Value(value = DEMO_DATA_PATH + TRANSFER_PROPERTIES_TABLE + FILE_EXTENSION)
  private Resource transferPropertiesResource;

  private Resource2Db loader;

  @Autowired
  public TestDataInitializer(JdbcTemplate template) {
    this(new Resource2Db(template));
  }

  TestDataInitializer(Resource2Db loader) {
    this.loader = loader;
  }

  /**
   * Initializes test data.
   * @param args command line arguments
   */
  public void run(String... args) throws IOException {
    XLOGGER.entry();

    loader.insertToDbFromCsv(ORDERS_TABLE, ordersResource);
    loader.insertToDbFromCsv(ORDER_LINE_ITEMS_TABLE, orderLineItemsResource);

    loader.insertToDbFromCsv(SHIPMENTS_TABLE, shipmentsResource);
    loader.insertToDbFromCsv(SHIPMENT_LINE_ITEMS_TABLE, shipmentLineItemsResource);

    loader.insertToDbFromCsv(PROOFS_OF_DELIVERY_TABLE, proofsOfDeliveryResource);
    loader.insertToDbFromCsv(PROOF_OF_DELIVERY_LINE_ITEMS_TABLE, proofOfDeliveryLineItemsResource);

    loader.insertToDbFromCsv(STATUS_CHANGES_TABLE, statusChangesResource);

    loader.insertToDbFromCsv(TRANSFER_PROPERTIES_TABLE, transferPropertiesResource);

    XLOGGER.exit();
  }

}
