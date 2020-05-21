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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.openlmis.requisition.domain.requisition.Requisition.REQUISITION_LINE_ITEMS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_IS_HIDDEN;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_MUST_BE_NON_NEGATIVE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_VALUE_MUST_BE_ENTERED;

import java.util.Map;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.utils.Message;

/**
 * Interface that all domain validators should implement.
 */
interface RequisitionStatusChangeDomainValidator {

  /**
   * Call given validators.
   *
   * @param errors a map where errors will be put.
   */
  void validateCanChangeStatus(Map<String, Message> errors);

  boolean isForRegularOnly();

  boolean isForApprove();

  default void rejectIfNullOrNegative(Map<String, Message> errors, RequisitionTemplate template,
                                      Integer value, String field) {
    rejectIfLessThanZero(errors, template, value, field);
    rejectIfNull(errors, template, value, field);
  }

  default void rejectIfLessThanZero(Map<String, Message> errors, RequisitionTemplate template,
                                    Integer value, String field) {
    boolean columnDisplayed = template.isColumnDisplayed(field);

    rejectIfNonNullValueForHiddenColumn(errors, value, field, columnDisplayed);

    if (columnDisplayed && value != null && value < 0) {
      errors.put(REQUISITION_LINE_ITEMS, new Message(ERROR_MUST_BE_NON_NEGATIVE, field));
    }
  }

  default void rejectIfNull(Map<String, Message> errors, RequisitionTemplate template,
                            Object value, String field) {
    boolean columnDisplayed = template.isColumnDisplayed(field);

    rejectIfNonNullValueForHiddenColumn(errors, value, field, columnDisplayed);

    if (columnDisplayed && value == null) {
      errors.put(REQUISITION_LINE_ITEMS, new Message(ERROR_VALUE_MUST_BE_ENTERED, field));
    }
  }

  default void rejectIfEmpty(Map<String, Message> errors, RequisitionTemplate template,
                             String value, String field) {
    boolean columnDisplayed = template.isColumnDisplayed(field);

    rejectIfNonNullValueForHiddenColumn(errors, value, field, columnDisplayed);

    if (columnDisplayed && isBlank(value)) {
      errors.put(REQUISITION_LINE_ITEMS, new Message(ERROR_VALUE_MUST_BE_ENTERED, field));
    }
  }

  default void rejectIfNonNullValueForHiddenColumn(Map<String, Message> errors, Object value,
                                                   String field, boolean columnDisplayed) {
    if (!columnDisplayed && value != null) {
      errors.put(REQUISITION_LINE_ITEMS, new Message(ERROR_IS_HIDDEN, field));
    }
  }

}
