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

import java.time.LocalDate;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.SupportedProgram;
import org.openlmis.referencedata.domain.SupportedProgramPrimaryKey;

public class SupportedProgramDataBuilder {
  private Facility facility;
  private Program program;
  private boolean active = true;

  /**
   * Returns instance of {@link SupportedProgramDataBuilder} with sample data.
   */
  public SupportedProgramDataBuilder() {
  }

  /**
   * Builds instance of {@link SupportedProgram}.
   */
  public SupportedProgram build() {
    return new SupportedProgram(new SupportedProgramPrimaryKey(facility, program),
        active, false, LocalDate.now());
  }

  /**
   * Builds instance of {@link SupportedProgram}.
   */
  public SupportedProgram buildAsInactive() {
    this.active = false;
    return build();
  }

  /**
   * Adds program for new {@link SupportedProgram}.
   */
  public SupportedProgramDataBuilder withProgram(Program program) {
    this.program = program;
    return this;
  }

  /**
   * Adds facility for new {@link SupportedProgram}.
   */
  public SupportedProgramDataBuilder withFacility(Facility facility) {
    this.facility = facility;
    return this;
  }

  public SupportedProgramDataBuilder withActiveFlag(boolean active) {
    this.active = active;
    return this;
  }
}
