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

package org.openlmis.referencedata.fhir;

import static org.apache.commons.lang3.StringUtils.joinWith;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class FhirIdentifier {

  public static final String SYSTEM_RFC_3986 = "urn:ietf:rfc:3986";
  private static final String SEPARATOR = "/";

  private final String system;
  private final String value;

  FhirIdentifier(String serviceUrl, String path, UUID uuid) {
    this.system = SYSTEM_RFC_3986;
    this.value = joinWith(SEPARATOR, serviceUrl + path, uuid);
  }

}
