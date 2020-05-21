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

package org.openlmis.referencedata.util.messagekeys;

public abstract class TradeItemMessageKeys extends MessageKeys {
  private static final String ERROR = join(SERVICE_ERROR, TRADE_ITEM);

  public static final String ERROR_NOT_FOUND_WITH_ID = join(ERROR, NOT_FOUND, WITH, ID);
  public static final String ERROR_NULL = join(ERROR, NULL);

  public static final String ERROR_MANUFACTURER_REQUIRED =
      join(ERROR, "manufacturerOfTradeItem", REQUIRED);

  public static final String ERROR_GTIN_NUMERIC =
      join(ERROR, "gtin", NUMERIC);
  public static final String ERROR_GTIN_INVALID_LENGTH =
      join(ERROR, "gtin", INVALID_LENGTH);
  public static final String ERROR_GTIN_DUPLICATED =
      join(ERROR, "gtin", DUPLICATED);
}
