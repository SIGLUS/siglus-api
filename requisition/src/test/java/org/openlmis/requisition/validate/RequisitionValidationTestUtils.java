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

package org.openlmis.requisition.validate;

import static org.openlmis.requisition.domain.SourceType.CALCULATED;
import static org.openlmis.requisition.domain.SourceType.USER_INPUT;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.ADDITIONAL_QUANTITY_REQUIRED;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.APPROVED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.AVERAGE_CONSUMPTION;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.BEGINNING_BALANCE;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY_ISA;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.MAXIMUM_STOCK_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.NUMBER_OF_NEW_PATIENTS_ADDED;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.REQUESTED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.REQUESTED_QUANTITY_EXPLANATION;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.SKIPPED_COLUMN;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.STOCK_ON_HAND;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_COLUMN;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_CONSUMED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_LOSSES_AND_ADJUSTMENTS;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_RECEIVED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_STOCKOUT_DAYS;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import org.openlmis.requisition.domain.AvailableRequisitionColumnOption;
import org.openlmis.requisition.domain.RequisitionTemplateColumn;
import org.openlmis.requisition.domain.RequisitionTemplateColumnDataBuilder;
import org.openlmis.requisition.domain.SourceType;

public class RequisitionValidationTestUtils {

  /**
   * Returns a initiate columns map for requisition template.
   */
  public static Map<String, RequisitionTemplateColumn> initiateColumns() {
    Map<String, RequisitionTemplateColumn> columns = new HashMap<>();

    columns.put(REQUESTED_QUANTITY,
        generateTemplateColumn(REQUESTED_QUANTITY, "J", USER_INPUT)
    );
    columns.put(
        TOTAL_RECEIVED_QUANTITY,
        generateTemplateColumn(TOTAL_RECEIVED_QUANTITY, "B", USER_INPUT)
    );
    columns.put(
        STOCK_ON_HAND,
        generateTemplateColumn(STOCK_ON_HAND, "E", USER_INPUT, CALCULATED)
    );
    columns.put(
        BEGINNING_BALANCE,
        generateTemplateColumn(BEGINNING_BALANCE, "A", USER_INPUT)
    );
    columns.put(
        REQUESTED_QUANTITY_EXPLANATION,
        generateTemplateColumn(REQUESTED_QUANTITY_EXPLANATION, "W", USER_INPUT)
    );
    columns.put(
        TOTAL_CONSUMED_QUANTITY,
        generateTemplateColumn(TOTAL_CONSUMED_QUANTITY, "C", USER_INPUT)
    );
    columns.put(
        TOTAL_LOSSES_AND_ADJUSTMENTS,
        generateTemplateColumn(TOTAL_LOSSES_AND_ADJUSTMENTS, "D", USER_INPUT)
    );
    columns.put(
        APPROVED_QUANTITY,
        generateTemplateColumn(APPROVED_QUANTITY, "K", USER_INPUT)
    );
    columns.put(
        TOTAL_STOCKOUT_DAYS,
        generateTemplateColumn(TOTAL_STOCKOUT_DAYS, "X", USER_INPUT)
    );
    columns.put(
        TOTAL_COLUMN,
        generateTemplateColumn(TOTAL_COLUMN, "Y", CALCULATED)
    );
    columns.put(
        NUMBER_OF_NEW_PATIENTS_ADDED,
        generateTemplateColumn(NUMBER_OF_NEW_PATIENTS_ADDED, "F", USER_INPUT)
    );
    columns.put(
        SKIPPED_COLUMN,
        generateTemplateColumn(SKIPPED_COLUMN, "S", USER_INPUT)
    );
    columns.put(
        MAXIMUM_STOCK_QUANTITY,
        generateTemplateColumn(
            MAXIMUM_STOCK_QUANTITY, "H",
            new AvailableRequisitionColumnOption(null, "default", "Default"),
            CALCULATED
        )
    );
    columns.put(
        CALCULATED_ORDER_QUANTITY,
        generateTemplateColumn(CALCULATED_ORDER_QUANTITY, "I", CALCULATED)
    );
    columns.put(
        CALCULATED_ORDER_QUANTITY_ISA,
        generateTemplateColumn(CALCULATED_ORDER_QUANTITY_ISA, "S", CALCULATED)
    );
    columns.put(
        AVERAGE_CONSUMPTION,
        generateTemplateColumn(AVERAGE_CONSUMPTION, "P", CALCULATED)
    );

    RequisitionTemplateColumn column = generateTemplateColumn(ADDITIONAL_QUANTITY_REQUIRED,
        "Z", USER_INPUT);
    column.setIsDisplayed(false);
    columns.put(
        ADDITIONAL_QUANTITY_REQUIRED,
        column
    );

    return columns;
  }

  private static RequisitionTemplateColumn generateTemplateColumn(
      String name, String indicator, SourceType... sourceType) {
    return generateTemplateColumn(name, indicator, null, sourceType);
  }

  private static RequisitionTemplateColumn generateTemplateColumn(
      String name, String indicator, AvailableRequisitionColumnOption option,
      SourceType... sourceType) {
    return new RequisitionTemplateColumnDataBuilder()
        .withName(name)
        .withColumnDefinition(name, indicator, Sets.newHashSet(sourceType))
        .withOption(option)
        .build();
  }
}
