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

package org.siglus.siglusapi.web;

import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.web.ProofOfDeliveryController;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusPodService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

@RunWith(MockitoJUnitRunner.class)
public class SiglusPodControllerTest {

  @InjectMocks
  private SiglusPodController controller;

  @Mock
  private ProofOfDeliveryController podController;

  @Mock
  private SiglusNotificationService notificationService;

  @Mock
  private OAuth2Authentication auth2Authentication;

  @Mock
  private SiglusPodService proofOfDeliveryService;

  @Test
  public void shouldCallSiglusServiceWhenGetProofOfDelivery() {
    // given
    UUID podId = UUID.randomUUID();

    // when
    controller.getProofOfDelivery(podId, null);

    // then
    verify(proofOfDeliveryService).getPodExtensionResponse(podId, null);
  }

  @Test
  public void shouldAetAllProofsOfDelivery() {
    // given
    UUID orderId = UUID.randomUUID();
    UUID shipmentId = UUID.randomUUID();
    Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);

    // when
    controller.getAllProofsOfDelivery(orderId, shipmentId, pageable);

    // then
    verify(podController).getAllProofsOfDelivery(orderId, shipmentId, pageable);
  }

}