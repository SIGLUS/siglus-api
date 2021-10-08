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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.service.fc.FcVariables.LAST_UPDATED_AT;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.CmmDomain;
import org.siglus.siglusapi.dto.fc.CmmDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.repository.CmmRepository;

@RunWith(MockitoJUnitRunner.class)
public class FcCmmServiceTest {

  @InjectMocks
  private FcCmmService fcCmmService;

  @Captor
  private ArgumentCaptor<CmmDomain> cmmArgumentCaptor;

  @Mock
  private CmmRepository cmmRepository;

  private final String facilityCode = "facilityCode";
  private final String productCode = "productCode";
  private final String period = "M5";
  private final int year = 2020;
  private final String queryDate = "05-2020";
  private final UUID cmmId = UUID.randomUUID();

  @Test
  public void shouldReturnWhenCmmDataEmpty() {
    // given
    CmmDto dto = CmmDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    dto.setLastUpdatedAt(LAST_UPDATED_AT.plusDays(1));

    // when
    FcIntegrationResultDto result = fcCmmService.processData(emptyList(), queryDate, LAST_UPDATED_AT);

    // then
    verify(cmmRepository, times(0)).findCmmByFacilityCodeAndProductCodeAndQueryDate(
        facilityCode, productCode, queryDate);
    verify(cmmRepository, times(0)).save(any(CmmDomain.class));
    assertNull(result);
  }

  @Test
  public void shouldFinalSuccessBeTrueWhenCreateCmmSuccess() {
    // given
    CmmDto dto = CmmDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    dto.setLastUpdatedAt(LAST_UPDATED_AT.plusDays(1));

    // when
    FcIntegrationResultDto result = fcCmmService.processData(newArrayList(dto), queryDate, LAST_UPDATED_AT);

    // then
    verify(cmmRepository).findCmmByFacilityCodeAndProductCodeAndQueryDate(facilityCode, productCode, queryDate);
    verify(cmmRepository).save(any(CmmDomain.class));
    assertEquals(LAST_UPDATED_AT.plusDays(1), result.getLastUpdatedAt());
    assertTrue(result.getFinalSuccess());
  }

  @Test
  public void shouldFinalSuccessBeTrueWhenUpdateCmmSuccess() {
    // given
    CmmDto dto = CmmDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    dto.setLastUpdatedAt(LAST_UPDATED_AT.plusDays(1));
    CmmDomain existCmm = new CmmDomain();
    existCmm.setId(cmmId);
    when(cmmRepository.findCmmByFacilityCodeAndProductCodeAndQueryDate(facilityCode,
        productCode, queryDate)).thenReturn(existCmm);

    // when
    FcIntegrationResultDto result = fcCmmService.processData(newArrayList(dto), queryDate, LAST_UPDATED_AT);

    // then
    verify(cmmRepository).save(cmmArgumentCaptor.capture());
    assertEquals(cmmId, cmmArgumentCaptor.getValue().getId());
    assertEquals(LAST_UPDATED_AT.plusDays(1), result.getLastUpdatedAt());
    assertTrue(result.getFinalSuccess());
  }

  @Test
  public void shouldFinalSuccessBeFalseWhenSaveCmmFailed() {
    // given
    CmmDto dto = CmmDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    dto.setLastUpdatedAt(LAST_UPDATED_AT.plusDays(1));
    when(cmmRepository.save(any(CmmDomain.class))).thenThrow(new RuntimeException());

    // when
    FcIntegrationResultDto result = fcCmmService.processData(newArrayList(dto), queryDate, LAST_UPDATED_AT);

    // then
    verify(cmmRepository).findCmmByFacilityCodeAndProductCodeAndQueryDate(facilityCode, productCode, queryDate);
    verify(cmmRepository).save(any(CmmDomain.class));
    assertFalse(result.getFinalSuccess());
    assertEquals(LAST_UPDATED_AT, result.getLastUpdatedAt());
  }

}
