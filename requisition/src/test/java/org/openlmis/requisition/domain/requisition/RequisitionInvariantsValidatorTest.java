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

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.openlmis.requisition.domain.requisition.Requisition.EMERGENCY_FIELD;
import static org.openlmis.requisition.domain.requisition.Requisition.FACILITY_ID;
import static org.openlmis.requisition.domain.requisition.Requisition.NUMBER_OF_MONTHS_IN_PERIOD;
import static org.openlmis.requisition.domain.requisition.Requisition.PROCESSING_PERIOD_ID;
import static org.openlmis.requisition.domain.requisition.Requisition.PROGRAM_ID;
import static org.openlmis.requisition.domain.requisition.Requisition.REQUISITION_LINE_ITEMS;
import static org.openlmis.requisition.domain.requisition.Requisition.SUPERVISORY_NODE_ID;
import static org.openlmis.requisition.domain.requisition.RequisitionInvariantsValidator.EXTRA_DATA_ORIGINAL_REQUISITION;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_IS_INVARIANT;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_LINE_ITEM_ADDED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_LINE_ITEM_REMOVED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_STOCK_BASED_VALUE_MODIFIED;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.utils.Message;

@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionInvariantsValidatorTest {
  private static final String COLUMN_NAME = RequisitionLineItem.STOCK_ON_HAND;

  private Requisition requisitionToUpdate;
  private Requisition requisitionUpdater;
  private Map<VersionIdentityDto, OrderableDto> orderables;
  private RequisitionInvariantsValidator validator;
  private Map<String, Message> errors;

  @Before
  public void setUp() {
    requisitionToUpdate = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder().build(), false)
        .addLineItem(new RequisitionLineItemDataBuilder().build(), false)
        .addLineItem(new RequisitionLineItemDataBuilder().withSkippedFlag().build(), false)
        .addLineItem(new RequisitionLineItemDataBuilder().build(), false)
        .addLineItem(new RequisitionLineItemDataBuilder().withSkippedFlag().build(), false)
        .withSupervisoryNodeId(UUID.randomUUID())
        .withOriginalRequisitionId(UUID.randomUUID())
        .build();

    requisitionUpdater = new RequisitionDataBuilder()
        .withFacilityId(requisitionToUpdate.getFacilityId())
        .withProgramId(requisitionToUpdate.getProgramId())
        .withProcessingPeriodId(requisitionToUpdate.getProcessingPeriodId())
        .withSupervisoryNodeId(requisitionToUpdate.getSupervisoryNodeId())
        .withLineItems(requisitionToUpdate.getRequisitionLineItems(), false)
        .withTemplate(requisitionToUpdate.getTemplate())
        .withOriginalRequisitionId(requisitionToUpdate.getOriginalRequisitionId())
        .build();

    orderables = requisitionUpdater
        .getRequisitionLineItems()
        .stream()
        .map(line -> new OrderableDtoDataBuilder()
            .withId(line.getOrderable().getId())
            .withVersionNumber(line.getOrderable().getVersionNumber())
            .withProgramOrderable(requisitionUpdater.getProgramId(), true)
            .buildAsDto())
        .collect(Collectors.toMap(OrderableDto::getIdentity, Function.identity()));

    validator = new RequisitionInvariantsValidator(
        requisitionUpdater, requisitionToUpdate, orderables
    );

    errors = new HashMap<>();
  }

  @Test
  public void shouldValidate() {
    validator.validateCanUpdate(errors);
    assertThat(errors.entrySet(), hasSize(0));
  }

  @Test
  public void shouldRejectIfFacilityIdWasChanged() {
    requisitionUpdater.setFacilityId(UUID.randomUUID());

    validator.validateCanUpdate(errors);

    assertThat(errors, hasEntry(FACILITY_ID, new Message(ERROR_IS_INVARIANT)));
  }

  @Test
  public void shouldRejectIfProgramIdWasChanged() {
    requisitionUpdater.setProgramId(UUID.randomUUID());

    validator.validateCanUpdate(errors);

    assertThat(errors, hasEntry(PROGRAM_ID, new Message(ERROR_IS_INVARIANT)));
  }

  @Test
  public void shouldRejectIfProcessingPeriodIdWasChanged() {
    requisitionUpdater.setProcessingPeriodId(UUID.randomUUID());

    validator.validateCanUpdate(errors);

    assertThat(errors, hasEntry(PROCESSING_PERIOD_ID, new Message(ERROR_IS_INVARIANT)));
  }

  @Test
  public void shouldRejectIfEmergencyFacilityIdWasChanged() {
    requisitionUpdater.setEmergency(true);

    validator.validateCanUpdate(errors);

    assertThat(errors, hasEntry(EMERGENCY_FIELD, new Message(ERROR_IS_INVARIANT)));
  }

  @Test
  public void shouldRejectIfSupervisoryNodeIdWasChanged() {
    requisitionUpdater.setSupervisoryNodeId(UUID.randomUUID());

    validator.validateCanUpdate(errors);

    assertThat(errors, hasEntry(SUPERVISORY_NODE_ID, new Message(ERROR_IS_INVARIANT)));
  }

  @Test
  public void shouldRejectIfOrderableIdentityHasBeenChanged() {
    requisitionUpdater.setRequisitionLineItems(Lists.newArrayList(
        new RequisitionLineItemDataBuilder()
            .withId(requisitionToUpdate.getRequisitionLineItems().get(0).getId())
            .build()
    ));

    validator.validateCanUpdate(errors);

    assertThat(errors, hasEntry(REQUISITION_LINE_ITEMS, new Message(ERROR_IS_INVARIANT)));
  }

  @Test
  public void shouldRejectIfFullSupplyLineWasRemovedForRegularRequisition() {
    requisitionUpdater.setRequisitionLineItems(Lists.newArrayList());

    validator.validateCanUpdate(errors);

    assertThat(errors, hasEntry(REQUISITION_LINE_ITEMS, new Message(ERROR_LINE_ITEM_REMOVED)));
  }

  @Test
  public void shouldRejectIfNewFullSupplyLineWasAddedForRegularRequisition() {
    RequisitionLineItem newLineItem = new RequisitionLineItemDataBuilder().build();
    OrderableDto newOrderable = new OrderableDtoDataBuilder()
        .withId(newLineItem.getOrderable().getId())
        .withVersionNumber(newLineItem.getOrderable().getVersionNumber())
        .withProgramOrderable(requisitionUpdater.getProgramId(), true)
        .buildAsDto();

    orderables.put(newOrderable.getIdentity(), newOrderable);
    requisitionUpdater.getRequisitionLineItems().add(newLineItem);
    validator = new RequisitionInvariantsValidator(
        requisitionUpdater, requisitionToUpdate, orderables
    );

    validator.validateCanUpdate(errors);

    assertThat(errors, hasEntry(REQUISITION_LINE_ITEMS, new Message(ERROR_LINE_ITEM_ADDED)));
  }

  @Test
  public void shouldRejectIfValueWasChangedForStockColumn() throws Exception {
    requisitionToUpdate.setTemplate(new RequisitionTemplateDataBuilder()
        .withAllColumns()
        .withPopulateStockOnHandFromStockCards()
        .build()
    );
    requisitionUpdater.setTemplate(requisitionToUpdate.getTemplate());

    List<RequisitionLineItem> lineItems = requisitionToUpdate.getRequisitionLineItems();
    for (int i = 0, size = lineItems.size(); i < size; ++i) {
      lineItems.get(i).setStockOnHand(i % 2 == 0 ? 1000 : null);

      RequisitionLineItem element = new RequisitionLineItem(lineItems.get(i));
      element.setRequisition(requisitionUpdater);
      element.setStockOnHand(5000);

      requisitionUpdater.getRequisitionLineItems().set(i, element);
    }

    validator.validateCanUpdate(errors);

    assertThat(errors, hasEntry(
        REQUISITION_LINE_ITEMS,
        new Message(ERROR_STOCK_BASED_VALUE_MODIFIED, COLUMN_NAME)
    ));
  }

  @Test
  public void shouldValidateIfNewValueIsNullAndColumnIsHidden() throws Exception {
    requisitionToUpdate.setTemplate(new RequisitionTemplateDataBuilder()
            .withAllColumns()
            .withPopulateStockOnHandFromStockCards()
            .build()
    );
    requisitionUpdater.setTemplate(requisitionToUpdate.getTemplate());
    requisitionUpdater.getTemplate().changeColumnDisplay("totalReceivedQuantity", false);

    List<RequisitionLineItem> lineItems = requisitionToUpdate.getRequisitionLineItems();
    for (int i = 0, size = lineItems.size(); i < size; ++i) {
      lineItems.get(i).setTotalReceivedQuantity(5000);

      RequisitionLineItem element = new RequisitionLineItem(lineItems.get(i));
      element.setRequisition(requisitionUpdater);
      element.setTotalReceivedQuantity(null);

      requisitionUpdater.getRequisitionLineItems().set(i, element);
    }

    validator.validateCanUpdate(errors);

    assertThat(errors.entrySet(), hasSize(0));
  }

  @Test
  public void shouldNotRejectIfValueWasChangedInNotFullSupplyForStockColumn() throws Exception {
    RequisitionLineItem newLineItem = new RequisitionLineItemDataBuilder()
        .withStockOnHand(1000)
        .build();
    OrderableDto newOrderable = new OrderableDtoDataBuilder()
        .withId(newLineItem.getOrderable().getId())
        .withVersionNumber(newLineItem.getOrderable().getVersionNumber())
        .withProgramOrderable(requisitionUpdater.getProgramId(), false)
        .buildAsDto();

    orderables.put(newOrderable.getIdentity(), newOrderable);

    requisitionToUpdate.setTemplate(new RequisitionTemplateDataBuilder()
        .withAllColumns()
        .withPopulateStockOnHandFromStockCards()
        .build()
    );
    requisitionToUpdate.setRequisitionLineItems(Lists.newArrayList(newLineItem));

    requisitionUpdater.setTemplate(requisitionToUpdate.getTemplate());
    RequisitionLineItem requisitionLineItem = new RequisitionLineItem(newLineItem);
    requisitionLineItem.setRequisition(requisitionUpdater);

    requisitionUpdater.setRequisitionLineItems(Lists.newArrayList(requisitionLineItem));
    requisitionUpdater.getRequisitionLineItems().get(0).setStockOnHand(5000);

    validator = new RequisitionInvariantsValidator(
        requisitionUpdater, requisitionToUpdate, orderables
    );

    validator.validateCanUpdate(errors);

    assertThat(errors.entrySet(), hasSize(0));
  }

  @Test
  public void shouldRejectIfNumberOfMonthsInPeriodWasChanged() {
    requisitionUpdater
        .setNumberOfMonthsInPeriod(requisitionToUpdate.getNumberOfMonthsInPeriod() + 10);

    validator.validateCanUpdate(errors);

    assertThat(errors, hasEntry(NUMBER_OF_MONTHS_IN_PERIOD, new Message(ERROR_IS_INVARIANT)));
  }

  @Test
  public void shouldRejectIfOriginalRequisitionWasChanged() {
    requisitionUpdater.setOriginalRequisitionId(UUID.randomUUID());

    validator.validateCanUpdate(errors);

    assertThat(errors, hasEntry(EXTRA_DATA_ORIGINAL_REQUISITION, new Message(ERROR_IS_INVARIANT)));
  }
}
