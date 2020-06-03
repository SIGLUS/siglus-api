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

package org.openlmis.fulfillment.web.validator;

import static java.util.Arrays.asList;

import java.util.List;
import org.openlmis.fulfillment.domain.FileColumn;
import org.openlmis.fulfillment.domain.TemplateType;
import org.openlmis.fulfillment.i18n.MessageKeys;
import org.openlmis.fulfillment.web.util.FileTemplateDto;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class FileTemplateValidator extends BaseValidator implements Validator {

  private static final String INVALID_FORMAT_DATE = "Invalid date format";

  private static final String[] ACCEPTED_VALUES = {"MM/yy", "MM/yyyy", "yy/MM", "yyyy/MM",
      "dd/MM/yy", "dd/MM/yyyy", "MM/dd/yy", "MM/dd/yyyy", "yy/MM/dd", "yyyy/MM/dd", "MM-yy",
      "MM-yyyy", "yy-MM", "yyyy-MM", "dd-MM-yy", "dd-MM-yyyy", "MM-dd-yy", "MM-dd-yyyy", "yy-MM-dd",
      "yyyy-MM-dd", "MMyy", "MMyyyy", "yyMM", "yyyyMM", "ddMMyy", "ddMMyyyy", "MMddyy", "MMddyyyy",
      "yyMMdd", "yyyyMMdd"};

  private static final List<String> ORDERABLE_COLUMN_NAMES = asList("orderableId", "productCode");
  private static final List<String> ORDER_IDENTIFIER_COLUMN_NAMES = asList("orderCode", "orderId");
  private static final List<String> SHIPPED_QUANTITY_COLUMN_NAMES = asList("quantityShipped");

  @Override
  public boolean supports(Class<?> clazz) {
    return FileTemplateDto.class.equals(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    FileTemplateDto fileTemplate = (FileTemplateDto) target;
    List<FileColumn.Importer> columns = fileTemplate.getFileColumns();
    List<String> acceptedValues = asList(ACCEPTED_VALUES);

    for (int i = 0; i < columns.size(); i++) {
      FileColumn.Importer orderFileColumn = columns.get(i);
      if ((orderFileColumn.getFormat() != null)
          && (!acceptedValues.contains(orderFileColumn.getFormat()))) {
        errors.rejectValue("fileColumns[" + i + "].format",
            INVALID_FORMAT_DATE, INVALID_FORMAT_DATE);
      }
    }
    if (TemplateType.SHIPMENT.equals(fileTemplate.getTemplateType())) {
      validateShipmentTemplate(fileTemplate, errors);
    }
  }

  private void validateShipmentTemplate(FileTemplateDto shipmentTemplate, Errors errors) {
    validateColumnPresence(shipmentTemplate, ORDER_IDENTIFIER_COLUMN_NAMES, errors);
    validateColumnPresence(shipmentTemplate, ORDERABLE_COLUMN_NAMES, errors);
    validateColumnPresence(shipmentTemplate, SHIPPED_QUANTITY_COLUMN_NAMES, errors);
  }

  private void validateColumnPresence(FileTemplateDto shipmentTemplate,
      List<String> expectedColumnKeys, Errors errors) {
    long columnCount = shipmentTemplate
        .getFileColumns()
        .stream()
        .filter(c -> expectedColumnKeys.contains(c.getKeyPath()))
        .count();
    if (columnCount == 0L) {
      errors.reject(MessageKeys.ERROR_MISSING_REQUIRED_COLUMN,
          null,
          getErrorMessage(MessageKeys.ERROR_MISSING_REQUIRED_COLUMN,
              String.join("/", expectedColumnKeys)).getMessage()
      );
    } else if (columnCount > 1) {
      errors.reject(MessageKeys.ERROR_DUPLICATE_COLUMN_FOUND,
          null,
          getErrorMessage(MessageKeys.ERROR_DUPLICATE_COLUMN_FOUND,
              String.join("/", expectedColumnKeys)).getMessage());
    }
  }
}



