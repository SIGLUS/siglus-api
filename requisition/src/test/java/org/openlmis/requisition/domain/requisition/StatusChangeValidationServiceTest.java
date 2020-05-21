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
import static org.junit.Assert.assertFalse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.utils.Message;
import org.springframework.test.util.ReflectionTestUtils;

public class StatusChangeValidationServiceTest {

  private Requisition requisition = new RequisitionDataBuilder()
      .addLineItem(new RequisitionLineItemDataBuilder()
          .buildForInitiatedRegularRequisition(), false)
      .buildInitiatedRegularRequisition();

  private Map<VersionIdentityDto, OrderableDto> orderables = requisition
      .getRequisitionLineItems()
      .stream()
      .map(line -> new OrderableDtoDataBuilder()
          .withId(line.getOrderable().getId())
          .withVersionNumber(line.getOrderable().getVersionNumber())
          .withProgramOrderable(requisition.getProgramId(), true)
          .buildAsDto())
      .collect(Collectors.toMap(OrderableDto::getIdentity, Function.identity()));

  private Map<VersionIdentityDto, ApprovedProductDto> approvedProducts = requisition
      .getRequisitionLineItems()
      .stream()
      .map(line -> new ApprovedProductDtoDataBuilder()
          .withId(line.getFacilityTypeApprovedProduct().getId())
          .withVersionNumber(line.getFacilityTypeApprovedProduct().getVersionNumber())
          .buildAsDto())
      .collect(Collectors.toMap(ApprovedProductDto::getIdentity, Function.identity()));

  private Class[] expectedClasses = {
      RequisitionInvariantsValidator.class,
      ApprovalFieldsValidator.class,
      DatePhysicalStockCountCompletedValidator.class,
      StockOnHandValidator.class,
      TotalConsumedQuantityValidator.class,
      StockOutDaysValidator.class,
      BeginningBalanceValidator.class,
      CalculatedFieldsValidator.class,
      NumberOfNewPatientsAddedValidator.class,
      RequestedQuantityValidator.class,
      StockAdjustmentsValidator.class,
      TotalFieldValidator.class,
      TotalReceivedQuantityValidator.class
  };

  @Test
  public void shouldPassValidations() {
    StatusChangeValidationService statusChangeValidationService =
        new StatusChangeValidationService(requisition, LocalDate.now(), true,
            orderables, approvedProducts);

    ValidationResult validationResult =
        statusChangeValidationService.validateRequisitionCanChangeStatus();

    assertFalse(validationResult.hasErrors());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldCallValidatorsNotForApprovalIfRequisitionIsNotDuringApproval() {
    addValidatorForApprovalAndCallValidationService();
  }

  @Test
  public void shouldNotCallValidatorsNotForApprovalDuringApprove() {
    requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .buildForInitiatedRegularRequisition(), false)
        .withStatus(RequisitionStatus.AUTHORIZED)
        .build();
    addValidatorForApprovalAndCallValidationService();
  }

  @Test
  public void shouldUseAllRequiredValidators() {
    StatusChangeValidationService statusChangeValidationService =
        new StatusChangeValidationService(requisition, LocalDate.now(), true,
            orderables, approvedProducts);

    List<RequisitionStatusChangeDomainValidator> validators =
        (List<RequisitionStatusChangeDomainValidator>)
            ReflectionTestUtils.getField(statusChangeValidationService, "validators");

    List<Class> actual = validators.stream()
        .map(RequisitionStatusChangeDomainValidator::getClass)
        .collect(Collectors.toList());
    assertThat(actual).containsExactlyInAnyOrder(expectedClasses);

  }

  private void addValidatorForApprovalAndCallValidationService() {
    StatusChangeValidationService statusChangeValidationService =
        new StatusChangeValidationService(requisition, LocalDate.now(), true,
            orderables, approvedProducts);

    List<RequisitionStatusChangeDomainValidator> validators =
        (List<RequisitionStatusChangeDomainValidator>)
            ReflectionTestUtils.getField(statusChangeValidationService, "validators");

    validators.add(new DummyRequisitionStatusChangeDomainValidator());

    statusChangeValidationService.validateRequisitionCanChangeStatus();
  }

  private static class DummyRequisitionStatusChangeDomainValidator implements
      RequisitionStatusChangeDomainValidator {

    @Override
    public void validateCanChangeStatus(Map<String, Message> errors) {
      throw new IllegalStateException();
    }

    @Override
    public boolean isForRegularOnly() {
      return false;
    }

    @Override
    public boolean isForApprove() {
      return false;
    }
  }
}
