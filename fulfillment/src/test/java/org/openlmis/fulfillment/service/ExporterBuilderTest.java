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

package org.openlmis.fulfillment.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.UpdateDetails;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.OrderableReferenceDataService;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ExporterBuilderTest {

  private static final String SERVICE_URL = "localhost";

  @Mock
  private OrderableReferenceDataService products;

  @InjectMocks
  private ExporterBuilder exportBuilder;

  @Captor
  private ArgumentCaptor<Set<VersionEntityReference>> argumentCaptor;

  private UUID orderableId = UUID.randomUUID();
  private UUID lastUpdaterId = UUID.randomUUID();
  private VersionEntityReference orderable = new VersionEntityReference(orderableId, 1L);
  private OrderableDto orderableDto = mock(OrderableDto.class);
  private Order order = mock(Order.class);
  private OrderLineItem orderLineItem = mock(OrderLineItem.class);
  private ZonedDateTime updatedDate = ZonedDateTime.now();

  @Before
  public void setUp() {
    when(orderLineItem.getOrderable()).thenReturn(orderable);
    when(orderableDto.getId()).thenReturn(orderableId);
    when(orderableDto.getVersionNumber()).thenReturn(orderable.getVersionNumber());
    when(order.getOrderLineItems()).thenReturn(Collections.singletonList(orderLineItem));

    ReflectionTestUtils.setField(exportBuilder, "serviceUrl", SERVICE_URL);
  }

  @Test
  public void shouldExportOrderWithReferenceDto() {
    when(order.getUpdateDetails())
        .thenReturn(new UpdateDetails(lastUpdaterId, updatedDate));

    OrderDto exporter = new OrderDto();
    exportBuilder.export(order, exporter);

    assertEquals(lastUpdaterId, exporter.getLastUpdater().getId());
    assertEquals(
        SERVICE_URL + ResourceNames.getUsersPath() + lastUpdaterId,
        exporter.getLastUpdater().getHref());

    assertEquals(updatedDate, exporter.getLastUpdatedDate());
  }

  @Test
  public void lineItemExportShouldSetOrderableFromProvidedList() {
    // given
    OrderLineItemDto orderLineItemDto = new OrderLineItemDto();

    // when
    exportBuilder.export(orderLineItem, orderLineItemDto,
        Collections.singletonList(orderableDto));

    // then
    assertEquals(orderLineItemDto.getOrderable(), orderableDto);
  }

  @Test
  public void lineItemExportShouldFetchOrderableIfNoneProvided() {
    // given
    OrderLineItemDto orderLineItemDto = new OrderLineItemDto();
    when(products.findOne(orderableId)).thenReturn(orderableDto);
    when(products.findByIdentities(argumentCaptor.capture())).thenReturn(
        Collections.singletonList(orderableDto));

    // when
    exportBuilder.export(orderLineItem, orderLineItemDto, Collections.emptyList());

    // then
    assertEquals(orderLineItemDto.getOrderable(), orderableDto);
    verify(products, times(1)).findOne(orderableId);
  }

  @Test
  public void lineItemExportShouldCalculateTotalDispensingUnits() {
    // given
    final Long netContent = 100L;
    final Long orderedQuantity = 35L;
    when(products.findOne(orderableId)).thenReturn(orderableDto);
    when(orderableDto.getNetContent()).thenReturn(netContent);
    when(orderLineItem.getOrderedQuantity()).thenReturn(orderedQuantity);

    OrderLineItemDto orderLineItemDto = new OrderLineItemDto();

    // when
    exportBuilder.export(orderLineItem, orderLineItemDto, Collections.emptyList());

    // then
    assertEquals(orderedQuantity, orderLineItemDto.getOrderedQuantity());
    assertEquals((Long) (netContent * orderedQuantity), orderLineItemDto.getTotalDispensingUnits());
    verify(products, times(1)).findOne(orderableId);
  }

  @Test
  public void shouldGetLineItemOrderables() {
    // given
    when(products.findByIdentities(argumentCaptor.capture())).thenReturn(
        Collections.singletonList(orderableDto));

    // when
    List<OrderableDto> orderables = exportBuilder.getLineItemOrderables(order);

    // then
    Set<VersionEntityReference> searchedIds = argumentCaptor.getValue();
    VersionEntityReference ref = new VersionEntityReference(
        orderableDto.getId(), orderableDto.getVersionNumber());
    assertTrue(searchedIds.contains(ref));
    assertTrue(searchedIds.size() == 1);
    assertTrue(orderables.contains(orderableDto));
  }
}
