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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_POD_OR_POD_LINE_ITEM_FOUNT;

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
import org.apache.commons.collections.CollectionUtils;
import org.javers.common.collections.Sets;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.service.ProofOfDeliveryService;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
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
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.service.client.SiglusProofOfDeliveryFulfillmentService;
import org.siglus.siglusapi.util.CustomListSortHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.CreatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.PodSubDraftListResponse;
import org.siglus.siglusapi.web.response.PodSubDraftListResponse.SubDraftInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiglusProofOfDeliveryService {

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
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().proofOfDeliveryId(proofOfDeliveryId).build());
    List<PodSubDraft> podSubDrafts = podSubDraftRepository.findAll(example);
    if (CollectionUtils.isEmpty(podSubDrafts)) {
      throw new BusinessDataException(new Message(ERROR_NO_POD_OR_POD_LINE_ITEM_FOUNT), proofOfDeliveryId);
    }
    return PodSubDraftListResponse.builder()
        .proofOfDeliveryId(podSubDrafts.get(0).getProofOfDeliveryId())
        .subDrafts(buildSubDraftInfos(podSubDrafts))
        .canMergeOrDeleteDrafts(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts())
        .canSubmitDrafts(isTheAllSubDraftIsSubmitted(podSubDrafts))
        .build();
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
    Map<Integer, UUID> numberSubDraftIdMap = subDrafts.stream()
        .collect(Collectors.toMap(PodSubDraft::getNumber, PodSubDraft::getId));
    List<PodLineItemsExtension> podLineItemsExtensions = Lists.newArrayList();
    for (int i = 0; i < splitGroupList.size(); i++) {
      List<List<SimpleLineItem>> simpleLineItemList = splitGroupList.get(i);
      UUID subDraftId = numberSubDraftIdMap.get(i);
      simpleLineItemList.forEach(productLineItems ->
          productLineItems.forEach(lineItem ->
              podLineItemsExtensions.add(PodLineItemsExtension.builder()
                  .subDraftId(subDraftId)
                  .podLineItemId(lineItem.getLineItemId())
                  .build())));
    }
    podLineItemsExtensionRepository.save(podLineItemsExtensions);
  }

  private List<PodSubDraft> buildAndSavePodSubDrafts(List<List<List<SimpleLineItem>>> splitGroupList) {
    List<PodSubDraft> subDrafts = Lists.newArrayList();
    for (int i = 0; i < splitGroupList.size(); i++) {
      List<List<SimpleLineItem>> simpleLineItemList = splitGroupList.get(i);
      subDrafts.add(PodSubDraft.builder()
          .number(i + 1)
          .proofOfDeliveryId(simpleLineItemList.get(0).get(0).proofOfDeliveryId)
          .status(PodSubDraftEnum.NOT_YET_STARTED)
          .build());
    }
    return podSubDraftRepository.save(subDrafts);
  }

  private List<List<SimpleLineItem>> getGroupByProductIdLineItemList(List<SimpleLineItem> simpleLineItems) {
    List<List<SimpleLineItem>> groupByProductIdLineItems = new ArrayList<>();
    Map<UUID, List<SimpleLineItem>> productIdToLineItemsMap = simpleLineItems.stream()
        .collect(Collectors.groupingBy(SimpleLineItem::getProductId));
    productIdToLineItemsMap.forEach((productId, lineItems) -> {
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
      throw new BusinessDataException(new Message(ERROR_NO_POD_OR_POD_LINE_ITEM_FOUNT), orderId);
    }

    ProofOfDelivery proofOfDelivery = proofOfDeliveries.getContent().get(0);
    if (proofOfDelivery.getLineItems().isEmpty()) {
      throw new BusinessDataException(new Message(ERROR_NO_POD_OR_POD_LINE_ITEM_FOUNT), orderId);
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
