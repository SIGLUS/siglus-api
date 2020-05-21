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
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_STOCK_ADJUSTMENT_NON_NEGATIVE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_STOCK_ADJUSTMENT_NOT_FOUND;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.StockAdjustmentReasonDataBuilder;
import org.openlmis.requisition.utils.Message;

public class StockAdjustmentsValidatorTest {

  @Test
  public void shouldPassValidationIfNoAdjustment() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder().build(), false)
        .build();

    StockAdjustmentsValidator validator = getStockAdjustmentsValidator(requisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).isEmpty();
  }

  @Test
  public void shouldPassIfStockAdjustmentsHaveValidReasons() {
    UUID reasonId = UUID.randomUUID();
    StockAdjustmentsValidator validator = getStockAdjustmentsValidator(reasonId, 10);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).isEmpty();
  }

  @Test
  public void shouldRejectIfStockAdjustmentsHaveNullQuantity() {
    UUID reasonId = UUID.randomUUID();
    StockAdjustmentsValidator validator = getStockAdjustmentsValidator(reasonId, null);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).hasSize(1);
    assertThat(errors).containsEntry(REQUISITION_LINE_ITEMS,
        new Message(ERROR_STOCK_ADJUSTMENT_NON_NEGATIVE, reasonId));
  }

  @Test
  public void shouldRejectIfStockAdjustmentsHaveNegativeQuantity() {
    UUID reasonId = UUID.randomUUID();
    StockAdjustmentsValidator validator = getStockAdjustmentsValidator(reasonId, -30);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).hasSize(1);
    assertThat(errors).containsEntry(REQUISITION_LINE_ITEMS,
        new Message(ERROR_STOCK_ADJUSTMENT_NON_NEGATIVE, reasonId));
  }

  @Test
  public void shouldRejectIfStockAdjustmentsHaveInvalidReasons() {
    UUID reasonId = UUID.randomUUID();
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .addStockAdjustment(new StockAdjustment(reasonId, 10))
            .build(), false)
        .addStockAdjustmentReason(new StockAdjustmentReasonDataBuilder().build())
        .build();

    StockAdjustmentsValidator validator = getStockAdjustmentsValidator(requisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).hasSize(1);
    assertThat(errors).containsEntry(REQUISITION_LINE_ITEMS,
        new Message(ERROR_STOCK_ADJUSTMENT_NOT_FOUND, reasonId));

  }

  private StockAdjustmentsValidator getStockAdjustmentsValidator(UUID reasonId, Integer quantity) {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .addStockAdjustment(new StockAdjustment(reasonId, quantity)).build(), false)
        .addStockAdjustmentReason(new StockAdjustmentReasonDataBuilder()
            .withReasonId(reasonId).build())
        .build();

    return getStockAdjustmentsValidator(requisition);
  }

  private StockAdjustmentsValidator getStockAdjustmentsValidator(Requisition requisition) {
    Map<VersionIdentityDto, OrderableDto> orderables = requisition
        .getRequisitionLineItems()
        .stream()
        .map(line -> new OrderableDtoDataBuilder()
            .withId(line.getOrderable().getId())
            .withVersionNumber(line.getOrderable().getVersionNumber())
            .withProgramOrderable(requisition.getProgramId(), true)
            .buildAsDto())
        .collect(Collectors.toMap(OrderableDto::getIdentity, Function.identity()));

    return new StockAdjustmentsValidator(requisition, orderables);
  }


}
