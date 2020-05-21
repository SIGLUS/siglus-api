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

package org.openlmis.requisition.domain.requisition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openlmis.requisition.domain.requisition.Requisition.REQUISITION_LINE_ITEMS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REASON_NOT_IN_REQUISITION_REASON_LIST;

import java.util.HashMap;
import java.util.UUID;
import org.junit.Test;
import org.openlmis.requisition.testutils.StockAdjustmentReasonDataBuilder;
import org.openlmis.requisition.utils.Message;

public class StockAdjustmentReasonsValidatorTest {

  @Test
  public void shouldPassValidationIfNoAdjustment() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder().build(), false)
        .build();

    StockAdjustmentReasonsValidator validator =
        new StockAdjustmentReasonsValidator(requisition, requisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanUpdate(errors);

    assertThat(errors).isEmpty();
  }

  @Test
  public void shouldPassIfStockAdjustmentsHaveValidReasons() {
    UUID reasonId = UUID.randomUUID();
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .addStockAdjustment(new StockAdjustment(reasonId, 10)).build(), false)
        .build();
    Requisition savedRequisition = new RequisitionDataBuilder()
        .addStockAdjustmentReason(new StockAdjustmentReasonDataBuilder()
            .withReasonId(reasonId).build())
        .build();

    StockAdjustmentReasonsValidator validator =
        new StockAdjustmentReasonsValidator(requisition, savedRequisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanUpdate(errors);

    assertThat(errors).isEmpty();
  }

  @Test
  public void shouldRejectIfStockAdjustmentsHaveInvalidReasons() {
    UUID reasonId = UUID.randomUUID();
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .addStockAdjustment(new StockAdjustment(reasonId, 10)).build(), false)
        .build();
    Requisition savedRequisition = new RequisitionDataBuilder()
        .addStockAdjustmentReason(new StockAdjustmentReasonDataBuilder().build())
        .build();

    StockAdjustmentReasonsValidator validator =
        new StockAdjustmentReasonsValidator(requisition, savedRequisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanUpdate(errors);

    assertThat(errors).hasSize(1);
    assertThat(errors).containsEntry(REQUISITION_LINE_ITEMS,
        new Message(ERROR_REASON_NOT_IN_REQUISITION_REASON_LIST, reasonId));

  }

}
