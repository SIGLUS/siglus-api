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

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RightAssignmentErrorParser {

  private static final String TARGET_CONSTRAINT = "right_assignment_unq";

  // Matches referencedata.right_assignments with or without quotes and case-insensitively
  // e.g. "referencedata"."right_assignments" or referencedata.right_assignments
  private static final Pattern TARGET_TABLE_PATTERN = Pattern.compile(
      "\"?referencedata\"?\\s*\\.\\s*\"?right_assignments\"?",
      Pattern.CASE_INSENSITIVE
  );

  // Captures: Detail: Key (col1, col2, ...)=(val1, val2, ...)
  private static final Pattern PG_KEY_DETAIL_PATTERN = Pattern.compile(
      "Detail:\\s*Key\\s*\\(([^)]+)\\)\\s*=\\s*\\(([^)]+)\\)",
      Pattern.CASE_INSENSITIVE
  );

  private RightAssignmentErrorParser() {}

  /**
   * Checks if the error message is specifically from "referencedata"."right_assignments"
   * violating "right_assignment_unq".
   */
  public static boolean isTargetTableConflict(String errorMessage) {
    if (errorMessage == null) {
      return false;
    }
    return errorMessage.contains(TARGET_CONSTRAINT)
        && TARGET_TABLE_PATTERN.matcher(errorMessage).find();
  }

  /**
   * Extracts the userId UUID from a Throwable if it originates from
   * "referencedata"."right_assignments".
   */
  public static Optional<UUID> extractUserId(Throwable throwable) {
    if (throwable == null) {
      return Optional.empty();
    }
    return extractUserId(collectFullErrorMessage(throwable));
  }

  /**
   * Extracts the userId UUID from an error string if it originates from
   * "referencedata"."right_assignments".
   */
  public static Optional<UUID> extractUserId(String errorMessage) {
    if (!isTargetTableConflict(errorMessage)) {
      return Optional.empty();
    }

    Matcher matcher = PG_KEY_DETAIL_PATTERN.matcher(errorMessage);
    while (matcher.find()) {
      String[] keys = matcher.group(1).split(",");
      String[] values = matcher.group(2).split(",");

      for (int i = 0; i < keys.length; i++) {
        if ("userid".equalsIgnoreCase(keys[i].trim()) && i < values.length) {
          String rawUserId = values[i].trim().replaceAll("^['\"]|['\"]$", "");
          try {
            return Optional.of(UUID.fromString(rawUserId));
          } catch (IllegalArgumentException e) {
            return Optional.empty();
          }
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Traverses standard cause chains and JDBC SQLException nextException chains.
   */
  public static String collectFullErrorMessage(Throwable t) {
    StringBuilder sb = new StringBuilder();
    Throwable current = t;

    while (current != null) {
      if (current.getMessage() != null) {
        sb.append(current.getMessage()).append('\n'); // Line 103: use '\n' instead of "\n"
      }
      if (current instanceof SQLException) {
        SQLException next = ((SQLException) current).getNextException();
        while (next != null) {
          if (next.getMessage() != null) {
            sb.append(next.getMessage()).append('\n'); // Line 109: use '\n' instead of "\n"
          }
          next = next.getNextException();
        }
      }
      current = current.getCause();
    }
    return sb.toString();
  }
}