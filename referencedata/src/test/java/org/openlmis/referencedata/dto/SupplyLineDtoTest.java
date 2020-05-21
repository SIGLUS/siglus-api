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

package org.openlmis.referencedata.dto;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.referencedata.ToStringTestUtils;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;

public class SupplyLineDtoTest {

  @Test
  public void equalsContract() {
    FacilityDto red = new FacilityDto();
    FacilityDto black = new FacilityDto();

    new FacilityDataBuilder().build().export(red);
    new FacilityDataBuilder().build().export(black);

    EqualsVerifier
        .forClass(SupplyLineDto.class)
        .withRedefinedSuperclass()
        .withPrefabValues(FacilityDto.class, red, black)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    SupplyLineDto dto = new SupplyLineDto();
    ToStringTestUtils.verify(SupplyLineDto.class, dto);
  }

}
