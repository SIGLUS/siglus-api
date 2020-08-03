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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.UsageGroupDto;
import org.siglus.siglusapi.repository.UsageTemplateColumnSectionRepository;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;

/**
 * Validate usage line items in {@linkplain SiglusRequisitionDto SiglusRequisitionDto}.
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
@RequiredArgsConstructor
@Slf4j
public abstract class UsageLineItemValidator<A extends Annotation, G extends UsageGroupDto<C>, C>
    implements ConstraintValidator<A, SiglusRequisitionDto> {

  private static final String GROUP_NAME = "groupName";

  private final RequisitionTemplateExtensionRepository templateExtensionRepo;

  private final SiglusRequisitionRequisitionService requisitionService;

  private final UsageTemplateColumnSectionRepository columnSectionRepo;

  private final UsageCategory usageCategory;

  private final String propertyPath;

  @Override
  public void initialize(A constraintAnnotation) {
    // empty init
  }

  @Override
  public boolean isValid(SiglusRequisitionDto uploadedValue, ConstraintValidatorContext context) {
    context.disableDefaultConstraintViolation();
    UUID requisitionId = uploadedValue.getId();
    RequisitionV2Dto storedRequisition = requisitionService.searchRequisition(requisitionId);
    UUID templateId = storedRequisition.getTemplate().getId();
    RequisitionTemplateExtension templateExtension = templateExtensionRepo
        .findByRequisitionTemplateId(templateId);
    if (!checkEnable(templateExtension)) {
      log.info("requisition {} disable {}, skip validation", requisitionId,
          usageCategory.getName());
      return true;
    }
    if (storedRequisition.getEmergency()) {
      log.info("requisition {} is emergency, skip validation {}", requisitionId,
          usageCategory.getName());
      return true;
    }
    log.info("start to validate {} of requisition {}", usageCategory.getName(), requisitionId);
    HibernateConstraintValidatorContext actualContext =
        context.unwrap(HibernateConstraintValidatorContext.class);
    return validateGroups(uploadedValue, templateId, actualContext);
  }

  protected abstract boolean checkEnable(RequisitionTemplateExtension templateExtension);

  protected abstract Collection<G> getUploadedGroups(SiglusRequisitionDto uploadedValue);

  protected abstract int mapColumnToInt(C column);

  protected boolean isThisColumnTotalColumn(UsageTemplateColumn column) {
    return "CALCULATED".equalsIgnoreCase(column.getSource());
  }

  private boolean validateGroups(SiglusRequisitionDto uploadedValue, UUID templateId,
      HibernateConstraintValidatorContext context) {
    Map<String, UsageTemplateColumnSection> storedGroupMap = columnSectionRepo
        .findByRequisitionTemplateId(templateId).stream()
        .filter(sectionTemplate -> sectionTemplate.getCategory() == usageCategory)
        .collect(Collectors.toMap(UsageTemplateColumnSection::getName, Function.identity()));
    Collection<G> uploadedGroups = getUploadedGroups(uploadedValue);
    Map<String, G> uploadedGroupMap = uploadedGroups.stream()
        .collect(Collectors.toMap(G::getName, Function.identity(),
            (oldValue, newValue) -> {
              throw new IllegalStateException(String.format("Duplicate key %s", oldValue));
            }, LinkedHashMap::new)
        );
    int index = 0;
    for (Entry<String, UsageTemplateColumnSection> group : storedGroupMap.entrySet()) {
      int groupIndex = index++;
      String groupName = group.getKey();
      G uploadedGroup = uploadedGroupMap.remove(groupName);
      UsageTemplateColumnSection storedGroup = group.getValue();
      if (!validateGroup(uploadedGroup, storedGroup, context, groupName, groupIndex)) {
        return false;
      }
    }
    if (!uploadedGroupMap.isEmpty()) {
      context
          .addExpressionVariable("groupNames", uploadedGroupMap.keySet())
          .buildConstraintViolationWithTemplate(
              "{}siglus.validation.requisition.usageLineItems.extraGroups")
          .addPropertyNode(propertyPath)
          .addConstraintViolation();
      return false;
    }
    return true;
  }

  private boolean validateGroup(G uploadedGroup,
      UsageTemplateColumnSection storedGroup, HibernateConstraintValidatorContext context,
      String groupName, int groupIndex) {
    if (uploadedGroup == null) {
      context
          .addExpressionVariable(GROUP_NAME, groupName)
          .buildConstraintViolationWithTemplate(
              "{siglus.validation.requisition.usageLineItems.missingGroup}")
          .addPropertyNode(propertyPath)
          .addConstraintViolation();
      return false;
    }
    List<UsageTemplateColumn> storedColumns = storedGroup.getColumns();
    Map<String, C> uploadedColumns = new HashMap<>(uploadedGroup.getColumns());
    boolean checkSum = false;
    C totalColumn = null;
    List<C> columnsToSum = new ArrayList<>();
    for (UsageTemplateColumn storedColumn : storedColumns) {
      if (!storedColumn.getIsDisplayed()) {
        continue;
      }
      C uploadedColumn = uploadedColumns.remove(storedColumn.getName());
      if (!validateColumn(uploadedColumn, storedColumn, context, groupName, groupIndex)) {
        return false;
      }
      if (isThisColumnTotalColumn(storedColumn)) {
        if (!checkSum) {
          checkSum = true;
          totalColumn = uploadedColumn;
        } else {
          context
              .addExpressionVariable(GROUP_NAME, groupName)
              .buildConstraintViolationWithTemplate(
                  "{siglus.validation.requisition.usageLineItems.columns.duplicateTotalColumns}")
              .addConstraintViolation();
          return false;
        }
      } else {
        columnsToSum.add(uploadedColumn);
      }
    }
    if (!uploadedColumns.isEmpty()) {
      context
          .addExpressionVariable("columnNames", uploadedColumns.keySet())
          .addExpressionVariable(GROUP_NAME, groupName)
          .buildConstraintViolationWithTemplate(
              "{siglus.validation.requisition.usageLineItems.columns.extraColumns}")
          .addConstraintViolation();
      return false;
    }
    return validateColumnsSumOverflowOrNotEqual(columnsToSum, totalColumn, context, groupName);
  }

  private boolean validateColumn(C uploadedColumn, UsageTemplateColumn storedColumn,
      HibernateConstraintValidatorContext context, String groupName, int groupIndex) {
    if (uploadedColumn == null) {
      NodeBuilderCustomizableContext propertyNode = context
          .addExpressionVariable(GROUP_NAME, groupName)
          .addExpressionVariable("columnName", storedColumn.getName())
          .buildConstraintViolationWithTemplate(
              "{siglus.validation.requisition.patientLineItems.columns.missingColumn}")
          .addPropertyNode(propertyPath).addPropertyNode("columns");
      propertyNode.inIterable().atIndex(groupIndex).addConstraintViolation();
      return false;
    }
    // not need to check each value's overflow, json framework will do the job
    return true;
  }

  private boolean validateColumnsSumOverflowOrNotEqual(List<C> columnsToSum,
      C totalColumn, HibernateConstraintValidatorContext context, String groupName) {
    if (totalColumn == null) {
      return true;
    }
    if (columnsToSum.isEmpty()) {
      context
          .addExpressionVariable(GROUP_NAME, groupName)
          .buildConstraintViolationWithTemplate(
              "{siglus.validation.requisition.usageLineItems.columns.missingNonTotalColumns}")
          .addConstraintViolation();
      return false;
    }
    int sum;
    try {
      sum = columnsToSum.stream()
          .mapToInt(this::mapColumnToInt)
          .reduce(Math::addExact)
          .getAsInt();
    } catch (ArithmeticException ignore) {
      context
          .addExpressionVariable(GROUP_NAME, groupName)
          .buildConstraintViolationWithTemplate(
              "{siglus.validation.requisition.usageLineItems.columns.sumUpOverflow}")
          .addConstraintViolation();
      return false;
    }
    if (mapColumnToInt(totalColumn) != sum) {
      context
          .addExpressionVariable(GROUP_NAME, groupName)
          .buildConstraintViolationWithTemplate(
              "{siglus.validation.requisition.usageLineItems.columns.sumUpNotMatch}")
          .addConstraintViolation();
      return false;
    }
    return true;
  }

}