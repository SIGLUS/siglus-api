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
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.StockAdjustmentReason;

public class StockAdjustmentReasonDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private Program program;
  private String name;
  private String description;
  private Boolean additive;
  private Integer displayOrder;

  /**
   * Builds instance of {@link StockAdjustmentReasonDataBuilder} with sample data.
   */
  public StockAdjustmentReasonDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    program = new ProgramDataBuilder().build();
    name = "Stock Adjustment Reason " + instanceNumber;
    description = "description";
    additive = true;
    displayOrder = 0;
  }

  /**
   * Builds instance of {@link StockAdjustmentReason} without id field.
   */
  public StockAdjustmentReason buildAsNew() {
    return new StockAdjustmentReason(program, name, description, additive, displayOrder);
  }

  /**
   * Builds instance of {@link StockAdjustmentReason}.
   */
  public StockAdjustmentReason build() {
    StockAdjustmentReason reason = buildAsNew();
    reason.setId(id);

    return reason;
  }

  public StockAdjustmentReasonDataBuilder withName(String name) {
    this.name = name;
    return this;
  }


  public StockAdjustmentReasonDataBuilder withProgram(Program program) {
    this.program = program;
    return this;
  }
}
