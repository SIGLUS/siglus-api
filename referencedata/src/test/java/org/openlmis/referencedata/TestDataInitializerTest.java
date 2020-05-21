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

import static org.mockito.Mockito.verify;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.util.Resource2Db;
import org.springframework.core.io.Resource;

@RunWith(MockitoJUnitRunner.class)
public class TestDataInitializerTest {

  @Mock
  private Resource geographicLevelsResource;

  @Mock
  private Resource geographicZonesResource;

  @Mock
  private Resource facilityOperatorsResource;

  @Mock
  private Resource facilityTypesResource;

  @Mock
  private Resource facilitiesResource;

  @Mock
  private Resource supervisoryNodesResource;

  @Mock
  private Resource processingSchedulesResource;

  @Mock
  private Resource processingPeriodsResource;

  @Mock
  private Resource commodityTypesResource;

  @Mock
  private Resource orderableDisplayCategoriesResource;

  @Mock
  private Resource dispensablesResource;

  @Mock
  private Resource dispensableAttributesResource;

  @Mock
  private Resource orderablesResource;

  @Mock
  private Resource programsResource;

  @Mock
  private Resource programOrderablesResource;

  @Mock
  private Resource supplyLinesResource;

  @Mock
  private Resource usersResource;

  @Mock
  private Resource rolesResource;

  @Mock
  private Resource roleRightsResource;

  @Mock
  private Resource roleAssignmentsResource;

  @Mock
  private Resource requisitionGroupsResource;

  @Mock
  private Resource requisitionGroupProgramSchedulesResource;

  @Mock
  private Resource requisitionGroupMembersResource;

  @Mock
  private Resource supportedProgramsResource;

  @Mock
  private Resource tradeItemsResource;

  @Mock
  private Resource tradeItemClassificationsResource;

  @Mock
  private Resource idealStockAmountsResource;

  @Mock
  private Resource lotsResource;

  @Mock
  private Resource orderableIdentifiersResource;

  @Mock
  private Resource serviceAccountsResource;

  @Mock
  private Resource facilityTypeApprovedProductsResource;

  @Mock
  private Resource supplyPartnersResource;

  @Mock
  private Resource supplyPartnerAssociationsResource;

  @Mock
  private Resource supplyPartnerAssociationFacilitiesResource;

  @Mock
  private Resource supplyPartnerAssociationOrderablesResource;

  @Mock
  private Resource2Db loader;

  @InjectMocks
  private TestDataInitializer initializer = new TestDataInitializer(loader);

  @Test
  public void shouldLoadData() throws IOException {
    initializer.run();

    verify(loader).insertToDbFromCsv("referencedata.geographic_levels", geographicLevelsResource);
    verify(loader).insertToDbFromCsv("referencedata.geographic_zones", geographicZonesResource);
    verify(loader).insertToDbFromCsv("referencedata.facility_operators",
        facilityOperatorsResource);
    verify(loader).insertToDbFromCsv("referencedata.facility_types", facilityTypesResource);
    verify(loader).insertToDbFromCsv("referencedata.facilities", facilitiesResource);
    verify(loader).insertToDbFromCsv("referencedata.supervisory_nodes", supervisoryNodesResource);
    verify(loader).insertToDbFromCsv("referencedata.processing_schedules",
        processingSchedulesResource);
    verify(loader).insertToDbFromCsv("referencedata.processing_periods",
        processingPeriodsResource);
    verify(loader).insertToDbFromCsv("referencedata.commodity_types", commodityTypesResource);
    verify(loader).insertToDbFromCsv("referencedata.orderable_display_categories",
        orderableDisplayCategoriesResource);
    verify(loader).insertToDbFromCsv("referencedata.dispensables", dispensablesResource);
    verify(loader).insertToDbFromCsv("referencedata.dispensable_attributes",
        dispensableAttributesResource);
    verify(loader).insertToDbFromCsv("referencedata.orderables", orderablesResource);
    verify(loader).insertToDbFromCsv("referencedata.orderable_identifiers",
        orderableIdentifiersResource);
    verify(loader).insertToDbFromCsv("referencedata.programs", programsResource);
    verify(loader).insertToDbFromCsv("referencedata.program_orderables",
        programOrderablesResource);
    verify(loader).insertToDbFromCsv("referencedata.supply_lines", supplyLinesResource);
    verify(loader).insertToDbFromCsv("referencedata.users", usersResource);
    verify(loader).insertToDbFromCsv("referencedata.roles", rolesResource);
    verify(loader).insertToDbFromCsv("referencedata.role_rights", roleRightsResource);
    verify(loader).insertToDbFromCsv("referencedata.role_assignments", roleAssignmentsResource);
    verify(loader).insertToDbFromCsv("referencedata.requisition_groups",
        requisitionGroupsResource);
    verify(loader).insertToDbFromCsv("referencedata.requisition_group_program_schedules",
        requisitionGroupProgramSchedulesResource);
    verify(loader).insertToDbFromCsv("referencedata.requisition_group_members",
        requisitionGroupMembersResource);
    verify(loader).insertToDbFromCsv("referencedata.supported_programs",
        supportedProgramsResource);
    verify(loader).insertToDbFromCsv("referencedata.trade_items", tradeItemsResource);
    verify(loader).insertToDbFromCsv("referencedata.trade_item_classifications",
        tradeItemClassificationsResource);
    verify(loader).insertToDbFromCsv("referencedata.ideal_stock_amounts",
        idealStockAmountsResource);
    verify(loader).insertToDbFromCsv("referencedata.lots", lotsResource);
    verify(loader).insertToDbFromCsv("referencedata.service_accounts", serviceAccountsResource);
    verify(loader).insertToDbFromCsv("referencedata.facility_type_approved_products",
        facilityTypeApprovedProductsResource);
    verify(loader).insertToDbFromCsv("referencedata.supply_partners",
        supplyPartnersResource);
    verify(loader).insertToDbFromCsv("referencedata.supply_partner_associations",
        supplyPartnerAssociationsResource);
    verify(loader).insertToDbFromCsv("referencedata.supply_partner_association_facilities",
        supplyPartnerAssociationFacilitiesResource);
    verify(loader).insertToDbFromCsv("referencedata.supply_partner_association_orderables",
        supplyPartnerAssociationOrderablesResource);
  }
}
