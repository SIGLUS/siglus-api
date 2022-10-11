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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
import static org.siglus.siglusapi.service.fc.FcVariables.START_DATE;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.FcIntegrationResult;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.repository.FcIntegrationResultRepository;
import org.siglus.siglusapi.util.SiglusDateHelper;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class FcIntegrationResultServiceTest {

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

  @Test
  public void shouldGetNextStartDate() {
    // given
    FcIntegrationResult result = FcIntegrationResult.builder().lastUpdatedAt(LAST_UPDATED_AT).build();
    when(fcIntegrationResultRepository.findTopByApiAndFinalSuccessOrderByLastUpdatedAtDesc(anyString(),
        eq(true))).thenReturn(result);

    // when
    ZonedDateTime date = fcIntegrationResultService.getLastUpdatedAt(ISSUE_VOUCHER_API);

    // then
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fcIntegrationResultRepository).findTopByApiAndFinalSuccessOrderByLastUpdatedAtDesc(
        captor.capture(), eq(true));
    assertEquals(ISSUE_VOUCHER_API, captor.getValue());
    assertEquals(LAST_UPDATED_AT, date);
  }

  @Test
  public void shouldGetLatestSuccessPeriod() {
    // given
    FcIntegrationResult result = FcIntegrationResult.builder().lastUpdatedAt(LAST_UPDATED_AT).build();
    when(fcIntegrationResultRepository.findTopByApiAndFinalSuccessOrderByLastUpdatedAtDesc(anyString(),
        eq(true))).thenReturn(result);

    // when
    ZonedDateTime date = fcIntegrationResultService.getLastUpdatedAt(CP_API);

    // then
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(fcIntegrationResultRepository).findTopByApiAndFinalSuccessOrderByLastUpdatedAtDesc(
        captor.capture(), eq(true));
    assertEquals(CP_API, captor.getValue());
    assertEquals(LAST_UPDATED_AT, date);
  }


  @Test
  public void shouldGetDefaultDateWhenResultNotExisted() {
    // given
    when(fcIntegrationResultRepository.findTopByApiAndFinalSuccessOrderByLastUpdatedAtDesc(anyString(),
        eq(true))).thenReturn(null);

    // when
    ZonedDateTime date = fcIntegrationResultService.getLastUpdatedAt(ISSUE_VOUCHER_API);

    // then
    assertEquals(LAST_UPDATED_AT, date);
  }

  @Test
  public void shouldGetDefaultPeroidWhenResultNotExisted() {
    // given
    when(fcIntegrationResultRepository.findTopByApiAndFinalSuccessOrderByLastUpdatedAtDesc(anyString(),
        eq(true))).thenReturn(null);

    // when
    ZonedDateTime date = fcIntegrationResultService.getLastUpdatedAt(CMM_API);

    // then
    assertEquals(LAST_UPDATED_AT, date);
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallIssueVoucherApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(ISSUE_VOUCHER_API)
        .build();
    when(dateHelper.getTodayDateStr()).thenReturn(START_DATE);

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(START_DATE, resultCaptor.getValue().getEndDate());
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallCmmApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(CMM_API)
        .lastUpdatedAt(LAST_UPDATED_AT)
        .build();
    when(callFcService.getCmms()).thenReturn(new ArrayList<>());

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(LAST_UPDATED_AT, resultCaptor.getValue().getLastUpdatedAt());
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallCpApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(CP_API)
        .lastUpdatedAt(LAST_UPDATED_AT)
        .build();
    when(callFcService.getCps()).thenReturn(new ArrayList<>());

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(LAST_UPDATED_AT, resultCaptor.getValue().getLastUpdatedAt());
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallProgramApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(PROGRAM_API)
        .lastUpdatedAt(LAST_UPDATED_AT)
        .build();
    when(callFcService.getPrograms()).thenReturn(new ArrayList<>());

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(LAST_UPDATED_AT, resultCaptor.getValue().getLastUpdatedAt());
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallProductApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(PRODUCT_API)
        .lastUpdatedAt(LAST_UPDATED_AT)
        .build();
    when(callFcService.getProducts()).thenReturn(new ArrayList<>());

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(LAST_UPDATED_AT, resultCaptor.getValue().getLastUpdatedAt());
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallFacilityTypeApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(FACILITY_TYPE_API)
        .lastUpdatedAt(LAST_UPDATED_AT)
        .build();
    when(callFcService.getFacilityTypes()).thenReturn(new ArrayList<>());

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(LAST_UPDATED_AT, resultCaptor.getValue().getLastUpdatedAt());
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallFacilityApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(FACILITY_API)
        .lastUpdatedAt(LAST_UPDATED_AT)
        .build();
    when(callFcService.getFacilities()).thenReturn(new ArrayList<>());

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(LAST_UPDATED_AT, resultCaptor.getValue().getLastUpdatedAt());
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallRegimenApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(REGIMEN_API)
        .lastUpdatedAt(LAST_UPDATED_AT)
        .build();
    when(callFcService.getRegimens()).thenReturn(new ArrayList<>());

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(LAST_UPDATED_AT, resultCaptor.getValue().getLastUpdatedAt());
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallGzApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(GEOGRAPHIC_ZONE_API)
        .lastUpdatedAt(LAST_UPDATED_AT)
        .build();
    when(callFcService.getGeographicZones()).thenReturn(new ArrayList<>());

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(LAST_UPDATED_AT, resultCaptor.getValue().getLastUpdatedAt());
  }

  @Test
  public void shouldGetEndDateWhenRecordFcIntegrationResultWhenCallReceiptPlanApi() {
    // given
    FcIntegrationResultDto resultDto = FcIntegrationResultDto.builder()
        .api(RECEIPT_PLAN_API)
        .lastUpdatedAt(LAST_UPDATED_AT)
        .build();
    when(callFcService.getReceiptPlans()).thenReturn(new ArrayList<>());

    // when
    fcIntegrationResultService.recordFcIntegrationResult(resultDto);

    // then
    verify(fcIntegrationResultRepository).save(resultCaptor.capture());
    assertEquals(LAST_UPDATED_AT, resultCaptor.getValue().getLastUpdatedAt());
  }

}
