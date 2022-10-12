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
import org.siglus.siglusapi.service.SiglusStockCardLocationMovementService;
import org.siglus.siglusapi.service.SiglusStockCardService;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
public class SiglusStockCardSummariesWithLocationControllerTest {

  @InjectMocks
  private SiglusStockCardSummariesWithLocationController controller;

  @Mock
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Mock
  private SiglusStockCardService siglusStockCardService;

  @Mock
  private SiglusStockCardLocationMovementService siglusStockCardLocationMovementService;

  private final UUID draftId = UUID.randomUUID();
  private final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
  private final Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);
  private final UUID stockCardId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();

  private final String locationCode = "AAA";

  @Test
  public void shouldCallServiceAndBuilderWhenSearchStockCardSummaries() {
    controller.getStockCardSummaryDtos(parameters, draftId, pageable);

    verify(siglusStockCardSummariesService)
        .getStockCardSummaryWithLocationDtos(parameters, draftId, pageable);
  }

  @Test
  public void shouldCallServiceAndBuilderWhenSearchStockCardById() {
    controller.searchStockCardById(stockCardId);

    verify(siglusStockCardService).findStockCardWithLocationById(stockCardId);
  }

  @Test
  public void shouldCallServiceWhenGetMovementByOrderableIdAndFacilityId() {
    controller.getProductMovement(orderableId, facilityId);

    verify(siglusStockCardService).getMovementByProduct(facilityId, orderableId);
  }

  @Test
  public void shouldCallServiceWhenGetLocationMovement() {
    controller.getLocationMovement(stockCardId, locationCode);

    verify(siglusStockCardLocationMovementService).getLocationMovementDto(stockCardId, locationCode);
  }
}
