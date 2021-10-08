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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import static org.siglus.siglusapi.service.fc.FcVariables.LAST_UPDATED_AT;

import java.util.ArrayList;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.fc.CmmDto;
import org.siglus.siglusapi.dto.fc.CpDto;
import org.siglus.siglusapi.dto.fc.FcFacilityDto;
import org.siglus.siglusapi.dto.fc.FcFacilityTypeDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneNationalDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.siglus.siglusapi.dto.fc.ProductInfoDto;
import org.siglus.siglusapi.dto.fc.ProgramDto;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
import org.siglus.siglusapi.dto.fc.RegimenDto;
import org.siglus.siglusapi.service.client.SiglusIssueVoucherService;
import org.siglus.siglusapi.service.client.SiglusReceiptPlanService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class FcScheduleServiceTest {

  public static final String DATE = "20000101";
  public static final String PERIOD = "08-2020";

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
  private FcCmmService fcCmmService;

  @Mock
  private FcCpService fcCpService;

  @Mock
  private FcReceiptPlanService fcReceiptPlanService;

  @Mock
  private SiglusReceiptPlanService siglusReceiptPlanService;

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
  private FcFacilityService fcFacilityService;

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @Before
  public void setup() {
    when(fcIssueVoucherService.processData(any(), any(), any())).thenReturn(new FcIntegrationResultDto());
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(any(), any())).thenReturn(true);
    doNothing().when(redisTemplate).delete(anyString());
  }

  @Test
  public void shouldFetchReceiptPlanFromFc() {
    // given
    when(callFcService.getReceiptPlans()).thenReturn(Collections.singletonList(new ReceiptPlanDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(RECEIPT_PLAN_API)).thenReturn(LAST_UPDATED_AT);
    when(fcReceiptPlanService.processData(any(), any(), any())).thenReturn(FcIntegrationResultDto.builder()
        .api(RECEIPT_PLAN_API).build());

    // when
    fcScheduleService.syncReceiptPlans(DATE);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcReceiptPlanService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(RECEIPT_PLAN_API, resultCaptor.getValue().getApi());
  }

  @Test
  public void shouldDeleteRedisKeyWhenFetchReceiptPlansScheduleFromFcError() {
    // given
    when(fcIntegrationResultService.getLastUpdatedAt(RECEIPT_PLAN_API)).thenReturn(LAST_UPDATED_AT);
    doThrow(new RuntimeException()).when(siglusReceiptPlanService).processingReceiptPlans(anyString());

    // when
    fcScheduleService.syncReceiptPlansScheduler();

    // then
    verify(redisTemplate).delete(anyString());
  }

  @Test
  public void shouldGeographicZoneScheduleFromFc() {
    // given
    when(callFcService.getGeographicZones()).thenReturn(Collections.singletonList(new FcGeographicZoneNationalDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(GEOGRAPHIC_ZONE_API)).thenReturn(LAST_UPDATED_AT);
    when(fcGeographicZoneService.processData(any(), any(), any())).thenReturn(FcIntegrationResultDto.builder()
        .api(GEOGRAPHIC_ZONE_API).build());

    // when
    fcScheduleService.syncGeographicZones(DATE);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcGeographicZoneService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(GEOGRAPHIC_ZONE_API, resultCaptor.getValue().getApi());
  }

  @Test
  public void shouldFetchFacilityScheduleFromFc() {
    // given
    when(callFcService.getFacilities()).thenReturn(Collections.singletonList(new FcFacilityDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(FACILITY_API)).thenReturn(LAST_UPDATED_AT);
    when(fcFacilityService.processData(any(), any(), any())).thenReturn(FcIntegrationResultDto.builder()
        .api(FACILITY_API).build());

    // when
    fcScheduleService.syncFacilities(DATE);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcFacilityService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(FACILITY_API, resultCaptor.getValue().getApi());
  }


  @Test
  public void shouldFetchIssueVoucherScheduleFromFc() {
    // given
    when(callFcService.getIssueVouchers()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(ISSUE_VOUCHER_API)).thenReturn(LAST_UPDATED_AT);

    // when
    fcScheduleService.syncIssueVouchersScheduler();

    // then
    verify(redisTemplate).delete(anyString());
    verify(issueVoucherService).updateIssueVoucher(anyString());
  }

  @Test
  public void shouldDeleteRedisKeyWhenFetchIssueVoucherScheduleFromFcError() {
    // given
    when(fcIntegrationResultService.getLastUpdatedAt(ISSUE_VOUCHER_API)).thenReturn(LAST_UPDATED_AT);
    doThrow(new RuntimeException()).when(issueVoucherService).updateIssueVoucher(anyString());

    // when
    fcScheduleService.syncIssueVouchersScheduler();

    // then
    verify(redisTemplate).delete(anyString());
  }


  @Test
  public void shouldFetchIssueVoucherFromFc() {
    // given
    when(callFcService.getIssueVouchers()).thenReturn(Collections.singletonList(new IssueVoucherDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIssueVoucherService.processData(any(), any(), any())).thenReturn(FcIntegrationResultDto.builder()
        .api(ISSUE_VOUCHER_API).build());

    // when
    fcScheduleService.syncIssueVouchers(DATE);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcIssueVoucherService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(ISSUE_VOUCHER_API, resultCaptor.getValue().getApi());
  }

  @Test
  public void shouldFetchProductsFromFc() {
    // given
    when(callFcService.getProducts()).thenReturn(Collections.singletonList(new ProductInfoDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(PRODUCT_API)).thenReturn(LAST_UPDATED_AT);
    when(fcProductService.processData(any(), any(), any())).thenReturn(FcIntegrationResultDto.builder()
        .api(PRODUCT_API).build());

    // when
    fcScheduleService.syncProducts(DATE);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcProductService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(PRODUCT_API, resultCaptor.getValue().getApi());
  }

  @Test
  public void shouldFetchCmmsFromFcWithCurrentPeriod() {
    // given
    when(callFcService.getCmms()).thenReturn(Collections.singletonList(new CmmDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(CMM_API)).thenReturn(LAST_UPDATED_AT);
    when(fcCmmService.processData(any(), any(), any())).thenReturn(
        FcIntegrationResultDto.builder().api(CMM_API).startDate(PERIOD).build());

    // when
    fcScheduleService.syncCmms(PERIOD);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcCmmService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(CMM_API, resultCaptor.getValue().getApi());
    assertEquals(PERIOD, resultCaptor.getValue().getStartDate());
  }

  @Test
  public void shouldFetchCmmsFromFcWithNextPeriod() {
    // given
    when(callFcService.getCmms()).thenReturn(Collections.singletonList(new CmmDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(CMM_API)).thenReturn(LAST_UPDATED_AT);
    when(fcCmmService.processData(any(), any(), any())).thenReturn(
        FcIntegrationResultDto.builder().api(CMM_API).startDate(PERIOD).build());

    // when
    fcScheduleService.syncCmms(PERIOD);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcCmmService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(CMM_API, resultCaptor.getValue().getApi());
    assertEquals(PERIOD, resultCaptor.getValue().getStartDate());
  }

  @Test
  public void shouldFetchCpsFromFcWithCurrentPeriod() {
    // given
    when(callFcService.getCps()).thenReturn(Collections.singletonList(new CpDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(CP_API)).thenReturn(LAST_UPDATED_AT);
    when(fcCpService.processData(any(), any(), any())).thenReturn(
        FcIntegrationResultDto.builder().api(CP_API).startDate(PERIOD).build());

    // when
    fcScheduleService.syncCps(PERIOD);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcCpService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(CP_API, resultCaptor.getValue().getApi());
    assertEquals(PERIOD, resultCaptor.getValue().getStartDate());
  }

  @Test
  public void shouldFetchCpsFromFcWithNextPeriod() {
    // given
    when(callFcService.getCps()).thenReturn(Collections.singletonList(new CpDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(CP_API)).thenReturn(LAST_UPDATED_AT);
    when(fcCpService.processData(any(), any(), any())).thenReturn(
        FcIntegrationResultDto.builder().api(CP_API).startDate(PERIOD).build());

    // when
    fcScheduleService.syncCps(PERIOD);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcCpService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(CP_API, resultCaptor.getValue().getApi());
    assertEquals(PERIOD, resultCaptor.getValue().getStartDate());
  }

  @Test
  public void shouldFetchProgramsFromFc() {
    // given
    when(callFcService.getPrograms()).thenReturn(Collections.singletonList(new ProgramDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(PROGRAM_API)).thenReturn(LAST_UPDATED_AT);
    when(fcProgramService.processData(any(), any(), any())).thenReturn(FcIntegrationResultDto.builder()
        .api(PROGRAM_API).build());

    // when
    fcScheduleService.syncPrograms(DATE);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcProgramService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(PROGRAM_API, resultCaptor.getValue().getApi());
  }

  @Test
  public void shouldFetchRegimensFromFc() {
    // given
    when(callFcService.getRegimens()).thenReturn(Collections.singletonList(new RegimenDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(REGIMEN_API)).thenReturn(LAST_UPDATED_AT);
    when(fcRegimenService.processData(any(), any(), any())).thenReturn(FcIntegrationResultDto.builder()
        .api(REGIMEN_API).build());

    // when
    fcScheduleService.syncRegimens(DATE);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcRegimenService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(REGIMEN_API, resultCaptor.getValue().getApi());
  }

  @Test
  public void shouldFetchFacilityTypeFromFc() {
    // given
    when(callFcService.getFacilityTypes()).thenReturn(Collections.singletonList(new FcFacilityTypeDto()));
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLastUpdatedAt(FACILITY_TYPE_API)).thenReturn(LAST_UPDATED_AT);
    when(fcFacilityTypeService.processData(any(), any(), any())).thenReturn(FcIntegrationResultDto.builder()
        .api(FACILITY_TYPE_API).build());

    // when
    fcScheduleService.syncFacilityTypes(DATE);

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcFacilityTypeService).processData(any(), any(), any());
    verify(fcIntegrationResultService).recordFcIntegrationResult(resultCaptor.capture());
    assertEquals(FACILITY_TYPE_API, resultCaptor.getValue().getApi());
  }

  @Test
  public void shouldUseLatestUpdatedAtDateWhenFetchReceiptPlan() {
    // given
    when(fcIntegrationResultService.getLastUpdatedAt(RECEIPT_PLAN_API)).thenReturn(LAST_UPDATED_AT);

    // when
    fcScheduleService.syncReceiptPlansScheduler();

    // then
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(siglusReceiptPlanService).processingReceiptPlans(captor.capture());
    assertEquals(DATE, captor.getValue());
  }

  @Test(expected = Exception.class)
  public void shouldRecordCallFcFailedWhenFetchDataFromFcFailed() {
    // given
    when(callFcService.getPageInfoDto()).thenReturn(null);

    // when
    fcScheduleService.fetchData(ISSUE_VOUCHER_API, DATE);
  }

}
