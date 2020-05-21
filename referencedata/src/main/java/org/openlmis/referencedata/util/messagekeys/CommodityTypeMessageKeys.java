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

public abstract class CommodityTypeMessageKeys extends MessageKeys {
  private static final String ERROR = join(SERVICE_ERROR, COMMODITY_TYPE);
  private static final String TRADE_ITEMS = "tradeItems";
  private static final String PARENT = "parent";
  private static final String IS_DESCENDANT = "isDescendant";

  public static final String ERROR_NULL = join(ERROR, NULL);
  public static final String ERROR_NOT_FOUND = join(ERROR, NOT_FOUND);
  public static final String ERROR_TRADE_ITEMS_NULL = join(ERROR, TRADE_ITEMS, NULL);
  public static final String ERROR_PARENT_IS_DESCENDANT = join(ERROR, PARENT, IS_DESCENDANT);
  public static final String ERROR_PARENT_NOT_FOUND = join(ERROR, PARENT, NOT_FOUND);
  public static final String ERROR_NAME_REQUIRED = join(ERROR, NAME, REQUIRED);
  public static final String ERROR_CLASSIFICATION_SYSTEM_REQUIRED =
      join(ERROR, "classificationSystem", REQUIRED);
  public static final String ERROR_CLASSIFICATION_ID_REQUIRED =
      join(ERROR, "classificationId", REQUIRED);

}
