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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiglusOrderCloseSchedulerService {

  @Autowired
  private SiglusShipmentService shipmentService;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private SiglusProcessingPeriodReferenceDataService periodService;

  @Value("${time.zoneId}")
  private String timeZoneId;

  @Scheduled(cron = "${fulfillment.close.cron}", zone = "${time.zoneId}")
  public void closeFulfillmentIfCurrentDateIsAfterNextPeriodEndDate() {
    List<Order> orders = getCanFulfillOrder();
    log.info("get can Fulfill order : {}", orders);

    HashMap<UUID, ProcessingPeriodDto> processingPeriodMap
        = getNextProcessingPeriodDtoHashMap(orders);
    log.info("get next processing period map: {}", processingPeriodMap);

    if (!CollectionUtils.isEmpty(processingPeriodMap.values())) {
      List<Order> needClosedOrders = getNeedClosedOrder(orders, processingPeriodMap);
      log.info("get need close order : {}", needClosedOrders);

      needClosedOrders.forEach(order ->
          shipmentService.revertOrderToCloseStatus(order));
    }
  }

  private List<Order> getCanFulfillOrder() {
    return orderRepository.findCanFulfillOrder();
  }

  private HashMap<UUID, ProcessingPeriodDto> getNextProcessingPeriodDtoHashMap(List<Order> orders) {
    Set<UUID> periodIds = orders.stream()
        .map(order -> order.getProcessingPeriodId())
        .collect(Collectors.toSet());
    List<ProcessingPeriodDto> periodDtos = periodService.findByIds(periodIds);
    log.info("get orders period dtos: {}", periodDtos);

    HashMap<UUID, ProcessingPeriodDto> processingPeriodDtoHashMap = new HashMap<>();
    periodDtos.forEach(periodDto -> {
      List<ProcessingPeriodDto> nextPeriods = getNextProcessingPeriodDto(periodDto);
      if (!CollectionUtils.isEmpty(nextPeriods)) {
        processingPeriodDtoHashMap.put(periodDto.getId(), nextPeriods.get(0));
      }
    });
    return processingPeriodDtoHashMap;
  }

  private List<ProcessingPeriodDto> getNextProcessingPeriodDto(ProcessingPeriodDto period) {
    Pageable pageable = new PageRequest(0, 1);
    return periodService
        .searchProcessingPeriods(period.getProcessingSchedule().getId(),
            null, null, period.getStartDate(),
            null,
            null, pageable)
        .getContent();
  }

  private List<Order> getNeedClosedOrder(List<Order> orders,
      HashMap<UUID, ProcessingPeriodDto> processingPeriodMap) {
    LocalDate currentDate = LocalDate.now(ZoneId.of(timeZoneId));
    return orders.stream()
        .filter(order -> {
          if (processingPeriodMap.containsKey(order.getProcessingPeriodId())) {
            ProcessingPeriodDto nextPeriod = processingPeriodMap.get(order.getProcessingPeriodId());
            return nextPeriod.getEndDate().isAfter(currentDate);
          }
          return true;
        }).collect(Collectors.toList());
  }

}
