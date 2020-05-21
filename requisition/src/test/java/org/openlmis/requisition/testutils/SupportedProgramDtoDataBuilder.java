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

import java.time.LocalDate;
import java.util.UUID;
import org.openlmis.requisition.dto.SupportedProgramDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class SupportedProgramDtoDataBuilder implements DtoDataBuilder<SupportedProgramDto> {

  private UUID id;
  private String code;
  private String name;
  private String description;
  private boolean programActive;
  private boolean periodsSkippable;
  private boolean showNonFullSupplyTab;
  private boolean supportActive;
  private boolean supportLocallyFulfilled;
  private LocalDate supportStartDate;

  /**
   * Builder for {@link SupportedProgramDto}.
   */
  public SupportedProgramDtoDataBuilder() {
    id = UUID.randomUUID();
    code = "code";
    name = "name";
    description = "description";
    programActive = false;
    periodsSkippable = false;
    showNonFullSupplyTab = false;
    supportActive = false;
    supportLocallyFulfilled = false;
    supportStartDate = LocalDate.now();
  }

  @Override
  public SupportedProgramDto buildAsDto() {
    SupportedProgramDto supportedProgram = new SupportedProgramDto();
    supportedProgram.setId(id);
    supportedProgram.setCode(code);
    supportedProgram.setName(name);
    supportedProgram.setDescription(description);
    supportedProgram.setProgramActive(programActive);
    supportedProgram.setPeriodsSkippable(periodsSkippable);
    supportedProgram.setShowNonFullSupplyTab(showNonFullSupplyTab);
    supportedProgram.setSupportActive(supportActive);
    supportedProgram.setSupportLocallyFulfilled(supportLocallyFulfilled);
    supportedProgram.setSupportStartDate(supportStartDate);
    return supportedProgram;
  }

  public SupportedProgramDtoDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public SupportedProgramDtoDataBuilder withSupportActive(boolean supportActive) {
    this.supportActive = supportActive;
    return this;
  }

  public SupportedProgramDtoDataBuilder withProgramActive(boolean programActive) {
    this.programActive = programActive;
    return this;
  }

  public SupportedProgramDtoDataBuilder withSupportStartDate(LocalDate supportStartDate) {
    this.supportStartDate = supportStartDate;
    return this;
  }

  public SupportedProgramDtoDataBuilder withSupportLocallyFulfilled(
      boolean supportLocallyFulfilled) {
    this.supportLocallyFulfilled = supportLocallyFulfilled;
    return this;
  }

  public SupportedProgramDtoDataBuilder withPeriodsSkippable(Boolean periodsSkippable) {
    this.periodsSkippable = periodsSkippable;
    return this;
  }

  public SupportedProgramDtoDataBuilder withShowNonFullSupplyTab(Boolean showNonFullSupplyTab) {
    this.showNonFullSupplyTab = showNonFullSupplyTab;
    return this;
  }
}
