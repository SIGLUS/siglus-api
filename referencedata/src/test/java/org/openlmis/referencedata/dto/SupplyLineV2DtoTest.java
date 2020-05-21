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
import org.openlmis.referencedata.testbuilder.GeographicZoneDataBuilder;
import org.openlmis.referencedata.testbuilder.SupervisoryNodeDataBuilder;
import org.openlmis.referencedata.testbuilder.SupplyLineDataBuilder;

public class SupplyLineV2DtoTest {

  @Test
  public void equalsContract() {
    final SupplyLineObjectReferenceDto line1 = new SupplyLineObjectReferenceDto();
    final SupplyLineObjectReferenceDto line2 = new SupplyLineObjectReferenceDto();
    new SupplyLineDataBuilder().build().export(line1);
    new SupplyLineDataBuilder().build().export(line2);

    final SupervisoryNodeObjectReferenceDto node1 = new SupervisoryNodeObjectReferenceDto();
    final SupervisoryNodeObjectReferenceDto node2 = new SupervisoryNodeObjectReferenceDto();
    new SupervisoryNodeDataBuilder().build().export(node1);
    new SupervisoryNodeDataBuilder().build().export(node2);

    final FacilityObjectReferenceDto facility1 = new FacilityObjectReferenceDto();
    final FacilityObjectReferenceDto facility2 = new FacilityObjectReferenceDto();
    new FacilityDataBuilder().build().export(facility1);
    new FacilityDataBuilder().build().export(facility2);

    final GeographicZoneSimpleDto zone1 = new GeographicZoneSimpleDto();
    final GeographicZoneSimpleDto zone2 = new GeographicZoneSimpleDto();
    new GeographicZoneDataBuilder().build().export(zone1);
    new GeographicZoneDataBuilder().build().export(zone2);

    EqualsVerifier
        .forClass(SupplyLineObjectReferenceDto.class)
        .withPrefabValues(SupplyLineObjectReferenceDto.class, line1, line2)
        .withPrefabValues(SupervisoryNodeObjectReferenceDto.class, node1, node2)
        .withPrefabValues(FacilityObjectReferenceDto.class, facility1, facility2)
        .withPrefabValues(GeographicZoneSimpleDto.class, zone1, zone2)
        .withRedefinedSuperclass()
        .withIgnoredFields("serviceUrl")
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    SupplyLineObjectReferenceDto dto = new SupplyLineObjectReferenceDto();
    ToStringTestUtils.verify(SupplyLineObjectReferenceDto.class, dto);
  }
}
