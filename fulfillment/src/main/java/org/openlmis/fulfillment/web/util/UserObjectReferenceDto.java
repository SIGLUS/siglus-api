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

package org.openlmis.fulfillment.web.util;

import static org.openlmis.fulfillment.service.ResourceNames.USERS;

import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
public final class UserObjectReferenceDto extends ObjectReferenceDto {

  @Getter
  @Setter
  private String username;

  private UserObjectReferenceDto() {
    this.username = null;
  }

  private UserObjectReferenceDto(UUID id, String serviceUrl, String resourceName) {
    super(id, serviceUrl, resourceName);
    this.username = null;
  }

  public static UserObjectReferenceDto create(UUID id, String serviceUrl) {
    return new UserObjectReferenceDto(id, serviceUrl, USERS);
  }

}
