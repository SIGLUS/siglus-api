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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.openlmis.requisition.domain.requisition.Requisition.REQUISITION_LINE_ITEMS;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.CALCULATED_ORDER_QUANTITY_ISA;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.REQUESTED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.REQUESTED_QUANTITY_EXPLANATION;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_IS_HIDDEN;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_NON_NEGATIVE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALUE_MUST_BE_ENTERED;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateColumn;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.utils.Message;

@SuppressWarnings("PMD.TooManyMethods")
public class RequestedQuantityValidatorTest {
  private static final String EXPLANATION = "test";

  private RequisitionLineItem fullSupply;
  private RequisitionLineItem nonFullSupply;

  private Requisition requisitionToValidate;

  private RequisitionTemplate template;

  private RequestedQuantityValidator validator;

  private Map<String, Message> errors;

  @Before
  public void setUp() {
    fullSupply = new RequisitionLineItemDataBuilder().build();
    nonFullSupply = new RequisitionLineItemDataBuilder().build();

    requisitionToValidate = new RequisitionDataBuilder()
        .addLineItem(fullSupply, false)
        .addLineItem(nonFullSupply, true)
        .build();

    Map<VersionIdentityDto, OrderableDto> orderables = Stream
        .of(new OrderableDtoDataBuilder()
                .withId(fullSupply.getOrderable().getId())
                .withVersionNumber(fullSupply.getOrderable().getVersionNumber())
                .withProgramOrderable(requisitionToValidate.getProgramId(), true)
                .buildAsDto(),
            new OrderableDtoDataBuilder()
                .withId(nonFullSupply.getOrderable().getId())
                .withVersionNumber(nonFullSupply.getOrderable().getVersionNumber())
                .withProgramOrderable(requisitionToValidate.getProgramId(), false)
                .buildAsDto())
        .collect(Collectors.toMap(OrderableDto::getIdentity, Function.identity()));

    template = requisitionToValidate.getTemplate();
    validator = new RequestedQuantityValidator(requisitionToValidate, orderables);

    errors = new HashMap<>();
  }

  @Test
  public void shouldValidate() {
    validator.validateCanChangeStatus(errors);
    assertThat(errors.entrySet(), hasSize(0));
  }

  @Test
  public void shouldValidateIfQuantityIsNullAndCalcOrderQtyIsDisplayed() {
    template.findColumn(CALCULATED_ORDER_QUANTITY_ISA).setIsDisplayed(false);
    template.findColumn(CALCULATED_ORDER_QUANTITY).setIsDisplayed(true);

    fullSupply.setCalculatedOrderQuantity(10);
    fullSupply.setRequestedQuantity(null);

    validator.validateCanChangeStatus(errors);
    assertThat(errors.entrySet(), hasSize(0));
  }

  @Test
  public void shouldValidateIfQuantityIsNullAndCalcOrderQtyIsaIsDisplayed() {
    template.findColumn(CALCULATED_ORDER_QUANTITY_ISA).setIsDisplayed(true);
    template.findColumn(CALCULATED_ORDER_QUANTITY).setIsDisplayed(false);

    fullSupply.setCalculatedOrderQuantityIsa(10);
    fullSupply.setRequestedQuantity(null);

    validator.validateCanChangeStatus(errors);
    assertThat(errors.entrySet(), hasSize(0));
  }

  @Test
  public void shouldValidateIfCalcOrderQtyIsaNotExists() {
    Map<String, RequisitionTemplateColumn> columns = new HashMap<>(template.viewColumns());
    columns.get(CALCULATED_ORDER_QUANTITY).setIsDisplayed(false);
    columns.remove(CALCULATED_ORDER_QUANTITY_ISA);

    template = new RequisitionTemplate(columns);
    requisitionToValidate.setTemplate(template);

    validator.validateCanChangeStatus(errors);
    assertThat(errors.entrySet(), hasSize(0));
  }

  @Test
  public void shouldRejectIfQuantityIsNullForNonFullSupplyLine() {
    nonFullSupply.setRequestedQuantity(null);

    validator.validateCanChangeStatus(errors);

    assertErrors(ERROR_VALUE_MUST_BE_ENTERED, REQUESTED_QUANTITY);
  }

  @Test
  public void shouldRejectIfQuantityIsNegativeForNonFullSupplyLine() {
    nonFullSupply.setRequestedQuantity(-10);

    validator.validateCanChangeStatus(errors);

    assertErrors(ERROR_MUST_BE_NON_NEGATIVE, REQUESTED_QUANTITY);
  }

  @Test
  public void shouldRejectIfExplanationIsSetForHiddenColumnForFullSupplyLine() {
    template.findColumn(REQUESTED_QUANTITY_EXPLANATION).setIsDisplayed(false);
    fullSupply.setRequestedQuantityExplanation(EXPLANATION);

    validator.validateCanChangeStatus(errors);

    assertErrors(ERROR_IS_HIDDEN, REQUESTED_QUANTITY_EXPLANATION);
  }

