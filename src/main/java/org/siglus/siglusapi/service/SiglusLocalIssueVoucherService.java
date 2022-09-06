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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ADDITIONAL_ORDERABLE_DUPLICATED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCAL_ISSUE_VOUCHER_ID_INVALID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ORDER_CODE_EXISTS;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.siglus.siglusapi.domain.LocalIssueVoucher;
import org.siglus.siglusapi.dto.LocalIssueVoucherDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.PodLineItemsRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.repository.SiglusLocalIssueVoucherRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiglusLocalIssueVoucherService {

  private final OrderController orderController;

  private final SiglusAuthenticationHelper authenticationHelper;

  private final SiglusLocalIssueVoucherRepository localReceiptVoucherRepository;

  private final PodSubDraftRepository podSubDraftRepository;

  private final SiglusPodService siglusPodService;

  private final PodLineItemsRepository podLineItemsRepository;

  public LocalIssueVoucherDto createLocalIssueVoucher(LocalIssueVoucherDto dto) {
    checkOrderCodeExists(dto);
    LocalIssueVoucher localIssueVoucher = LocalIssueVoucher.createLocalReceiptVoucher(dto);
    log.info("save local receipt voucher with requestingFacilityId {}", dto.getRequestingFacilityId());
    LocalIssueVoucher savedLocalIssueVoucher = localReceiptVoucherRepository.save(localIssueVoucher);
    return LocalIssueVoucherDto.from(savedLocalIssueVoucher);
  }

  private void checkOrderCodeExists(LocalIssueVoucherDto dto) {
    Set<String> status = Sets.newHashSet(OrderStatus.TRANSFER_FAILED.toString(), OrderStatus.SHIPPED.toString(),
        OrderStatus.RECEIVED.toString(), OrderStatus.IN_ROUTE.toString(), OrderStatus.READY_TO_PACK.toString());
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
    List<LocalIssueVoucher> localIssueVouchers = localReceiptVoucherRepository
        .findByOrderCodeAndProgramIdAndRequestingFacilityIdAndSupplyingFacilityId(
            dto.getOrderCode(), dto.getProgramId(), dto.getRequestingFacilityId(), dto.getSupplyingFacilityId());
    if (orderCode.contains(dto.getOrderCode()) || !localIssueVouchers.isEmpty()) {
      throw new BusinessDataException(new Message(ERROR_ORDER_CODE_EXISTS), "order code already exists");
    }
  }

  public void deleteLocalIssueVoucher(UUID id) {
    validateInitialDraftId(id);
    log.info("delete proof subDrafts with proof of delivery id: {}", id);
    podSubDraftRepository.deleteAllByPodId(id);
    log.info("delete local issue voucher with id: {}", id);
    localReceiptVoucherRepository.delete(id);
  }

  public ProofOfDeliveryDto getSubDraftDetail(UUID podId, UUID subDraftId, Set<String> expand) {
    return siglusPodService.getSubDraftDetail(podId, subDraftId, expand);
  }

  public void updateSubDraft(UpdatePodSubDraftRequest request, UUID subDraftId) {
    validateOrderableDuplicated(request, subDraftId);
    siglusPodService.updateSubDraft(request, subDraftId);
  }

  public void deleteSubDraft(UUID podId, UUID subDraftId) {
    siglusPodService.deleteSubDraft(podId, subDraftId);
  }

  private void validateInitialDraftId(UUID id) {
    LocalIssueVoucher localIssueVoucher = localReceiptVoucherRepository.findOne(id);
    if (localIssueVoucher == null) {
      throw new ValidationMessageException(ERROR_LOCAL_ISSUE_VOUCHER_ID_INVALID);
    }
  }

  private void validateOrderableDuplicated(UpdatePodSubDraftRequest request, UUID subDraftId) {
    List<UUID> orderableIds = request
        .getPodDto()
        .getLineItems()
        .stream()
        .map(proofOfDeliveryLineItemDto -> proofOfDeliveryLineItemDto.getOrderableIdentity().getId())
        .collect(Collectors.toList());
    UUID podId = request.getPodDto().getId();
    List<ProofOfDeliveryLineItem> duplicatedOrderableLineItem = podLineItemsRepository.findDuplicatedOrderableLineItem(
        orderableIds, podId, subDraftId);
    if (CollectionUtils.isNotEmpty(duplicatedOrderableLineItem)) {
      throw new ValidationMessageException(ERROR_ADDITIONAL_ORDERABLE_DUPLICATED);
    }
  }
}
