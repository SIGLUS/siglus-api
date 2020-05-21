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

import static org.openlmis.requisition.domain.RequisitionTemplateColumn.DEFINITION_KEY;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_ADJUSTED_CONSUMPTION_MUST_BE_DISPLAYED_WITH_ADDITIONAL_QUANTITY;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_CANNOT_CALCULATE_AT_THE_SAME_TIME;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_COLUMNS_TAG_NOT_SET;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_COLUMN_SOURCE_INVALID;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_DISPLAYED_WHEN_CALC_ORDER_QUANTITY_EXPLANATION_NOT_DISPLAYED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_DISPLAYED_WHEN_REQUESTED_QUANTITY_EXPLANATION_IS_DISPLAYED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_DISPLAYED_WHEN_REQUESTED_QUANTITY_IS_DISPLAYED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_DISPLAYED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_DISPLAYED_WHEN_AVERAGE_CONSUMPTION_IS_CALCULATED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_DISPLAYED_WHEN_CONSUMED_QUANTITY_IS_CALCULATED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_DISPLAYED_WHEN_CONSUMPTION_IS_CALCULATED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_DISPLAYED_WHEN_ON_HAND_IS_CALCULATED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_ONLY_UTF8_LABEL_IS_ACCEPTED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_OPTION_NOT_AVAILABLE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SOURCE_NOT_AVAILABLE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SOURCE_OF_REQUISITION_TEMPLATE_COLUMN_CANNOT_BE_NULL;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALIDATION_COLUMN_DEFINITION_MODIFIED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALIDATION_COLUMN_DEFINITION_NOT_FOUND;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALIDATION_FIELD_CANNOT_BE_NULL;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALIDATION_FIELD_IS_TOO_LONG;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALIDATION_FIELD_MUST_BE_GREATER_OR_EQUAL;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALIDATION_FIELD_MUST_BE_IN_TEMPLATE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALIDATION_REFERENCED_OBJECT_DOES_NOT_EXIST;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.javers.common.collections.Sets;
import org.openlmis.requisition.domain.AvailableRequisitionColumn;
import org.openlmis.requisition.domain.SourceType;
import org.openlmis.requisition.dto.AvailableRequisitionColumnOptionDto;
import org.openlmis.requisition.dto.RequisitionTemplateColumnDto;
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.openlmis.requisition.repository.AvailableRequisitionColumnRepository;
import org.openlmis.requisition.service.referencedata.FacilityTypeReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.utils.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionTemplateDtoValidator extends BaseValidator {

  static final String COLUMNS_MAP = "columnsMap";
  static final String NUMBER_OF_PERIODS_TO_AVERAGE = "numberOfPeriodsToAverage";
  static final String PROGRAM_ID = "programId";
  static final String FACILITY_TYPE_ID = "facilityTypeId";
  static final String PROGRAM = "program";
  static final String FACILITY_TYPE = "facility type";
  static final String REQUESTED_QUANTITY = "requestedQuantity";
  static final String REQUESTED_QUANTITY_EXPLANATION = "requestedQuantityExplanation";
  static final String TOTAL_CONSUMED_QUANTITY = "totalConsumedQuantity";
  static final String ADJUSTED_CONSUMPTION = "adjustedConsumption";
  static final String AVERAGE_CONSUMPTION = "averageConsumption";
  static final String TOTAL_STOCKOUT_DAYS = "totalStockoutDays";
  static final String STOCK_ON_HAND = "stockOnHand";
  static final String CALCULATED_ORDER_QUANTITY = "calculatedOrderQuantity";
  static final String BEGINNING_BALANCE = "beginningBalance";
  static final String TOTAL_RECEIVED_QUANTITY = "totalReceivedQuantity";
  static final String TOTAL_LOSSES_AND_ADJUSTMENTS = "totalLossesAndAdjustments";
  static final String ADDITIONAL_QUANTITY_REQUIRED = "additionalQuantityRequired";
  static final int MAX_COLUMN_DEFINITION_LENGTH = 140;
  static final Set<String> STOCK_BASED_COLUMNS = Sets.asSet(
      BEGINNING_BALANCE, STOCK_ON_HAND, TOTAL_RECEIVED_QUANTITY, TOTAL_CONSUMED_QUANTITY,
      TOTAL_LOSSES_AND_ADJUSTMENTS, TOTAL_STOCKOUT_DAYS, AVERAGE_CONSUMPTION);

  private Errors errors;

  @Autowired
  private AvailableRequisitionColumnRepository availableRequisitionColumnRepository;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private FacilityTypeReferenceDataService facilityTypeReferenceDataService;

  @Override
  public boolean supports(Class<?> clazz) {
    return RequisitionTemplateDto.class.equals(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    this.errors = errors;

    RequisitionTemplateDto template = (RequisitionTemplateDto) target;

    validateRequestedQuantity(template);
    validateColumns(template);
    validateCalculatedFields(template);

    if (!errors.hasErrors()) {
      validateCalculatedField(template, STOCK_ON_HAND,
          ERROR_MUST_BE_DISPLAYED_WHEN_ON_HAND_IS_CALCULATED, TOTAL_CONSUMED_QUANTITY
      );
      validateCalculatedField(template, TOTAL_CONSUMED_QUANTITY,
          ERROR_MUST_BE_DISPLAYED_WHEN_CONSUMED_QUANTITY_IS_CALCULATED, STOCK_ON_HAND
      );
      validateForAdjustedConsumption(template);
      validateForAverageConsumption(template);
    }

    validateNumberOfPeriodsToAverage(template);

    if (template.isPopulateStockOnHandFromStockCards()) {
      validateStockManagementFields(template);
    }

    if (null == programReferenceDataService.findOne(template.getProgramId())) {
      rejectValue(errors, PROGRAM_ID,
          new Message(ERROR_VALIDATION_REFERENCED_OBJECT_DOES_NOT_EXIST,
              PROGRAM, template.getProgramId()));
    }

    for (UUID facilityTypeId : template.getFacilityTypeIds()) {
      if (null == facilityTypeReferenceDataService.findOne(facilityTypeId)) {
        rejectValue(errors, FACILITY_TYPE_ID,
            new Message(ERROR_VALIDATION_REFERENCED_OBJECT_DOES_NOT_EXIST,
                FACILITY_TYPE, facilityTypeId));
      }
    }
  }

  private void validateRequestedQuantity(RequisitionTemplateDto template) {
    boolean quantityDisplayed = template.isColumnDisplayed(REQUESTED_QUANTITY);
    boolean explanationDisplayed = template.isColumnDisplayed(REQUESTED_QUANTITY_EXPLANATION);
    boolean calcOrderQuantityDisplayed = template.isColumnDisplayed(CALCULATED_ORDER_QUANTITY);

    if (quantityDisplayed) {
      if (!explanationDisplayed) {
        rejectValue(errors, COLUMNS_MAP,
            new Message(ERROR_DISPLAYED_WHEN_REQUESTED_QUANTITY_IS_DISPLAYED,
                REQUESTED_QUANTITY_EXPLANATION));
      }
    } else {
      if (explanationDisplayed) {
        rejectValue(errors, COLUMNS_MAP,
            new Message(ERROR_DISPLAYED_WHEN_REQUESTED_QUANTITY_EXPLANATION_IS_DISPLAYED,
                REQUESTED_QUANTITY));
      }
    }
    if (!calcOrderQuantityDisplayed && !quantityDisplayed) {
      rejectValue(errors, COLUMNS_MAP,
          new Message(ERROR_DISPLAYED_WHEN_CALC_ORDER_QUANTITY_EXPLANATION_NOT_DISPLAYED,
              REQUESTED_QUANTITY));
    }
  }

  private void validateCalculatedFields(RequisitionTemplateDto template) {
    if (template.isColumnInTemplate(TOTAL_CONSUMED_QUANTITY)
        && template.isColumnCalculated(TOTAL_CONSUMED_QUANTITY)
        && template.isColumnInTemplate(STOCK_ON_HAND)
        && template.isColumnCalculated(STOCK_ON_HAND)) {
      rejectValue(errors, COLUMNS_MAP, new Message(ERROR_CANNOT_CALCULATE_AT_THE_SAME_TIME,
          TOTAL_CONSUMED_QUANTITY, STOCK_ON_HAND));
    }
  }

  private void validateCalculatedField(RequisitionTemplateDto template, String field,
                                       String suffix, String... requiredFields) {
    if (template.isColumnInTemplate(field) && template.isColumnCalculated(field)) {
      for (String requiredField : requiredFields) {
        if (template.isColumnInTemplate(requiredField)
            && template.isColumnUserInput(requiredField)) {
          rejectIfNotDisplayed(errors, template, requiredField, COLUMNS_MAP,
              new Message(suffix, requiredField));
        }
      }
    }
  }

  private void validateColumns(RequisitionTemplateDto template) {
    for (RequisitionTemplateColumnDto column : template.getColumnsMap().values()) {
      rejectIfNotUtf8(
          errors, column.getLabel(), COLUMNS_MAP,
          new Message(ERROR_ONLY_UTF8_LABEL_IS_ACCEPTED, column.getName())
      );

      validateColumnDefinition(column);
      validateChosenSources(template, column);

      Set<AvailableRequisitionColumnOptionDto> options = column
          .getColumnDefinition()
          .getOptions();
      Optional.ofNullable(column.getOption())
          .ifPresent(option -> rejectIfNotContains(errors, options, option, COLUMNS_MAP,
              new Message(ERROR_OPTION_NOT_AVAILABLE, option.getOptionName(), column.getName())));

      rejectIfLengthTooLong(
          errors, column.getDefinition(), MAX_COLUMN_DEFINITION_LENGTH, COLUMNS_MAP,
          new Message(ERROR_VALIDATION_FIELD_IS_TOO_LONG, DEFINITION_KEY,
              MAX_COLUMN_DEFINITION_LENGTH)
      );
    }
  }

  private void validateColumnDefinition(RequisitionTemplateColumnDto column) {
    AvailableRequisitionColumn actual = AvailableRequisitionColumn
        .newInstance(column.getColumnDefinition());
    AvailableRequisitionColumn expected = availableRequisitionColumnRepository
        .findOne(actual.getId());

    if (null == expected) {
      rejectValue(
          errors, COLUMNS_MAP,
          new Message(ERROR_VALIDATION_COLUMN_DEFINITION_NOT_FOUND, column.getLabel())
      );
    } else if (!expected.equals(actual)) {
      rejectValue(
          errors, COLUMNS_MAP,
          new Message(ERROR_VALIDATION_COLUMN_DEFINITION_MODIFIED, column.getLabel())
      );
    }
  }

  private void validateChosenSources(RequisitionTemplateDto template,
                                     RequisitionTemplateColumnDto column) {
    SourceType chosenSource = column.getSource();

    if (chosenSource == null) {
      rejectValue(errors, COLUMNS_MAP,
          new Message(ERROR_SOURCE_OF_REQUISITION_TEMPLATE_COLUMN_CANNOT_BE_NULL,
              column.getLabel()));
    } else {
      AvailableRequisitionColumn.Importer definition = column.getColumnDefinition();
      Set<SourceType> sources = definition.getSources();

      if (sources.size() > 1 && template.isColumnUserInput(definition.getName())) {
        rejectIfNotDisplayed(
            errors, template, definition.getName(), COLUMNS_MAP,
            new Message(ERROR_MUST_BE_DISPLAYED, definition.getName())
        );
      }
      rejectIfNotContains(
          errors, sources, chosenSource, COLUMNS_MAP,
          new Message(ERROR_SOURCE_NOT_AVAILABLE, chosenSource.toString(), column.getName())
      );
    }
  }

  private void validateStockManagementFields(RequisitionTemplateDto template) {
    for (String columnName : STOCK_BASED_COLUMNS) {
      if (template.isColumnInTemplate(columnName)) {
        RequisitionTemplateColumnDto column = template.getColumnsMap().get(columnName);

        if (!SourceType.STOCK_CARDS.equals(column.getSource())) {
          rejectValue(errors, COLUMNS_MAP,
              new Message(ERROR_COLUMN_SOURCE_INVALID, column.getLabel(), SourceType.STOCK_CARDS,
                  column.getSource()));
        }
      }
    }

    validateStockBasedColumn(template, TOTAL_CONSUMED_QUANTITY);
    validateStockBasedColumn(template, TOTAL_RECEIVED_QUANTITY);
    validateStockBasedColumn(template, TOTAL_LOSSES_AND_ADJUSTMENTS);
    validateStockBasedColumn(template, TOTAL_STOCKOUT_DAYS);
  }

  private void validateForAdjustedConsumption(RequisitionTemplateDto template) {
    if (template.isColumnInTemplate(ADJUSTED_CONSUMPTION)) {
      rejectIfStockoutDaysOrConsumedQuantityNotInTemplate(
          template, ADJUSTED_CONSUMPTION);
      if (template.isColumnDisplayed(ADJUSTED_CONSUMPTION)) {
        validateCalculatedField(template, ADJUSTED_CONSUMPTION,
            ERROR_MUST_BE_DISPLAYED_WHEN_CONSUMPTION_IS_CALCULATED, TOTAL_CONSUMED_QUANTITY,
            TOTAL_STOCKOUT_DAYS);
      }
    } else if (template.isColumnDisplayed(ADDITIONAL_QUANTITY_REQUIRED)) {
      rejectValue(errors, ADDITIONAL_QUANTITY_REQUIRED,
          new Message(ERROR_ADJUSTED_CONSUMPTION_MUST_BE_DISPLAYED_WITH_ADDITIONAL_QUANTITY));
    }
  }

  private void validateForAverageConsumption(RequisitionTemplateDto template) {
    if (template.isColumnInTemplate(AVERAGE_CONSUMPTION)) {
      rejectIfStockoutDaysOrConsumedQuantityNotInTemplate(template, AVERAGE_CONSUMPTION);
      if (template.isColumnDisplayed(AVERAGE_CONSUMPTION)) {
        validateCalculatedField(template, AVERAGE_CONSUMPTION,
            ERROR_MUST_BE_DISPLAYED_WHEN_AVERAGE_CONSUMPTION_IS_CALCULATED, TOTAL_CONSUMED_QUANTITY,
            TOTAL_STOCKOUT_DAYS);
      }
      rejectIfNumberOfPeriodsToAverageIsNull(template);
    }
  }

  private void validateStockBasedColumn(RequisitionTemplateDto template, String column) {
    if (template.isPopulateStockOnHandFromStockCards()
        && template.findColumn(column).getTag() == null) {
      new Message(ERROR_COLUMNS_TAG_NOT_SET, template.findColumn(column).getLabel());
    }
  }

  private void rejectIfStockoutDaysOrConsumedQuantityNotInTemplate(
      RequisitionTemplateDto template, String validatedColumn) {
    if (!template.isColumnInTemplate(TOTAL_CONSUMED_QUANTITY)) {
      rejectValue(errors, COLUMNS_MAP,
          new Message(ERROR_VALIDATION_FIELD_MUST_BE_IN_TEMPLATE,
              TOTAL_CONSUMED_QUANTITY, validatedColumn));
    }
    if (!template.isColumnInTemplate(TOTAL_STOCKOUT_DAYS)) {
      rejectValue(errors, COLUMNS_MAP,
          new Message(ERROR_VALIDATION_FIELD_MUST_BE_IN_TEMPLATE,
              TOTAL_STOCKOUT_DAYS, validatedColumn));
    }
  }


  private void rejectIfNumberOfPeriodsToAverageIsNull(RequisitionTemplateDto template) {
    if (template.getNumberOfPeriodsToAverage() == null) {
      rejectValue(errors, NUMBER_OF_PERIODS_TO_AVERAGE,
          new Message(ERROR_VALIDATION_FIELD_CANNOT_BE_NULL,
              NUMBER_OF_PERIODS_TO_AVERAGE, AVERAGE_CONSUMPTION));
    }
  }

  private void validateNumberOfPeriodsToAverage(RequisitionTemplateDto template) {
    if (template.getNumberOfPeriodsToAverage() != null
        && template.getNumberOfPeriodsToAverage() < 2) {
      rejectValue(errors, NUMBER_OF_PERIODS_TO_AVERAGE,
          new Message(ERROR_VALIDATION_FIELD_MUST_BE_GREATER_OR_EQUAL,
              NUMBER_OF_PERIODS_TO_AVERAGE, "2"));
    }
  }

  private void rejectIfNotDisplayed(Errors errors, RequisitionTemplateDto template, String column,
                                    String field, Message message) {
    if (!template.isColumnDisplayed(column)) {
      rejectValue(errors, field, message);
    }
  }

  private void rejectIfDisplayed(Errors errors, RequisitionTemplateDto template, String column,
                                 String field, Message message) {
    if (template.isColumnDisplayed(column)) {
      rejectValue(errors, field, message);
    }
  }
}
