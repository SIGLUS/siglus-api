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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_CANNOT_OPERATE_WHEN_SUB_DRAFT_SUBMITTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_POD_OR_POD_LINE_ITEM_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_POD_SUB_DRAFT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_POD_ID_SUB_DRAFT_ID_NOT_MATCH;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.javers.common.collections.Sets;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem.Importer;
import org.openlmis.fulfillment.service.ProofOfDeliveryService;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.domain.referencedata.Orderable;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.PodLineItemsExtension;
import org.siglus.siglusapi.domain.PodSubDraft;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.enums.PodSubDraftEnum;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PodLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.service.client.SiglusProofOfDeliveryFulfillmentService;
import org.siglus.siglusapi.util.CustomListSortHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.CreatePodSubDraftRequest;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.PodSubDraftListResponse;
import org.siglus.siglusapi.web.response.PodSubDraftListResponse.SubDraftInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SiglusProofOfDeliveryService {

  private static final Set<String> DEFAULT_EXPAND = Sets.asSet("shipment.order");

  @Autowired
  private SiglusProofOfDeliveryFulfillmentService fulfillmentService;

  @Autowired
  private SiglusOrderService siglusOrderService;

  @Autowired
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Autowired
  private OrderExternalRepository orderExternalRepository;

  @Autowired
  private ProofOfDeliveryService proofOfDeliveryService;

  @Autowired
  private OrderableRepository orderableRepository;

  @Autowired
  private PodSubDraftRepository podSubDraftRepository;

  @Autowired
  private PodLineItemsExtensionRepository podLineItemsExtensionRepository;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private PodLineItemsRepository podLineItemsRepository;

  public ProofOfDeliveryDto getProofOfDelivery(UUID id,
      Set<String> expand) {
    ProofOfDeliveryDto proofOfDeliveryDto = fulfillmentService.searchProofOfDelivery(id, expand);
    OrderObjectReferenceDto order = proofOfDeliveryDto.getShipment().getOrder();
    OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
    UUID requisitionId = external == null ? order.getExternalId() : external.getRequisitionId();
    order.setRequisitionNumber(
        siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId));
    if (CollectionUtils.isNotEmpty(order.getOrderLineItems())) {
      proofOfDeliveryDto.getShipment().setOrder(siglusOrderService.getExtensionOrder(order));
    }
    return proofOfDeliveryDto;
  }

  @Transactional
  public void createSubDraft(CreatePodSubDraftRequest request) {
    // TODO no need order id
    // TODO 校验 pod id 和 order id
    // TODO 判断 order 是否已经是 received 状态 ( pod 是否已经是 summited 状态), permission
    ProofOfDelivery proofOfDelivery = getProofOfDeliveryByOrderId(request.getOrderId());
    List<SimpleLineItem> simpleLineItems = buildSimpleLineItems(proofOfDelivery);
    List<List<SimpleLineItem>> groupByProductIdLineItems = getGroupByProductIdLineItemList(simpleLineItems);
    List<List<List<SimpleLineItem>>> splitGroupList = CustomListSortHelper.averageAssign(groupByProductIdLineItems,
        request.getSplitNum());

    List<PodSubDraft> subDrafts = buildAndSavePodSubDrafts(splitGroupList);
    buildAndSavePodLineItemsExtensions(splitGroupList, subDrafts);
  }

  public PodSubDraftListResponse searchSubDraftList(UUID proofOfDeliveryId) {
    List<PodSubDraft> podSubDrafts = getPodSubDraftsByProofOfDeliveryId(proofOfDeliveryId);
    return PodSubDraftListResponse.builder()
        .proofOfDeliveryId(podSubDrafts.get(0).getProofOfDeliveryId())
        .subDrafts(buildSubDraftInfos(podSubDrafts))
        .canMergeOrDeleteDrafts(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts())
        .canSubmitDrafts(isTheAllSubDraftIsSubmitted(podSubDrafts))
        .build();
  }

  public ProofOfDeliveryDto getSubDraftDetail(UUID proofOfDeliveryId, UUID subDraftId) {
    checkIfProofOfDeliveryIdAndSubDraftIdMatch(proofOfDeliveryId, subDraftId);

    ProofOfDeliveryDto dto = getProofOfDelivery(proofOfDeliveryId, DEFAULT_EXPAND);
    List<ProofOfDeliveryLineItemDto> currentSubDraftLineItems = getCurrentSubDraftPodLineItemDtos(subDraftId, dto);
    dto.setLineItems(currentSubDraftLineItems);
    return dto;
  }

  @Transactional
  public void updateSubDraft(UpdatePodSubDraftRequest request, UUID subDraftId) {
    PodSubDraft podSubDraft = getPodSubDraft(subDraftId);
    checkIfCanOperate(podSubDraft);

    Set<UUID> lineItemIds = getPodLineItemIdsBySubDraftId(subDraftId);
    List<ProofOfDeliveryLineItem> lineItems = podLineItemsRepository.findAll(lineItemIds);

    List<ProofOfDeliveryLineItem> toBeUpdatedLineItems = buildToBeUpdatedLineItems(
        request.getProofOfDeliveryDto(), lineItems, request.getSubDraftStatus());
    log.info("update ProofOfDeliveryLineItem list, subDraftId:{}, lineItemIds:{}", subDraftId, lineItemIds);
    podLineItemsRepository.save(toBeUpdatedLineItems);

    updateSubDraftStatusAndOperator(podSubDraft, request.getSubDraftStatus());
  }

  @Transactional
  public void deleteSubDraft(UUID proofOfDeliveryId, UUID subDraftId) {
    PodSubDraft podSubDraft = getPodSubDraft(subDraftId);
    checkIfCanOperate(podSubDraft);

    Set<UUID> lineItemIds = getPodLineItemIdsBySubDraftId(subDraftId);
    ProofOfDeliveryDto proofOfDeliveryDto = fulfillmentService.searchProofOfDelivery(proofOfDeliveryId, null);
    clearAndSaveLineItems(lineItemIds, proofOfDeliveryDto);

    updateSubDraftStatusAndOperator(podSubDraft, PodSubDraftEnum.NOT_YET_STARTED);
  }

  @Transactional
  public void deleteAllSubDraft(UUID proofOfDeliveryId) {
    ProofOfDeliveryDto proofOfDeliveryDto = fulfillmentService.searchProofOfDelivery(proofOfDeliveryId, null);
    List<PodSubDraft> subDrafts = getPodSubDraftsByProofOfDeliveryId(proofOfDeliveryId);
    Set<UUID> subDraftIds = subDrafts.stream().map(PodSubDraft::getId).collect(Collectors.toSet());

    Set<UUID> lineItemIds = getPodLineItemIdsBySubDraftIds(subDraftIds);
    clearAndSaveLineItems(lineItemIds, proofOfDeliveryDto);

    log.info("delete proof of delivery line item extension, subDraftIds:{}", subDraftIds);
    podLineItemsExtensionRepository.deleteAllBySubDraftIds(subDraftIds);

    log.info("delete proof of delivery sub draft, subDraftIds:{}", subDraftIds);
    podSubDraftRepository.deleteAllByIds(subDraftIds);
  }

  private void clearAndSaveLineItems(Set<UUID> lineItemIds, ProofOfDeliveryDto proofOfDeliveryDto) {
    List<ProofOfDeliveryLineItem> lineItems = podLineItemsRepository.findAll(lineItemIds);
    List<ProofOfDeliveryLineItem> toBeUpdatedLineItems = buildToBeUpdatedLineItems(proofOfDeliveryDto, lineItems,
        PodSubDraftEnum.NOT_YET_STARTED);
    log.info("update ProofOfDeliveryLineItem list, podIdd:{}, lineItemIds:{}", proofOfDeliveryDto.getId(),
        lineItemIds);
    podLineItemsRepository.save(toBeUpdatedLineItems);
  }

  private List<PodSubDraft> getPodSubDraftsByProofOfDeliveryId(UUID proofOfDeliveryId) {
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().proofOfDeliveryId(proofOfDeliveryId).build());
    List<PodSubDraft> podSubDrafts = podSubDraftRepository.findAll(example);
    if (CollectionUtils.isEmpty(podSubDrafts)) {
      throw new BusinessDataException(new Message(ERROR_NO_POD_OR_POD_LINE_ITEM_FOUND), proofOfDeliveryId);
    }
    return podSubDrafts;
  }

  private void checkIfCanOperate(PodSubDraft podSubDraft) {
    if (PodSubDraftEnum.SUBMITTED == podSubDraft.getStatus()) {
      throw new BusinessDataException(new Message(ERROR_CANNOT_OPERATE_WHEN_SUB_DRAFT_SUBMITTED), podSubDraft.getId());
    }
  }

  private List<ProofOfDeliveryLineItem> buildToBeUpdatedLineItems(ProofOfDeliveryDto proofOfDeliveryDto,
      List<ProofOfDeliveryLineItem> lineItems, PodSubDraftEnum subDraftStatus) {
    Map<UUID, ProofOfDeliveryLineItemDto> idToLineItemDto = convertToLineItemDtos(
        proofOfDeliveryDto.getLineItems()).stream()
        .collect(Collectors.toMap(ProofOfDeliveryLineItemDto::getId, e -> e));
    List<ProofOfDeliveryLineItem> toBeUpdatedLineItems = Lists.newArrayListWithExpectedSize(lineItems.size());
    lineItems.forEach(lineItem -> {
      ProofOfDeliveryLineItemDto lineItemDto = idToLineItemDto.get(lineItem.getId());
      ProofOfDeliveryLineItem toBeUpdatedLineItem;
      if (PodSubDraftEnum.NOT_YET_STARTED == subDraftStatus) {
        toBeUpdatedLineItem = buildClearedProofOfDeliveryLineItem(lineItem);
      } else {
        toBeUpdatedLineItem = buildProofOfDeliveryLineItem(lineItem, lineItemDto);
      }
      toBeUpdatedLineItems.add(toBeUpdatedLineItem);
    });
    return toBeUpdatedLineItems;
  }

  private ProofOfDeliveryLineItem buildProofOfDeliveryLineItem(ProofOfDeliveryLineItem lineItem,
      ProofOfDeliveryLineItemDto lineItemDto) {
    // TODO vvmStatus ?
    ProofOfDeliveryLineItem toBeUpdatedLineItem = new ProofOfDeliveryLineItem(lineItem.getOrderable(),
        lineItem.getLotId(), lineItemDto.getQuantityAccepted(), null,
        lineItem.getQuantityRejected(), lineItem.getRejectionReasonId(), lineItemDto.getNotes());
    toBeUpdatedLineItem.setId(lineItem.getId());
    return toBeUpdatedLineItem;
  }

  private ProofOfDeliveryLineItem buildClearedProofOfDeliveryLineItem(ProofOfDeliveryLineItem lineItem) {
    // TODO vvmStatus ?
    ProofOfDeliveryLineItem toBeUpdatedLineItem = new ProofOfDeliveryLineItem(lineItem.getOrderable(),
        lineItem.getLotId(), null, null,
        lineItem.getQuantityRejected(), lineItem.getRejectionReasonId(), null);
    toBeUpdatedLineItem.setId(lineItem.getId());
    return toBeUpdatedLineItem;
  }


  private void updateSubDraftStatusAndOperator(PodSubDraft podSubDraft, PodSubDraftEnum status) {
    UUID currentUserId = authenticationHelper.getCurrentUserId().orElseThrow(IllegalStateException::new);
    podSubDraft.setOperatorId(PodSubDraftEnum.NOT_YET_STARTED == status ? null : currentUserId);
    podSubDraft.setStatus(status);
    log.info("save pod sub draft: {}", podSubDraft);
    podSubDraftRepository.save(podSubDraft);
  }

  private PodSubDraft getPodSubDraft(UUID subDraftId) {
    PodSubDraft podSubDraft = podSubDraftRepository.findOne(subDraftId);
    if (Objects.isNull(podSubDraft)) {
      throw new BusinessDataException(new Message(ERROR_NO_POD_SUB_DRAFT_FOUND), subDraftId);
    }
    return podSubDraft;
  }

  private List<ProofOfDeliveryLineItemDto> getCurrentSubDraftPodLineItemDtos(UUID subDraftId, ProofOfDeliveryDto dto) {
    Set<UUID> lineItemIds = getPodLineItemIdsBySubDraftId(subDraftId);
    return convertToLineItemDtos(dto.getLineItems()).stream()
        .filter(lineItemDto -> lineItemIds.contains(lineItemDto.getId())).collect(Collectors.toList());
  }

  private List<ProofOfDeliveryLineItemDto> convertToLineItemDtos(List<Importer> lineItems) {
    List<ProofOfDeliveryLineItemDto> proofOfDeliveryLineItemDtos = Lists.newArrayListWithExpectedSize(lineItems.size());
    for (Importer lineItem : lineItems) {
      proofOfDeliveryLineItemDtos.add((ProofOfDeliveryLineItemDto) lineItem);
    }
    return proofOfDeliveryLineItemDtos;
  }

  private Set<UUID> getPodLineItemIdsBySubDraftId(UUID subDraftId) {
    Example<PodLineItemsExtension> example = Example.of(PodLineItemsExtension.builder().subDraftId(subDraftId).build());
    List<PodLineItemsExtension> podLineItemsExtensions = podLineItemsExtensionRepository.findAll(example);
    return podLineItemsExtensions.stream().map(PodLineItemsExtension::getPodLineItemId).collect(Collectors.toSet());
  }

  private Set<UUID> getPodLineItemIdsBySubDraftIds(Set<UUID> subDraftIds) {
    List<PodLineItemsExtension> podLineItemsExtensions = podLineItemsExtensionRepository.findAllBySubDraftIds(
        subDraftIds);
    return podLineItemsExtensions.stream().map(PodLineItemsExtension::getPodLineItemId).collect(Collectors.toSet());
  }

  private void checkIfProofOfDeliveryIdAndSubDraftIdMatch(UUID proofOfDeliveryId, UUID subDraftId) {
    PodSubDraft podSubDraft = podSubDraftRepository.findOne(subDraftId);
    if (Objects.isNull(podSubDraft)) {
      throw new BusinessDataException(new Message(ERROR_NO_POD_SUB_DRAFT_FOUND), subDraftId);
    }
    if (!podSubDraft.getProofOfDeliveryId().equals(proofOfDeliveryId)) {
      throw new BusinessDataException(new Message(ERROR_POD_ID_SUB_DRAFT_ID_NOT_MATCH), subDraftId);
    }
  }

  private boolean isTheAllSubDraftIsSubmitted(List<PodSubDraft> subDraftList) {
    return subDraftList.stream().allMatch(e -> PodSubDraftEnum.SUBMITTED == e.getStatus());
  }

  private List<SubDraftInfo> buildSubDraftInfos(List<PodSubDraft> podSubDrafts) {
    return podSubDrafts.stream().map(podSubDraft ->
            SubDraftInfo.builder()
                .subDraftId(podSubDraft.getId())
                .groupNum(podSubDraft.getNumber())
                .saver(authenticationHelper.getUserNameByUserId(podSubDraft.getOperatorId()))
                .status(podSubDraft.getStatus())
                .build())
        .collect(Collectors.toList());
  }

  private void buildAndSavePodLineItemsExtensions(List<List<List<SimpleLineItem>>> splitGroupList,
      List<PodSubDraft> subDrafts) {
    Map<Integer, UUID> numberToSubDraftId = subDrafts.stream()
        .collect(Collectors.toMap(PodSubDraft::getNumber, PodSubDraft::getId));
    List<PodLineItemsExtension> podLineItemsExtensions = Lists.newArrayList();
    for (int i = 0; i < splitGroupList.size(); i++) {
      List<List<SimpleLineItem>> simpleLineItemList = splitGroupList.get(i);
      UUID subDraftId = numberToSubDraftId.get(i + 1);
      simpleLineItemList.forEach(productLineItems ->
          productLineItems.forEach(lineItem ->
              podLineItemsExtensions.add(PodLineItemsExtension.builder()
                  .subDraftId(subDraftId)
                  .podLineItemId(lineItem.getLineItemId())
                  .build())));
    }
    log.info("save pod line items extensions, sub draft id: {}", numberToSubDraftId.values());
    podLineItemsExtensionRepository.save(podLineItemsExtensions);
  }

  private List<PodSubDraft> buildAndSavePodSubDrafts(List<List<List<SimpleLineItem>>> splitGroupList) {
    List<PodSubDraft> subDrafts = Lists.newArrayList();
    UUID proofOfDeliveryId = splitGroupList.get(0).get(0).get(0).getProofOfDeliveryId();
    for (int i = 0; i < splitGroupList.size(); i++) {
      subDrafts.add(PodSubDraft.builder()
          .number(i + 1)
          .proofOfDeliveryId(proofOfDeliveryId)
          .status(PodSubDraftEnum.NOT_YET_STARTED)
          .build());
    }
    log.info("save pod sub drafts, proof of delivery id: {}", proofOfDeliveryId);
    return podSubDraftRepository.save(subDrafts);
  }

  private List<List<SimpleLineItem>> getGroupByProductIdLineItemList(List<SimpleLineItem> simpleLineItems) {
    List<List<SimpleLineItem>> groupByProductIdLineItems = new ArrayList<>();
    Map<UUID, List<SimpleLineItem>> productIdToLineItems = simpleLineItems.stream()
        .collect(Collectors.groupingBy(SimpleLineItem::getProductId));
    productIdToLineItems.forEach((productId, lineItems) -> {
      groupByProductIdLineItems.add(lineItems);
    });
    groupByProductIdLineItems.sort(Comparator.comparing(o -> o.get(0).getProductCode()));
    return groupByProductIdLineItems;
  }

  private List<SimpleLineItem> buildSimpleLineItems(ProofOfDelivery proofOfDelivery) {
    List<SimpleLineItem> simpleLineItems = proofOfDelivery.getLineItems().stream()
        .map(lineItem -> SimpleLineItem.builder()
            .lineItemId(lineItem.getId())
            .proofOfDeliveryId(proofOfDelivery.getId())
            .productId(lineItem.getOrderable().getId())
            .build())
        .collect(Collectors.toList());
    setProductCode(simpleLineItems);
    return simpleLineItems;
  }

  private void setProductCode(List<SimpleLineItem> simpleLineItems) {
    List<Orderable> orderables = orderableRepository.findLatestByIds(
        simpleLineItems.stream().map(SimpleLineItem::getProductId).collect(
            Collectors.toList()));
    Map<UUID, Orderable> productIdToProduct = orderables.stream()
        .collect(Collectors.toMap(Orderable::getId, e -> e, (a, b) -> a));
    simpleLineItems.forEach(simpleLineItem -> simpleLineItem.setProductCode(
        productIdToProduct.get(simpleLineItem.getProductId()).getProductCode().toString()));
  }

  private ProofOfDelivery getProofOfDeliveryByOrderId(UUID orderId) {
    Page<ProofOfDelivery> proofOfDeliveries = proofOfDeliveryService.search(null, orderId, new PageRequest(0, 1));
    if (proofOfDeliveries.getTotalElements() == 0 || proofOfDeliveries.getContent().isEmpty()) {
      throw new BusinessDataException(new Message(ERROR_NO_POD_OR_POD_LINE_ITEM_FOUND), orderId);
    }

    ProofOfDelivery proofOfDelivery = proofOfDeliveries.getContent().get(0);
    if (proofOfDelivery.getLineItems().isEmpty()) {
      throw new BusinessDataException(new Message(ERROR_NO_POD_OR_POD_LINE_ITEM_FOUND), orderId);
    }

    return proofOfDelivery;
  }

  @Data
  @Builder
  private static class SimpleLineItem {

    private UUID lineItemId;
    private UUID proofOfDeliveryId;
    private UUID productId;
    private String productCode;
  }
}
