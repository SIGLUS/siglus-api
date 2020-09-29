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

package org.siglus.siglusapi.service.fc;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FcConstants.CMM_API;
import static org.siglus.siglusapi.constant.FcConstants.CP_API;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_API;
import static org.siglus.siglusapi.constant.FcConstants.FACILITY_TYPE_API;
import static org.siglus.siglusapi.constant.FcConstants.GEOGRAPHIC_ZONE_API;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.PRODUCT_API;
import static org.siglus.siglusapi.constant.FcConstants.PROGRAM_API;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;
import static org.siglus.siglusapi.constant.FcConstants.REGIMEN_API;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.siglus.siglusapi.service.client.SiglusIssueVoucherService;
import org.siglus.siglusapi.service.client.SiglusReceiptPlanService;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class FcScheduleServiceTest {

  public static final String DATE = "20200825";
  public static final String PERIOD = "08-2020";
  public static final String NEXT_PERIOD = "09-2020";

  @InjectMocks
  private FcScheduleService fcScheduleService;

  @Captor
  private ArgumentCaptor<FcIntegrationResultDto> resultCaptor;

  @Mock
  private CallFcService callFcService;

  @Mock
  private FcIntegrationResultService fcIntegrationResultService;

  @Mock
  private FcIssueVoucherService fcIssueVoucherService;

  @Mock
  private FcIntegrationCmmCpService fcIntegrationCmmCpService;

  @Mock
  private FcReceiptPlanService fcReceiptPlanService;

  @Mock
  private SiglusReceiptPlanService siglusReceiptPlanService;

  @Mock
  private SiglusDateHelper dateHelper;

  @Mock
  private FcProgramService fcProgramService;

  @Mock
  private FcRegimenService fcRegimenService;

  @Mock
  private FcFacilityTypeService fcFacilityTypeService;

  @Mock
  private FcProductService fcProductService;

  @Mock
  private SiglusIssueVoucherService issueVoucherService;

  @Mock
  private FcGeographicZoneService fcGeographicZoneService;

  @Mock
  private FcFacilityService facilityService;

  @Before
  public void setup() {
    when(fcIssueVoucherService.processIssueVouchers(any())).thenReturn(true);
  }

  @Test
  public void shouldFetchReceiptPlanFromFc() {
    // given
    when(callFcService.getReceiptPlans()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(RECEIPT_PLAN_API)).thenReturn(DATE);
    when(fcReceiptPlanService.processReceiptPlans(any())).thenReturn(true);

    // when
    fcScheduleService.fetchReceiptPlansFromFc(DATE);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(RECEIPT_PLAN_API, resultCaptor.getValue().getApi());
  }

  @Test
  public void shouldGeographicZoneScheduleFromFc() {
    // given
    when(callFcService.getFacilities()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(GEOGRAPHIC_ZONE_API)).thenReturn(DATE);

    // when
    fcScheduleService.fetchGeographicZones();

    // then
    verify(fcGeographicZoneService).processGeographicZones(any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(GEOGRAPHIC_ZONE_API, resultCaptor.getValue().getApi());
  }

  @Test
  public void shouldFetchFacilityScheduleFromFc() {
    // given
    when(callFcService.getFacilities()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(FACILITY_API)).thenReturn(DATE);

    // when
    fcScheduleService.fetchFacility();

    // then
    verify(facilityService).processFacility(any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(FACILITY_API, resultCaptor.getValue().getApi());
  }


  @Test
  public void shouldFetchIssueVoucherScheduleFromFc() {
    // given
    when(callFcService.getIssueVouchers()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());

    // when
    fcScheduleService.fetchIssueVouchersFromFc();

    // then
    verify(issueVoucherService).updateIssueVourch(anyString());
  }


  @Test
  public void shouldFetchIssueVoucherFromFc() {
    // given
    when(callFcService.getIssueVouchers()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());

    // when
    fcScheduleService.fetchIssueVouchersFromFc("20000101");

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(ISSUE_VOUCHER_API, resultCaptor.getValue().getApi());
  }

  @Test
  public void shouldFetchProductsFromFc() {
    // given
    when(callFcService.getProducts()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(PRODUCT_API)).thenReturn(DATE);
    when(fcProductService.processProductData(any())).thenReturn(true);

    // when
    fcScheduleService.fetchProductsFromFc();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(PRODUCT_API, resultCaptor.getValue().getApi());
  }

  @Test
  public void shouldFetchCmmsFromFcWithCurrentPeriod() {
    // given
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(CMM_API)).thenReturn(PERIOD);
    when(fcIntegrationCmmCpService.processCmms(any(), any())).thenReturn(true);
    when(dateHelper.getCurrentMonthStr()).thenReturn(PERIOD);

    // when
    fcScheduleService.fetchCmmsFromFc();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(CMM_API, resultCaptor.getValue().getApi());
    assertEquals(PERIOD, resultCaptor.getValue().getDate());
  }

  @Test
  public void shouldFetchCmmsFromFcWithNextPeriod() {
    // given
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(CMM_API)).thenReturn(PERIOD);
    when(fcIntegrationCmmCpService.processCmms(any(), any())).thenReturn(true);
    when(dateHelper.getCurrentMonthStr()).thenReturn(NEXT_PERIOD);

    // when
    fcScheduleService.fetchCmmsFromFc();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(CMM_API, resultCaptor.getValue().getApi());
    assertEquals(NEXT_PERIOD, resultCaptor.getValue().getDate());
  }

  @Test
  public void shouldFetchCpsFromFcWithCurrentPeriod() {
    // given
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(CP_API)).thenReturn(PERIOD);
    when(fcIntegrationCmmCpService.processCps(any(), any())).thenReturn(true);
    when(dateHelper.getCurrentMonthStr()).thenReturn(PERIOD);

    // when
    fcScheduleService.fetchCpsFromFc();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(CP_API, resultCaptor.getValue().getApi());
    assertEquals(PERIOD, resultCaptor.getValue().getDate());
  }

  @Test
  public void shouldFetchCpsFromFcWithNextPeriod() {
    // given
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(CP_API)).thenReturn(PERIOD);
    when(fcIntegrationCmmCpService.processCps(any(), any())).thenReturn(true);
    when(dateHelper.getCurrentMonthStr()).thenReturn(NEXT_PERIOD);

    // when
    fcScheduleService.fetchCpsFromFc();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(CP_API, resultCaptor.getValue().getApi());
    assertEquals(NEXT_PERIOD, resultCaptor.getValue().getDate());
  }

  @Test
  public void shouldFetchProgramsFromFc() {
    // given
    when(callFcService.getPrograms()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(PROGRAM_API)).thenReturn(DATE);
    when(fcProgramService.processPrograms(any())).thenReturn(true);

    // when
    fcScheduleService.fetchProgramsFromFcForScheduled();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    ArgumentCaptor<FcIntegrationResultDto> captor =
        ArgumentCaptor.forClass(FcIntegrationResultDto.class);
    verify(fcIntegrationResultService).recordFcIntegrationResult(captor.capture());
    assertEquals(PROGRAM_API, captor.getValue().getApi());
  }

  @Test
  public void shouldFetchRegimensFromFc() {
    // given
    when(callFcService.getRegimens()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(REGIMEN_API)).thenReturn(DATE);
    when(fcRegimenService.processRegimens(any())).thenReturn(true);

    // when
    fcScheduleService.fetchRegimenFromFcForScheduled();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    ArgumentCaptor<FcIntegrationResultDto> captor =
        ArgumentCaptor.forClass(FcIntegrationResultDto.class);
    verify(fcIntegrationResultService).recordFcIntegrationResult(captor.capture());
    assertEquals(REGIMEN_API, captor.getValue().getApi());
  }

  @Test
  public void shouldFetchFacilityTypeFromFc() {
    // given
    when(callFcService.getFacilityTypes()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(FACILITY_TYPE_API)).thenReturn(DATE);
    when(fcFacilityTypeService.processFacilityTypes(any())).thenReturn(true);

    // when
    fcScheduleService.fetchFacilityTypeFromFcForScheduled();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    ArgumentCaptor<FcIntegrationResultDto> captor =
        ArgumentCaptor.forClass(FcIntegrationResultDto.class);
    verify(fcIntegrationResultService).recordFcIntegrationResult(captor.capture());
    assertEquals(FACILITY_TYPE_API, captor.getValue().getApi());
  }

  @Test
  public void shouldUseLatestSuccessDateWhenFetchReceiptPlan() {
    // given
    when(fcIntegrationResultService.getLatestSuccessDate(RECEIPT_PLAN_API)).thenReturn(DATE);

    // when
    fcScheduleService.fetchReceiptPlan();

    // then
    ArgumentCaptor<String> captor =
        ArgumentCaptor.forClass(String.class);
    verify(siglusReceiptPlanService).processingReceiptPlans(captor.capture());
    assertEquals(DATE, captor.getValue());
  }

  @Test(expected = Exception.class)
  public void shouldRecordCallFcFailedWhenFetchDataFromFcFailed() {
    // given
    when(callFcService.getPageInfoDto()).thenReturn(null);

    // when
    fcScheduleService.fetchDataFromFc(ISSUE_VOUCHER_API, DATE);

    // then
    verify(fcIntegrationResultService).recordCallFcFailed(anyString(), anyString());
  }

}
