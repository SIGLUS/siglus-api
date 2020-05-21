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

package org.openlmis.referencedata;

import java.io.IOException;
import org.openlmis.referencedata.util.Resource2Db;
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

  @Value(value = DEMO_DATA_PATH + "referencedata.geographic_levels.csv")
  private Resource geographicLevelsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.geographic_zones.csv")
  private Resource geographicZonesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.facility_operators.csv")
  private Resource facilityOperatorsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.facility_types.csv")
  private Resource facilityTypesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.facilities.csv")
  private Resource facilitiesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.supervisory_nodes.csv")
  private Resource supervisoryNodesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.processing_schedules.csv")
  private Resource processingSchedulesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.processing_periods.csv")
  private Resource processingPeriodsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.commodity_types.csv")
  private Resource commodityTypesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.orderable_display_categories.csv")
  private Resource orderableDisplayCategoriesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.dispensables.csv")
  private Resource dispensablesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.dispensable_attributes.csv")
  private Resource dispensableAttributesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.orderables.csv")
  private Resource orderablesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.programs.csv")
  private Resource programsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.program_orderables.csv")
  private Resource programOrderablesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.supply_lines.csv")
  private Resource supplyLinesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.users.csv")
  private Resource usersResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.roles.csv")
  private Resource rolesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.role_rights.csv")
  private Resource roleRightsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.role_assignments.csv")
  private Resource roleAssignmentsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.requisition_groups.csv")
  private Resource requisitionGroupsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.requisition_group_program_schedules.csv")
  private Resource requisitionGroupProgramSchedulesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.requisition_group_members.csv")
  private Resource requisitionGroupMembersResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.supported_programs.csv")
  private Resource supportedProgramsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.trade_items.csv")
  private Resource tradeItemsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.trade_item_classifications.csv")
  private Resource tradeItemClassificationsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.ideal_stock_amounts.csv")
  private Resource idealStockAmountsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.lots.csv")
  private Resource lotsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.orderable_identifiers.csv")
  private Resource orderableIdentifiersResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.service_accounts.csv")
  private Resource serviceAccountsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.facility_type_approved_products.csv")
  private Resource facilityTypeApprovedProductsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.supply_partners.csv")
  private Resource supplyPartnersResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.supply_partner_associations.csv")
  private Resource supplyPartnerAssociationsResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.supply_partner_association_facilities.csv")
  private Resource supplyPartnerAssociationFacilitiesResource;

  @Value(value = DEMO_DATA_PATH + "referencedata.supply_partner_association_orderables.csv")
  private Resource supplyPartnerAssociationOrderablesResource;

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

    loader.insertToDbFromCsv("referencedata.geographic_levels", geographicLevelsResource);
    loader.insertToDbFromCsv("referencedata.geographic_zones", geographicZonesResource);
    loader.insertToDbFromCsv("referencedata.facility_operators", facilityOperatorsResource);
    loader.insertToDbFromCsv("referencedata.facility_types", facilityTypesResource);
    loader.insertToDbFromCsv("referencedata.facilities", facilitiesResource);
    loader.insertToDbFromCsv("referencedata.supervisory_nodes", supervisoryNodesResource);
    loader.insertToDbFromCsv("referencedata.processing_schedules", processingSchedulesResource);
    loader.insertToDbFromCsv("referencedata.processing_periods", processingPeriodsResource);
    loader.insertToDbFromCsv("referencedata.commodity_types", commodityTypesResource);
    loader.insertToDbFromCsv("referencedata.orderable_display_categories",
        orderableDisplayCategoriesResource);
    loader.insertToDbFromCsv("referencedata.dispensables", dispensablesResource);
    loader.insertToDbFromCsv("referencedata.dispensable_attributes", dispensableAttributesResource);
    loader.insertToDbFromCsv("referencedata.orderables", orderablesResource);
    loader.insertToDbFromCsv("referencedata.orderable_identifiers", orderableIdentifiersResource);
    loader.insertToDbFromCsv("referencedata.programs", programsResource);
    loader.insertToDbFromCsv("referencedata.program_orderables", programOrderablesResource);
    loader.insertToDbFromCsv("referencedata.supply_lines", supplyLinesResource);
    loader.insertToDbFromCsv("referencedata.users", usersResource);
    loader.insertToDbFromCsv("referencedata.roles", rolesResource);
    loader.insertToDbFromCsv("referencedata.role_rights", roleRightsResource);
    loader.insertToDbFromCsv("referencedata.role_assignments", roleAssignmentsResource);
    loader.insertToDbFromCsv("referencedata.requisition_groups", requisitionGroupsResource);
    loader.insertToDbFromCsv("referencedata.requisition_group_program_schedules",
        requisitionGroupProgramSchedulesResource);
    loader.insertToDbFromCsv("referencedata.requisition_group_members",
        requisitionGroupMembersResource);
    loader.insertToDbFromCsv("referencedata.supported_programs", supportedProgramsResource);
    loader.insertToDbFromCsv("referencedata.trade_items", tradeItemsResource);
    loader.insertToDbFromCsv("referencedata.trade_item_classifications",
        tradeItemClassificationsResource);
    loader.insertToDbFromCsv("referencedata.ideal_stock_amounts", idealStockAmountsResource);
    loader.insertToDbFromCsv("referencedata.lots", lotsResource);
    loader.insertToDbFromCsv("referencedata.service_accounts", serviceAccountsResource);
    loader.insertToDbFromCsv("referencedata.facility_type_approved_products",
        facilityTypeApprovedProductsResource);

    loader.insertToDbFromCsv("referencedata.supply_partners", supplyPartnersResource);
    loader.insertToDbFromCsv("referencedata.supply_partner_associations",
        supplyPartnerAssociationsResource);
    loader.insertToDbFromCsv("referencedata.supply_partner_association_facilities",
        supplyPartnerAssociationFacilitiesResource);
    loader.insertToDbFromCsv("referencedata.supply_partner_association_orderables",
        supplyPartnerAssociationOrderablesResource);

    XLOGGER.exit();
  }
}
