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

import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.testutils.DtoGenerator;

public class OrderObjectReferenceDtoTest {

  @Test
  public void equalsContract() {
    List<FacilityDto> facilities = DtoGenerator.of(FacilityDto.class, 2);
    List<UserDto> users = DtoGenerator.of(UserDto.class, 2);

    EqualsVerifier
        .forClass(OrderObjectReferenceDto.class)
        .withRedefinedSuperclass()
        .withPrefabValues(FacilityDto.class, facilities.get(0), facilities.get(1))
        .withPrefabValues(UserDto.class, users.get(0), users.get(1))
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

}
