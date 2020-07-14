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

package org.siglus.common.util.referencedata;

import java.util.Optional;
import java.util.UUID;

/**
 * Generic utility functions for {@link UUID}.
 */
public final class UuidUtil {

  public static final String UUID_PATTERN =
      "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$";

  private UuidUtil() {}

  /**
   * Parses a String into a UUID safely.  Unlike {@link UUID#fromString(String)} however, this
   * method will either return an Optional with the UUID, or it will return an empty Optional
   * in case the String can't be parsed into a UUID.
   *
   * @param uuid see {@link UUID#fromString(String)}.
   * @return An {@link Optional} with either the UUID as parsed from the paramater, or an empty
   *     Optional should it be un-parseable.
   */
  public static Optional<UUID> fromString(String uuid) {
    if (uuid == null) {
      return Optional.empty();
    }
    try {
      return uuid.matches(UUID_PATTERN)
          ? Optional.of(UUID.fromString(uuid))
          : Optional.empty();
    } catch (IllegalArgumentException iae) {
      return Optional.empty();
    }
  }

}