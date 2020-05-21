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

package org.openlmis.referencedata.domain;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.openlmis.referencedata.dto.FacilityDto;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;
import org.openlmis.referencedata.testbuilder.ProgramDataBuilder;
import org.openlmis.referencedata.testbuilder.SupportedProgramDataBuilder;

public class FacilityTest {
  private Facility facility;
  private Program program;

  @Before
  public void setUp() {
    program = new ProgramDataBuilder().build();
    facility = new FacilityDataBuilder().withSupportedProgram(program).build();
  }

  @Test
  public void supportsShouldReturnTrueIfSupportsProgram() {
    assertThat(facility.supports(program)).isTrue();
  }

  @Test
  public void supportsShouldReturnFalseIfDoesNotSupportProgram() {
    Program otherProgram = new ProgramDataBuilder().build();
    assertThat(facility.supports(otherProgram)).isFalse();
  }

  @Test
  public void supportsShouldReturnFalseIfSupportsProgramButSupportNotActive() {
    SupportedProgram supportedProgram = new SupportedProgramDataBuilder()
        .withFacility(facility)
        .withProgram(program)
        .buildAsInactive();
    facility.setSupportedPrograms(newHashSet(supportedProgram));
    assertThat(facility.supports(program)).isFalse();
  }

  @Test
  public void shouldUpdateFromAnotherFacility() {
    FacilityDto newVersion = new FacilityDto();
    new FacilityDataBuilder().build().export(newVersion);

    facility.updateFrom(newVersion);

    assertThat(facility)
        .isEqualToIgnoringGivenFields(newVersion,
            "id", "geographicZone", "type", "operator", "supportedPrograms");
  }

}
