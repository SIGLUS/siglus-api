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

import static org.openlmis.referencedata.web.BaseController.API_PATH;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString(of = "reference")
@EqualsAndHashCode(of = "reference")
public final class FhirReference {

  @JsonIgnore
  @Getter(AccessLevel.PACKAGE)
  private final UUID resourceId;

  // Literal reference, Relative, internal or absolute URL
  @Getter
  private final String reference;

  FhirReference(String serviceUrl, String path, UUID id) {
    this.resourceId = id;
    this.reference = String.format("%s%s%s/%s", serviceUrl, API_PATH, path, resourceId);
  }
}
