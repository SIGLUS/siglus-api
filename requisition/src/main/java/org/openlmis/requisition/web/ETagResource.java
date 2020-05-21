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

package org.openlmis.requisition.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ETagResource<T> {

  private static final String WEAK_E_TAG_PREFIX = "W/";

  private T resource;
  private String etag;

  /**
   * Wraps the given versioned resource and builds a weak eTag based on the resource version.
   *
   * @param resource the resource to wrap
   * @param version the version of the resource
   */
  public ETagResource(T resource, Long version) {
    this.resource = resource;
    this.etag = buildWeakETag(version);
  }

  /**
   * Builds weak eTag (with 'W/' prefix) based on the passed version.
   *
   * @param version version to use in the eTag
   * @return weak eTag string representation
   */
  public static String buildWeakETag(Long version) {
    return new StringBuilder(WEAK_E_TAG_PREFIX).append(version).toString();
  }

  /**
   * Reads version (as Long) from the etag string. Supports both weak and strong eTags.
   *
   * @param etag etag to read version from
   * @return version read from etag (as Long)
   */
  public static Long readVersionFromEtag(String etag) {
    if (etag.startsWith("W/")) {
      return Long.valueOf(etag.substring(2));
    } else {
      return Long.valueOf(etag);
    }
  }
}
