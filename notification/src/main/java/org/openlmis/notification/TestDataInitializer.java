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

package org.openlmis.notification;

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
  private static final String USER_CONTACT_DETAILS = "user_contact_details";
  private static final String NOTIFICATIONS = "notifications";
  private static final String NOTIFICATION_MESSAGES = "notification_messages";
  private static final String DIGEST_SUBSCRIPTIONS = "digest_subscriptions";

  // database path
  private static final String DB_SCHEMA = "notification.";
  static final String USER_CONTACT_DETAILS_TABLE = DB_SCHEMA + USER_CONTACT_DETAILS;
  static final String NOTIFICATIONS_TABLE = DB_SCHEMA + NOTIFICATIONS;
  static final String NOTIFICATION_MESSAGES_TABLE = DB_SCHEMA + NOTIFICATION_MESSAGES;
  static final String DIGEST_SUBSCRIPTIONS_TABLE = DB_SCHEMA + DIGEST_SUBSCRIPTIONS;


  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + USER_CONTACT_DETAILS + FILE_EXTENSION)
  private Resource userContactDetailsResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + NOTIFICATIONS + FILE_EXTENSION)
  private Resource notificationsResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + NOTIFICATION_MESSAGES + FILE_EXTENSION)
  private Resource notificationMessagesResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + DIGEST_SUBSCRIPTIONS + FILE_EXTENSION)
  private Resource digestSubscriptionsResource;

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

    loader.insertToDbFromCsv(USER_CONTACT_DETAILS_TABLE, userContactDetailsResource);
    
    loader.insertToDbFromCsv(NOTIFICATIONS_TABLE, notificationsResource);
    loader.insertToDbFromCsv(NOTIFICATION_MESSAGES_TABLE, notificationMessagesResource);

    loader.insertToDbFromCsv(DIGEST_SUBSCRIPTIONS_TABLE, digestSubscriptionsResource);

    XLOGGER.exit();
  }

}
