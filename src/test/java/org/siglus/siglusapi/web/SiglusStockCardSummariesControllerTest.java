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

import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
public class SiglusStockCardSummariesControllerTest {

  @InjectMocks
  private SiglusStockCardSummariesController controller;

  @Mock
  private SiglusStockCardSummariesService service;

  private final UUID draftId = UUID.randomUUID();
  private final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
  private final Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);

  @Test
  public void shouldCallServiceAndBuilderWhenSearchStockCardSummaries() {
    controller.searchStockCardSummaries(parameters, Collections.emptyList(), draftId, pageable);

    verify(service).searchStockCardSummaryV2Dtos(parameters, Collections.emptyList(), draftId, pageable, false);
  }

  @Test
  public void shouldCallServiceWhenGetStockCardDetailsDtoByGroup() {
    controller.getStockCardDetailsDtoByGroup(parameters, Collections.emptyList(), draftId, pageable);

    verify(service).getStockCardDetailsDtoByGroup(parameters, Collections.emptyList(), draftId, pageable);
  }

  @Test
  public void shouldCallServiceWhenGetStockCardDetailsDtos() {
    controller.getStockCardSummaryDtos(parameters, Collections.emptyList(), draftId, pageable);

    verify(service).getStockCardSummaryDtos(parameters, Collections.emptyList(), draftId, pageable);
  }

}
