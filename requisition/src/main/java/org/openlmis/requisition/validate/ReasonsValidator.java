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

import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.BEGINNING_BALANCE;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_CONSUMED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.TOTAL_RECEIVED_QUANTITY;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SPECIAL_REASON_NOT_VALID;

import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.StockAdjustmentReason;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.settings.service.ConfigurationSettingService;
import org.openlmis.requisition.utils.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReasonsValidator {

  @Autowired
  private ConfigurationSettingService configurationSettingService;

  /**
   * Validate special reasons are in reason list when showed in template.
   */
  public void validate(List<StockAdjustmentReason> reasons, RequisitionTemplate template) {
    checkReason(reasons, template, TOTAL_CONSUMED_QUANTITY,
        configurationSettingService.getReasonIdForConsumed());
    checkReason(reasons, template, TOTAL_RECEIVED_QUANTITY,
        configurationSettingService.getReasonIdForReceipts());
    checkReason(reasons, template, BEGINNING_BALANCE,
        configurationSettingService.getReasonIdForBeginningBalanceExcess());
    checkReason(reasons, template, BEGINNING_BALANCE,
        configurationSettingService.getReasonIdForBeginningBalanceInsufficiency());
  }

  private void checkReason(List<StockAdjustmentReason> reasons, RequisitionTemplate template,
                           String column, UUID reasonId) {
    if (template.isColumnInTemplateAndDisplayed(column)
        && reasons.parallelStream().noneMatch(r -> r.getReasonId().equals(reasonId))) {
      throw new ValidationMessageException(
          new Message(ERROR_SPECIAL_REASON_NOT_VALID, reasonId));
    }
  }
}
