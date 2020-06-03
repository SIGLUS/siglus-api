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

package org.openlmis.fulfillment.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.openlmis.fulfillment.repository.ShipmentRepository;
import org.openlmis.fulfillment.testutils.ShipmentDataBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ShipmentServiceTest {

  @Mock
  private ShipmentRepository shipmentRepository;

  @Mock
  private ProofOfDeliveryRepository proofOfDeliveryRepository;

  @InjectMocks
  private ShipmentService shipmentService;

  @Test
  public void shouldSaveShipmentAndCreateProofOfDelivery() {
    // given
    final Shipment shipment = new ShipmentDataBuilder().build();

    // when
    when(shipmentRepository.save(shipment)).thenReturn(shipment);
    shipmentService.save(shipment);

    // then
    verify(shipmentRepository).save(shipment);
    // creating a POD based on shipment and map is check in
    // ProofOfDeliveryTest.shouldCreateInstanceBasedOnShipment
    // here we only verify that POD has been saved
    verify(proofOfDeliveryRepository).save(any(ProofOfDelivery.class));
  }
}
