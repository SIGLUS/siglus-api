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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ORDER_CODE_EXISTS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.siglus.siglusapi.domain.LocalReceiptVoucher;
import org.siglus.siglusapi.dto.LocalReceiptVoucherDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.SiglusLocalReceiptVoucherRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiglusLocalReceiptVoucherService {

  private final OrderController orderController;

  private final SiglusAuthenticationHelper authenticationHelper;

  private final SiglusLocalReceiptVoucherRepository localReceiptVoucherRepository;

  public LocalReceiptVoucherDto createLocalReceiptVoucher(LocalReceiptVoucherDto dto) {
    validateLocalReceiptVoucher(dto);
    checkOrderCodeExists(dto);
    LocalReceiptVoucher localReceiptVoucher = LocalReceiptVoucher.createLocalReceiptVoucher(dto);
    log.info("save local receipt voucher");
    LocalReceiptVoucher savedLocalReceiptVoucher = localReceiptVoucherRepository.save(localReceiptVoucher);
    return LocalReceiptVoucherDto.from(savedLocalReceiptVoucher);
  }

  private void checkOrderCodeExists(LocalReceiptVoucherDto dto) {
    Set<String> status = new HashSet(
        Arrays.asList(OrderStatus.TRANSFER_FAILED.toString(), OrderStatus.SHIPPED.toString(),
            OrderStatus.RECEIVED.toString(), OrderStatus.IN_ROUTE.toString(),
            OrderStatus.READY_TO_PACK.toString()));
    OrderSearchParams params = OrderSearchParams
        .builder()
        .programId(dto.getProgramId())
        .supplyingFacilityId(dto.getSupplyingFacilityId())
        .status(status)
        .build();
    PageRequest pageRequest = new PageRequest(0, Integer.MAX_VALUE);
    Set<String> orderCode = orderController.searchOrders(params, pageRequest).getContent().stream()
        .map(BasicOrderDto::getOrderCode).collect(
            Collectors.toSet());
    List<LocalReceiptVoucher> localReceiptVouchers = localReceiptVoucherRepository
        .findByOrderCodeAndProgramIdAndRequestingFacilityIdAndSupplyingFacilityId(
            dto.getOrderCode(), dto.getProgramId(), dto.getRequestingFacilityId(), dto.getSupplyingFacilityId());
    if (orderCode.contains(dto.getOrderCode()) || !localReceiptVouchers.isEmpty()) {
      throw new BusinessDataException(new Message(ERROR_ORDER_CODE_EXISTS), "order code already exists");
    }
  }

  private void validateLocalReceiptVoucher(LocalReceiptVoucherDto dto) {
    if (Objects.isNull(dto.getOrderCode()) || Objects.isNull(dto.getStatus()) || Objects.isNull(dto.getProgramId())
        || Objects.isNull(dto.getRequestingFacilityId()) || Objects.isNull(dto.getSupplyingFacilityId())) {
      throw new ValidationMessageException("Validation failed");
    }
  }
}
