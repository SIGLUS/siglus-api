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

package org.openlmis.requisition.dto;

import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.openlmis.requisition.testutils.DtoGenerator;

public class BasicRequisitionDtoTest extends ToStringContractTest<BasicRequisitionDto> {

  @Override
  protected Class<BasicRequisitionDto> getTestClass() {
    return BasicRequisitionDto.class;
  }

  @Override
  protected void prepare(EqualsVerifier<BasicRequisitionDto> verifier) {
    List<MinimalFacilityDto> facilities = DtoGenerator.of(MinimalFacilityDto.class, 2);
    List<GeographicZoneDto> geoZones = DtoGenerator.of(GeographicZoneDto.class, 2);

    verifier
        .withRedefinedSuperclass()
        .withPrefabValues(MinimalFacilityDto.class, facilities.get(0), facilities.get(1))
        .withPrefabValues(GeographicZoneDto.class, geoZones.get(0), geoZones.get(1));
  }

}
