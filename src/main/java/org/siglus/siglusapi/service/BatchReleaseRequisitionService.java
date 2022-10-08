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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERIOD_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_REQUISITION_EXPIRED;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javers.common.collections.Lists;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ReleasableRequisitionBatchDto;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.dto.RequisitionsProcessingStatusDto;
import org.openlmis.requisition.web.BatchRequisitionController;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.OrdersRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unchecked")
public class BatchReleaseRequisitionService {

  private final SiglusRequisitionRepository siglusRequisitionRepository;

  private final OrdersRepository ordersRepository;

  private final SiglusProcessingPeriodReferenceDataService siglusProcessingPeriodReferenceDataService;

  private final BatchRequisitionController batchRequisitionController;

  public ResponseEntity<RequisitionsProcessingStatusDto> getRequisitionsProcessingStatusDtoResponse(
      ReleasableRequisitionBatchDto releaseDto) {

    validateParam(releaseDto);
    Set<UUID> requisitionIds = releaseDto.getRequisitionsToRelease().stream()
        .map(ReleasableRequisitionDto::getRequisitionId)
        .collect(Collectors.toSet());
    Map<UUID, List<Requisition>> programIdToRequisition = siglusRequisitionRepository.findAll(requisitionIds).stream()
        .collect(Collectors.groupingBy(Requisition::getProgramId));
    UUID supplyingDepotId = releaseDto.getRequisitionsToRelease().get(0).getSupplyingDepotId();

    checkExpiredRequisitionAndCloseExpiredOrders(programIdToRequisition, supplyingDepotId);

    return batchRequisitionController.batchReleaseRequisitions(releaseDto);
  }

  private void checkExpiredRequisitionAndCloseExpiredOrders(Map<UUID, List<Requisition>> programIdToRequisition,
      UUID supplyingDepotId) {
    programIdToRequisition.keySet().forEach(programId -> {
      List<Requisition> requisitions = programIdToRequisition.get(programId);
      List<Order> orders = ordersRepository
          .findBySupplyingFacilityIdAndProgramIdAndStatusIn(supplyingDepotId, programId,
              Lists.asList(OrderStatus.ORDERED, OrderStatus.FULFILLING, OrderStatus.PARTIALLY_FULFILLED));
      LocalDate ordersMaxPeriodEndDate = getOrdersMaxPeriodEndDate(orders);
      if (ordersMaxPeriodEndDate == null) {
        return;
      }
      Set<UUID> requisitionProcessingPeriodIds = requisitions.stream().map(Requisition::getProcessingPeriodId)
          .collect(Collectors.toSet());
      LocalDate requisitionMinPeriodEndDate = checkExpiredRequisition(ordersMaxPeriodEndDate,
          requisitionProcessingPeriodIds);

      closeExpiredOrders(orders, requisitionMinPeriodEndDate);
    });
  }

  private LocalDate checkExpiredRequisition(LocalDate ordersMaxPeriodEndDate,
      Set<UUID> requisitionProcessingPeriodIds) {
    Optional<ProcessingPeriodDto> minProcessingPeriodOpt = siglusProcessingPeriodReferenceDataService
        .findByIds(requisitionProcessingPeriodIds)
        .stream().min(Comparator.comparing(ProcessingPeriodDto::getEndDate));
    if (!minProcessingPeriodOpt.isPresent()) {
      throw new NotFoundException(ERROR_PERIOD_NOT_FOUND);
    }
    LocalDate requisitionMinPeriodEndDate = minProcessingPeriodOpt.get().getEndDate();
    if (requisitionMinPeriodEndDate.isBefore(ordersMaxPeriodEndDate)) {
      throw new BusinessDataException(new Message(ERROR_REQUISITION_EXPIRED), "expired R&R");
    }
    return requisitionMinPeriodEndDate;
  }

  private void closeExpiredOrders(List<Order> orders, LocalDate requisitionMinPeriodEndDate) {
    Map<UUID, List<Order>> periodIdToOrder = orders.stream()
        .collect(Collectors.groupingBy(Order::getProcessingPeriodId));
    periodIdToOrder.keySet().forEach(periodId -> {
      LocalDate endDate = siglusProcessingPeriodReferenceDataService.findOne(periodId).getEndDate();
      if (endDate.isBefore(requisitionMinPeriodEndDate)) {
        List<Order> expiredOrders = periodIdToOrder.get(periodId);
        expiredOrders.forEach(order -> order.setStatus(OrderStatus.CLOSED));
        ordersRepository.save(expiredOrders);
      }
    });
  }

  private void validateParam(ReleasableRequisitionBatchDto releaseDto) {
    List<ReleasableRequisitionDto> requisitionsToRelease = releaseDto.getRequisitionsToRelease();
    if (requisitionsToRelease.isEmpty()) {
      throw new IllegalArgumentException("not found releasable requisition");
    }
  }

  private LocalDate getOrdersMaxPeriodEndDate(List<Order> orders) {
    if (orders.isEmpty()) {
      return null;
    }
    Set<UUID> orderProcessingPeriodIds = orders.stream().map(Order::getProcessingPeriodId).collect(Collectors.toSet());
    Set<LocalDate> endDates = siglusProcessingPeriodReferenceDataService.findByIds(orderProcessingPeriodIds).stream()
        .map(ProcessingPeriodDto::getEndDate).collect(Collectors.toSet());
    return endDates.isEmpty() ? null : Collections.max(endDates);
  }
}
