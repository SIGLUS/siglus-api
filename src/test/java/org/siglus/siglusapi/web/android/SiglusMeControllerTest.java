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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.service.android.SiglusMeService;
import org.springframework.mock.web.MockHttpServletRequest;

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
  public void shouldCallServiceWhenProcessAppInfo() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest("POST",
            "/api/siglusapi/android/me/app-info");
    doNothing().when(service).processAppInfo(any(AppInfo.class));

    // when
    controller.processAppInfo(request);

    // then
    verify(service).processAppInfo(any(AppInfo.class));
  }

  @Test
  public void shouldUpdateCurrentFacilityCmms() {
    // given
    List<HfCmmDto> cmmsInfos = mockFacilityCmms();
    doNothing().when(service).processHfCmms(cmmsInfos);

    // when
    controller.updateCmmsForFacility(cmmsInfos);

    // then
    verify(service).processHfCmms(cmmsInfos);
  }

  private List<HfCmmDto> mockFacilityCmms() {
    List<HfCmmDto> list = new ArrayList<>();
    list.add(HfCmmDto.builder()
            .cmm(122.0)
            .productCode("05A13")
            .periodBegin(LocalDate.of(2021,4,21))
            .periodEnd(LocalDate.of(2021,5,20))
            .build());
    return list;
  }
}
