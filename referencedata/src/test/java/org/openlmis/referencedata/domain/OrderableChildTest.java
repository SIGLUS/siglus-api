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

package org.openlmis.referencedata.domain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.testbuilder.OrderableDataBuilder;

@RunWith(MockitoJUnitRunner.class)
public class OrderableChildTest {

  @Test
  public void equalsContract() throws Exception {
    EqualsVerifier
        .forClass(OrderableChild.class)
        .withRedefinedSuperclass()
        .withPrefabValues(Orderable.class,
            new OrderableDataBuilder().buildAsNew(),
            new OrderableDataBuilder().buildAsNew())
        .withOnlyTheseFields("parent", "orderable")
        .verify();
  }

  @Test
  public void shouldCreateNewOrderableChild() {
    Orderable parent = Mockito.mock(Orderable.class);
    Orderable child = Mockito.mock(Orderable.class);
    Long quantity = 10L;

    OrderableChild orderableChild = OrderableChild.newInstance(parent, child, quantity);

    assertEquals(orderableChild.getQuantity(), quantity);
    assertEquals(orderableChild.getOrderable(), child);
    assertEquals(orderableChild.getParent(), parent);
  }

  @Test
  public void exportShouldSetOrderableAndQuantity() {
    OrderableChild child = new OrderableChild();

    OrderableChild.Exporter exporter = Mockito.mock(OrderableChild.Exporter.class);
    child.export(exporter);

    verify(exporter).setQuantity(any());
    verify(exporter).setOrderable(any());
  }

}