  @Test
  public void shouldRejectIfExplanationIsSetForHiddenColumnForNonFullSupplyLine() {
    template.findColumn(REQUESTED_QUANTITY_EXPLANATION).setIsDisplayed(false);
    nonFullSupply.setRequestedQuantityExplanation(EXPLANATION);

    validator.validateCanChangeStatus(errors);

    assertErrors(ERROR_IS_HIDDEN, REQUESTED_QUANTITY_EXPLANATION);
  }

  @Test
  public void shouldRejectIfQuantityIfDifferentFromCalcOrderQtyAndExplanationIsNotSet() {
    template.findColumn(CALCULATED_ORDER_QUANTITY_ISA).setIsDisplayed(false);
    template.findColumn(CALCULATED_ORDER_QUANTITY).setIsDisplayed(true);

    fullSupply.setRequestedQuantity(10);
    fullSupply.setCalculatedOrderQuantity(50);
    fullSupply.setRequestedQuantityExplanation(null);

    validator.validateCanChangeStatus(errors);

    assertErrors(ERROR_VALUE_MUST_BE_ENTERED, REQUESTED_QUANTITY_EXPLANATION);
  }

  @Test
  public void shouldRejectIfQuantityIfDifferentFromCalcOrderQtyIsaAndExplanationIsNotSet() {
    template.findColumn(CALCULATED_ORDER_QUANTITY_ISA).setIsDisplayed(true);
    template.findColumn(CALCULATED_ORDER_QUANTITY).setIsDisplayed(false);

    fullSupply.setRequestedQuantity(10);
    fullSupply.setCalculatedOrderQuantityIsa(50);
    fullSupply.setRequestedQuantityExplanation(null);

    validator.validateCanChangeStatus(errors);

    assertErrors(ERROR_VALUE_MUST_BE_ENTERED, REQUESTED_QUANTITY_EXPLANATION);
  }

  @Test
  public void shouldRejectIfCalcOrderQtyIsDisplayedAndQuantityIsSetForHiddenColumn() {
    template.findColumn(CALCULATED_ORDER_QUANTITY).setIsDisplayed(true);
    template.findColumn(REQUESTED_QUANTITY).setIsDisplayed(false);

    fullSupply.setRequestedQuantity(10);
    fullSupply.setRequestedQuantityExplanation(EXPLANATION);

    validator.validateCanChangeStatus(errors);

    assertErrors(ERROR_IS_HIDDEN, REQUESTED_QUANTITY);
  }

  @Test
  public void shouldRejectIfCalcOrderQtyIsaIsDisplayedAndQuantityIsSetForHiddenColumn() {
    template.findColumn(CALCULATED_ORDER_QUANTITY_ISA).setIsDisplayed(true);
    template.findColumn(REQUESTED_QUANTITY).setIsDisplayed(false);

    fullSupply.setRequestedQuantity(10);
    fullSupply.setRequestedQuantityExplanation(EXPLANATION);

    validator.validateCanChangeStatus(errors);

    assertErrors(ERROR_IS_HIDDEN, REQUESTED_QUANTITY);
  }

  @Test
  public void shouldRejectIfQuantityIsNull() {
    template.findColumn(CALCULATED_ORDER_QUANTITY_ISA).setIsDisplayed(false);
    template.findColumn(CALCULATED_ORDER_QUANTITY).setIsDisplayed(false);

    fullSupply.setRequestedQuantity(null);

    validator.validateCanChangeStatus(errors);

    assertErrors(ERROR_VALUE_MUST_BE_ENTERED, REQUESTED_QUANTITY);
  }

  @Test
  public void shouldRejectIfRequestedQuantityIsNullForEmergencyRequisition() {
    requisitionToValidate.setEmergency(true);
    fullSupply.setRequestedQuantity(null);

    validator.validateCanChangeStatus(errors);

    assertErrors(ERROR_VALUE_MUST_BE_ENTERED, REQUESTED_QUANTITY);
  }

  @Test
  public void shouldRejectIfRequestedQuantityIsNegativeForEmergencyRequisition() {
    requisitionToValidate.setEmergency(true);
    fullSupply.setRequestedQuantity(-100);

    validator.validateCanChangeStatus(errors);

    assertErrors(ERROR_MUST_BE_NON_NEGATIVE, REQUESTED_QUANTITY);
  }

  private void assertErrors(String messageKey, String field) {
    assertThat(errors, hasKey(REQUISITION_LINE_ITEMS));

    Message message = errors.get(REQUISITION_LINE_ITEMS);
    assertThat(message, hasProperty("key", is(messageKey)));
    assertThat(message.toString().split(":")[1].trim(), containsString(field));
  }
}
