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
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FcConstants.CMM_API;
import static org.siglus.siglusapi.constant.FcConstants.CP_API;
import static org.siglus.siglusapi.constant.FcConstants.CP_JOB;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_JOB;
import static org.siglus.siglusapi.constant.FcConstants.PRODUCT_API;
import static org.siglus.siglusapi.constant.FcConstants.PRODUCT_JOB;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;

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
import org.siglus.siglusapi.domain.FcIntegrationResult;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.repository.FcIntegrationResultRepository;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class FcIntegrationResultServiceTest {

  public static final String YESTERDAY = "20200828";
  public static final String TODAY = "20200829";
  public static final String DEFAULT_PERIOD = "05-2020";

  @Captor
  private ArgumentCaptor<FcIntegrationResult> resultCaptor;

  @Mock
  private FcIntegrationResultRepository fcIntegrationResultRepository;

  @Mock
  private SiglusDateHelper dateHelper;

  @Mock
  private CallFcService callFcService;

  @InjectMocks
  private FcIntegrationResultService fcIntegrationResultService;

  @Before
  public void setup() {
    ReflectionTestUtils.setField(fcIntegrationResultService, "defaultStartDate", "20200501");
    ReflectionTestUtils.setField(fcIntegrationResultService, "defaultStartPeriod", DEFAULT_PERIOD);
  }

  @Test
  public void shouldGetLatestSuccessDate() {
    // given
    FcIntegrationResult result = FcIntegrationResult.builder()
        .endDate("20200805").build();
    when(fcIntegrationResultRepository.findTopByJobAndFinalSuccessOrderByEndDateDesc(anyString(),
        eq(true))).thenReturn(result);

    // when
    String date = fcIntegrationResultService.getLatestSuccessDate(ISSUE_VOUCHER_API);

    // then
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fcIntegrationResultRepository).findTopByJobAndFinalSuccessOrderByEndDateDesc(
        captor.capture(), eq(true));
    assertEquals(ISSUE_VOUCHER_JOB, captor.getValue());
    assertEquals("20200805", date);
  }

  @Test
  public void shouldGetLatestSuccessPeriod() {
    // given
    FcIntegrationResult result = FcIntegrationResult.builder()
        .endDate(DEFAULT_PERIOD).build();
    when(fcIntegrationResultRepository.findTopByJobAndFinalSuccessOrderByEndDateDesc(anyString(),
        eq(true))).thenReturn(result);

    // when
    String date = fcIntegrationResultService.getLatestSuccessDate(CP_API);

    // then
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fcIntegrationResultRepository).findTopByJobAndFinalSuccessOrderByEndDateDesc(
        captor.capture(), eq(true));
    assertEquals(CP_JOB, captor.getValue());
    assertEquals(DEFAULT_PERIOD, date);
  }


  @Test
  public void shouldGetDefaultDateWhenResultNotExisted() {
    // given
    when(fcIntegrationResultRepository.findTopByJobAndFinalSuccessOrderByEndDateDesc(anyString(),
        eq(true))).thenReturn(null);

    // when
    String date = fcIntegrationResultService.getLatestSuccessDate(ISSUE_VOUCHER_API);

    // then
    assertEquals("20200501", date);
  }

  @Test
  public void shouldGetDefaultPeroidWhenResultNotExisted() {
    // given
    when(fcIntegrationResultRepository.findTopByJobAndFinalSuccessOrderByEndDateDesc(anyString(),
        eq(true))).thenReturn(null);

    // when
    String date = fcIntegrationResultService.getLatestSuccessDate(CMM_API);

    // then
    assertEquals(DEFAULT_PERIOD, date);
  }

  @Test
  public void shouldCreateRecordWhenCallFcFailed() {
    // given
    when(dateHelper.getTodayDateStr()).thenReturn(TODAY);

    // when
    fcIntegrationResultService.recordCallFcFailed(RECEIPT_PLAN_API, YESTERDAY);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals("RECEIPT_PLAN", resultCaptor.getValue().getJob());
    assertEquals(YESTERDAY, resultCaptor.getValue().getStartDate());
    assertEquals(TODAY, resultCaptor.getValue().getEndDate());
    assertEquals(false, resultCaptor.getValue().getCallFcSuccess());
    assertNull(resultCaptor.getValue().getFinalSuccess());
  }

  @Test
  public void shouldGetJobNameWhenIsIssueVoucher() {
    // given
    when(dateHelper.getTodayDateStr()).thenReturn(TODAY);

    // when
    fcIntegrationResultService.recordCallFcFailed(ISSUE_VOUCHER_API, YESTERDAY);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals("ISSUE_VOUCHER", resultCaptor.getValue().getJob());
  }

  @Test
  public void shouldGetJobNameWhenIsCmm() {
    // given
    when(dateHelper.getTodayDateStr()).thenReturn(TODAY);

    // when
    fcIntegrationResultService.recordCallFcFailed(CMM_API, YESTERDAY);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals("CMM", resultCaptor.getValue().getJob());
  }

  @Test
  public void shouldGetJobNameWhenIsCp() {
    // given
    when(dateHelper.getTodayDateStr()).thenReturn(TODAY);

    // when
    fcIntegrationResultService.recordCallFcFailed(CP_API, YESTERDAY);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals("CP", resultCaptor.getValue().getJob());
  }

  @Test
  public void shouldGetJobNameWhenIsProduct() {
    // when
    fcIntegrationResultService.recordCallFcFailed(PRODUCT_API, YESTERDAY);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(PRODUCT_JOB, resultCaptor.getValue().getJob());
  }

  @Test
  public void shouldGetJobNameNullWhenApiIsNotExisted() {
    // given
    when(dateHelper.getTodayDateStr()).thenReturn(TODAY);

    // when
    fcIntegrationResultService.recordCallFcFailed("", YESTERDAY);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertNull(resultCaptor.getValue().getJob());
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallIssueVoucherApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(ISSUE_VOUCHER_API)
        .build();
    when(dateHelper.getTodayDateStr()).thenReturn(TODAY);

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(TODAY, resultCaptor.getValue().getEndDate());
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallCmmApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(CMM_API)
        .date(DEFAULT_PERIOD)
        .build();
    when(callFcService.getCmms()).thenReturn(new ArrayList<>());

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(DEFAULT_PERIOD, resultCaptor.getValue().getEndDate());
  }
}