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
import org.openlmis.referencedata.testbuilder.RequisitionGroupDataBuilder;
import org.openlmis.referencedata.testbuilder.SupervisoryNodeDataBuilder;

public class SupervisoryNodeObjectReferenceDtoTest {

  @Test
  public void equalsContract() {
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

    final RequisitionGroupObjectReferenceDto group1 = new RequisitionGroupObjectReferenceDto();
    final RequisitionGroupObjectReferenceDto group2 = new RequisitionGroupObjectReferenceDto();
    new RequisitionGroupDataBuilder().build().export(group1);
    new RequisitionGroupDataBuilder().build().export(group2);

    EqualsVerifier
        .forClass(SupervisoryNodeObjectReferenceDto.class)
        .withPrefabValues(SupervisoryNodeObjectReferenceDto.class, node1, node2)
        .withPrefabValues(FacilityObjectReferenceDto.class, facility1, facility2)
        .withPrefabValues(GeographicZoneSimpleDto.class, zone1, zone2)
        .withPrefabValues(RequisitionGroupObjectReferenceDto.class, group1, group2)
        .withRedefinedSuperclass()
        .withIgnoredFields("serviceUrl")
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    SupervisoryNodeObjectReferenceDto dto = new SupervisoryNodeObjectReferenceDto();
    ToStringTestUtils.verify(SupervisoryNodeObjectReferenceDto.class, dto, "SUPERVISORY_NODES");
  }
}
