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

import static com.google.common.collect.Lists.newArrayList;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.constant.android.AndroidConstants;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.dto.android.RequisitionStatusDto;
import org.siglus.siglusapi.dto.android.enumeration.TestProject;
import org.siglus.siglusapi.dto.android.request.AndroidHeader;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.PatientLineItemColumnRequest;
import org.siglus.siglusapi.dto.android.request.PatientLineItemsRequest;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardDeleteRequest;
import org.siglus.siglusapi.dto.android.request.TestConsumptionLineItemRequest;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.dto.android.response.RequisitionResponse;
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedEmitter;
import org.siglus.siglusapi.service.android.MeService;
import org.springframework.mock.web.MockHttpServletRequest;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals", "PMD.UnusedPrivateField"})
@RunWith(MockitoJUnitRunner.class)
public class SiglusMeControllerTest {

  @InjectMocks
  private SiglusMeController controller;

  @Mock
  private MeService service;
  @Mock
  private AndroidRequisitionSyncedEmitter androidRequisitionSyncedEmitter;

  @Mock
  private ProductSyncResponse syncResponse;

  @Mock
  private FacilityResponse facilityResponse;

  @Mock
  private FacilityProductMovementsResponse facilityProductMovementsResponse;

  @Mock
  private RequisitionResponse requisitionResponse;

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
    MockHttpServletRequest request = mockRequestHeader("POST", "/api/siglusapi/android/me/app-info");
    doNothing().when(service).processAppInfo(any(AppInfo.class));

    // when
    controller.processAppInfo(request);

