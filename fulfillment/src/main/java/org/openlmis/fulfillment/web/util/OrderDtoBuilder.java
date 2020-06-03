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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.service.referencedata.ProgramDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderDtoBuilder {
  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(OrderDtoBuilder.class);

  @Autowired
  private OrderExportHelper orderExportHelper;

  @Value("${service.url}")
  private String serviceUrl;


  /**
   * Create a new instance of OrderDto based on data from {@link Order}.
   *
   * @param order instance used to create {@link OrderDto} (can be {@code null})
   * @return new instance of {@link OrderDto}. {@code null} if passed argument is {@code
   * null}.
   */
  public OrderDto build(Order order) {
    return build(order,  null, null, null, null, null);
  }

  /**
   * Create a new instance of OrderDto based on data from {@link Order}.
   *
   * @param order instance used to create {@link OrderDto} (can be {@code null})
   * @return new instance of {@link OrderDto}. {@code null} if passed argument is {@code
   * null}.
   */
  public OrderDto build(Order order, Map<VersionIdentityDto, OrderableDto> orderables,
      Map<UUID, FacilityDto> facilities, Map<UUID, ProgramDto> programs,
      Map<UUID, ProcessingPeriodDto> periods, Map<UUID, UserDto> users) {
    XLOGGER.entry(order, orderables, facilities, programs, periods, users);
    if (null == order) {
      XLOGGER.exit();
      return null;
    }

    Profiler profiler = new Profiler("ORDER_DTO_BUILD");
    profiler.setLogger(XLOGGER);

    OrderDto orderDto = new OrderDto();

    profiler.start("EXPORT");
    order.export(orderDto);
    orderDto.setServiceUrl(serviceUrl);

    profiler.start("SET_SUB_RESOURCES");
    orderExportHelper.setSubResources(orderDto, order, facilities, programs, periods, users);

    profiler.start("GET_LINE_ITEMS");
    List<OrderLineItem> lineItems = order.getOrderLineItems();

    profiler.start("EXPORT_LINE_ITEMS_TO_DTOS");
    List<OrderLineItemDto> lineItemDtos =
        orderExportHelper.exportToDtos(lineItems, orderables);

    orderDto.setOrderLineItems(lineItemDtos);

    if (order.getStatusMessages() != null) {
      orderDto.setStatusMessages(order.getStatusMessages().stream()
          .map(StatusMessageDto::newInstance).collect(Collectors.toList()));
    }

    if (order.getStatusChanges() != null) {
      orderDto.setStatusChanges(order.getStatusChanges().stream()
          .map(StatusChangeDto::newInstance).collect(Collectors.toList()));
    }

    profiler.start("SET_LINE_ITEMS");
    orderDto.setOrderLineItems(lineItemDtos);

    profiler.stop().log();
    XLOGGER.exit(orderDto);
    return orderDto;
  }

}
