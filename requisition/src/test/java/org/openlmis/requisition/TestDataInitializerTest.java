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

import static org.mockito.Mockito.verify;
import static org.openlmis.requisition.TestDataInitializer.COLUMNS_MAPS_TABLE;
import static org.openlmis.requisition.TestDataInitializer.JASPER_TEMPLATES_TABLE;
import static org.openlmis.requisition.TestDataInitializer.JASPER_TEMPLATE_PARAMETER_DEPENDENCIES_TABLE;
import static org.openlmis.requisition.TestDataInitializer.PREVIOUS_ADJUSTED_CONSUMPTIONS_TABLE;
import static org.openlmis.requisition.TestDataInitializer.REQUISITIONS_TABLE;
import static org.openlmis.requisition.TestDataInitializer.REQUISITION_LINE_ITEMS_TABLE;
import static org.openlmis.requisition.TestDataInitializer.REQUISITION_TEMPLATES_TABLE;
import static org.openlmis.requisition.TestDataInitializer.REQUISITION_TEMPLATE_ASSIGNMENTS_TABLE;
import static org.openlmis.requisition.TestDataInitializer.STATUS_CHANGES_TABLE;
import static org.openlmis.requisition.TestDataInitializer.STATUS_MESSAGES_TABLE;
import static org.openlmis.requisition.TestDataInitializer.STOCK_ADJUSTMENTS_TABLE;
import static org.openlmis.requisition.TestDataInitializer.STOCK_ADJUSTMENT_REASONS_TABLE;
import static org.openlmis.requisition.TestDataInitializer.TEMPLATE_PARAMETERS_TABLE;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.utils.Resource2Db;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

@RunWith(MockitoJUnitRunner.class)
public class TestDataInitializerTest {

  @Mock
  private Resource columnsMapsResource;

  @Mock
  private Resource jasperTemplateParameterDependenciesResource;

  @Mock
  private Resource jasperTemplatesResource;

  @Mock
  private Resource previousAdjustedConsumptionsResource;

  @Mock
  private Resource requisitionLineItemsResource;

  @Mock
  private Resource requisitionTemplateAssignmentsResource;

  @Mock
  private Resource requisitionTemplatesResource;

  @Mock
  private Resource requisitionsResource;

  @Mock
  private Resource statusChangesResource;

  @Mock
  private Resource statusMessagesResource;

  @Mock
  private Resource stockAdjustmentReasonsResource;

  @Mock
  private Resource stockAdjustmentsResource;

  @Mock
  private Resource templateParametersResource;

  @Mock
  private Resource generateRequisitionPermissionStringsResource;

  @Mock
  private JdbcTemplate template;

  @Mock
  private Resource2Db loader;

  @InjectMocks
  private TestDataInitializer initializer = new TestDataInitializer(template, loader);

  @Test
  public void shouldLoadData() throws IOException {
    initializer.run();

    verify(loader).insertToDbFromCsv(COLUMNS_MAPS_TABLE, columnsMapsResource);
    verify(loader).insertToDbFromCsv(
        JASPER_TEMPLATE_PARAMETER_DEPENDENCIES_TABLE, jasperTemplateParameterDependenciesResource);
    verify(loader).insertToDbFromCsv(JASPER_TEMPLATES_TABLE, jasperTemplatesResource);
    verify(loader).insertToDbFromCsv(
        PREVIOUS_ADJUSTED_CONSUMPTIONS_TABLE, previousAdjustedConsumptionsResource);
    verify(loader).insertToDbFromCsv(REQUISITION_LINE_ITEMS_TABLE, requisitionLineItemsResource);
    verify(loader).insertToDbFromCsv(REQUISITION_TEMPLATE_ASSIGNMENTS_TABLE,
        requisitionTemplateAssignmentsResource);
    verify(loader).insertToDbFromCsv(REQUISITION_TEMPLATES_TABLE, requisitionTemplatesResource);
    verify(loader).insertToDbFromCsv(REQUISITIONS_TABLE, requisitionsResource);
    verify(loader).insertToDbFromCsv(STATUS_CHANGES_TABLE, statusChangesResource);
    verify(loader).insertToDbFromCsv(STATUS_MESSAGES_TABLE, statusMessagesResource);
    verify(loader)
        .insertToDbFromCsv(STOCK_ADJUSTMENT_REASONS_TABLE, stockAdjustmentReasonsResource);
    verify(loader).insertToDbFromCsv(STOCK_ADJUSTMENTS_TABLE, stockAdjustmentsResource);
    verify(loader).insertToDbFromCsv(TEMPLATE_PARAMETERS_TABLE, templateParametersResource);

    verify(template).update("DELETE FROM requisition.requisition_permission_strings;");
    verify(loader).updateDbFromSqlSingle(generateRequisitionPermissionStringsResource);
  }
}
