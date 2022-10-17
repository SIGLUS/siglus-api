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

package org.siglus.siglusapi.web.withlocation;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.siglus.siglusapi.dto.StockEventForMultiUserDto;
import org.siglus.siglusapi.service.SiglusStockEventsService;
import org.siglus.siglusapi.util.MovementDateValidator;

@RunWith(MockitoJUnitRunner.class)
public class SiglusStockEventsWithLocationControllerTest {

  @InjectMocks
  private SiglusStockEventsWithLocationController controller;

  @Mock
  private SiglusStockEventsService service;

  @Mock
  private MovementDateValidator movementDateValidator;

  private final UUID facilityId = UUID.randomUUID();
  private final LocalDate occurredDate = LocalDate.now();

  @Test
  public void shouldCallServiceWhenCreateStockEventGivenAllProductsProgramId() {
    // given
    StockEventDto dto = new StockEventDto();
    dto.setProgramId(ALL_PRODUCTS_PROGRAM_ID);

    // when
    controller.createStockEventWithLocation(dto);

    // then
    verify(service).processStockEvent(dto, true);
  }

  @Test
  public void shouldCallServiceWhenCreateStockEventGivenRandomProgramId() {
    // given
    StockEventDto dto = new StockEventDto();
    dto.setProgramId(UUID.randomUUID());

    // when
    controller.createStockEventWithLocation(dto);

    // then
    verify(service).processStockEvent(dto, true);
  }

  @Test
  public void shouldCallServiceWhenCreateStockEventsForMultiUser() {
    StockEventForMultiUserDto stockEventForMultiUserDto = new StockEventForMultiUserDto();
    StockEventLineItemDto stockEventLineItemDto = new StockEventLineItemDto();
    stockEventLineItemDto.setOccurredDate(occurredDate);
    StockEventDto stockEventDto = StockEventDto.builder()
        .facilityId(facilityId)
        .lineItems(newArrayList(stockEventLineItemDto))
        .build();
    stockEventForMultiUserDto.setStockEvent(stockEventDto);
    doNothing().when(movementDateValidator).validateMovementDate(occurredDate, facilityId);

    controller.createStockEventForMultiUserWithLocation(stockEventForMultiUserDto);

    verify(service).processStockEventForMultiUser(stockEventForMultiUserDto, true);
  }

}