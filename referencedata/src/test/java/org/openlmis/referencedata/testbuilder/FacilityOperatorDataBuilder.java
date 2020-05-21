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

package org.openlmis.referencedata.testbuilder;

import java.util.UUID;
import org.openlmis.referencedata.domain.FacilityOperator;

public class FacilityOperatorDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private String code;
  private String name;
  private String description;
  private Integer displayOrder;

  /**
   * Returns instance of {@link FacilityOperatorDataBuilder} with sample data.
   */
  public FacilityOperatorDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = "FO" + instanceNumber;
    name = "Facility Operator " + instanceNumber;
    displayOrder = 1;
  }

  /**
   * Builds instance of {@link FacilityOperator}.
   */
  public FacilityOperator build() {
    FacilityOperator operator = new FacilityOperator(code, name, description, displayOrder);
    operator.setId(id);

    return operator;
  }
}
