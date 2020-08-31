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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FcConstants.CMM_API;
import static org.siglus.siglusapi.constant.FcConstants.CP_API;
import static org.siglus.siglusapi.constant.FcConstants.ISSUE_VOUCHER_API;
import static org.siglus.siglusapi.constant.FcConstants.RECEIPT_PLAN_API;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.domain.FcIntegrationResult;
import org.siglus.siglusapi.repository.FcIntegrationResultRepository;

@RunWith(MockitoJUnitRunner.class)
public class FcIntegrationResultServiceTest {

  public static final String YESTERDAY = "20200828";
  public static final String TODAY = "20200829";

  @Mock
  private FcIntegrationResultRepository fcIntegrationResultRepository;

  @Mock
  private SiglusDateHelper dateHelper;

  @InjectMocks
  private FcIntegrationResultService fcIntegrationResultService;

  @Test
  public void shouldCreateRecordWhenCallFcFailed() {
    // given
    when(dateHelper.getTodayDateStr()).thenReturn(TODAY);

    // when
    fcIntegrationResultService.recordCallFcFailed(RECEIPT_PLAN_API, YESTERDAY);

    // then
    ArgumentCaptor<FcIntegrationResult> captor = ArgumentCaptor.forClass(FcIntegrationResult.class);
    verify(fcIntegrationResultRepository).save(captor.capture());
    assertEquals("RECEIPT_PLAN", captor.getValue().getJob());
    assertEquals(YESTERDAY, captor.getValue().getStartDate());
    assertEquals(TODAY, captor.getValue().getEndDate());
    assertEquals(false, captor.getValue().getCallFcSuccess());
    assertEquals(false, captor.getValue().getFinalSuccess());
  }

  @Test
  public void shouldGetJobNameWhenIsIssueVoucher() {
    // given
    when(dateHelper.getTodayDateStr()).thenReturn(TODAY);

    // when
    fcIntegrationResultService.recordCallFcFailed(ISSUE_VOUCHER_API, YESTERDAY);

    // then
    ArgumentCaptor<FcIntegrationResult> captor = ArgumentCaptor.forClass(FcIntegrationResult.class);
    verify(fcIntegrationResultRepository).save(captor.capture());
    assertEquals("ISSUE_VOUCHER", captor.getValue().getJob());
  }

  @Test
  public void shouldGetJobNameWhenIsCmm() {
    // given
    when(dateHelper.getTodayDateStr()).thenReturn(TODAY);

    // when
    fcIntegrationResultService.recordCallFcFailed(CMM_API, YESTERDAY);

    // then
    ArgumentCaptor<FcIntegrationResult> captor = ArgumentCaptor.forClass(FcIntegrationResult.class);
    verify(fcIntegrationResultRepository).save(captor.capture());
    assertEquals("CMM", captor.getValue().getJob());
  }

  @Test
  public void shouldGetJobNameWhenIsCp() {
    // given
    when(dateHelper.getTodayDateStr()).thenReturn(TODAY);

    // when
    fcIntegrationResultService.recordCallFcFailed(CP_API, YESTERDAY);

    // then
    ArgumentCaptor<FcIntegrationResult> captor = ArgumentCaptor.forClass(FcIntegrationResult.class);
    verify(fcIntegrationResultRepository).save(captor.capture());
    assertEquals("CP", captor.getValue().getJob());
  }

  @Test
  public void shouldGetJobNameNullWhenApiIsNotExisted() {
    // given
    when(dateHelper.getTodayDateStr()).thenReturn(TODAY);

    // when
    fcIntegrationResultService.recordCallFcFailed("", YESTERDAY);

    // then
    ArgumentCaptor<FcIntegrationResult> captor = ArgumentCaptor.forClass(FcIntegrationResult.class);
    verify(fcIntegrationResultRepository).save(captor.capture());
    assertNull(captor.getValue().getJob());
  }
}