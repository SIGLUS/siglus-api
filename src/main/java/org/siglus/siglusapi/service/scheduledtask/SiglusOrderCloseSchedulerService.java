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

package org.siglus.siglusapi.service.scheduledtask;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.BasicOrderDtoBuilder;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;
import org.openlmis.stockmanagement.service.referencedata.FacilityReferenceDataService;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.dto.FulfillOrderDto;
import org.siglus.siglusapi.service.SiglusOrderService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusOrderCloseSchedulerService {

  private final SiglusOrderService orderService;
  private final OrderRepository orderRepository;
  private final SiglusProcessingPeriodReferenceDataService periodService;
  private final ExecutorService executorService;
  private final OrderExternalRepository orderExternalRepository;
  private final ProcessingPeriodExtensionRepository periodExtensionRepository;
  private final FacilityReferenceDataService facilityReferenceDataService;

  private final BasicOrderDtoBuilder basicOrderDtoBuilder;

  @Value("${time.zoneId}")
  private String timeZoneId;

  @Value("${fc.facilityTypeId}")
  private UUID fcFacilityTypeId;

  //@Scheduled(cron = "${fulfillment.close.cron}", zone = "${time.zoneId}")
  public void closeFulfillmentIfCurrentDateIsAfterNextPeriodEndDate() {
    List<Order> orders = getCanFulfillOrder();
    HashMap<UUID, ProcessingPeriodDto> currentPeriodIdToNextPeriod = getCurrentPeriodIdToNextPeriod(orders);
    log.info("close order: get next processing period map: {}", currentPeriodIdToNextPeriod);
    if (!CollectionUtils.isEmpty(currentPeriodIdToNextPeriod.values())) {
      List<Order> needClosedOrders = getNeedClosedOrders(orders, currentPeriodIdToNextPeriod);
      closeOrders(needClosedOrders);
    }
  }

  //  @Scheduled(cron = "${fulfillment.close.cron}", zone = "${time.zoneId}")
  public void batchCloseExpiredOrder() {
    List<Order> orders = getCanFulfillOrder();
    batchProcessExpiredOrders(orders);
  }

  public void batchProcessExpiredOrders(List<Order> orders) {
    if (ObjectUtils.isEmpty(orders)) {
      return;
    }
    List<BasicOrderDto> dtos = basicOrderDtoBuilder.build(orders);
    List<FulfillOrderDto> fulfillOrderDtos = dtos.stream()
        .map(basicOrderDto -> FulfillOrderDto.builder().basicOrder(basicOrderDto).build())
        .collect(toList());
    List<FulfillOrderDto> processedFulfillOrderDtos = orderService.processExpiredFulfillOrder(fulfillOrderDtos);
    Set<UUID> expiredOrderIds = processedFulfillOrderDtos
        .stream()
        .filter(dto -> dto.isExpired())
        .map(dto -> dto.getBasicOrder().getId())
        .collect(Collectors.toSet());

    List<Order> needClosedOrders = orders.stream()
        .filter(o -> expiredOrderIds.contains(o.getId())).collect(toList());
    closeOrders(needClosedOrders);
  }

  private void closeOrders(List<Order> needClosedOrders) {
    log.info("close order: get need close order : {}", needClosedOrders);
    List<CompletableFuture<Void>> futures = Lists.newArrayList();
    for (Order order : needClosedOrders) {
      CompletableFuture<Void> future = CompletableFuture.runAsync(
          () -> orderService.revertOrderToCloseStatus(order), executorService);
      futures.add(future);
    }
    futures.forEach(CompletableFuture::join);
    log.info("close order: close all orders");
  }

  private List<Order> getCanFulfillOrder() {
    return orderRepository.findCanFulfillOrder();
  }

  private HashMap<UUID, ProcessingPeriodDto> getCurrentPeriodIdToNextPeriod(List<Order> orders) {
    Set<UUID> periodIds = orders.stream().map(Order::getProcessingPeriodId).collect(Collectors.toSet());
    List<ProcessingPeriodDto> periodDtos = periodService.findByIds(periodIds);
    HashMap<UUID, ProcessingPeriodDto> currentPeriodIdToNextPeriod = new HashMap<>();
    List<CompletableFuture<List<ProcessingPeriodDto>>> futures = Lists.newArrayList();
    for (ProcessingPeriodDto periodDto : periodDtos) {
      CompletableFuture<List<ProcessingPeriodDto>> future = CompletableFuture.supplyAsync(() ->
          getNextProcessingPeriodDto(periodDto), executorService);
      futures.add(future);
    }
    List<List<ProcessingPeriodDto>> nextPeriodCollections = futures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());
    log.info("get next periods: {}", nextPeriodCollections);
    for (List<ProcessingPeriodDto> nextPeriods : nextPeriodCollections) {
      if (!CollectionUtils.isEmpty(nextPeriods)) {
        ProcessingPeriodDto nextPeriod = nextPeriods.get(0);
        ProcessingPeriodDto currentPeriod = getProcessingPeriod(periodDtos, nextPeriod);
        if (currentPeriod != null) {
          currentPeriodIdToNextPeriod.put(currentPeriod.getId(), nextPeriods.get(0));
        }
      }
    }
    log.info("get currentPeriodIdToNextPeriod : {}", currentPeriodIdToNextPeriod);
    return currentPeriodIdToNextPeriod;
  }

  private HashMap<UUID, ProcessingPeriodExtension> getExtensions(HashMap<UUID, ProcessingPeriodDto>
      currentPeriodIdToNextPeriod) {
    List<UUID> periodIds = currentPeriodIdToNextPeriod.values()
        .stream()
        .map(ProcessingPeriodDto::getId)
        .collect(Collectors.toList());
    List<ProcessingPeriodExtension> extensions = periodExtensionRepository.findByProcessingPeriodIdIn(periodIds);
    HashMap<UUID, ProcessingPeriodExtension> currentPeriodIdToExtension = new HashMap<>();
    extensions.forEach(extension -> currentPeriodIdToExtension.put(extension.getProcessingPeriodId(), extension));
    return currentPeriodIdToExtension;
  }

  private ProcessingPeriodDto getProcessingPeriod(List<ProcessingPeriodDto> periodDtos,
      ProcessingPeriodDto nextPeriod) {
    List<ProcessingPeriodDto> currentPeriods = periodDtos.stream()
        .filter(periodDto ->
            periodDto.getProcessingSchedule().getId().equals(nextPeriod.getProcessingSchedule().getId())
                && periodDto.getEndDate().plusDays(1).equals(nextPeriod.getStartDate()))
        .collect(Collectors.toList());
    return CollectionUtils.isEmpty(currentPeriods) ? null : currentPeriods.get(0);
  }

  private List<ProcessingPeriodDto> getNextProcessingPeriodDto(ProcessingPeriodDto period) {
    Pageable pageable = new PageRequest(0, 1);
    return periodService.searchProcessingPeriods(period.getProcessingSchedule().getId(), null, null,
        period.getEndDate().plusDays(1), null, null, pageable).getContent();
  }

  private Map<UUID, FacilityDto> getFacilities(List<Order> orders) {
    List<UUID> facilityIds = orders.stream().map(Order::getFacilityId)
        .collect(Collectors.toList());
    return facilityReferenceDataService.findByIds(facilityIds);
  }

  private List<Order> getNeedClosedOrders(List<Order> orders, HashMap<UUID,
      ProcessingPeriodDto> currentPeriodIdToNextPeriod) {
    HashMap<UUID, ProcessingPeriodExtension> currentPeriodIdToExtension = getExtensions(currentPeriodIdToNextPeriod);
    Map<UUID, FacilityDto> facilityIds = getFacilities(orders);
    return orders.stream()
        .filter(order -> {
          FacilityDto facilityDto = facilityIds.get(order.getFacilityId());
          if (facilityDto.getType().getId().equals(fcFacilityTypeId)) {
            return false;
          }
          return isNextPeriodSubmitEndDateBeforeCurrentDate(currentPeriodIdToNextPeriod,
              currentPeriodIdToExtension, order);
        })
        .collect(Collectors.toList());
  }

  private boolean isNextPeriodSubmitEndDateBeforeCurrentDate(
      HashMap<UUID, ProcessingPeriodDto> currentPeriodIdToNextPeriod,
      HashMap<UUID, ProcessingPeriodExtension> currentPeriodIdToExtension, Order order) {
    OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
    if (external != null && currentPeriodIdToNextPeriod.containsKey(order.getProcessingPeriodId())) {
      ProcessingPeriodDto nextPeriod = currentPeriodIdToNextPeriod.get(order.getProcessingPeriodId());
      if (currentPeriodIdToExtension.containsKey(nextPeriod.getId())) {
        ProcessingPeriodExtension nextPeriodExtension = currentPeriodIdToExtension.get(nextPeriod.getId());
        LocalDate currentDate = LocalDate.now(ZoneId.of(timeZoneId));
        return nextPeriodExtension.getSubmitEndDate().isBefore(currentDate);
      }
    }
    return false;
  }

}
