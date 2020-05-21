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
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.APPROVED_QUANTITY;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_NON_NEGATIVE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_ONLY_AVAILABLE_FOR_APPROVAL;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALUE_MUST_BE_ENTERED;

import java.util.AbstractMap;
import java.util.HashMap;
import org.junit.Test;
import org.openlmis.requisition.utils.Message;

public class ApprovalFieldsValidatorTest {

  @Test
  public void shouldPassValidations() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder().build(), false)
        .build();

    ApprovalFieldsValidator validator = new ApprovalFieldsValidator(requisition, requisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanUpdate(errors);
    validator.validateCanChangeStatus(errors);

    assertThat(errors).isEmpty();
  }

  @Test
  public void shouldRejectIfApprovedQuantityIsNullForApprovableState() {
    ApprovalFieldsValidator validator =
        getApprovalFieldsValidator(null, RequisitionStatus.IN_APPROVAL);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).hasSize(1);
    assertThat(errors).contains(new AbstractMap.SimpleEntry<>(
        REQUISITION_LINE_ITEMS, new Message(ERROR_VALUE_MUST_BE_ENTERED, APPROVED_QUANTITY)));
  }

  @Test
  public void shouldNotRejectIfApprovedQuantityIsLessThanZeroForApprovableState() {
    ApprovalFieldsValidator validator =
        getApprovalFieldsValidator(-15, RequisitionStatus.AUTHORIZED);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).hasSize(1);
    assertThat(errors).contains(new AbstractMap.SimpleEntry<>(
        REQUISITION_LINE_ITEMS, new Message(ERROR_MUST_BE_NON_NEGATIVE, APPROVED_QUANTITY)));
  }

  @Test
  public void shouldNotRejectIfApprovedQuantityIsNullForNonApprovableState() {
    ApprovalFieldsValidator validator =
        getApprovalFieldsValidator(null, RequisitionStatus.SUBMITTED);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).hasSize(0);
  }

  @Test
  public void shouldNotRejectIfApprovedQuantityIsLessThanZeroForNonApprovableState() {
    ApprovalFieldsValidator validator =
        getApprovalFieldsValidator(-20, RequisitionStatus.INITIATED);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);

    assertThat(errors).hasSize(0);
  }

  @Test
  public void shouldNotValidateSkippedLineItems() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withApprovedQuantity(null)
            .withSkippedFlag()
            .build(), false)
        .withStatus(RequisitionStatus.IN_APPROVAL)
        .build();

    ApprovalFieldsValidator validator = new ApprovalFieldsValidator(requisition, requisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanChangeStatus(errors);
    validator.validateCanUpdate(errors);

    assertThat(errors).isEmpty();
  }

  @Test
  public void shouldRejectIfNotDuringApprovalAndApprovedQuantityNotNull() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withApprovedQuantity()
            .build(), false)
        .withStatus(RequisitionStatus.INITIATED)
        .build();

    ApprovalFieldsValidator validator = new ApprovalFieldsValidator(requisition, requisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanUpdate(errors);

    assertThat(errors).hasSize(1);
    assertThat(errors).containsEntry(REQUISITION_LINE_ITEMS,
        new Message(ERROR_ONLY_AVAILABLE_FOR_APPROVAL));
  }

  @Test
  public void shouldNotRejectIfDuringApprovalAndApprovedQuantityNotNull() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withApprovedQuantity()
            .build(), false)
        .withStatus(RequisitionStatus.IN_APPROVAL)
        .build();

    ApprovalFieldsValidator validator = new ApprovalFieldsValidator(requisition, requisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanUpdate(errors);

    assertThat(errors).isEmpty();
  }

  @Test
  public void shouldRejectIfNotDuringApprovalAndRemarksNotNull() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withRemarks()
            .build(), false)
        .withStatus(RequisitionStatus.INITIATED)
        .build();

    ApprovalFieldsValidator validator = new ApprovalFieldsValidator(requisition, requisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanUpdate(errors);

    assertThat(errors).hasSize(1);
    assertThat(errors).containsEntry(REQUISITION_LINE_ITEMS,
        new Message(ERROR_ONLY_AVAILABLE_FOR_APPROVAL));
  }

  @Test
  public void shouldNotRejectIfDuringApprovalAndRemarksNotNull() {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withRemarks()
            .build(), false)
        .withStatus(RequisitionStatus.IN_APPROVAL)
        .build();

    ApprovalFieldsValidator validator = new ApprovalFieldsValidator(requisition, requisition);

    HashMap<String, Message> errors = new HashMap<>();
    validator.validateCanUpdate(errors);

    assertThat(errors).isEmpty();
  }

  private ApprovalFieldsValidator getApprovalFieldsValidator(Integer approvedQuantity,
                                                             RequisitionStatus status) {
    Requisition requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withApprovedQuantity(approvedQuantity)
            .build(), false)
        .withStatus(status)
        .build();

    return new ApprovalFieldsValidator(null, requisition);
  }

}
