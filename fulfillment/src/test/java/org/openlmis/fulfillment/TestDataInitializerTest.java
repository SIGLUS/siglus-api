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

package org.openlmis.fulfillment;

import static org.mockito.Mockito.verify;
import static org.openlmis.fulfillment.TestDataInitializer.ORDERS_TABLE;
import static org.openlmis.fulfillment.TestDataInitializer.ORDER_LINE_ITEMS_TABLE;
import static org.openlmis.fulfillment.TestDataInitializer.PROOFS_OF_DELIVERY_TABLE;
import static org.openlmis.fulfillment.TestDataInitializer.PROOF_OF_DELIVERY_LINE_ITEMS_TABLE;
import static org.openlmis.fulfillment.TestDataInitializer.SHIPMENTS_TABLE;
import static org.openlmis.fulfillment.TestDataInitializer.SHIPMENT_LINE_ITEMS_TABLE;
import static org.openlmis.fulfillment.TestDataInitializer.STATUS_CHANGES_TABLE;
import static org.openlmis.fulfillment.TestDataInitializer.TRANSFER_PROPERTIES_TABLE;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

@RunWith(MockitoJUnitRunner.class)
public class TestDataInitializerTest {

  @Mock
  private Resource ordersResource;

  @Mock
  private Resource orderLineItemsResource;

  @Mock
  private Resource shipmentsResource;

  @Mock
  private Resource shipmentLineItemsResource;

  @Mock
  private Resource proofsOfDeliveryResource;

  @Mock
  private Resource proofOfDeliveryLineItemsResource;

  @Mock
  private Resource statusChangesResource;

  @Mock
  private Resource transferPropertiesResource;

  @Mock
  private Resource2Db loader;

  @InjectMocks
  private TestDataInitializer initializer = new TestDataInitializer(loader);

  @Test
  public void shouldLoadData() throws IOException {
    initializer.run();

    verify(loader).insertToDbFromCsv(ORDERS_TABLE, ordersResource);
    verify(loader).insertToDbFromCsv(ORDER_LINE_ITEMS_TABLE, orderLineItemsResource);

    verify(loader).insertToDbFromCsv(SHIPMENTS_TABLE, shipmentsResource);
    verify(loader).insertToDbFromCsv(SHIPMENT_LINE_ITEMS_TABLE, shipmentLineItemsResource);

    verify(loader).insertToDbFromCsv(PROOFS_OF_DELIVERY_TABLE, proofsOfDeliveryResource);
    verify(loader).insertToDbFromCsv(PROOF_OF_DELIVERY_LINE_ITEMS_TABLE,
        proofOfDeliveryLineItemsResource);

    verify(loader).insertToDbFromCsv(STATUS_CHANGES_TABLE, statusChangesResource);

    verify(loader).insertToDbFromCsv(TRANSFER_PROPERTIES_TABLE, transferPropertiesResource);
  }
}
