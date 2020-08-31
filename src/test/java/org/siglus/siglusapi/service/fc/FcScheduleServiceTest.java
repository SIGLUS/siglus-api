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

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;

import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.dto.fc.PageInfoDto;

@RunWith(MockitoJUnitRunner.class)
public class FcScheduleServiceTest {

  public static final String YESTERDAY = "20200825";
  public static final String CURRENT_MONTH = "2020-08";

  @InjectMocks
  private FcScheduleService fcScheduleService;

  @Mock
  private CallFcService callFcService;

  @Mock
  private FcIntegrationResultService fcIntegrationResultService;

  @Mock
  private SiglusDateHelper dateHelper;

  @Test
  public void shouldFetchReceiptPlanFromFc() throws Exception {
    // given
    when(callFcService.getReceiptPlans()).thenReturn(new ArrayList<>());
    when(callFcService.getIssueVouchers()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(dateHelper.getYesterdayDateStr()).thenReturn(YESTERDAY);
    when(dateHelper.getCurrentMonthStr()).thenReturn(CURRENT_MONTH);

    // when
    fcScheduleService.fetchReceiptPlansFromFc();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
    verify(fcIntegrationResultService).recordFcIntegrationResult(anyString(), anyString(),
        anyBoolean(), anyInt(), anyBoolean(), anyInt());
  }

  @Test
  public void shouldFetchIssueVohcerFromFc() throws Exception {
    // given
    when(callFcService.getReceiptPlans()).thenReturn(new ArrayList<>());
    when(callFcService.getIssueVouchers()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(dateHelper.getYesterdayDateStr()).thenReturn(YESTERDAY);
    when(dateHelper.getCurrentMonthStr()).thenReturn(CURRENT_MONTH);

    // when
    fcScheduleService.fetchIssueVouchersFromFc();

    // then
    verify(callFcService).fetchData(anyString(), anyString());
  }

  @Test
  public void shouldUseDefaultDateWhenFetchDataFromFcIfDateIsEmpty() throws Exception {
    // given
    when(callFcService.getReceiptPlans()).thenReturn(new ArrayList<>());
    when(callFcService.getIssueVouchers()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(dateHelper.getYesterdayDateStr()).thenReturn(YESTERDAY);
    when(dateHelper.getCurrentMonthStr()).thenReturn(CURRENT_MONTH);

    // when
    fcScheduleService.fetchDataFromFc(RECEIPT_PLAN_API, "");

    // then
    verify(dateHelper).getYesterdayDateStr();
  }

  @Test
  public void shouldUseDefaultDateWhenFetchDataFromFcIfDateIsNull() throws Exception {
    // given
    when(callFcService.getReceiptPlans()).thenReturn(new ArrayList<>());
    when(callFcService.getIssueVouchers()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(dateHelper.getYesterdayDateStr()).thenReturn(YESTERDAY);
    when(dateHelper.getCurrentMonthStr()).thenReturn(CURRENT_MONTH);

    // when
    fcScheduleService.fetchDataFromFc(RECEIPT_PLAN_API, null);

    // then
    verify(dateHelper).getYesterdayDateStr();
  }

  @Test
  public void shouldNotUseDefaultDateWhenFetchDataFromFcIfDateIsNotEmpty() throws Exception {
    // given
    when(callFcService.getReceiptPlans()).thenReturn(new ArrayList<>());
    when(callFcService.getIssueVouchers()).thenReturn(new ArrayList<>());
    when(callFcService.getPageInfoDto()).thenReturn(new PageInfoDto());
    when(dateHelper.getYesterdayDateStr()).thenReturn(YESTERDAY);
    when(dateHelper.getCurrentMonthStr()).thenReturn(CURRENT_MONTH);

    // when
    fcScheduleService.fetchDataFromFc(RECEIPT_PLAN_API, "20200828");

    // then
    verify(dateHelper, times(0)).getYesterdayDateStr();
  }

}