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

import static org.openlmis.requisition.domain.requisition.Requisition.REQUISITION_LINE_ITEMS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_FIELD_IS_CALCULATED;

import java.util.Map;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.utils.Message;

/**
 * Interface that all domain validators should implement.
 */
interface RequisitionUpdateDomainValidator {

  /**
   * Call given validators.
   *
   * @param errors a map where errors will be put.
   */
  void validateCanUpdate(Map<String, Message> errors);

  boolean isForRegularOnly();

  /**
   * Returns validator name.
   */
  default String getName() {
    return getClass().getSimpleName();
  }

  /**
   * Rejects if template field is null and calculated.
   */
  default void rejectIfCalculatedAndNotNull(Map<String, Message> errors,
                                            RequisitionTemplate template,
                                            Object value, String field) {
    if (template.isColumnCalculated(field) && value != null) {
      errors.put(REQUISITION_LINE_ITEMS, new Message(ERROR_FIELD_IS_CALCULATED, field));
    }
  }

}
