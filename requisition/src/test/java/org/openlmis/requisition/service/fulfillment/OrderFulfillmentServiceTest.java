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

package org.openlmis.requisition.service.fulfillment;

import static java.util.Arrays.asList;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.OrderDtoDataBuilder;
import org.springframework.web.client.RestClientException;

public class OrderFulfillmentServiceTest extends BaseFulfillmentServiceTest<OrderDto> {

  private OrderFulfillmentService service;

  @Override
  protected BaseCommunicationService<OrderDto> getService() {
    return new OrderFulfillmentService();
  }

  @Override
  protected OrderDto generateInstance() {
    return new OrderDtoDataBuilder().buildAsDto();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (OrderFulfillmentService) prepareService();
  }

  @Test
  public void shouldSendRequestToCreateMultipleOrders() {
    // given
    OrderDto order = generateInstance();
    OrderDto order2 = generateInstance();
    List<OrderDto> body = asList(order, order2);

    // when
    service.create(body);

    // then
    verifyPostRequest()
        .hasAuthHeader()
        .hasBody(body)
        .isUriStartsWith(service.getServiceUrl() + service.getBatchUrl());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenCreatingMultipleOrdersFails() {
    // given
    OrderDto order = generateInstance();
    OrderDto order2 = generateInstance();

    // when
    mockPostRequestFail(new RestClientException("test"));
    service.create(asList(order, order2));
  }

}
