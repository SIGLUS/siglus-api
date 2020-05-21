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

import static org.javers.common.collections.Sets.asSet;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.OrderableChild;

@RunWith(MockitoJUnitRunner.class)
public class OrderableChildDtoTest {

  @Test
  public void equalsContract() throws Exception {
    EqualsVerifier
        .forClass(OrderableChildDto.class)
        .withRedefinedSuperclass()
        .suppress(Warning.STRICT_INHERITANCE)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldCreateNewInstancesForSet() {
    OrderableChild child = Mockito.mock(OrderableChild.class);
    doNothing().when(child).export(any());

    OrderableChildDto.newInstance(asSet(child));

    verify(child).export(any());
  }
}