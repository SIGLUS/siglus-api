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
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY_ISA;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.MAXIMUM_STOCK_QUANTITY;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALUE_DOES_NOT_MATCH_CALCULATED_VALUE;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.utils.Message;

public class CalculatedFieldsValidatorTest {

  @Test
  public void shouldPassValidation() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder().build(), false)
        .build();

    CalculatedFieldsValidator validator = getCalculatedFieldsValidator(requisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).isEmpty();
  }

  @Test
  public void shouldRejectIfMaximumStockQuantityIsIncorrectlyCalculated() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withIncorrectMaximumStockQuantity()
            .build(), false)
        .build();

    CalculatedFieldsValidator validator = getCalculatedFieldsValidator(requisition);

    Map<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).hasSize(1);
    assertThat(errors)
        .containsEntry(REQUISITION_LINE_ITEMS,
            new Message(ERROR_VALUE_DOES_NOT_MATCH_CALCULATED_VALUE, MAXIMUM_STOCK_QUANTITY));
  }

  @Test
  public void shouldRejectIfCalculatedOrderQuantityIsIncorrectlyCalculated() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withIncorrectCalculatedOrderQuantityIsa()
            .build(), false)
        .build();

    CalculatedFieldsValidator validator = getCalculatedFieldsValidator(requisition);

    Map<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).hasSize(1);
    assertThat(errors)
        .containsEntry(REQUISITION_LINE_ITEMS,
            new Message(ERROR_VALUE_DOES_NOT_MATCH_CALCULATED_VALUE, CALCULATED_ORDER_QUANTITY));
  }

  @Test
  public void shouldRejectIfCalculatedOrderQuantityIsaIsIncorrectlyCalculated() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withIncorrectCalculatedOrderQuantityIsa()
            .build(), false)
        .build();

    CalculatedFieldsValidator validator = getCalculatedFieldsValidator(requisition);

    Map<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).hasSize(1);
    assertThat(errors)
        .containsEntry(REQUISITION_LINE_ITEMS,
            new Message(ERROR_VALUE_DOES_NOT_MATCH_CALCULATED_VALUE,
                CALCULATED_ORDER_QUANTITY_ISA));
  }

  private CalculatedFieldsValidator getCalculatedFieldsValidator(Requisition requisition) {
    Map<VersionIdentityDto, OrderableDto> orderables = requisition
        .getRequisitionLineItems()
        .stream()
        .map(line -> new OrderableDtoDataBuilder()
            .withId(line.getOrderable().getId())
            .withVersionNumber(line.getOrderable().getVersionNumber())
            .withProgramOrderable(requisition.getProgramId(), true)
            .buildAsDto())
        .collect(Collectors.toMap(OrderableDto::getIdentity, Function.identity()));

    Map<VersionIdentityDto, ApprovedProductDto> approvedProducts =  requisition
        .getRequisitionLineItems()
        .stream()
        .map(line -> new ApprovedProductDtoDataBuilder()
            .withId(line.getFacilityTypeApprovedProduct().getId())
            .withVersionNumber(line.getFacilityTypeApprovedProduct().getVersionNumber())
            .buildAsDto())
        .collect(Collectors.toMap(ApprovedProductDto::getIdentity, Function.identity()));

    return new CalculatedFieldsValidator(
        requisition, requisition.getTemplate(), orderables, approvedProducts);
  }

}
