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

import static org.openlmis.referencedata.util.messagekeys.MessageKeys.COMMODITY_TYPE;
import static org.openlmis.referencedata.util.messagekeys.MessageKeys.FACILITY;
import static org.openlmis.referencedata.util.messagekeys.MessageKeys.NOT_FOUND;
import static org.openlmis.referencedata.util.messagekeys.MessageKeys.PROCESSING_PERIOD;
import static org.openlmis.referencedata.util.messagekeys.MessageKeys.REQUIRED;
import static org.openlmis.referencedata.util.messagekeys.MessageKeys.SERVICE_ERROR;
import static org.openlmis.referencedata.util.messagekeys.MessageKeys.join;

public class IdealStockAmountMessageKeys {
  private static final String ERROR = join(SERVICE_ERROR, "idealStockAmount");

  public static final String ERROR_FORMAT_NOT_ALLOWED = join(ERROR, "format.notAllowed");
  public static final String ERROR_FROM_FIELD_REQUIRED = join(ERROR, "field", REQUIRED);

  public static final String ERROR_FACILITY_NOT_FOUND = join(ERROR, FACILITY, NOT_FOUND);
  public static final String ERROR_PROCESSING_PERIOD_NOT_FOUND = join(ERROR,
      PROCESSING_PERIOD, NOT_FOUND);
  public static final String ERROR_COMMODITY_TYPE_NOT_FOUND = join(ERROR,
      COMMODITY_TYPE, NOT_FOUND);
}
