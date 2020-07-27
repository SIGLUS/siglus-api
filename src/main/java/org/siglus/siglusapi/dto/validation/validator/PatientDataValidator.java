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

package org.siglus.siglusapi.dto.validation.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.validation.constraint.PatientDataConstraint;
import org.siglus.siglusapi.repository.UsageTemplateColumnSectionRepository;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.springframework.stereotype.Component;

/**
 * Validate patientLineItems in {@linkplain SiglusRequisitionDto#getPatientLineItems()
 * SiglusRequisitionDto}.
 * <p>
 * Check following:
 * <ul>
 *   <li>No missing or extra groups and columns</li>
 *   <li>Total column equals the sum of the rest column values
 *   if the source of the total column is calculated</li>
 *   <li>No value overflow</li>
 * </ul>
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PatientDataValidator implements
    ConstraintValidator<PatientDataConstraint, SiglusRequisitionDto> {

  private final RequisitionTemplateExtensionRepository templateExtensionRepo;

  private final SiglusRequisitionRequisitionService requisitionService;

  private final UsageTemplateColumnSectionRepository columnSectionRepo;


  @Override
  public void initialize(PatientDataConstraint constraintAnnotation) {
  }

  @Override
  public boolean isValid(SiglusRequisitionDto uploadedValue, ConstraintValidatorContext context) {
    context.disableDefaultConstraintViolation();
    UUID requisitionId = uploadedValue.getId();
    RequisitionV2Dto storedRequisition = requisitionService.searchRequisition(requisitionId);
    RequisitionTemplateExtension templateExtension = templateExtensionRepo
        .findByRequisitionTemplateId(storedRequisition.getTemplate().getId());
    if (!templateExtension.getEnablePatientLineItem()) {
      log.info("requisition {} disable patient, skip validation", requisitionId);
      return true;
    }
    log.info("start to validate patient of requisition {}", requisitionId);
    List<PatientGroupDto> patientLineItems = uploadedValue.getPatientLineItems();
    if (patientLineItems == null) {
      context
          .buildConstraintViolationWithTemplate("{javax.validation.constraints.NotNull.message}")
          .addPropertyNode("patientLineItems")
          .addConstraintViolation();
      return false;
    }
    Map<String, UsageTemplateColumnSection> storedGroupMap = columnSectionRepo
        .findByRequisitionTemplateId(storedRequisition.getTemplate().getId()).stream()
        .filter(sectionTemplate -> sectionTemplate.getCategory() == UsageCategory.PATIENT)
        .collect(Collectors.toMap(UsageTemplateColumnSection::getName, Function.identity()));
    Map<String, PatientGroupDto> uploadedGroupMap = uploadedValue.getPatientLineItems().stream()
        .collect(Collectors.toMap(PatientGroupDto::getName, Function.identity()));
    for (String groupName : storedGroupMap.keySet()) {
      PatientGroupDto uploadedGroup = uploadedGroupMap.remove(groupName);
      UsageTemplateColumnSection storedGroup = storedGroupMap.get(groupName);
      if (!validateGroup(uploadedGroup, storedGroup, context, groupName)) {
        return false;
      }
    }
    if (!uploadedGroupMap.isEmpty()) {
      context
          .buildConstraintViolationWithTemplate("extra group:" + uploadedGroupMap.keySet())
          .addConstraintViolation();
      return false;
    }

    return true;
  }

  private boolean validateGroup(PatientGroupDto uploadedGroup,
      UsageTemplateColumnSection storedGroup, ConstraintValidatorContext context,
      String groupName) {
    if (uploadedGroup == null) {
      context
          .buildConstraintViolationWithTemplate("missing group:" + groupName)
          .addConstraintViolation();
      return false;
    }
    List<UsageTemplateColumn> storedColumns = storedGroup.getColumns();
    Map<String, PatientColumnDto> uploadedColumns = new HashMap<>(uploadedGroup.getColumns());
    boolean checkSum = false;
    PatientColumnDto totalColumn = null;
    List<PatientColumnDto> columnsToSum = new ArrayList<>();
    for (UsageTemplateColumn storedColumn : storedColumns) {
      if (!storedColumn.getIsDisplayed()) {
        continue;
      }
      PatientColumnDto uploadedColumn = uploadedColumns.remove(storedColumn.getName());
      if (!validateColumn(uploadedColumn, storedColumn, context, groupName)) {
        return false;
      }
      if (!"CALCULATED".equalsIgnoreCase(storedColumn.getSource())) {
        columnsToSum.add(uploadedColumn);
      } else {
        if (!checkSum) {
          checkSum = true;
          totalColumn = uploadedColumn;
        } else {
          context
              .buildConstraintViolationWithTemplate(
                  "more than 1 total column in group " + groupName)
              .addConstraintViolation();
          return false;
        }
      }
    }
    if (!uploadedColumns.isEmpty()) {
      context
          .buildConstraintViolationWithTemplate(
              "extra column:" + uploadedColumns.keySet() + " in group " + groupName)
          .addConstraintViolation();
      return false;
    }
    return validateColumnsSumOverflowOrNotEqual(columnsToSum, totalColumn, context, groupName);
  }

  private boolean validateColumn(PatientColumnDto uploadedColumn, UsageTemplateColumn storedColumn,
      ConstraintValidatorContext context, String groupName) {
    if (uploadedColumn == null) {
      context
          .buildConstraintViolationWithTemplate(
              "missing column:" + storedColumn.getLabel() + " in group " + groupName)
          .addConstraintViolation();
      return false;
    }
    if (uploadedColumn.getValue() == null) {
      context
          .buildConstraintViolationWithTemplate(
              "missing column value:" + storedColumn.getLabel() + " in group " + groupName)
          .addConstraintViolation();
      return false;
    }
    if (uploadedColumn.getValue() < 0) {
      context
          .buildConstraintViolationWithTemplate(
              "column value:" + storedColumn.getLabel() + " must be positive in group " + groupName)
          .addConstraintViolation();
      return false;
    }
    // not need to check each value's overflow, json framework will do the job
    return true;
  }

  private boolean validateColumnsSumOverflowOrNotEqual(List<PatientColumnDto> columnsToSum,
      PatientColumnDto totalColumn, ConstraintValidatorContext context, String groupName) {
    if (columnsToSum.isEmpty()) {
      context
          .buildConstraintViolationWithTemplate("no columns to sum up in group:" + groupName)
          .addConstraintViolation();
      return false;
    }
    if (totalColumn == null) {
      return true;
    }
    int sum;
    try {
      sum = columnsToSum.stream()
          .mapToInt(PatientColumnDto::getValue)
          .reduce(Math::addExact)
          .getAsInt();
    } catch (ArithmeticException ignore) {
      context
          .buildConstraintViolationWithTemplate("columns sum up overflow in group:" + groupName)
          .addConstraintViolation();
      return false;
    }
    if (totalColumn.getValue() != sum) {
      context
          .buildConstraintViolationWithTemplate(
              "columns sum is not equals to the total column in group:" + groupName)
          .addConstraintViolation();
      return false;
    }
    return true;
  }

}