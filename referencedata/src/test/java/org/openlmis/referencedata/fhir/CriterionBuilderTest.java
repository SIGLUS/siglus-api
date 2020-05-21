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

package org.openlmis.referencedata.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.rest.gclient.ICriterion;
import java.util.UUID;
import org.junit.Test;

public class CriterionBuilderTest {

  private static final String SERVICE_URL = "http://localhost";

  private CriterionBuilder builder = new CriterionBuilder(SERVICE_URL);

  @Test
  public void shouldBuildIdentifierCriterion() {
    // given
    UUID id = UUID.randomUUID();

    // when
    ICriterion criterion = builder.buildIdentifierCriterion(id);

    // then
    assertThat(criterion)
        .hasFieldOrPropertyWithValue("myName", "identifier")
        .hasFieldOrPropertyWithValue("myValue", SERVICE_URL + "|" + id.toString());
  }
}
