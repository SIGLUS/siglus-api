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
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Program;

public class ProgramDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private Code code;
  private String name;
  private String description;
  private Boolean active;
  private Boolean periodSkippable;
  private Boolean skipAuthorization;
  private Boolean showNonFullSupplyTab;
  private Boolean enableDatePhysicalStockCountCompleted;

  /**
   * Returns instance of {@link ProgramDataBuilder} with sample data.
   */
  public ProgramDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = Code.code("P" + instanceNumber);
    name = "Program " + instanceNumber;
    active = true;
    periodSkippable = true;
    skipAuthorization = false;
    showNonFullSupplyTab = true;
    enableDatePhysicalStockCountCompleted = false;
  }

  /**
   * Builds instance of {@link Program}.
   */
  public Program build() {
    Program program = new Program(code, name, description, active, periodSkippable,
        skipAuthorization, showNonFullSupplyTab, enableDatePhysicalStockCountCompleted);
    program.setId(id);

    return program;
  }

  /**
   * Sets id for new {@link Program}.
   */
  public ProgramDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Sets null id for new {@link Program}.
   */
  public ProgramDataBuilder withoutId() {
    return withId(null);
  }

  public ProgramDataBuilder withCode(String code) {
    this.code = Code.code(code);
    return this;
  }
}
