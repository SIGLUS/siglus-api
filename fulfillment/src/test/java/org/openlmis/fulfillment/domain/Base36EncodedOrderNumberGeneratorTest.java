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

package org.openlmis.fulfillment.domain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Base36EncodedOrderNumberGeneratorTest {

  @InjectMocks
  private Base36EncodedOrderNumberGenerator base36EncodedOrderNumberGenerator;

  private Order order = mock(Order.class);

  @Test
  public void shouldEncodeIdAsBase36() {
    // given
    UUID id = UUID.fromString("83b9ed10-083b-47be-9253-ddca442440d2");
    when(order.getExternalId()).thenReturn(id);

    // when
    String orderNumber = base36EncodedOrderNumberGenerator.generate(order);

    // then
    assertEquals("7SQVH872", orderNumber);
  }
}
