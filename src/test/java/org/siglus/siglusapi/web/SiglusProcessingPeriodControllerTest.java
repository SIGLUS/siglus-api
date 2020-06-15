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

package org.siglus.siglusapi.web;

import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
public class SiglusProcessingPeriodControllerTest {

  @InjectMocks
  private SiglusProcessingPeriodController controller;

  @Mock
  private SiglusProcessingPeriodService service;

  @Test
  public void shouldCallServiceCreateProcessingPeriodWhenCreateProcessingPeriod() {
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    controller.createProcessingPeriod(processingPeriodDto);

    verify(service).createProcessingPeriod(processingPeriodDto);
  }

  @Test
  public void shouldCallServiceGetAllProcessingPeriodsWhenSearchAllProcessingPeriods() {
    MultiValueMap<String, Object> requestParams = new LinkedMultiValueMap<>();
    Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);
    controller.searchAllProcessingPeriods(requestParams, pageable);

    verify(service).getAllProcessingPeriods(requestParams, pageable);
  }

  @Test
  public void shouldCallServiceGetProcessingPeriodWhenSearchProcessingPeriod() {
    UUID periodId = UUID.randomUUID();
    controller.searchProcessingPeriod(periodId);

    verify(service).getProcessingPeriod(periodId);
  }

}