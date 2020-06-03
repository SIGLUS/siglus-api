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

package org.openlmis.fulfillment.web.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;

@RunWith(MockitoJUnitRunner.class)
public class BasicOrderDtoBuilderTest {

  @Mock
  private OrderExportHelper orderExportHelper;

  @InjectMocks
  private BasicOrderDtoBuilder orderDtoBuilder = new BasicOrderDtoBuilder();

  private Order order;

  @Before
  public void setUp() {
    order = new OrderDataBuilder()
        .build();
  }

  @Test
  public void shouldBuildDtoFromOrder() {
    BasicOrderDto orderDto = orderDtoBuilder.build(order);

    verify(orderExportHelper).setSubResources(orderDto, order, null, null, null, null);
    assertNotNull(orderDto);
    assertEquals(order.getId(), orderDto.getId());
    assertEquals(order.getExternalId(), orderDto.getExternalId());
    assertEquals(order.getEmergency(), orderDto.getEmergency());
    assertEquals(order.getStatus(), orderDto.getStatus());
  }

  @Test
  public void shouldBuildDtoFromOrderWithProvidedResources() {
    Map<UUID, FacilityDto> facilities = new HashMap<>();
    Map<UUID, ProgramDto> programs = new HashMap<>();
    Map<UUID, ProcessingPeriodDto> periods = new HashMap<>();
    Map<UUID, UserDto> users = new HashMap<>();

    BasicOrderDto orderDto = orderDtoBuilder.build(
        order, facilities, programs, periods, users);

    verify(orderExportHelper).setSubResources(
        eq(orderDto), eq(order), eq(facilities), eq(programs), eq(periods), eq(users));
    assertNotNull(orderDto);
    assertEquals(order.getId(), orderDto.getId());
    assertEquals(order.getExternalId(), orderDto.getExternalId());
    assertEquals(order.getEmergency(), orderDto.getEmergency());
    assertEquals(order.getStatus(), orderDto.getStatus());
  }

  @Test
  public void shouldReturnNullIfOrderIsNull() {
    assertNull(orderDtoBuilder.build((Order) null));
  }
}
