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

package org.openlmis.requisition.testutils;

import java.util.UUID;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class ObjectReferenceDtoDataBuilder implements DtoDataBuilder<ObjectReferenceDto> {

  private UUID id;
  private String serviceUrl;
  private String path;

  /**
   * Creates builder for creating new instance of {@link ObjectReferenceDto}.
   */
  public ObjectReferenceDtoDataBuilder() {
    id = UUID.randomUUID();
    serviceUrl = "https://openlmis/";
    path = "api/resource";
  }

  /**
   * Creates new instance of {@link ObjectReferenceDto} with properties.
   * @return created object reference.
   */
  public ObjectReferenceDto buildAsDto() {
    return new ObjectReferenceDto(id, serviceUrl, path);
  }

  public ObjectReferenceDtoDataBuilder withPath(String path) {
    this.path = path;
    return this;
  }

  public ObjectReferenceDtoDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }
}