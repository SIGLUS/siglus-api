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

package org.openlmis.fulfillment.testutils;

import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;

@Getter
@Setter
@NoArgsConstructor
public class ExpandedObjectReferenceDto extends ObjectReferenceDto {

  private String expandedStringProperty;

  private List<String> expandedListProperty;

  private UUID expandedUuidProperty;

  private ExpandedObjectReferenceDto expandedNestedProperty;

  /**
   * Test object constructor.
   */
  public ExpandedObjectReferenceDto(UUID id, String serviceUrl, String resourceName) {
    super(id, serviceUrl, resourceName);
  }
}