    // then
    verify(service).processAppInfo(any(AppInfo.class));
    ArgumentCaptor<AppInfo> argument = ArgumentCaptor.forClass(AppInfo.class);
    verify(service).processAppInfo(argument.capture());
    assertEquals("CS_Moine_Role1", argument.getValue().getUsername());
    assertEquals("01080904", argument.getValue().getFacilityCode());
    assertEquals("ac36c07a09f2fdca", argument.getValue().getUniqueId());
    assertEquals("87", argument.getValue().getVersionCode());
    assertEquals("28", argument.getValue().getAndroidSdkVersion());
  }

  @Test
  public void shouldCallServiceWhenProcessResyncInfo() {
    // given
    MockHttpServletRequest request = mockRequestHeader("POST", "/api/siglusapi/android/me/resync-info");
    AndroidHeader header = AndroidConstants.getAndroidHeader(request);
    doNothing().when(service).processResyncInfo(header);

    // when
    controller.processResyncInfo(request);

    // then
    verify(service).processResyncInfo(header);
    ArgumentCaptor<AndroidHeader> argument = ArgumentCaptor.forClass(AndroidHeader.class);
    verify(service).processResyncInfo(argument.capture());
    assertEquals("CS_Moine_Role1", argument.getValue().getUsername());
    assertEquals("01080904", argument.getValue().getFacilityCode());
    assertEquals("ac36c07a09f2fdca", argument.getValue().getUniqueId());
    assertEquals("87", argument.getValue().getVersionCode());
    assertEquals("28", argument.getValue().getAndroidSdkVersion());
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

  @Test
  public void shouldCallServiceWhenGetFacilityStockCards() {
    // given
    when(service.getProductMovements(LocalDate.of(2021, 6, 30), LocalDate.of(2021, 7, 2)))
        .thenReturn(facilityProductMovementsResponse);

    // when
    FacilityProductMovementsResponse productMovementsResponse = controller
        .getFacilityStockCards(LocalDate.of(2021, 6, 30), LocalDate.of(2021, 7, 2));

    // then
    assertSame(productMovementsResponse, this.facilityProductMovementsResponse);
    verify(service).getProductMovements(LocalDate.of(2021, 6, 30), LocalDate.of(2021, 7, 2));
  }

  @Test
  public void shouldCallDeleteAdditionalOrderable() {
    List<StockCardDeleteRequest> requests = new ArrayList<>();

    controller.deleteAdditionalOrderable(requests);

    verify(service).deleteStockCardByProduct(requests);
  }

  @Test
  public void shouldCallServiceWhenCreateRequisition() {
    // given
    RequisitionCreateRequest requisitionRequest = new RequisitionCreateRequest();

    // when
    controller.createRequisition(requisitionRequest);

    // then
    verify(service).createRequisition(requisitionRequest);
  }

  @Test
  public void shouldCallServiceWhenGetRequisition() {
    // given
    PatientLineItemColumnRequest columnRequest = new PatientLineItemColumnRequest();
    columnRequest.setName("12345");
    PatientLineItemsRequest patientLineItemsRequest = new PatientLineItemsRequest();
    patientLineItemsRequest.setColumns(Collections.singletonList(columnRequest));
    RequisitionCreateRequest request = new RequisitionCreateRequest();
    request.setPatientLineItems(Collections.singletonList(patientLineItemsRequest));

    TestConsumptionLineItemRequest testConsumptionLineItemRequest1 = new TestConsumptionLineItemRequest();
    testConsumptionLineItemRequest1.setTestProject(TestProject.NEWTEST.name());
    TestConsumptionLineItemRequest testConsumptionLineItemRequest2 = new TestConsumptionLineItemRequest();
    testConsumptionLineItemRequest2.setTestProject(TestProject.HIVUNIGOLD.name());
    request.setTestConsumptionLineItems(newArrayList(testConsumptionLineItemRequest1, testConsumptionLineItemRequest2));
    RequisitionResponse requisitionResponse = new RequisitionResponse();
    requisitionResponse.setRequisitionResponseList(Collections.singletonList(request));
    when(service.getRequisitionResponse("2021-06-01")).thenReturn(requisitionResponse);

    // when
    RequisitionResponse result = controller.getRequisitionResponse("2021-06-01");

    // then
    assertSame(1, result.getRequisitionResponseList().get(0).getPatientLineItems().size());
    assertSame(0, result.getRequisitionResponseList().get(0).getPatientLineItems().get(0).getColumns().size());
    assertSame(1, result.getRequisitionResponseList().get(0).getTestConsumptionLineItems().size());
    verify(service).getRequisitionResponse("2021-06-01");
  }

  @Test
  public void shouldCallServiceWhenConfirmPod() {
    // given
    PodRequest podRequest = new PodRequest();

    // when
    controller.confirmPod(podRequest);

    // then
    verify(service).confirmPod(podRequest, false);
  }

  @Test
  public void shouldCallServiceWhenGetPod() {
    // given
    LocalDate since = LocalDate.of(2020, 12, 31);

    // when
    controller.getProofsOfDelivery(false, since);

    // then
    verify(service).getProofsOfDeliveryWithFilter(since, false);
  }

  @Test
  public void shouldCallGetRequisitionStatus() {
    RequisitionStatusDto requisitionStatusDto = new RequisitionStatusDto();
    requisitionStatusDto.setId(UUID.randomUUID().toString());

    controller.getRegularRequisitionStatusResponse(Collections.singletonList(requisitionStatusDto));

    verify(service).getRegularRequisitionStatus(any());
  }

  private List<HfCmmDto> mockFacilityCmms() {
    List<HfCmmDto> list = new ArrayList<>();
    list.add(HfCmmDto.builder()
        .cmm(122.0)
        .productCode("05A13")
        .periodBegin(LocalDate.of(2021, 4, 21))
        .periodEnd(LocalDate.of(2021, 5, 20))
        .build());
    return list;
  }

  private MockHttpServletRequest mockRequestHeader(String method, String requestUri) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
    request.addHeader("UserName", "CS_Moine_Role1");
    request.addHeader("FacilityCode", "01080904");
    request.addHeader("FacilityName", "Centro de Saude de Chiunze");
    request.addHeader("UniqueId", "ac36c07a09f2fdca");
    request.addHeader("DeviceInfo", "OS: 9 Model: google Android SDK built for x86versionCode:87.1");
    request.addHeader("VersionCode", "87");
    request.addHeader("AndroidSDKVersion", "28");
    return request;
  }
}
