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
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.siglus.siglusapi.service.SiglusFcIntegrationService;

@RunWith(MockitoJUnitRunner.class)
public class FcScheduleServiceTest {

  public static final String DATE = "20200825";
  public static final String PERIOD = "08-2020";

  @InjectMocks
  private FcScheduleService fcScheduleService;

  @Mock
  private CallFcService callFcService;

  @Mock
  private FcIntegrationResultService fcIntegrationResultService;

  @Mock
  private SiglusFcIntegrationService fcIntegrationService;

  @Before
  public void setup() {
    when(fcIntegrationService.createIssueVouchers(any())).thenReturn(true);
  }

  @Test
  public void shouldFetchReceiptPlanFromFc() {
    // given
    when(callFcService.getReceiptPlans()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(RECEIPT_PLAN_API)).thenReturn(DATE);

    // when
    fcScheduleService.fetchReceiptPlansFromFc();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    ArgumentCaptor<FcIntegrationResultDto> captor =
        ArgumentCaptor.forClass(FcIntegrationResultDto.class);
    verify(fcIntegrationResultService).recordFcIntegrationResult(captor.capture());
    assertEquals(RECEIPT_PLAN_API, captor.getValue().getApi());
  }

  @Test
  public void shouldFetchIssueVohcerFromFc() {
    // given
    when(callFcService.getIssueVouchers()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(ISSUE_VOUCHER_API)).thenReturn(DATE);

    // when
    fcScheduleService.fetchIssueVouchersFromFc();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    ArgumentCaptor<FcIntegrationResultDto> captor =
        ArgumentCaptor.forClass(FcIntegrationResultDto.class);
    verify(fcIntegrationResultService).recordFcIntegrationResult(captor.capture());
    assertEquals(ISSUE_VOUCHER_API, captor.getValue().getApi());
  }

  @Test
  public void shouldFetchCmmsFromFc() {
    // given
    when(callFcService.getIssueVouchers()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(CMM_API)).thenReturn(PERIOD);

    // when
    fcScheduleService.fetchCmmsFromFc();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    ArgumentCaptor<FcIntegrationResultDto> captor =
        ArgumentCaptor.forClass(FcIntegrationResultDto.class);
    verify(fcIntegrationResultService).recordFcIntegrationResult(captor.capture());
    assertEquals(CMM_API, captor.getValue().getApi());
  }

  @Test
  public void shouldFetchCpsFromFc() {
    // given
    when(callFcService.getIssueVouchers()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(CP_API)).thenReturn(PERIOD);

    // when
    fcScheduleService.fetchCpsFromFc();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    ArgumentCaptor<FcIntegrationResultDto> captor =
        ArgumentCaptor.forClass(FcIntegrationResultDto.class);
    verify(fcIntegrationResultService).recordFcIntegrationResult(captor.capture());
    assertEquals(CP_API, captor.getValue().getApi());
  }

  @Test
  public void shouldUseLatestSuccessDateWhenFetchReceiptPlansFromFc() {
    // given
    when(callFcService.getReceiptPlans()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(fcIntegrationResultService.getLatestSuccessDate(RECEIPT_PLAN_API)).thenReturn(DATE);

    // when
    fcScheduleService.fetchReceiptPlansFromFc();

    // then
    ArgumentCaptor<FcIntegrationResultDto> captor =
        ArgumentCaptor.forClass(FcIntegrationResultDto.class);
    verify(fcIntegrationResultService).recordFcIntegrationResult(captor.capture());
    assertEquals(DATE, captor.getValue().getDate());
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