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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.siglus.siglusapi.service.SiglusOrderService;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

@RunWith(MockitoJUnitRunner.class)
public class SiglusOrderControllerTest {

  @InjectMocks
  private SiglusOrderController controller;

  @Mock
  private OrderController actualController;

  @Mock
  private SiglusOrderService siglusOrderService;

  @Mock
  private OAuth2Authentication authentication;

  @Mock
  private List<OrderDto> orders;

  @Test
  public void shouldCallOpenlmisControllerWhenBatchCreateOrders() {
    // when
    controller.batchCreateOrders(orders, authentication);

    // then
    verify(actualController).batchCreateOrders(orders, authentication);
  }

  @Test
  public void shouldCallOpenlmisControllerWhenBatchCreateOrdersForFulfill() {
    // given
    OrderSearchParams params = mock(OrderSearchParams.class);
    Pageable pageable = mock(Pageable.class);

    // when
    controller.searchOrdersForFulfill(params, pageable);

    // then
    verify(siglusOrderService).searchOrdersForFulfill(params, pageable);
  }

  @Test
  public void shouldCallSiglusOrderServiceWhenSearchOrders() {
    // given
    OrderSearchParams params = mock(OrderSearchParams.class);
    Pageable pageable = mock(Pageable.class);

    // when
    controller.searchOrders(params, pageable);

    // then
    verify(siglusOrderService).searchOrdersWithSubDraftStatus(params, pageable);
  }

  @Test
  public void shouldCallOrderControllerWhenPrintOrders() throws IOException {
    // given
    ServletContext servletContext = new MockServletContext("");
    HttpServletRequest httpServletRequest = new MockHttpServletRequest(servletContext);
    UUID id = UUID.randomUUID();
    String format = "";

    // when
    controller.printOrder(httpServletRequest, id, format);

    verify(actualController).printOrder(httpServletRequest, id, format);
  }

  @Test
  public void shouldCallOrderControllerWhenExportOrders() throws IOException {
    // given
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    UUID orderId = UUID.randomUUID();
    String type = "";

    // when
    controller.export(orderId, type, httpServletResponse);

    verify(actualController).export(orderId, type, httpServletResponse);
  }

  @Test
  public void shouldCallCloseExpiredOrder() {
    // given
    UUID orderId = UUID.randomUUID();

    // when
    controller.closeExpiredOrder(orderId);

    verify(siglusOrderService).closeExpiredOrder(orderId);
  }

}