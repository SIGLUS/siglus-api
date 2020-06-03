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

package org.openlmis.fulfillment.web.stockmanagement;

import java.util.UUID;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.RandomStringUtils;

@NoArgsConstructor
public class ValidSourceDestinationDtoDataBuilder {
  private UUID id = UUID.randomUUID();
  private UUID programId = UUID.randomUUID();
  private UUID facilityTypeId = UUID.randomUUID();
  private NodeDto node = new NodeDtoDataBuilder().build();
  private String name = RandomStringUtils.randomAlphanumeric(5);
  private boolean isFreeTextAllowed = true;

  public ValidSourceDestinationDtoDataBuilder withNode(UUID referenceId) {
    node = new NodeDtoDataBuilder().withReferenceId(referenceId).build();
    return this;
  }

  /**
   * Builds instance of {@link ValidSourceDestinationDto}.
   */
  public ValidSourceDestinationDto build() {
    return new ValidSourceDestinationDto(
        id, programId, facilityTypeId, node, name, isFreeTextAllowed
    );
  }
}
