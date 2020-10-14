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

package org.siglus.siglusapi.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.siglus.common.repository.OrderExternalRepository;

@RunWith(MockitoJUnitRunner.class)
public class SiglusFilterAddProductForEmergencyServiceTest {

  @Mock
  private OrderExternalRepository externalRepository;

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private SiglusFilterAddProductForEmergencyService filterAddProductForEmergencyService;

  @Test
  public void shouldEmptyIfPreRequisitionNotInProgress() {
    // given
    RequisitionV2Dto preV2 =  new RequisitionV2Dto();
    preV2.setStatus(RequisitionStatus.RELEASED);

    // when
    Set<UUID> progressProducts = filterAddProductForEmergencyService
        .getInProgressProducts(Arrays.asList(preV2));

    // then
    assertEquals(0, progressProducts.size());
  }

  @Test
  public void shouldGetOneProgressProductIfPreRequisitionInProgress() {
    // given
    RequisitionV2Dto preV2 =  new RequisitionV2Dto();
    preV2.setStatus(RequisitionStatus.IN_APPROVAL);
    RequisitionLineItemV2Dto lineItemV2Dto = new RequisitionLineItemV2Dto();
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(UUID.randomUUID());
    lineItemV2Dto.setOrderable(orderableDto);
    preV2.setRequisitionLineItems(Arrays.asList(lineItemV2Dto));

    // when
    Set<UUID> progressProducts = filterAddProductForEmergencyService
        .getInProgressProducts(Arrays.asList(preV2));

    // then
    assertEquals(1, progressProducts.size());
    assertEquals(orderableDto.getId(), progressProducts.iterator().next());
  }

  @Test
  public void shouldEmptyForGetNotFullyShippedWhenPreRequisitionStatusInApproval() {
    // given
    RequisitionV2Dto preV2 =  new RequisitionV2Dto();
    preV2.setStatus(RequisitionStatus.IN_APPROVAL);

    // when
    Set<UUID> notFullyShippedProducts = filterAddProductForEmergencyService
        .getNotFullyShippedProducts(Arrays.asList(preV2));

    // then
    assertEquals(0, notFullyShippedProducts.size());
  }

  @Test
  public void shouldGetOneWhenPreRequisitionStatusReleaseAndHaveNoFullyShippedProducts() {
    // given
    RequisitionV2Dto preV2 =  new RequisitionV2Dto();
    preV2.setId(UUID.randomUUID());
    preV2.setStatus(RequisitionStatus.RELEASED);

    // when
    when(externalRepository.findByRequisitionId(preV2.getId()))
        .thenReturn(Collections.emptyList());
    Order order = new Order();
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setId(UUID.randomUUID());
    lineItem.setOrderedQuantity((long)10);
    order.setOrderLineItems(Arrays.asList(lineItem));
    lineItem.setOrderable(new VersionEntityReference(UUID.randomUUID(), (long) 1));
    when(orderRepository.findCanFulfillOrderByExternalIdIn(Arrays.asList(preV2.getId())))
        .thenReturn(order);
    Set<UUID> notFullyShippedProducts = filterAddProductForEmergencyService
        .getNotFullyShippedProducts(Arrays.asList(preV2));

    // then
    assertEquals(1, notFullyShippedProducts.size());
    assertEquals(lineItem.getOrderable().getId(),
        notFullyShippedProducts.iterator().next());
  }


  @Test
  public void shouldGetOneProductsWhenNotExistSuborderAndHaveNoFullyShippedProducts() {
    // given
    RequisitionV2Dto preV2 =  new RequisitionV2Dto();
    preV2.setId(UUID.randomUUID());
    preV2.setStatus(RequisitionStatus.RELEASED);

    // when
    String externalId = UUID.randomUUID().toString();
    when(externalRepository.findOrderExternalIdByRequisitionId(preV2.getId()))
        .thenReturn(Arrays.asList(externalId));
    Order order = new Order();
    OrderLineItem lineItem = new OrderLineItem();
    lineItem.setId(UUID.randomUUID());
    lineItem.setOrderedQuantity((long)10);
    order.setOrderLineItems(Arrays.asList(lineItem));
    lineItem.setOrderable(new VersionEntityReference(UUID.randomUUID(), (long) 1));
    when(orderRepository.findCanFulfillOrderByExternalIdIn(
        Arrays.asList(UUID.fromString(externalId))))
        .thenReturn(order);
    Set<UUID> notFullyShippedProducts = filterAddProductForEmergencyService
        .getNotFullyShippedProducts(Arrays.asList(preV2));

    // then
    assertEquals(1, notFullyShippedProducts.size());
    assertEquals(lineItem.getOrderable().getId(),
        notFullyShippedProducts.iterator().next());
  }

}
