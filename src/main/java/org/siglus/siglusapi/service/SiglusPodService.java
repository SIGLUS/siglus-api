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
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_ALL_SUB_DRAFTS_SUBMITTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_POD_OR_POD_LINE_ITEM_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_POD_SUB_DRAFT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_POD_ID_SUB_DRAFT_ID_NOT_MATCH;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SPLIT_NUM_TOO_LARGE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SUB_DRAFTS_ALREADY_EXISTED;

import com.google.common.collect.Lists;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem.Importer;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.web.ProofOfDeliveryController;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.repository.StatusChangeRepository;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.PodLineItemsExtension;
import org.siglus.siglusapi.domain.PodSubDraft;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.enums.PodSubDraftStatusEnum;
import org.siglus.siglusapi.exception.AuthenticationException;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.OrdersRepository;
import org.siglus.siglusapi.repository.PodLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.repository.RequisitionsRepository;
import org.siglus.siglusapi.repository.dto.OrderDto;
import org.siglus.siglusapi.repository.dto.PodLineItemDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusPodFulfillmentService;
import org.siglus.siglusapi.util.CustomListSortHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.CreatePodSubDraftRequest;
import org.siglus.siglusapi.web.request.OperateTypeEnum;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.PodPrintInfoResponse;
import org.siglus.siglusapi.web.response.PodPrintInfoResponse.LineItemInfo;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse.SubDraftInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SiglusPodService {

  @Autowired
  private SiglusPodFulfillmentService fulfillmentService;

  @Autowired
  private SiglusOrderService siglusOrderService;

  @Autowired
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Autowired
  private OrderExternalRepository orderExternalRepository;

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

  @Autowired
  private ProofOfDeliveryController podController;

  @Autowired
  private SiglusNotificationService notificationService;

  @Autowired
  private OrdersRepository ordersRepository;

  @Autowired
  private RequisitionsRepository requisitionsRepository;

  @Autowired
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Autowired
  private StatusChangeRepository requisitionStatusChangeRepository;

  private static final String FILE_NAME_PREFIX_EMERGENCY = "OF.REM.";
  private static final String FILE_NAME_PREFIX_NORMAL = "OF.RNO.";
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyMM");
  private static final List<String> REQUISITION_STATUS_POST_SUBMIT = Lists.newArrayList(
      RequisitionStatus.APPROVED.name(),
      RequisitionStatus.RELEASED.name(),
      RequisitionStatus.RELEASED_WITHOUT_ORDER.name());
  private static final Integer PROVINCE_LEVEL_NUMBER = 2;
  private static final Integer DISTRICT_LEVEL_NUMBER = 3;

  public ProofOfDeliveryDto getPodDtoByIdAndExpand(UUID id, Set<String> expand) {
    ProofOfDeliveryDto podDto = fulfillmentService.searchProofOfDelivery(id, expand);
    if (Objects.isNull(podDto)) {
      throw new NotFoundException(ERROR_NO_POD_OR_POD_LINE_ITEM_FOUND);
    }
    OrderObjectReferenceDto order = podDto.getShipment().getOrder();
    OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
    UUID requisitionId = external == null ? order.getExternalId() : external.getRequisitionId();
    order.setRequisitionNumber(
        siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId));
    if (CollectionUtils.isNotEmpty(order.getOrderLineItems())) {
      podDto.getShipment().setOrder(siglusOrderService.getExtensionOrder(order));
    }
    return podDto;
  }

  @Transactional
  public void createSubDrafts(UUID podId, CreatePodSubDraftRequest request) {
    checkIfSubDraftsExist(podId);

    ProofOfDeliveryDto podDto = getPodDtoByPodId(podId);
    List<SimpleLineItem> simpleLineItems = buildSimpleLineItems(podDto);
    List<List<SimpleLineItem>> groupByProductIdLineItems = getGroupByProductIdLineItemList(simpleLineItems);
    if (groupByProductIdLineItems.size() < request.getSplitNum()) {
      throw new ValidationMessageException(ERROR_SPLIT_NUM_TOO_LARGE);
    }
    List<List<List<SimpleLineItem>>> splitGroupList = CustomListSortHelper.averageAssign(groupByProductIdLineItems,
        request.getSplitNum());

    List<PodSubDraft> subDrafts = buildAndSavePodSubDrafts(splitGroupList);
    buildAndSavePodLineItemsExtensions(splitGroupList, subDrafts);
  }

  public PodSubDraftsSummaryResponse getSubDraftSummary(UUID podId) {
    List<PodSubDraft> podSubDrafts = getPodSubDraftsByPodId(podId);
    return PodSubDraftsSummaryResponse.builder()
        .podId(podSubDrafts.get(0).getPodId())
        .subDrafts(buildSubDraftInfos(podSubDrafts))
        .canMergeOrDeleteDrafts(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts())
        .canSubmitDrafts(isAllSubDraftsSubmitted(podSubDrafts))
        .build();
  }

  public ProofOfDeliveryDto getSubDraftDetail(UUID podId, UUID subDraftId, Set<String> expand) {
    checkIfPodIdAndSubDraftIdMatch(podId, subDraftId);

    ProofOfDeliveryDto dto = getPodDtoByIdAndExpand(podId, expand);
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
        request.getPodDto(), lineItems, OperateTypeEnum.getPodSubDraftEnum(request.getOperateType()));
    log.info("update ProofOfDeliveryLineItem list, subDraftId:{}, lineItemIds:{}", subDraftId, lineItemIds);
    podLineItemsRepository.save(toBeUpdatedLineItems);

    updateSubDraftStatusAndOperator(podSubDraft, OperateTypeEnum.getPodSubDraftEnum(request.getOperateType()));
  }

  @Transactional
  public void deleteSubDraft(UUID podId, UUID subDraftId) {
    PodSubDraft podSubDraft = getPodSubDraft(subDraftId);
    checkIfCanOperate(podSubDraft);

    Set<UUID> lineItemIds = getPodLineItemIdsBySubDraftId(subDraftId);
    ProofOfDeliveryDto proofOfDeliveryDto = getPodDtoByPodId(podId);
    resetLineItems(lineItemIds, proofOfDeliveryDto);

    updateSubDraftStatusAndOperator(podSubDraft, PodSubDraftStatusEnum.NOT_YET_STARTED);
  }

  @Transactional
  public void deleteSubDrafts(UUID podId) {
    checkAuth();
    Set<UUID> subDraftIds = getPodSubDraftsByPodId(podId).stream().map(PodSubDraft::getId).collect(Collectors.toSet());

    ProofOfDeliveryDto proofOfDeliveryDto = getPodDtoByPodId(podId);
    Set<UUID> lineItemIds = getPodLineItemIdsBySubDraftIds(subDraftIds);
    resetLineItems(lineItemIds, proofOfDeliveryDto);

    deleteSubDraftAndLineExtensionBySubDraftIds(subDraftIds);
  }

  public ProofOfDeliveryDto mergeSubDrafts(UUID podId, Set<String> expand) {
    checkAuth();
    checkIfSubDraftsSubmitted(podId);
    return getPodDtoByIdAndExpand(podId, expand);
  }

  @Transactional
  public ProofOfDeliveryDto submitSubDrafts(UUID podId, ProofOfDeliveryDto requestPodDto,
      OAuth2Authentication authentication) {
    checkAuth();
    List<PodSubDraft> subDrafts = checkIfSubDraftsSubmitted(podId);

    deleteSubDraftAndLineExtensionBySubDraftIds(subDrafts.stream().map(PodSubDraft::getId).collect(Collectors.toSet()));

    ProofOfDeliveryDto podDto = podController.updateProofOfDelivery(podId, requestPodDto, authentication);
    if (podDto.getStatus() == ProofOfDeliveryStatus.CONFIRMED) {
      notificationService.postConfirmPod(requestPodDto);
    }
    return requestPodDto;
  }

  public PodPrintInfoResponse getPintInfo(UUID orderId, UUID podId) {
    PodPrintInfoResponse response = new PodPrintInfoResponse();

    OrderDto orderDto = ordersRepository.findOrderDtoById(orderId);
    UUID realRequisitionId =
        Objects.isNull(orderDto.getRequisitionId()) ? orderDto.getExternalId() : orderDto.getRequisitionId();

    response.setFileName(getFileName(orderDto, realRequisitionId));
    response.setClient(orderDto.getReceivingFacilityName());
    response.setSupplier(orderDto.getSupplyingFacilityName());
    response.setReceivedBy(orderDto.getPodReceivedBy());
    response.setDeliveredBy(orderDto.getPodDeliveredBy());
    response.setReceivedDate(orderDto.getPodReceivedDate());
    response.setIssueVoucherDate(orderDto.getOrderFulfillDate());
    response.setRequisitionId(realRequisitionId);

    FacilityDto facilityDto = siglusFacilityReferenceDataService.findOneFacility(orderDto.getSupplyingFacilityId());
    GeographicZoneDto zoneDto = facilityDto.getGeographicZone();
    if (Objects.nonNull(zoneDto)) {
      if (zoneDto.getLevel().getLevelNumber().equals(DISTRICT_LEVEL_NUMBER)) {
        response.setSupplierDistrict(zoneDto.getName());
        response.setSupplierProvince(Objects.nonNull(zoneDto.getParent()) ? zoneDto.getParent().getName() : null);
      } else if (zoneDto.getLevel().getLevelNumber().equals(PROVINCE_LEVEL_NUMBER)) {
        response.setSupplierProvince(zoneDto.getName());
      }
    }

    StatusChange requisitionStatusChange = requisitionStatusChangeRepository.findByRequisitionId(realRequisitionId)
        .stream()
        .filter(e -> RequisitionStatus.RELEASED == e.getStatus()).findFirst().orElse(null);
    response.setRequisitionDate(
        Objects.nonNull(requisitionStatusChange) ? requisitionStatusChange.getCreatedDate() : null);

    List<PodLineItemDto> podLineItemDtos = podLineItemsRepository.lineItemDtos(podId, orderId, realRequisitionId);
    response.setLineItems(toLineItemInfo(podLineItemDtos));

    return response;
  }

  private String getFileName(OrderDto orderDto, UUID realRequisitionId) {
    long orderCount = getOrderCount(orderDto, realRequisitionId);
    long requisitionCount = getRequisitionCount(orderDto);

    StringBuilder fileName = new StringBuilder()
        .append(orderDto.getEmergency() ? FILE_NAME_PREFIX_EMERGENCY : FILE_NAME_PREFIX_NORMAL)
        .append(orderDto.getReceivingFacilityCode()).append(".")
        .append(DATE_FORMAT.format(orderDto.getPeriodEndDate())).append(".")
        .append(formatCount(requisitionCount)).append("/")
        .append(formatCount(orderCount));

    return fileName.toString();
  }

  private long getOrderCount(OrderDto orderDto, UUID realRequisitionId) {
    if (Objects.isNull(orderDto.getRequisitionId())) {
      return 1L;
    }

    OrderExternal orderExternal = new OrderExternal();
    orderExternal.setRequisitionId(realRequisitionId);
    Example<OrderExternal> orderExternalExample = Example.of(orderExternal);
    return orderExternalRepository.count(orderExternalExample);
  }

  private long getRequisitionCount(OrderDto orderDto) {
    return requisitionsRepository.countByOrderInfo(orderDto.getReceivingFacilityId(), orderDto.getProgramId(),
        orderDto.getProcessingPeriodId(), orderDto.getEmergency(), REQUISITION_STATUS_POST_SUBMIT);
  }

  private String formatCount(long count) {
    String formatString = String.valueOf(count);
    if (formatString.length() >= 2) {
      return formatString;
    }
    return "0" + formatString;
  }

  private List<LineItemInfo> toLineItemInfo(List<PodLineItemDto> lineItemDtos) {
    List<LineItemInfo> lineItemInfos = Lists.newArrayListWithExpectedSize(lineItemDtos.size());
    lineItemDtos.forEach(podLineItemDto -> {
      LineItemInfo lineItemInfo = new LineItemInfo();
      BeanUtils.copyProperties(podLineItemDto, lineItemInfo);
      lineItemInfos.add(lineItemInfo);
    });
    return lineItemInfos;
  }

  private ProofOfDeliveryDto getPodDtoByPodId(UUID podId) {
    ProofOfDeliveryDto podDto = fulfillmentService.searchProofOfDelivery(podId, null);
    if (Objects.isNull(podDto) || CollectionUtils.isEmpty(podDto.getLineItems())) {
      throw new NotFoundException(ERROR_NO_POD_OR_POD_LINE_ITEM_FOUND);
    }
    return podDto;
  }

  private void checkIfSubDraftsExist(UUID podId) {
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    long subDraftsCount = podSubDraftRepository.count(example);
    if (subDraftsCount > 0) {
      throw new BusinessDataException(new Message(ERROR_SUB_DRAFTS_ALREADY_EXISTED), podId);
    }
  }

  private void checkAuth() {
    if (!authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()) {
      throw new AuthenticationException(new Message(ERROR_PERMISSION_NOT_SUPPORTED));
    }
  }

  private List<PodSubDraft> checkIfSubDraftsSubmitted(UUID podId) {
    List<PodSubDraft> subDrafts = getPodSubDraftsByPodId(podId);
    if (subDrafts.stream().anyMatch(podSubDraft -> PodSubDraftStatusEnum.SUBMITTED != podSubDraft.getStatus())) {
      throw new BusinessDataException(new Message(ERROR_NOT_ALL_SUB_DRAFTS_SUBMITTED),
          subDrafts.stream().map(PodSubDraft::getId).collect(Collectors.toSet()));
    }
    return subDrafts;
  }

  private void deleteSubDraftAndLineExtensionBySubDraftIds(Set<UUID> subDraftIds) {
    log.info("delete proof of delivery line item extension, subDraftIds:{}", subDraftIds);
    podLineItemsExtensionRepository.deleteAllBySubDraftIds(subDraftIds);

    log.info("delete proof of delivery sub draft, subDraftIds:{}", subDraftIds);
    podSubDraftRepository.deleteAllByIds(subDraftIds);
  }

  private void resetLineItems(Set<UUID> lineItemIds, ProofOfDeliveryDto podDto) {
    List<ProofOfDeliveryLineItem> lineItems = podLineItemsRepository.findAll(lineItemIds);
    List<ProofOfDeliveryLineItem> toBeUpdatedLineItems = buildToBeUpdatedLineItems(podDto, lineItems,
        PodSubDraftStatusEnum.NOT_YET_STARTED);
    log.info("update ProofOfDeliveryLineItem list, podIdd:{}, lineItemIds:{}", podDto.getId(),
        lineItemIds);
    podLineItemsRepository.save(toBeUpdatedLineItems);
  }

  private List<PodSubDraft> getPodSubDraftsByPodId(UUID podId) {
    Example<PodSubDraft> example = Example.of(PodSubDraft.builder().podId(podId).build());
    List<PodSubDraft> podSubDrafts = podSubDraftRepository.findAll(example);
    if (CollectionUtils.isEmpty(podSubDrafts)) {
      throw new NotFoundException(ERROR_NO_POD_SUB_DRAFT_FOUND);
    }
    return podSubDrafts;
  }

  private void checkIfCanOperate(PodSubDraft podSubDraft) {
    if (PodSubDraftStatusEnum.SUBMITTED == podSubDraft.getStatus()) {
      throw new BusinessDataException(new Message(ERROR_CANNOT_OPERATE_WHEN_SUB_DRAFT_SUBMITTED), podSubDraft.getId());
    }
  }

  private List<ProofOfDeliveryLineItem> buildToBeUpdatedLineItems(ProofOfDeliveryDto proofOfDeliveryDto,
      List<ProofOfDeliveryLineItem> lineItems, PodSubDraftStatusEnum subDraftStatus) {
    Map<UUID, ProofOfDeliveryLineItemDto> idToLineItemDto = convertToLineItemDtos(
        proofOfDeliveryDto.getLineItems()).stream()
        .collect(Collectors.toMap(ProofOfDeliveryLineItemDto::getId, e -> e));
    List<ProofOfDeliveryLineItem> toBeUpdatedLineItems = Lists.newArrayListWithExpectedSize(lineItems.size());
    lineItems.forEach(lineItem -> {
      ProofOfDeliveryLineItemDto lineItemDto = idToLineItemDto.get(lineItem.getId());
      ProofOfDeliveryLineItem toBeUpdatedLineItem;
      if (PodSubDraftStatusEnum.NOT_YET_STARTED == subDraftStatus) {
        toBeUpdatedLineItem = buildResetLineItem(lineItem);
      } else {
        toBeUpdatedLineItem = buildToBeUpdatedLineItem(lineItem, lineItemDto);
      }
      toBeUpdatedLineItems.add(toBeUpdatedLineItem);
    });
    return toBeUpdatedLineItems;
  }

  private ProofOfDeliveryLineItem buildToBeUpdatedLineItem(ProofOfDeliveryLineItem lineItem,
      ProofOfDeliveryLineItemDto lineItemDto) {
    ProofOfDeliveryLineItem toBeUpdatedLineItem = new ProofOfDeliveryLineItem(lineItem.getOrderable(),
        lineItem.getLotId(), lineItemDto.getQuantityAccepted(), null,
        lineItemDto.getQuantityRejected(), lineItemDto.getRejectionReasonId(), lineItemDto.getNotes());
    toBeUpdatedLineItem.setId(lineItem.getId());
    return toBeUpdatedLineItem;
  }

  private ProofOfDeliveryLineItem buildResetLineItem(ProofOfDeliveryLineItem lineItem) {
    ProofOfDeliveryLineItem toBeUpdatedLineItem = new ProofOfDeliveryLineItem(lineItem.getOrderable(),
        lineItem.getLotId(), null, null, null, null, null);
    toBeUpdatedLineItem.setId(lineItem.getId());
    return toBeUpdatedLineItem;
  }

  private void updateSubDraftStatusAndOperator(PodSubDraft podSubDraft, PodSubDraftStatusEnum status) {
    UUID currentUserId = authenticationHelper.getCurrentUserId().orElseThrow(IllegalStateException::new);
    podSubDraft.setOperatorId(PodSubDraftStatusEnum.NOT_YET_STARTED == status ? null : currentUserId);
    podSubDraft.setStatus(status);
    log.info("save pod sub draft: {}", podSubDraft);
    podSubDraftRepository.save(podSubDraft);
  }

  private PodSubDraft getPodSubDraft(UUID subDraftId) {
    PodSubDraft podSubDraft = podSubDraftRepository.findOne(subDraftId);
    if (Objects.isNull(podSubDraft)) {
      throw new NotFoundException(ERROR_NO_POD_SUB_DRAFT_FOUND);
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

  private void checkIfPodIdAndSubDraftIdMatch(UUID podId, UUID subDraftId) {
    PodSubDraft podSubDraft = getPodSubDraft(subDraftId);
    if (!podSubDraft.getPodId().equals(podId)) {
      throw new BusinessDataException(new Message(ERROR_POD_ID_SUB_DRAFT_ID_NOT_MATCH), subDraftId);
    }
  }

  private boolean isAllSubDraftsSubmitted(List<PodSubDraft> subDraftList) {
    return subDraftList.stream().allMatch(e -> PodSubDraftStatusEnum.SUBMITTED == e.getStatus());
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
    UUID podId = splitGroupList.get(0).get(0).get(0).getPodId();
    for (int i = 0; i < splitGroupList.size(); i++) {
      subDrafts.add(PodSubDraft.builder()
          .number(i + 1)
          .podId(podId)
          .status(PodSubDraftStatusEnum.NOT_YET_STARTED)
          .build());
    }
    log.info("save pod sub drafts, proof of delivery id: {}", podId);
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

  private List<SimpleLineItem> buildSimpleLineItems(ProofOfDeliveryDto proofOfDeliveryDto) {
    List<SimpleLineItem> simpleLineItems = convertToLineItemDtos(proofOfDeliveryDto.getLineItems()).stream()
        .map(lineItem -> SimpleLineItem.builder()
            .lineItemId(lineItem.getId())
            .podId(proofOfDeliveryDto.getId())
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

  @Data
  @Builder
  private static class SimpleLineItem {

    private UUID lineItemId;
    private UUID podId;
    private UUID productId;
    private String productCode;
  }
}
