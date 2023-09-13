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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProcessingScheduleDto;
import org.openlmis.stockmanagement.service.referencedata.FacilityReferenceDataService;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.service.scheduledtask.SiglusOrderCloseSchedulerService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SiglusOrderCloseSchedulerServiceTest {

  @Captor
  private ArgumentCaptor<Order> orderArgumentCaptor;

  @Mock
  private SiglusOrderService orderService;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private SiglusProcessingPeriodReferenceDataService periodService;

  @Mock
  private ExecutorService executorService;

  @Mock
  private OrderExternalRepository orderExternalRepository;

  @Mock
  private ProcessingPeriodExtensionRepository periodExtensionRepository;

  @InjectMocks
  private SiglusOrderCloseSchedulerService siglusOrderCloseSchedulerService;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  private final UUID facilityTypeId = UUID.randomUUID();

  @Before
  public void setUp() {
    ReflectionTestUtils.setField(siglusOrderCloseSchedulerService, "timeZoneId", "UTC");
    ReflectionTestUtils.setField(siglusOrderCloseSchedulerService, "fcFacilityTypeId",
        facilityTypeId);
  }

  @Test
  public void shouldCloseOrderWhenCurrentTimeAfterNextPeriodEndDate() {
    // given
    Mockito.doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Object[] args = invocationOnMock.getArguments();
        Runnable runnable = (Runnable) args[0];
        runnable.run();
        return null;
      }
    }).when(executorService).execute(Mockito.any(Runnable.class));
    Order order = new Order();
    List<OrderLineItem> list = new ArrayList<>();
    order.setOrderLineItems(list);
    order.setId(UUID.randomUUID());
    order.setStatus(OrderStatus.FULFILLING);
    order.setExternalId(UUID.randomUUID());
    UUID processingPeriodId = UUID.randomUUID();
    order.setProcessingPeriodId(processingPeriodId);
    when(orderRepository.findCanFulfillOrder()).thenReturn(Arrays.asList(order));
    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    ProcessingPeriodDto processingPeriod = new ProcessingPeriodDto();
    processingPeriod.setId(processingPeriodId);
    LocalDate localDate = LocalDate.now();
    processingPeriod.setStartDate(localDate.minusMonths(2));
    processingPeriod.setEndDate(localDate.minusMonths(1));
    processingPeriod.setProcessingSchedule(processingScheduleDto);
    ProcessingPeriodDto nextPeriod = new ProcessingPeriodDto();
    nextPeriod.setId(UUID.randomUUID());
    nextPeriod.setStartDate(processingPeriod.getEndDate().plusDays(1));
    nextPeriod.setEndDate(localDate.minusDays(10));
    nextPeriod.setProcessingSchedule(processingScheduleDto);
    when(periodService.findByIds(Sets.newHashSet(processingPeriodId)))
        .thenReturn(Arrays.asList(processingPeriod));
    ProcessingPeriodExtension extension = new ProcessingPeriodExtension();
    extension.setProcessingPeriodId(nextPeriod.getId());
    extension.setSubmitStartDate(processingPeriod.getEndDate().plusDays(1));
    extension.setSubmitEndDate(localDate.minusDays(2));
    when(periodExtensionRepository
        .findByProcessingPeriodIdIn(Arrays.asList(nextPeriod.getId())))
        .thenReturn(Arrays.asList(extension));
    Pageable pageable = new PageRequest(0, 1);
    when(periodService.searchProcessingPeriods(processingScheduleDto.getId(), null, null,
        processingPeriod.getEndDate().plusDays(1), null, null, pageable))
        .thenReturn(new PageImpl<>(asList(nextPeriod)));
    OrderExternal orderExternal = new OrderExternal();
    when(orderExternalRepository.findOne(order.getExternalId()))
        .thenReturn(orderExternal);
    UUID facilityId = order.getFacilityId();
    Map<UUID, org.openlmis.stockmanagement.dto.referencedata.FacilityDto> map = new HashMap<>();
    map.put(facilityId, getFacilityDto(facilityId));
    when(facilityReferenceDataService.findByIds(Arrays.asList(order.getFacilityId()))).thenReturn(
        map);

    // when
    siglusOrderCloseSchedulerService.closeFulfillmentIfCurrentDateIsAfterNextPeriodEndDate();

    // then
    verify(orderService).revertOrderToCloseStatus(orderArgumentCaptor.capture());
    Order convertOrder = orderArgumentCaptor.getValue();
    assertEquals(order.getId(), convertOrder.getId());
  }

  @Test
  public void shouldNotCloseOrderWhenCurrentTimeBeforeNextPeriodEndDate() {
    // given
    Mockito.doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Object[] args = invocationOnMock.getArguments();
        Runnable runnable = (Runnable) args[0];
        runnable.run();
        return null;
      }
    }).when(executorService).execute(Mockito.any(Runnable.class));
    Order order = new Order();
    List<OrderLineItem> list = new ArrayList<>();
    order.setOrderLineItems(list);
    order.setId(UUID.randomUUID());
    order.setStatus(OrderStatus.FULFILLING);
    order.setExternalId(UUID.randomUUID());
    order.setFacilityId(UUID.randomUUID());
    UUID processingPeriodId = UUID.randomUUID();
    order.setProcessingPeriodId(processingPeriodId);
    when(orderRepository.findCanFulfillOrder()).thenReturn(Arrays.asList(order));
    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    ProcessingPeriodDto processingPeriod = new ProcessingPeriodDto();
    processingPeriod.setId(processingPeriodId);
    LocalDate localDate = LocalDate.now();
    processingPeriod.setStartDate(localDate.minusMonths(2));
    processingPeriod.setEndDate(localDate.minusMonths(1));
    processingPeriod.setProcessingSchedule(processingScheduleDto);
    ProcessingPeriodDto nextPeriod = new ProcessingPeriodDto();
    nextPeriod.setId(UUID.randomUUID());
    nextPeriod.setStartDate(processingPeriod.getEndDate().plusDays(1));
    nextPeriod.setEndDate(localDate.plusDays(1));
    nextPeriod.setProcessingSchedule(processingScheduleDto);
    when(periodService.findByIds(Sets.newHashSet(processingPeriodId)))
        .thenReturn(Arrays.asList(processingPeriod));
    ProcessingPeriodExtension extension = new ProcessingPeriodExtension();
    extension.setProcessingPeriodId(nextPeriod.getId());
    extension.setSubmitStartDate(localDate.plusDays(1));
    extension.setSubmitEndDate(localDate.plusDays(10));
    when(periodExtensionRepository
        .findByProcessingPeriodIdIn(Arrays.asList(nextPeriod.getId())))
        .thenReturn(Arrays.asList(extension));
    Pageable pageable = new PageRequest(0, 1);
    when(periodService.searchProcessingPeriods(processingScheduleDto.getId(), null, null,
        processingPeriod.getEndDate().plusDays(1), null, null, pageable))
        .thenReturn(new PageImpl<>(asList(nextPeriod)));
    UUID facilityId = order.getFacilityId();
    Map<UUID, org.openlmis.stockmanagement.dto.referencedata.FacilityDto> map = new HashMap<>();
    map.put(facilityId, getFacilityDto(facilityId));
    when(facilityReferenceDataService.findByIds(Arrays.asList(order.getFacilityId()))).thenReturn(
        map);

    // when
    siglusOrderCloseSchedulerService.closeFulfillmentIfCurrentDateIsAfterNextPeriodEndDate();

    // then
    verify(orderService, times(0))
        .revertOrderToCloseStatus(orderArgumentCaptor.capture());
  }


  private org.openlmis.stockmanagement.dto.referencedata.FacilityDto getFacilityDto(UUID
      facilityId) {
    org.openlmis.stockmanagement.dto.referencedata.FacilityDto facilityDto =
        new org.openlmis.stockmanagement.dto.referencedata.FacilityDto();
    facilityDto.setId(facilityId);
    org.openlmis.stockmanagement.dto.referencedata.FacilityTypeDto typeDto = new
        org.openlmis.stockmanagement.dto.referencedata.FacilityTypeDto();
    typeDto.setId(UUID.randomUUID());
    facilityDto.setType(typeDto);
    return facilityDto;
  }

  @Test
  public void shouldNotCloseOrderWhenHaveNotNextPeriod() {
    // given
    Mockito.doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Object[] args = invocationOnMock.getArguments();
        Runnable runnable = (Runnable) args[0];
        runnable.run();
        return null;
      }
    }).when(executorService).execute(Mockito.any(Runnable.class));
    Order order = new Order();
    List<OrderLineItem> list = new ArrayList<>();
    order.setOrderLineItems(list);
    order.setId(UUID.randomUUID());
    order.setStatus(OrderStatus.FULFILLING);
    order.setExternalId(UUID.randomUUID());
    UUID processingPeriodId = UUID.randomUUID();
    order.setProcessingPeriodId(processingPeriodId);
    when(orderRepository.findCanFulfillOrder()).thenReturn(Arrays.asList(order));
    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    ProcessingPeriodDto processingPeriod = new ProcessingPeriodDto();
    processingPeriod.setId(processingPeriodId);
    LocalDate localDate = LocalDate.now();
    processingPeriod.setStartDate(localDate.minusMonths(2));
    processingPeriod.setEndDate(localDate.minusMonths(1));
    processingPeriod.setProcessingSchedule(processingScheduleDto);
    when(periodService.findByIds(Sets.newHashSet(processingPeriodId)))
        .thenReturn(Arrays.asList(processingPeriod));
    Pageable pageable = new PageRequest(0, 1);
    when(periodService.searchProcessingPeriods(processingScheduleDto.getId(), null, null,
        processingPeriod.getEndDate().plusDays(1), null, null, pageable))
        .thenReturn(new PageImpl<>(Collections.emptyList()));

    // when
    siglusOrderCloseSchedulerService.closeFulfillmentIfCurrentDateIsAfterNextPeriodEndDate();

    // then
    verify(orderService, times(0)).revertOrderToCloseStatus(orderArgumentCaptor.capture());
  }

}
