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

package org.openlmis.requisition.settings.service;

import java.util.UUID;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
public class ConfigurationSettingService {

  private static final String RESONS_SUFFIX = "reasons.";
  private static final String CONSUMED = RESONS_SUFFIX + "consumed";
  private static final String RECEIPTS = RESONS_SUFFIX + "receipts";
  private static final String BEGINNING_BALANCE_EXCESS = RESONS_SUFFIX + "beginningBalanceExcess";
  private static final String BEGINNING_BALANCE_INSUFFICIENCY =
      RESONS_SUFFIX + "beginningBalanceInsufficiency";

  @Autowired
  private Environment env;

  public UUID getReasonIdForConsumed() {
    return UUID.fromString(env.getProperty(CONSUMED));
  }

  public UUID getReasonIdForReceipts() {
    return UUID.fromString(env.getProperty(RECEIPTS));
  }

  public UUID getReasonIdForBeginningBalanceExcess() {
    return UUID.fromString(env.getProperty(BEGINNING_BALANCE_EXCESS));
  }

  public UUID getReasonIdForBeginningBalanceInsufficiency() {
    return UUID.fromString(env.getProperty(BEGINNING_BALANCE_INSUFFICIENCY));
  }
}
