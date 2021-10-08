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
import org.siglus.siglusapi.domain.CpDomain;
import org.siglus.siglusapi.dto.fc.CpDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.repository.CpRepository;

@RunWith(MockitoJUnitRunner.class)
public class FcCpServiceTest {

  @InjectMocks
  private FcCpService fcCpService;

  @Captor
  private ArgumentCaptor<CpDomain> cpArgumentCaptor;

  @Mock
  private CpRepository cpRepository;

  private final String facilityCode = "facilityCode";
  private final String productCode = "productCode";
  private final String period = "M5";
  private final int year = 2020;
  private final String queryDate = "05-2020";
  private final UUID cpId = UUID.randomUUID();

  @Test
  public void shouldReturnWhenCpDataEmpty() {
    // given
    CpDto dto = CpDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    dto.setLastUpdatedAt(LAST_UPDATED_AT.plusDays(1));

    // when
    FcIntegrationResultDto result = fcCpService.processData(emptyList(), queryDate, LAST_UPDATED_AT);

    // then
    verify(cpRepository, times(0)).findCpByFacilityCodeAndProductCodeAndQueryDate(facilityCode, productCode, queryDate);
    verify(cpRepository, times(0)).save(any(CpDomain.class));
    assertNull(result);
  }

  @Test
  public void shouldFinalSuccessBeTrueWhenCreateCpSuccess() {
    // given
    CpDto dto = CpDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    dto.setLastUpdatedAt(LAST_UPDATED_AT.plusDays(1));

    // when
    FcIntegrationResultDto result = fcCpService.processData(newArrayList(dto), queryDate, LAST_UPDATED_AT);

    // then
    verify(cpRepository).findCpByFacilityCodeAndProductCodeAndQueryDate(facilityCode, productCode, queryDate);
    verify(cpRepository).save(any(CpDomain.class));
    assertEquals(LAST_UPDATED_AT.plusDays(1), result.getLastUpdatedAt());
    assertTrue(result.getFinalSuccess());
  }

  @Test
  public void shouldFinalSuccessBeTrueWhenUpdateCpSuccess() {
    // given
    CpDto dto = CpDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    dto.setLastUpdatedAt(LAST_UPDATED_AT.plusDays(1));
    CpDomain existCp = new CpDomain();
    existCp.setId(cpId);
    when(cpRepository.findCpByFacilityCodeAndProductCodeAndQueryDate(facilityCode,
        productCode, queryDate)).thenReturn(existCp);

    // when
    FcIntegrationResultDto result = fcCpService.processData(newArrayList(dto), queryDate, LAST_UPDATED_AT);

    // then
    verify(cpRepository).save(cpArgumentCaptor.capture());
    assertEquals(cpId, cpArgumentCaptor.getValue().getId());
    assertEquals(LAST_UPDATED_AT.plusDays(1), result.getLastUpdatedAt());
    assertTrue(result.getFinalSuccess());
  }

  @Test
  public void shouldFinalSuccessBeFalseWhenSaveCpFailed() {
    // given
    CpDto dto = CpDto.builder()
        .clientCode(facilityCode)
        .productFnm(productCode)
        .period(period)
        .year(year)
        .build();
    dto.setLastUpdatedAt(LAST_UPDATED_AT.plusDays(1));
    when(cpRepository.save(any(CpDomain.class))).thenThrow(new RuntimeException());

    // when
    FcIntegrationResultDto result = fcCpService.processData(newArrayList(dto), queryDate, LAST_UPDATED_AT);

    // then
    verify(cpRepository).findCpByFacilityCodeAndProductCodeAndQueryDate(facilityCode, productCode, queryDate);
    verify(cpRepository).save(any(CpDomain.class));
    assertFalse(result.getFinalSuccess());
    assertEquals(LAST_UPDATED_AT, result.getLastUpdatedAt());
  }

}
