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

package org.siglus.siglusapi.web.android;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.response.android.FacilityResponse;
import org.siglus.siglusapi.dto.response.android.ProductSyncResponse;
import org.siglus.siglusapi.service.android.SiglusMeService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusMeControllerTest {

  @InjectMocks
  private SiglusMeController controller;

  @Mock
  private SiglusMeService service;

  @Mock
  private ProductSyncResponse syncResponse;

  @Mock
  private FacilityResponse facilityResponse;

  @Before
  public void setup() {
    when(service.getFacilityProducts(any())).thenReturn(syncResponse);
  }

  @Test
  public void shouldCallServiceWhenGetFacilityProducts() {
    // when
    ProductSyncResponse productSyncResponse = controller.getFacilityProducts(null);

    // then
    assertSame(syncResponse, productSyncResponse);
    verify(service).getFacilityProducts(null);
  }

  @Test
  public void shouldCallServiceWhenGetFacilityProductsGivenLastSyncTime() {
    // given
    Instant lastSyncTime = Instant.now();

    // when
    ProductSyncResponse productSyncResponse = controller.getFacilityProducts(lastSyncTime);

    // then
    assertSame(syncResponse, productSyncResponse);
    verify(service).getFacilityProducts(lastSyncTime);
  }

  @Test
  public void shouldCallServiceWhenGetFacility() {
    // given
    when(service.getFacility()).thenReturn(facilityResponse);

    // when
    FacilityResponse facilityResponse = controller.getFacility();

    // then
    assertSame(facilityResponse,this.facilityResponse);
    verify(service).getFacility();
  }
}
