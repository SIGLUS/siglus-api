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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.android.AppInfoDomain;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
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
    long timestamp = System.currentTimeMillis();

    // when
    ProductSyncResponse productSyncResponse = controller.getFacilityProducts(timestamp);

    // then
    assertSame(syncResponse, productSyncResponse);
    ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(service).getFacilityProducts(instantCaptor.capture());
    assertEquals(Instant.ofEpochMilli(timestamp), instantCaptor.getValue());
  }

  @Test
  public void shouldCallServiceWhenGetFacility() {
    // given
    when(service.getCurrentFacility()).thenReturn(facilityResponse);

    // when
    FacilityResponse facilityResponse = controller.getFacility();

    // then
    assertSame(facilityResponse, this.facilityResponse);
    verify(service).getCurrentFacility();
  }

  @Test
  public void shouldArchiveAllProducts() {
    // given
    List<String> productCodes = Arrays.asList("product1", "product2", "product3");
    doNothing().when(service).archiveAllProducts(productCodes);

    // when
    controller.archiveAllProducts(productCodes);

    // then
    verify(service).archiveAllProducts(productCodes);
  }

  @Test
  public void shouldCallServiceWhenProcessAppInfo() {
    // given
    AppInfoDomain appInfo = mockAppInfo();
    doNothing().when(service).processAppInfo(appInfo);

    // when
    controller.processAppInfo(appInfo.getUserName(),
        appInfo.getFacilityCode(),
        appInfo.getFacilityName(),
        appInfo.getUniqueId(),
        appInfo.getDeviceInfo(),
        appInfo.getVersionCode(),
        appInfo.getAndroidsdkVersion());

    // then
    verify(service).processAppInfo(appInfo);
  }

  private AppInfoDomain mockAppInfo() {
    return AppInfoDomain.builder()
        .deviceInfo("deviceinfo1")
        .facilityName("Centro de Saude de Chiunze")
        .androidsdkVersion(28)
        .versionCode(88)
        .facilityCode("01080904")
        .userName("CS_Moine_Role1")
        .uniqueId("ac36c07a09f2fdcd")
        .build();
  }
}
