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

package org.openlmis.requisition;

import java.io.IOException;
import org.openlmis.requisition.utils.Resource2Db;
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
  private static final String DB_MIGRATION_PATH = "classpath:db/migration/";

  private static final String FILE_EXTENSION = ".csv";

  // table names
  private static final String COLUMNS_MAPS = "columns_maps";
  private static final String JASPER_TEMPLATE_PARAMETER_DEPENDENCIES =
      "jasper_template_parameter_dependencies";
  private static final String JASPER_TEMPLATES = "jasper_templates";
  private static final String PREVIOUS_ADJUSTED_CONSUMPTIONS = "previous_adjusted_consumptions";
  private static final String REQUISITION_LINE_ITEMS = "requisition_line_items";
  private static final String REQUISITION_TEMPLATE_ASSIGNMENTS = "requisition_template_assignments";
  private static final String REQUISITION_TEMPLATES = "requisition_templates";
  private static final String REQUISITIONS = "requisitions";
  private static final String STATUS_CHANGES = "status_changes";
  private static final String STATUS_MESSAGES = "status_messages";
  private static final String STOCK_ADJUSTMENT_REASONS = "stock_adjustment_reasons";
  private static final String STOCK_ADJUSTMENTS = "stock_adjustments";
  private static final String TEMPLATE_PARAMETERS = "template_parameters";

  // database path
  private static final String DB_SCHEMA = "requisition.";
  static final String COLUMNS_MAPS_TABLE = DB_SCHEMA + COLUMNS_MAPS;
  static final String JASPER_TEMPLATE_PARAMETER_DEPENDENCIES_TABLE =
      DB_SCHEMA + JASPER_TEMPLATE_PARAMETER_DEPENDENCIES;
  static final String JASPER_TEMPLATES_TABLE = DB_SCHEMA + JASPER_TEMPLATES;
  static final String PREVIOUS_ADJUSTED_CONSUMPTIONS_TABLE =
      DB_SCHEMA + PREVIOUS_ADJUSTED_CONSUMPTIONS;
  static final String REQUISITION_LINE_ITEMS_TABLE = DB_SCHEMA + REQUISITION_LINE_ITEMS;
  static final String REQUISITION_TEMPLATE_ASSIGNMENTS_TABLE =
      DB_SCHEMA + REQUISITION_TEMPLATE_ASSIGNMENTS;
  static final String REQUISITION_TEMPLATES_TABLE = DB_SCHEMA + REQUISITION_TEMPLATES;
  static final String REQUISITIONS_TABLE = DB_SCHEMA + REQUISITIONS;
  static final String STATUS_CHANGES_TABLE = DB_SCHEMA + STATUS_CHANGES;
  static final String STATUS_MESSAGES_TABLE = DB_SCHEMA + STATUS_MESSAGES;
  static final String STOCK_ADJUSTMENT_REASONS_TABLE = DB_SCHEMA + STOCK_ADJUSTMENT_REASONS;
  static final String STOCK_ADJUSTMENTS_TABLE = DB_SCHEMA + STOCK_ADJUSTMENTS;
  static final String TEMPLATE_PARAMETERS_TABLE = DB_SCHEMA + TEMPLATE_PARAMETERS;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + COLUMNS_MAPS + FILE_EXTENSION)
  private Resource columnsMapsResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + JASPER_TEMPLATE_PARAMETER_DEPENDENCIES
      + FILE_EXTENSION)
  private Resource jasperTemplateParameterDependenciesResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + JASPER_TEMPLATES + FILE_EXTENSION)
  private Resource jasperTemplatesResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + PREVIOUS_ADJUSTED_CONSUMPTIONS + FILE_EXTENSION)
  private Resource previousAdjustedConsumptionsResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + REQUISITION_LINE_ITEMS + FILE_EXTENSION)
  private Resource requisitionLineItemsResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + REQUISITION_TEMPLATE_ASSIGNMENTS + FILE_EXTENSION)
  private Resource requisitionTemplateAssignmentsResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + REQUISITION_TEMPLATES + FILE_EXTENSION)
  private Resource requisitionTemplatesResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + REQUISITIONS + FILE_EXTENSION)
  private Resource requisitionsResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + STATUS_CHANGES + FILE_EXTENSION)
  private Resource statusChangesResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + STATUS_MESSAGES + FILE_EXTENSION)
  private Resource statusMessagesResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + STOCK_ADJUSTMENT_REASONS + FILE_EXTENSION)
  private Resource stockAdjustmentReasonsResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + STOCK_ADJUSTMENTS + FILE_EXTENSION)
  private Resource stockAdjustmentsResource;

  @Value(value = DEMO_DATA_PATH + DB_SCHEMA + TEMPLATE_PARAMETERS + FILE_EXTENSION)
  private Resource templateParametersResource;

  @Value(value = DB_MIGRATION_PATH
      + "20170822230153657__generate_requisition_permission_strings.sql")
  private Resource generateRequisitionPermissionStringsResource;

  private JdbcTemplate template;
  private Resource2Db loader;

  @Autowired
  public TestDataInitializer(JdbcTemplate template) {
    this(template, new Resource2Db(template));
  }

  TestDataInitializer(JdbcTemplate template, Resource2Db loader) {
    this.template = template;
    this.loader = loader;
  }

  /**
   * Initializes test data.
   * @param args command line arguments
   */
  public void run(String... args) throws IOException {
    XLOGGER.entry();

    loader.insertToDbFromCsv(REQUISITION_TEMPLATES_TABLE, requisitionTemplatesResource);
    loader.insertToDbFromCsv(COLUMNS_MAPS_TABLE, columnsMapsResource);
    loader.insertToDbFromCsv(
        REQUISITION_TEMPLATE_ASSIGNMENTS_TABLE, requisitionTemplateAssignmentsResource);

    loader.insertToDbFromCsv(REQUISITIONS_TABLE, requisitionsResource);
    loader.insertToDbFromCsv(STATUS_CHANGES_TABLE, statusChangesResource);
    loader.insertToDbFromCsv(STATUS_MESSAGES_TABLE, statusMessagesResource);
    loader.insertToDbFromCsv(STOCK_ADJUSTMENT_REASONS_TABLE, stockAdjustmentReasonsResource);
    loader.insertToDbFromCsv(REQUISITION_LINE_ITEMS_TABLE, requisitionLineItemsResource);
    loader.insertToDbFromCsv(
        PREVIOUS_ADJUSTED_CONSUMPTIONS_TABLE, previousAdjustedConsumptionsResource);
    loader.insertToDbFromCsv(STOCK_ADJUSTMENTS_TABLE, stockAdjustmentsResource);

    loader.insertToDbFromCsv(JASPER_TEMPLATES_TABLE, jasperTemplatesResource);
    loader.insertToDbFromCsv(TEMPLATE_PARAMETERS_TABLE, templateParametersResource);
    loader.insertToDbFromCsv(
        JASPER_TEMPLATE_PARAMETER_DEPENDENCIES_TABLE, jasperTemplateParameterDependenciesResource);

    template.update("DELETE FROM requisition.requisition_permission_strings;");
    loader.updateDbFromSqlSingle(generateRequisitionPermissionStringsResource);

    XLOGGER.exit();
  }

}
