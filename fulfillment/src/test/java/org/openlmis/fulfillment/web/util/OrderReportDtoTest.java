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

package org.openlmis.fulfillment.web.util;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.fulfillment.service.referencedata.GeographicZoneDto;
import org.openlmis.fulfillment.service.referencedata.RightDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.testutils.GeographicZoneDataBuilder;
import org.openlmis.fulfillment.testutils.RightDataBuilder;
import org.openlmis.fulfillment.testutils.UserDataBuilder;

public class OrderReportDtoTest {

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(OrderReportDto.class)
        .withPrefabValues(GeographicZoneDto.class, new GeographicZoneDataBuilder().build(),
            new GeographicZoneDataBuilder().build())
        .withPrefabValues(RightDto.class, new RightDataBuilder().build(),
            new RightDataBuilder().build())
        .withPrefabValues(UserDto.class, new UserDataBuilder().build(),
            new UserDataBuilder().build())
        .suppress(Warning.NONFINAL_FIELDS)
        .withRedefinedSuperclass()
        .withIgnoredFields("serviceUrl")
        .verify();
  }
}
