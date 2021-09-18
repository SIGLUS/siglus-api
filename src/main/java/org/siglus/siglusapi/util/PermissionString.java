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

package org.siglus.siglusapi.util;

import static java.util.Optional.ofNullable;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public final class PermissionString {

  private static final Pattern PATTERN = Pattern.compile("^([^|]*)(\\|([^|]*)(\\|([^|]*))?)?$");

  private final String rightName;
  private final UUID facilityId;
  private final UUID programId;

  public PermissionString(String permissionString) {
    Matcher matcher = PATTERN.matcher(permissionString);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(permissionString);
    }
    rightName = matcher.group(1);
    facilityId = ofNullable(matcher.group(3)).map(UUID::fromString).orElse(null);
    programId = ofNullable(matcher.group(5)).map(UUID::fromString).orElse(null);
  }

}
