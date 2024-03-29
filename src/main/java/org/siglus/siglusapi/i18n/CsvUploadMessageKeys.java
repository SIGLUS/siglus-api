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

package org.siglus.siglusapi.i18n;

import static org.siglus.siglusapi.i18n.MessageKeys.SERVICE_ERROR;

public class CsvUploadMessageKeys {
  private static final String ERROR_PREFIX = SERVICE_ERROR + ".upload";
  public static final String ERROR_FILE_IS_EMPTY = ERROR_PREFIX + ".file.empty";
  public static final String ERROR_INCORRECT_FILE_FORMAT = ERROR_PREFIX + ".file.format.incorrect";
  public static final String ERROR_UPLOAD_HEADER_MISSING = ERROR_PREFIX + ".header.missing";
  public static final String ERROR_UPLOAD_MISSING_ROW = ERROR_PREFIX + ".row.missing";
  public static final String ERROR_UPLOAD_HEADER_INVALID = ERROR_PREFIX + ".header.invalid";
  public static final String ERROR_DUPLICATE_LOCATION_CODE = ERROR_PREFIX + ".duplicate.locationCode";
  public static final String ERROR_IO = ERROR_PREFIX + ".io.error";

  private CsvUploadMessageKeys() {
    throw new UnsupportedOperationException();
  }
}
