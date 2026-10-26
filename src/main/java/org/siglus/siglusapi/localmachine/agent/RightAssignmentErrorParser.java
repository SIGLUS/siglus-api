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

package org.siglus.siglusapi.localmachine.agent;

import org.siglus.siglusapi.dto.RightAssignmentConflictKey;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RightAssignmentErrorParser {

  private static final String TARGET_CONSTRAINT = "right_assignment_unq";

  private static final Pattern TARGET_TABLE_PATTERN = Pattern.compile(
          "\"?referencedata\"?\\s*\\.\\s*\"?right_assignments\"?",
          Pattern.CASE_INSENSITIVE
  );

  private static final Pattern PG_KEY_DETAIL_PATTERN = Pattern.compile(
          "Detail:\\s*Key\\s*\\(([^)]+)\\)\\s*=\\s*\\(([^)]+)\\)",
          Pattern.CASE_INSENSITIVE
  );

  private RightAssignmentErrorParser() {}

  public static boolean isTargetTableConflict(String errorMessage) {
    return errorMessage != null
            && errorMessage.contains(TARGET_CONSTRAINT)
            && TARGET_TABLE_PATTERN.matcher(errorMessage).find();
  }

  public static Optional<RightAssignmentConflictKey> extractConflictKey(Throwable throwable) {
    if (throwable == null) {
      return Optional.empty();
    }
    return extractConflictKey(collectFullErrorMessage(throwable));
  }

  public static Optional<RightAssignmentConflictKey> extractConflictKey(String errorMessage) {
    if (!isTargetTableConflict(errorMessage)) {
      return Optional.empty();
    }

    Matcher matcher = PG_KEY_DETAIL_PATTERN.matcher(errorMessage);
    while (matcher.find()) {
      String[] keys = matcher.group(1).split(",");
      String[] values = matcher.group(2).split(",");

      if (keys.length != values.length) {
        continue;
      }

      Map<String, String> keyValMap = new HashMap<>();
      for (int i = 0; i < keys.length; i++) {
        keyValMap.put(keys[i].trim().toLowerCase(), values[i].trim().replaceAll("^['\"]|['\"]$", ""));
      }

      if (keyValMap.containsKey("rightname")
              && keyValMap.containsKey("facilityid")
              && keyValMap.containsKey("programid")
              && keyValMap.containsKey("userid")) {
        try {
          return Optional.of(RightAssignmentConflictKey.builder()
                  .rightName(keyValMap.get("rightname"))
                  .facilityId(UUID.fromString(keyValMap.get("facilityid")))
                  .programId(UUID.fromString(keyValMap.get("programid")))
                  .userId(UUID.fromString(keyValMap.get("userid")))
                  .build());
        } catch (IllegalArgumentException e) {
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }

  public static String collectFullErrorMessage(Throwable t) {
    StringBuilder sb = new StringBuilder();
    Throwable current = t;

    while (current != null) {
      if (current.getMessage() != null) {
        sb.append(current.getMessage()).append('\n');
      }
      if (current instanceof SQLException) {
        SQLException next = ((SQLException) current).getNextException();
        while (next != null) {
          if (next.getMessage() != null) {
            sb.append(next.getMessage()).append('\n');
          }
          next = next.getNextException();
        }
      }
      current = current.getCause();
    }
    return sb.toString();
  }
}