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

import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.siglus.siglusapi.dto.FulfillOrderDto;
import org.siglus.siglusapi.dto.OrderStatusDto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.repository.dto.StockCardReservedDto;
import org.siglus.siglusapi.service.SiglusOrderService;
import org.siglus.siglusapi.web.response.BasicOrderExtensionResponse;
import org.siglus.siglusapi.web.response.OrderPickPackResponse;
import org.siglus.siglusapi.web.response.OrderSuggestedQuantityResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

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

  @GetMapping
  public Page<BasicOrderExtensionResponse> searchOrders(OrderSearchParams params, Pageable pageable) {
    return siglusOrderService.searchOrdersWithSubDraftStatus(params, pageable);
  }

  @GetMapping("/{id}/print")
  public ModelAndView printOrder(HttpServletRequest request,
      @PathVariable("id") UUID orderId,
      @RequestParam("format") String format) throws IOException {
    return orderController.printOrder(request, orderId, format);
  }

  @GetMapping("/{id}/pickPackInfo")
  public OrderPickPackResponse getOrderPickPackResponse(@PathVariable("id") UUID orderId) {
    return siglusOrderService.getOrderPickPackResponse(orderId);
  }

  @GetMapping("/{id}/export")
  public void export(@PathVariable("id") UUID orderId,
      @RequestParam(value = "type", required = false, defaultValue = "csv") String type,
      HttpServletResponse response) throws IOException {
    orderController.export(orderId, type, response);
  }

  @GetMapping("/fulfill")
  public Page<FulfillOrderDto> searchOrdersForFulfill(OrderSearchParams params, Pageable pageable) {
    return siglusOrderService.searchOrdersForFulfill(params, pageable);
  }

  @PutMapping("/{id}")
  @ResponseStatus(NO_CONTENT)
  public void closeExpiredOrder(@PathVariable("id") UUID orderId) {
    siglusOrderService.closeExpiredOrder(orderId);
  }

  @PostMapping("/batchClose")
  @ResponseStatus(NO_CONTENT)
  public void batchCloseOrders() {
    siglusOrderService.batchCloseExpiredOrders();
  }

  @GetMapping("/{id}/status")
  public OrderStatusDto getOrderStatus(@PathVariable("id") UUID orderId) {
    return siglusOrderService.searchOrderStatusById(orderId);
  }

  @GetMapping("/{id}")
  public SiglusOrderDto getOrder(@PathVariable("id") UUID orderId) {
    return siglusOrderService.searchOrderById(orderId);
  }

  @GetMapping("/{id}/suggestedQuantity")
  public OrderSuggestedQuantityResponse getOrderableIdToSuggestedQuantity(@PathVariable("id") UUID orderId) {
    return siglusOrderService.getOrderSuggestedQuantityResponse(orderId);
  }

  @GetMapping("/{id}/reservedQuantity")
  public List<StockCardReservedDto> getOrderReservedQuantity(@PathVariable("id") UUID orderId) {
    return siglusOrderService.getOrderReservedQuantity(orderId);
  }
}
