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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.web.LotController;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.springframework.beans.BeanUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusLotReferenceDataServiceTest {

  @InjectMocks
  private SiglusLotReferenceDataService siglusLotReferenceDataService;

  @Mock
  private LotController lotController;


  @Test
  public void shouldCreateLotWhenSaveLot() {
    // given
    final org.openlmis.referencedata.dto.LotDto lotDtoResponse = new org.openlmis.referencedata.dto.LotDto();
    LotDto result = new LotDto();
    BeanUtils.copyProperties(lotDtoResponse, result);


    when(lotController.createLot(any(), any())).thenReturn(lotDtoResponse);
    // when
    siglusLotReferenceDataService.saveLot(LotDto.builder().build());
    // then
    verify(lotController, times(1)).createLot(any(), any());
  }

  @Test
  public void shouldCreateLotWhenBatchSaveLot() {
    // given
    final org.openlmis.referencedata.dto.LotDto lotDtoResponse = new org.openlmis.referencedata.dto.LotDto();
    LotDto result = new LotDto();
    BeanUtils.copyProperties(lotDtoResponse, result);

    // when
    siglusLotReferenceDataService.batchSaveLot(Lists.newArrayList(LotDto.builder().build()));
    //then
    verify(lotController, times(1)).batchCreateNewLot(any(List.class));
  }
}
