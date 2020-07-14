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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.siglus.common.domain.referencedata.User;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.service.SiglusStockEventsService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusStockEventsControllerTest {

  @InjectMocks
  private SiglusStockEventsController controller;

  @Mock
  private SiglusStockEventsService service;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Before
  public void prepare() {
    User user = new User();
    user.setId(UUID.randomUUID());
    when(authenticationHelper.getCurrentUser()).thenReturn(user);
  }

  @Test
  public void shouldCallServiceWhenCreateStockEventGivenAllProductsProgramId() {
    StockEventDto dto  = new StockEventDto();
    dto.setProgramId(ALL_PRODUCTS_PROGRAM_ID);
    controller.createStockEvent(dto);

    verify(service).createAndFillLotId(dto);
    assertEquals(dto.getUserId(), authenticationHelper.getCurrentUser().getId());
    verify(service).createStockEventForAllProducts(dto);
  }

  @Test
  public void shouldCallServiceWhenCreateStockEventGivenRandomProgramId() {
    StockEventDto dto  = new StockEventDto();
    dto.setProgramId(UUID.randomUUID());
    controller.createStockEvent(dto);

    verify(service).createAndFillLotId(dto);
    assertEquals(dto.getUserId(), authenticationHelper.getCurrentUser().getId());
    verify(service).createStockEvent(dto);
  }

}