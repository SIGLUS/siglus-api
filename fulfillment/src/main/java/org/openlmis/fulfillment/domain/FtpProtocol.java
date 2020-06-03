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

package org.openlmis.fulfillment.domain;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.util.Arrays;

public enum FtpProtocol {
  FTP, SFTP, FTPS;

  /**
   * Find correct FTP protocol by the given string.
   *
   * @param protocol string representation of FTP protocol.
   * @return {@link FtpProtocol} that is equal to the given string.
   */
  public static FtpProtocol fromString(String protocol) {
    return Arrays.stream(values())
        .filter(p -> equalsIgnoreCase(protocol, p.name()))
        .findFirst()
        .orElse(null);
  }
}
