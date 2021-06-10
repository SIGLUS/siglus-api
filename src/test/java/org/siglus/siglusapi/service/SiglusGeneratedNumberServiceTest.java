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

package org.siglus.siglusapi.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.GeneratedNumber;
import org.siglus.siglusapi.repository.GeneratedNumberRepository;

@RunWith(MockitoJUnitRunner.class)
public class SiglusGeneratedNumberServiceTest {

  @Captor
  private ArgumentCaptor<GeneratedNumber> generatedNumberArgumentCaptor;

  @InjectMocks
  private SiglusGeneratedNumberService siglusGeneratedNumberService;

  @Mock
  private GeneratedNumberRepository generatedNumberRepository;

  private final UUID facilityId = UUID.randomUUID();

  @Test
  public void shouldSave0WhenGeneratedNumberIsNull() {
    // given
    when(generatedNumberRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(generatedNumberRepository.save(any(GeneratedNumber.class)))
        .thenReturn(GeneratedNumber.builder().number(0).build());

    // when
    siglusGeneratedNumberService.getGeneratedNumber(facilityId);

    // then
    verify(generatedNumberRepository).save(generatedNumberArgumentCaptor.capture());
    GeneratedNumber generatedNumber = generatedNumberArgumentCaptor.getValue();
    assertEquals(facilityId, generatedNumber.getFacilityId());
    assertEquals(0, generatedNumber.getNumber().intValue());
  }


  @Test
  public void shouldSaveWhenGeneratedNumberIsNotNull() {
    // given
    when(generatedNumberRepository.findByFacilityId(facilityId))
        .thenReturn(GeneratedNumber.builder().facilityId(facilityId).number(3).build());
    when(generatedNumberRepository.save(any(GeneratedNumber.class)))
        .thenReturn(GeneratedNumber.builder().number(4).build());

    // when
    siglusGeneratedNumberService.getGeneratedNumber(facilityId);

    // then
    verify(generatedNumberRepository).save(generatedNumberArgumentCaptor.capture());
    GeneratedNumber generatedNumber = generatedNumberArgumentCaptor.getValue();
    assertEquals(facilityId, generatedNumber.getFacilityId());
    assertEquals(4, generatedNumber.getNumber().intValue());
  }

}
