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

import java.util.List;
import java.util.UUID;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.siglus.siglusapi.dto.OrderStatusDto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.dto.SiglusOrderLineItemDto;
import org.siglus.siglusapi.service.SiglusOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/orders")
public class SiglusOrderController {

  @Autowired
  private OrderController orderController;

  @Autowired
  private SiglusOrderService siglusOrderService;

  /**
   * why we redo this api?<br>
   * to support #245, we refactor
   * the {@linkplain org.openlmis.fulfillment.service.OrderService#setOrderStatus method}
   */
  @PostMapping("/batch")
  @ResponseStatus(HttpStatus.CREATED)
  public Iterable<BasicOrderDto> batchCreateOrders(@RequestBody List<OrderDto> orders,
      OAuth2Authentication authentication) {
    return orderController.batchCreateOrders(orders, authentication);
  }

  @GetMapping()
  public Page<BasicOrderDto> searchOrders(OrderSearchParams params, Pageable pageable) {
    return siglusOrderService.searchOrders(params, pageable);
  }

  @GetMapping("/{id}/status")
  public OrderStatusDto getOrderStatus(@PathVariable("id") UUID orderId) {
    return siglusOrderService.searchOrderStatusById(orderId);
  }

  @GetMapping("/{id}")
  public SiglusOrderDto getOrder(@PathVariable("id") UUID orderId) {
    return siglusOrderService.searchOrderById(orderId);
  }

  @PostMapping("/createLineItem")
  public List<SiglusOrderLineItemDto> createOrderLineItem(
      @RequestBody List<UUID> orderableIds) {
    return siglusOrderService.createOrderLineItem(orderableIds);
  }
}
