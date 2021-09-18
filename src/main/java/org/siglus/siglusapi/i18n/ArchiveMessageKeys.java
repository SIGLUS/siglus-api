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

public class ArchiveMessageKeys extends MessageKeys {

  private static final String ARCHIVE = "archive";

  private static final String ERROR_ARCHIVE_PREFIX = join(ERROR_PREFIX, ARCHIVE);

  public static final String ERROR_ARCHIVE_CANNOT_ARCHIVE_ORDERABLE_IN_KIT = ERROR_ARCHIVE_PREFIX
      + ".cannot.archive.orderable.in.kit";

  public static final String ERROR_ARCHIVE_SOH_SHOULD_BE_ZERO = ERROR_ARCHIVE_PREFIX
      + ".stockOnHand.should.be.zero";

  public static final String ERROR_ARCHIVE_ALREADY_ARCHIVED = ERROR_ARCHIVE_PREFIX
      + ".already.archived";
}
