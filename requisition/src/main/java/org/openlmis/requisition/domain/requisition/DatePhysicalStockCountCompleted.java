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

import static org.openlmis.requisition.domain.requisition.Requisition.DATE_PHYSICAL_STOCK_COUNT_COMPLETED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_DATE_STOCK_COUNT_IS_IN_FUTURE;

import java.time.LocalDate;
import java.util.Map;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.openlmis.requisition.utils.Message;

@Embeddable
@EqualsAndHashCode
public class DatePhysicalStockCountCompleted {
  @Getter
  private final LocalDate localDate;

  /**
   * Creates a new DatePhysicalStockCountCompleted.
   *
   * @param date the local code
   */
  public DatePhysicalStockCountCompleted(@NotNull LocalDate date) {
    if (date == null) {
      throw new IllegalArgumentException("Date can't be null");
    }
    this.localDate = date;
  }

  private DatePhysicalStockCountCompleted() {
    //Hibernate will replace this with proper date using reflection,
    //LocalDate.MIN used instead of null to satisfy static code analyzers
    localDate = LocalDate.MIN;
  }

  void validateNotInFuture(Map<String, Message> errors, LocalDate currentDate) {
    if (localDate.isAfter(currentDate)) {
      errors.put(DATE_PHYSICAL_STOCK_COUNT_COMPLETED,
          new Message(ERROR_DATE_STOCK_COUNT_IS_IN_FUTURE));
    }
  }

}
