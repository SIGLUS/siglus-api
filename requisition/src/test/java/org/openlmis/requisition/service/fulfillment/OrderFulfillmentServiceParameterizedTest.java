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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.openlmis.requisition.dto.OrderStatus.RECEIVED;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.MockitoAnnotations;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.OrderDtoDataBuilder;

@RunWith(Parameterized.class)
public class OrderFulfillmentServiceParameterizedTest
    extends BaseFulfillmentServiceTest<OrderDto> {

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
    MockitoAnnotations.initMocks(this);
    super.setUp();
  }

  private UUID supplyingFacility;
  private UUID requestingFacility;
  private UUID program;
  private UUID processingPeriod;
  private String status;

  /**
   * Creates new instance of Parameterized Test.
   */
  public OrderFulfillmentServiceParameterizedTest(UUID supplyingFacility, UUID requestingFacility,
                                                  UUID program, UUID processingPeriod,
                                                  String status) {
    this.supplyingFacility = supplyingFacility;
    this.requestingFacility = requestingFacility;
    this.program = program;
    this.processingPeriod = processingPeriod;
    this.status = status;
  }

  /**
   * Get test data.
   *
   * @return collection of objects that will be passed to test constructor.
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {null, null, null, null, null},
        {UUID.randomUUID(), null, null, null, null},
        {null, UUID.randomUUID(), null, null, null},
        {null, null, UUID.randomUUID(), null, null},
        {null, null, null, UUID.randomUUID(), null},
        {null, null, null, null, RECEIVED.toString()},
        {UUID.randomUUID(), UUID.randomUUID(), null, null, null},
        {null, UUID.randomUUID(), UUID.randomUUID(), null, null},
        {null, null, UUID.randomUUID(), UUID.randomUUID(), null},
        {null, null, null, UUID.randomUUID(), RECEIVED.toString()},
        {UUID.randomUUID(), null, UUID.randomUUID(), null, RECEIVED.toString()},
        {null, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null},
        {null, null, null, UUID.randomUUID(), null},
        {UUID.randomUUID(), null, null, null, null},
        {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, RECEIVED.toString()},
    });
  }

  @Test
  public void shouldFindOrders() {
    // given
    OrderFulfillmentService service = (OrderFulfillmentService) prepareService();

    // when
    OrderDto order = mockPageResponseEntityAndGetDto();
    List<OrderDto> result = service.search(
        supplyingFacility, requestingFacility, program, processingPeriod, status
    );

    // then
    assertThat(result, hasSize(1));
    assertThat(result, contains(order));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("supplyingFacilityId", supplyingFacility)
        .hasQueryParameter("requestingFacilityId", requestingFacility)
        .hasQueryParameter("programId", program)
        .hasQueryParameter("processingPeriodId", processingPeriod)
        .hasQueryParameter("status", status);
  }
}
