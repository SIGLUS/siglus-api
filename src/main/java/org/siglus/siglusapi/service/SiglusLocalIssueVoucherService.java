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

import static java.util.stream.Collectors.toList;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ADDITIONAL_ORDERABLE_DUPLICATED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_CANNOT_OPERATE_WHEN_SUB_DRAFT_SUBMITTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ID_NOT_MATCH_SUB_DRAFT_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCAL_ISSUE_VOUCHER_ID_INVALID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCAL_ISSUE_VOUCHER_SUB_DRAFTS_MORE_THAN_TEN;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_POD_SUB_DRAFT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_ORDER_CODE_EXISTS;

import com.google.common.collect.Sets;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Objects;
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
import org.openlmis.referencedata.dto.OrderableDto;
import org.siglus.siglusapi.domain.LocalIssueVoucher;
import org.siglus.siglusapi.domain.LocalIssueVoucherDraftLineItem;
import org.siglus.siglusapi.domain.LocalIssueVoucherSubDraft;
import org.siglus.siglusapi.domain.PodSubDraft;
import org.siglus.siglusapi.dto.LocalIssueVoucherDraftLineItemDto;
import org.siglus.siglusapi.dto.LocalIssueVoucherDto;
import org.siglus.siglusapi.dto.LocalIssueVoucherSubDraftDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.enums.PodSubDraftStatusEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.LocalIssueVoucherDraftLineItemRepository;
import org.siglus.siglusapi.repository.LocalIssueVoucherSubDraftRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.repository.SiglusLocalIssueVoucherRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.OperateTypeEnum;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse.SubDraftInfo;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiglusLocalIssueVoucherService {

  private final OrderController orderController;

  private final SiglusAuthenticationHelper authenticationHelper;

  private final SiglusLocalIssueVoucherRepository localIssueVoucherRepository;

  private final PodSubDraftRepository podSubDraftRepository;

  private final SiglusPodService siglusPodService;

  private final LocalIssueVoucherDraftLineItemRepository localIssueVoucherDraftLineItemRepository;

  private final LocalIssueVoucherSubDraftRepository localIssueVoucherSubDraftRepository;

  private final SiglusOrderableService siglusOrderableService;

  private static final Integer SUB_DRAFTS_LIMITATION = 10;
  private static final Integer SUB_DRAFTS_INCREMENT = 1;

  public LocalIssueVoucherDto createLocalIssueVoucher(LocalIssueVoucherDto dto) {
    checkOrderCodeExists(dto);
    LocalIssueVoucher localIssueVoucher = LocalIssueVoucher.createLocalReceiptVoucher(dto);
    log.info("save local receipt voucher with requestingFacilityId {}", dto.getRequestingFacilityId());
    LocalIssueVoucher savedLocalIssueVoucher = localIssueVoucherRepository.save(localIssueVoucher);
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
    List<LocalIssueVoucher> localIssueVouchers = localIssueVoucherRepository
        .findByOrderCodeAndProgramIdAndRequestingFacilityIdAndSupplyingFacilityId(
            dto.getOrderCode(), dto.getProgramId(), dto.getRequestingFacilityId(), dto.getSupplyingFacilityId());
    if (orderCode.contains(dto.getOrderCode()) || !localIssueVouchers.isEmpty()) {
      throw new BusinessDataException(new Message(ERROR_ORDER_CODE_EXISTS));
    }
  }

  @Transactional
  public void deleteLocalIssueVoucher(UUID localIssueVoucherId) {
    validateLocalIssueVoucherId(localIssueVoucherId);
    log.info("delete subDrafts with proof of delivery id: {}", localIssueVoucherId);
    podSubDraftRepository.deleteAllByPodId(localIssueVoucherId);
    log.info("delete local issue voucher with id: {}", localIssueVoucherId);
    localIssueVoucherRepository.delete(localIssueVoucherId);
  }

  public SubDraftInfo createLocalIssueVoucherSubDraft(UUID localIssueVoucherId) {
    validateLocalIssueVoucherId(localIssueVoucherId);
    int subDraftsQuantity = podSubDraftRepository.countAllByPodId(localIssueVoucherId);
    checkIfSubDraftsOversize(subDraftsQuantity);
    PodSubDraft subDraft = createPodSubDraft(localIssueVoucherId, subDraftsQuantity);
    log.info("save local issue voucher with localIssueVoucherId: {}", localIssueVoucherId);
    PodSubDraft localIssueVoucherSubDraft = podSubDraftRepository.save(subDraft);
    return buildSubDraftInfo(localIssueVoucherSubDraft);
  }

  private SubDraftInfo buildSubDraftInfo(PodSubDraft localIssueVoucherSubDraft) {
    return SubDraftInfo.builder()
        .subDraftId(localIssueVoucherSubDraft.getId())
        .groupNum(localIssueVoucherSubDraft.getNumber())
        .saver(authenticationHelper.getUserNameByUserId(localIssueVoucherSubDraft.getOperatorId()))
        .status(localIssueVoucherSubDraft.getStatus())
        .build();
  }

  private PodSubDraft createPodSubDraft(UUID localIssueVoucherId, int subDraftsQuantity) {
    return PodSubDraft.builder()
        .number(subDraftsQuantity + SUB_DRAFTS_INCREMENT)
        .podId(localIssueVoucherId)
        .status(PodSubDraftStatusEnum.NOT_YET_STARTED)
        .build();
  }

  public void validateLocalIssueVoucherId(UUID localIssueVoucherId) {
    LocalIssueVoucher localIssueVoucher = localIssueVoucherRepository.findOne(localIssueVoucherId);
    if (localIssueVoucher == null) {
      throw new ValidationMessageException(ERROR_LOCAL_ISSUE_VOUCHER_ID_INVALID);
    }
  }

  private void checkIfSubDraftsOversize(int subDraftsQuantity) {
    if (subDraftsQuantity > SUB_DRAFTS_LIMITATION - 1) {
      throw new BusinessDataException(new Message(ERROR_LOCAL_ISSUE_VOUCHER_SUB_DRAFTS_MORE_THAN_TEN));
    }
  }

  public PodSubDraftsSummaryResponse searchLocalIssueVoucherSubDrafts(UUID localIssueVoucherId) {
    return siglusPodService.getSubDraftSummary(localIssueVoucherId);
  }

  public LocalIssueVoucherSubDraftDto getSubDraftDetail(UUID subDraftId) {
    List<LocalIssueVoucherDraftLineItem> localIssueVoucherDraftLineItems =
        localIssueVoucherDraftLineItemRepository.findByLocalIssueVoucherSubDraftId(
            subDraftId);
    return LocalIssueVoucherSubDraftDto
        .builder()
        .lineItems(LocalIssueVoucherDraftLineItemDto.from(localIssueVoucherDraftLineItems))
        .build();
  }

  @Transactional
  public void updateSubDraft(UUID localIssueVoucherId, LocalIssueVoucherSubDraftDto subDraftDto, UUID subDraftId) {
    LocalIssueVoucherSubDraft subDraft = localIssueVoucherSubDraftRepository.findOne(subDraftId);
    if (!subDraft.getLocalIssueVoucherId().equals(localIssueVoucherId)) {
      throw new InputMismatchException(ERROR_ID_NOT_MATCH_SUB_DRAFT_ID);
    }
    checkIfCanOperate(subDraft);
    validateOrderableDuplicated(subDraftDto, subDraftId);
    log.info("save local issue voucher line items of subdraft {} ,size {}",
        subDraftId, subDraftDto.getLineItems().size());

    localIssueVoucherDraftLineItemRepository.save(LocalIssueVoucherDraftLineItemDto.to(subDraftDto.getLineItems()));
    if (subDraftDto.getOperateType().equals(OperateTypeEnum.SUBMIT)) {
      subDraft.setStatus(PodSubDraftStatusEnum.SUBMITTED);
    } else {
      subDraft.setStatus(PodSubDraftStatusEnum.DRAFT);
    }
    log.info("update local issue voucher subdraft {} status as {}", subDraftId, subDraft.getStatus());
    localIssueVoucherSubDraftRepository.save(subDraft);
  }

  @Transactional
  public void clearFillingPage(UUID subDraftId) {
    LocalIssueVoucherSubDraft subDraft = localIssueVoucherSubDraftRepository.findOne(subDraftId);
    checkIfCanOperate(subDraft);
    log.info("clear local issue voucher subDraft {} all line items", subDraftId);
    localIssueVoucherDraftLineItemRepository.deleteByLocalIssueVoucherSubDraftId(subDraftId);
    subDraft.setStatus(PodSubDraftStatusEnum.NOT_YET_STARTED);
    log.info("update local issue voucher subdraft {} status as {}", subDraftId, subDraft.getStatus());
    localIssueVoucherSubDraftRepository.save(subDraft);
  }

  private void validateOrderableDuplicated(LocalIssueVoucherSubDraftDto subDraftDto, UUID subDraftId) {
    List<UUID> orderableIds = subDraftDto
        .getLineItems()
        .stream()
        .map(LocalIssueVoucherDraftLineItemDto::getOrderableId)
        .collect(Collectors.toList());
    UUID localIssueVoucherId = subDraftDto.getLocalIssueVoucherId();
    List<ProofOfDeliveryLineItem> duplicatedOrderableLineItem =
        localIssueVoucherDraftLineItemRepository
            .findDuplicatedOrderableLineItem(orderableIds, localIssueVoucherId, subDraftId);
    if (CollectionUtils.isNotEmpty(duplicatedOrderableLineItem)) {
      throw new ValidationMessageException(ERROR_ADDITIONAL_ORDERABLE_DUPLICATED);
    }
  }

  public List<OrderableDto> getAvailableOrderables(UUID podId) {
    List<UUID> usedOrderableByPodId = localIssueVoucherDraftLineItemRepository.findUsedOrderableByPodId(podId);
    List<OrderableDto> allProducts = siglusOrderableService.getAllProducts();
    if (CollectionUtils.isNotEmpty(usedOrderableByPodId)) {
      allProducts = allProducts.stream()
          .filter(orderableDto -> usedOrderableByPodId.contains(orderableDto.getId())).collect(
              Collectors.toList());
    }
    return allProducts;
  }

  @Transactional
  public void deleteSubDraft(UUID podId, UUID subDraftId) {
    PodSubDraft podSubDraft = getPodSubDraft(subDraftId);
    checkIfCanOperate(podSubDraft);
    log.info("delete pod subDraft line items with subDraft id: {}", subDraftId);
    localIssueVoucherDraftLineItemRepository.deleteByLocalIssueVoucherSubDraftId(subDraftId);
    log.info("delete subDraft with id: {}", subDraftId);
    podSubDraftRepository.delete(subDraftId);
    resetSubDraftNumber(podId, podSubDraft);
  }

  private PodSubDraft getPodSubDraft(UUID subDraftId) {
    PodSubDraft podSubDraft = podSubDraftRepository.findOne(subDraftId);
    if (Objects.isNull(podSubDraft)) {
      throw new NotFoundException(ERROR_NO_POD_SUB_DRAFT_FOUND);
    }
    return podSubDraft;
  }

  private void checkIfCanOperate(PodSubDraft podSubDraft) {
    if (PodSubDraftStatusEnum.SUBMITTED == podSubDraft.getStatus()) {
      throw new BusinessDataException(new Message(ERROR_CANNOT_OPERATE_WHEN_SUB_DRAFT_SUBMITTED), podSubDraft.getId());
    }
  }

  public void checkIfCanOperate(LocalIssueVoucherSubDraft subDraft) {
    if (subDraft.getStatus().equals(PodSubDraftStatusEnum.SUBMITTED)) {
      throw new BusinessDataException(new Message(ERROR_CANNOT_OPERATE_WHEN_SUB_DRAFT_SUBMITTED), subDraft.getId());
    }
  }

  @Transactional
  public void resetSubDraftNumber(UUID podId, PodSubDraft podSubDraft) {
    List<PodSubDraft> podSubDrafts = podSubDraftRepository.findAllByPodId(podId);
    List<PodSubDraft> filterSubDrafts = podSubDrafts.stream()
        .filter(subDraft -> subDraft.getNumber() > podSubDraft.getNumber())
        .collect(toList());
    if (!filterSubDrafts.isEmpty()) {
      filterSubDrafts.forEach(subDraft -> subDraft.setNumber(subDraft.getNumber() - SUB_DRAFTS_INCREMENT));
      podSubDraftRepository.save(filterSubDrafts);
    }
  }
}
