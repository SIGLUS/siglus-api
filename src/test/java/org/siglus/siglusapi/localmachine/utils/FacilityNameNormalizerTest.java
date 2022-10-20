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

package org.siglus.siglusapi.localmachine.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;

public class FacilityNameNormalizerTest {

  @Test
  public void shouldMappingSpecialPtCharToEn() {
    assertThat(FacilityNameNormalizer.normalize("Çá")).isEqualTo("Ca");
  }

  @Test
  public void shouldReturnOriginNameWhenNormalizeGivenEmptyName() {
    assertThat(FacilityNameNormalizer.normalize("")).isEqualTo("");
  }

  @Test
  public void shouldReturnOriginNameWhenNormalizeGivenNullName() {
    assertThat(FacilityNameNormalizer.normalize(null)).isEqualTo(null);
  }

  @Test
  public void shouldReplaceSpecialCharsToUnderscoreWhenNormalize() {
    Map<String, String> testCases = ImmutableMap.of(
        "Lagoa Pathi/ Chicavele", "Lagoa_Pathi_Chicavele",
        "Emergencia da Covid-19/Dep. De Oftalmologia", "Emergencia_da_Covid-19_Dep_De_Oftalmologia"
    );
    testCases.forEach((name, expected) -> assertThat(FacilityNameNormalizer.normalize(name)).isEqualTo(expected));
  }
}
