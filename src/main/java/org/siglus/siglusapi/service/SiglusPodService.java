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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.javers.common.collections.Sets;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem.Importer;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.repository.ProofOfDeliveryRepository;
import org.openlmis.fulfillment.web.ProofOfDeliveryController;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.openlmis.fulfillment.web.util.ShipmentObjectReferenceDto;
import org.openlmis.fulfillment.web.util.StockEventBuilder;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.repository.StatusChangeRepository;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.PodLineItemsByLocation;
import org.siglus.siglusapi.domain.PodLineItemsExtension;
import org.siglus.siglusapi.domain.PodSubDraft;
import org.siglus.siglusapi.domain.PodSubDraftLineItemsByLocation;
import org.siglus.siglusapi.domain.ProofsOfDeliveryExtension;
import org.siglus.siglusapi.domain.StockCardLineItemExtension;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.PodLineItemWithLocationDto;
import org.siglus.siglusapi.dto.PodWithLocationDto;
import org.siglus.siglusapi.dto.enums.PodSubDraftStatusEnum;
import org.siglus.siglusapi.exception.AuthenticationException;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.OrdersRepository;
import org.siglus.siglusapi.repository.PodLineItemsByLocationRepository;
import org.siglus.siglusapi.repository.PodLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsRepository;
import org.siglus.siglusapi.repository.PodSubDraftLineItemsByLocationRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.repository.ProofsOfDeliveryExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardLineItemExtensionRepository;
import org.siglus.siglusapi.repository.dto.OrderDto;
import org.siglus.siglusapi.repository.dto.PodLineItemDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusPodFulfillmentService;
import org.siglus.siglusapi.util.CustomListSortHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.CreatePodSubDraftRequest;
import org.siglus.siglusapi.web.request.OperateTypeEnum;
import org.siglus.siglusapi.web.request.PodExtensionRequest;
import org.siglus.siglusapi.web.request.PodWithLocationRequest;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftWithLocationRequest;
import org.siglus.siglusapi.web.response.PodExtensionResponse;
import org.siglus.siglusapi.web.response.PodPrintInfoResponse;
import org.siglus.siglusapi.web.response.PodPrintInfoResponse.LineItemInfo;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse;
import org.siglus.siglusapi.web.response.PodSubDraftsSummaryResponse.SubDraftInfo;
import org.siglus.siglusapi.web.response.ProofOfDeliveryWithLocationResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiglusPodService {

  private final SiglusPodFulfillmentService fulfillmentService;

  private final SiglusOrderService siglusOrderService;

  private final SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  private final OrderExternalRepository orderExternalRepository;

  private final OrderableRepository orderableRepository;

  private final PodSubDraftRepository podSubDraftRepository;

  private final PodLineItemsExtensionRepository podLineItemsExtensionRepository;

  private final SiglusAuthenticationHelper authenticationHelper;

  private final PodLineItemsRepository podLineItemsRepository;

  private final ProofOfDeliveryController podController;

  private final SiglusNotificationService notificationService;

  private final OrdersRepository ordersRepository;

  private final SiglusRequisitionRepository siglusRequisitionRepository;

  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  private final StatusChangeRepository requisitionStatusChangeRepository;

  private final SiglusRequisitionExtensionService requisitionExtensionService;

  private final ProofsOfDeliveryExtensionRepository podExtensionRepository;

  private final PodLineItemsByLocationRepository podLineItemsByLocationRepository;

  private final PodSubDraftLineItemsByLocationRepository podSubDraftLineItemsByLocationRepository;

  private final CalculatedStocksOnHandByLocationService calculatedStocksOnHandByLocationService;

  private final ProofOfDeliveryRepository proofOfDeliveryRepository;

  private final StockEventBuilder stockEventBuilder;

  private final SiglusStockCardRepository siglusStockCardRepository;

  private final StockCardLineItemRepository stockCardLineItemRepository;

  private final StockCardLineItemExtensionRepository stockCardLineItemExtensionRepository;

  private static final String FILE_NAME_PREFIX_EMERGENCY = "OF.REM.";
  private static final String FILE_NAME_PREFIX_NORMAL = "OF.RNO.";
  private static final List<String> REQUISITION_STATUS_POST_SUBMIT = Lists.newArrayList(
      RequisitionStatus.APPROVED.name(),
      RequisitionStatus.RELEASED.name(),
      RequisitionStatus.RELEASED_WITHOUT_ORDER.name());
  private static final Integer PROVINCE_LEVEL_NUMBER = 2;
  private static final Integer DISTRICT_LEVEL_NUMBER = 3;

  private final DateFormat dateFormat = new SimpleDateFormat("yyMM");

  private static final Set<String> EXPAND_PARAM = Sets.asSet("shipment.order");

  public PodExtensionResponse getPodExtensionResponse(UUID id, Set<String> expand) {
    PodExtensionResponse response = new PodExtensionResponse();
    ProofOfDeliveryDto podDto = getExpandedPodDtoById(id, expand);
    response.setPodDto(podDto);

    ProofsOfDeliveryExtension podExtension = getPodExtensionByPodId(id);
    if (Objects.nonNull(podExtension)) {
      response.setPreparedBy(podExtension.getPreparedBy());
      response.setConferredBy(podExtension.getConferredBy());
    }
    mergeSamePodLineItems(podDto);
    mergeSameShipmentLineItems(podDto.getShipment());
    return response;
  }

  public ProofOfDeliveryDto getExpandedPodDtoById(UUID id, Set<String> expand) {
    ProofOfDeliveryDto podDto = fulfillmentService.searchProofOfDelivery(id, expand);
    if (Objects.isNull(podDto)) {
      throw new NotFoundException(ERROR_NO_POD_OR_POD_LINE_ITEM_FOUND);
    }
    OrderObjectReferenceDto order = podDto.getShipment().getOrder();
    OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
    UUID requisitionId = external == null ? order.getExternalId() : external.getRequisitionId();
    order.setRequisitionNumber(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId));
    if (CollectionUtils.isNotEmpty(order.getOrderLineItems())) {
      podDto.getShipment().setOrder(siglusOrderService.getExtensionOrder(order));
    }
    return podDto;
  }

  public Page<ProofOfDeliveryDto> getAllProofsOfDelivery(UUID orderId, UUID shipmentId, Pageable pageable) {
    Page<ProofOfDeliveryDto> podDto = podController.getAllProofsOfDelivery(orderId, shipmentId, pageable);
    mergeSamePodLineItems(podDto.getContent().get(0));
    return podDto;
  }

  @Transactional
  public void createSubDrafts(UUID podId, CreatePodSubDraftRequest request) {
    checkIfSubDraftsExist(podId);

    ProofOfDeliveryDto podDto = getPodDtoByPodId(podId);
    List<SimpleLineItem> simpleLineItems = buildSimpleLineItems(podDto);
    List<List<SimpleLineItem>> groupByProductIdLineItems = getGroupByProductIdLineItemList(simpleLineItems);
    if (groupByProductIdLineItems.size() < request.getSplitNum()) {
      throw new BusinessDataException(new Message(ERROR_SPLIT_NUM_TOO_LARGE));
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
    return getPodSubDraftDto(podId, subDraftId, expand);
  }

  public PodWithLocationDto getPodSubDraftWithLocation(UUID podId, UUID subDraftId) {
    ProofOfDeliveryDto podSubDraftDto = getPodSubDraftDto(podId, subDraftId, EXPAND_PARAM);
    PodWithLocationDto podWithLocationDto = new PodWithLocationDto();
    podWithLocationDto.setPodDto(podSubDraftDto);

    List<Importer> lineItems = podSubDraftDto.getLineItems();
    List<UUID> subDraftLineItemsIds = lineItems.stream().map(Importer::getId).collect(Collectors.toList());
    List<PodLineItemWithLocationDto> podLineItemWithLocationDtos =
        getSubDraftLineItemsWithLocation(subDraftLineItemsIds);
    podWithLocationDto.setPodLineItemLocation(podLineItemWithLocationDtos);
    mergeSamePodLineItems(podWithLocationDto.getPodDto());
    mergeSameShipmentLineItems(podWithLocationDto.getPodDto().getShipment());
    return podWithLocationDto;
  }

  @Transactional
  public void updateSubDraft(UpdatePodSubDraftRequest request, UUID subDraftId) {
    updatePodSubDraft(subDraftId, request.getPodDto(), request.getOperateType());
  }

  @Transactional
  public void updateSubDraftWithLocation(UpdatePodSubDraftWithLocationRequest request, UUID subDraftId) {
    validateQuantity(request.getPodDto(), request.getOperateType());
    updatePodSubDraft(subDraftId, request.getPodDto(), request.getOperateType());
    updatePodSubDraftLocation(request.getPodLineItemLocation());
  }

  @Transactional
  public void deleteSubDraft(UUID podId, UUID subDraftId) {
    deletePodSubDraft(podId, subDraftId);
  }

  @Transactional
  public void deleteSubDraftWithLocation(UUID podId, UUID subDraftId) {
    deletePodSubDraft(podId, subDraftId);
    Set<UUID> subDraftLineItemIds = getPodLineItemIdsBySubDraftId(subDraftId);

    log.info("delete pod sub draft by location, podLineItemId: {}", subDraftLineItemIds);
    podSubDraftLineItemsByLocationRepository.deleteByPodLineItemIdIn(subDraftLineItemIds);
  }

  @Transactional
  public void deleteSubDrafts(UUID podId) {
    deletePodSubDrafts(podId);
  }

  @Transactional
  public void deleteSubDraftsWithLocation(UUID podId) {
    Set<UUID> podLineItemIds = findLineItemsIdsByPodId(podId, Collections.emptySet());
    log.info("delete all sub drafts with location, pod line items id: {}", podLineItemIds);
    podSubDraftLineItemsByLocationRepository.deleteByPodLineItemIdIn(podLineItemIds);
    deletePodSubDrafts(podId);
  }

  public PodExtensionResponse mergeSubDrafts(UUID podId, Set<String> expand) {
    checkAuth();
    checkIfSubDraftsSubmitted(podId);
    ProofOfDeliveryDto expandedPodDto = getExpandedPodDtoById(podId, expand);
    ProofsOfDeliveryExtension podExtensionBy = getPodExtensionByPodId(podId);
    PodExtensionResponse podExtensionResponse = new PodExtensionResponse();
    podExtensionResponse.setPodDto(expandedPodDto);
    if (ObjectUtils.isEmpty(podExtensionBy)) {
      return podExtensionResponse;
    }
    podExtensionResponse.setPreparedBy(podExtensionBy.getPreparedBy());
    podExtensionResponse.setConferredBy(podExtensionBy.getConferredBy());
    return podExtensionResponse;
  }

  public ProofOfDeliveryWithLocationResponse getMergedSubDraftWithLocation(UUID podId) {
    checkAuth();
    checkIfSubDraftsSubmitted(podId);
    ProofOfDeliveryDto podDto = getExpandedPodDtoById(podId, EXPAND_PARAM);
    return getPodWithLocation(podDto);
  }

  @Transactional
  public ProofOfDeliveryDto submitSubDrafts(UUID podId, PodExtensionRequest request,
      OAuth2Authentication authentication) {
    checkAuth();
    List<PodSubDraft> subDrafts = checkIfSubDraftsSubmitted(podId);

    savePodExtension(request);
    deleteSubDraftAndLineExtensionBySubDraftIds(subDrafts.stream().map(PodSubDraft::getId).collect(Collectors.toSet()));

    ProofOfDeliveryDto podDto = podController.updateProofOfDelivery(podId, request.getPodDto(), authentication);
    if (podDto.getStatus() == ProofOfDeliveryStatus.CONFIRMED) {
      notificationService.postConfirmPod(request.getPodDto());
    }
    return podDto;
  }

  @Transactional
  public void submitSubDraftsWithLocation(UUID podId, PodWithLocationRequest request,
      OAuth2Authentication authentication) {
    checkAuth();
    List<PodSubDraft> subDrafts = checkIfSubDraftsSubmitted(podId);
    deleteSubDraftAndLineExtensionBySubDraftIds(subDrafts.stream().map(PodSubDraft::getId).collect(Collectors.toSet()));
    submitPodSubDraftsWithLocation(request.getPodLineItemLocation());
    ProofOfDeliveryDto podDto = podController.updateProofOfDelivery(podId, request.getPodDto(), authentication);
    if (podDto.getStatus() == ProofOfDeliveryStatus.CONFIRMED) {
      notificationService.postConfirmPod(request.getPodDto());
    }
    StockEventDto stockEventDto = createStockEventDto(podId);
    saveToStockCardLineItemByLocation(request, stockEventDto);
    calculatedStocksOnHandByLocationService.calculateStockOnHandByLocation(stockEventDto);
  }

  private ProofsOfDeliveryExtension getPodExtensionByPodId(UUID podId) {
    Example<ProofsOfDeliveryExtension> example = Example.of(ProofsOfDeliveryExtension.builder().podId(podId).build());
    return podExtensionRepository.findOne(example);
  }

  private void savePodExtension(PodExtensionRequest request) {
    ProofsOfDeliveryExtension podExtension = getPodExtensionByPodId(request.getPodDto().getId());
    if (Objects.isNull(podExtension)) {
      podExtension = ProofsOfDeliveryExtension.builder().podId(request.getPodDto().getId()).build();
    }
    podExtension.setPreparedBy(request.getPreparedBy());
    podExtension.setConferredBy(request.getConferredBy());
    log.info("save pod extension: {}", podExtension);
    podExtensionRepository.save(podExtension);
  }

  public PodPrintInfoResponse getPrintInfo(UUID orderId, UUID podId) {
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
    response.setRequisitionNum(requisitionExtensionService.formatRequisitionNumber(realRequisitionId));

    FacilityDto facilityDto = siglusFacilityReferenceDataService.findOneWithoutCache(orderDto.getSupplyingFacilityId());
    GeographicZoneDto zoneDto = facilityDto.getGeographicZone();
    if (Objects.nonNull(zoneDto)) {
      if (zoneDto.getLevel().getLevelNumber().equals(DISTRICT_LEVEL_NUMBER)) {
        response.setSupplierDistrict(zoneDto.getName());
        response.setSupplierProvince(Objects.nonNull(zoneDto.getParent()) ? zoneDto.getParent().getName() : null);
      } else if (zoneDto.getLevel().getLevelNumber().equals(PROVINCE_LEVEL_NUMBER)) {
        response.setSupplierProvince(zoneDto.getName());
      }
    }

    ProofsOfDeliveryExtension podExtension = getPodExtensionByPodId(podId);
    if (Objects.nonNull(podExtension)) {
      response.setPreparedBy(podExtension.getPreparedBy());
      response.setConferredBy(podExtension.getConferredBy());
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
    int requisitionCount = getRequisitionCount(orderDto, realRequisitionId);

    StringBuilder fileName = new StringBuilder()
        .append(Boolean.TRUE.equals(orderDto.getEmergency()) ? FILE_NAME_PREFIX_EMERGENCY : FILE_NAME_PREFIX_NORMAL)
        .append(orderDto.getReceivingFacilityCode()).append(".")
        .append(dateFormat.format(orderDto.getPeriodEndDate())).append(".")
        .append(formatCount(requisitionCount));

    return fileName.toString();
  }

  private int getRequisitionCount(OrderDto orderDto, UUID realRequisitionId) {
    List<String> requisitionIds = siglusRequisitionRepository.findRequisitionIdsByOrderInfo(
        orderDto.getReceivingFacilityId(),
        orderDto.getProgramId(),
        orderDto.getProcessingPeriodId(), orderDto.getEmergency(), REQUISITION_STATUS_POST_SUBMIT);
    List<StatusChange> statusChanges = requisitionStatusChangeRepository.findByRequisitionIdIn(
            requisitionIds.stream().map(UUID::fromString).collect(Collectors.toList())).stream()
        .filter(statusChange -> RequisitionStatus.SUBMITTED == statusChange.getStatus())
        .sorted(Comparator.comparing(StatusChange::getCreatedDate)).collect(Collectors.toList());

    for (int i = 0; i < statusChanges.size(); i++) {
      StatusChange statusChange = statusChanges.get(i);
      if (statusChange.getRequisition().getId().equals(realRequisitionId)) {
        return i + 1;
      }
    }
    return requisitionIds.size();
  }

  private String formatCount(int count) {
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
          subDrafts.stream().map(PodSubDraft::getId).collect(Collectors.toCollection(HashSet::new)));
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
    productIdToLineItems.forEach((productId, lineItems) -> groupByProductIdLineItems.add(lineItems));
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

  private void updatePodSubDraftLocation(List<PodLineItemWithLocationDto> podLineItemLocationList) {
    deleteAndUpdateSubDraftLineItemsWithLocation(podLineItemLocationList);
  }

  private void updatePodSubDraft(UUID subDraftId, ProofOfDeliveryDto podDto, OperateTypeEnum operateType) {
    PodSubDraft podSubDraft = getPodSubDraft(subDraftId);
    checkIfCanOperate(podSubDraft);
    Set<UUID> lineItemIds = getPodLineItemIdsBySubDraftId(subDraftId);
    List<ProofOfDeliveryLineItem> lineItems = podLineItemsRepository.findAll(lineItemIds);

    List<ProofOfDeliveryLineItem> toBeUpdatedLineItems = buildToBeUpdatedLineItems(
        podDto, lineItems, OperateTypeEnum.getPodSubDraftEnum(operateType));
    log.info("update ProofOfDeliveryLineItem list, subDraftId:{}, lineItemIds:{}", subDraftId, lineItemIds);
    podLineItemsRepository.save(toBeUpdatedLineItems);

    updateSubDraftStatusAndOperator(podSubDraft, OperateTypeEnum.getPodSubDraftEnum(operateType));
  }

  private void deletePodSubDraft(UUID podId, UUID subDraftId) {
    PodSubDraft podSubDraft = getPodSubDraft(subDraftId);
    checkIfCanOperate(podSubDraft);

    Set<UUID> lineItemIds = getPodLineItemIdsBySubDraftId(subDraftId);
    ProofOfDeliveryDto proofOfDeliveryDto = getPodDtoByPodId(podId);
    resetLineItems(lineItemIds, proofOfDeliveryDto);

    updateSubDraftStatusAndOperator(podSubDraft, PodSubDraftStatusEnum.NOT_YET_STARTED);
  }

  private void validateQuantity(ProofOfDeliveryDto podDto, OperateTypeEnum operateType) {
    if (OperateTypeEnum.SAVE.equals(operateType)) {
      return;
    }
    ShipmentObjectReferenceDto shipment = podDto.getShipment();
    List<ShipmentLineItemDto> shipmentLineItems = shipment.getLineItems();
    Map<String, Integer> uniqueKeyToShippedQuantity = new HashMap<>();
    shipmentLineItems.forEach(shipmentLineItem -> {
      String key = buildForQuantityShippedKey(shipmentLineItem.getOrderable().getId(), shipmentLineItem.getLotId());
      uniqueKeyToShippedQuantity.put(key, shipmentLineItem.getQuantityShipped().intValue());
    });

    Map<String, Integer> uniqueKeyToAcceptedQuantity = new HashMap<>();
    List<Importer> lineItems = podDto.getLineItems();
    lineItems.forEach(lineItem -> {
      String key = buildForQuantityShippedKey(lineItem.getOrderableIdentity().getId(), lineItem.getLotId());
      if (uniqueKeyToAcceptedQuantity.containsKey(key)) {
        uniqueKeyToAcceptedQuantity.put(key, uniqueKeyToAcceptedQuantity.get(key) + lineItem.getQuantityAccepted());
      } else {
        uniqueKeyToAcceptedQuantity.put(key, lineItem.getQuantityAccepted());
      }
    });

    uniqueKeyToAcceptedQuantity.forEach((key, acceptedQuantity) -> {
      if (acceptedQuantity > uniqueKeyToShippedQuantity.get(key)) {
        throw new BusinessDataException(new Message(ERROR_PERMISSION_NOT_SUPPORTED), podDto.getId());
      }
    });
  }

  private String buildForQuantityShippedKey(UUID orderableId, UUID lotId) {
    return orderableId + "&" + lotId;
  }

  private ProofOfDeliveryDto getPodSubDraftDto(UUID podId, UUID subDraftId, Set<String> expand) {
    checkIfPodIdAndSubDraftIdMatch(podId, subDraftId);
    ProofOfDeliveryDto dto = getExpandedPodDtoById(podId, expand);
    List<ProofOfDeliveryLineItemDto> currentSubDraftLineItems = getCurrentSubDraftPodLineItemDtos(subDraftId, dto);
    dto.setLineItems(currentSubDraftLineItems);
    return dto;
  }

  private void deletePodSubDrafts(UUID podId) {
    checkAuth();
    Set<UUID> subDraftIds = getPodSubDraftsByPodId(podId).stream().map(PodSubDraft::getId).collect(Collectors.toSet());
    ProofOfDeliveryDto proofOfDeliveryDto = getPodDtoByPodId(podId);
    Set<UUID> lineItemIds = findLineItemsIdsByPodId(podId, subDraftIds);
    resetLineItems(lineItemIds, proofOfDeliveryDto);
    deleteSubDraftAndLineExtensionBySubDraftIds(subDraftIds);
  }

  private Set<UUID> findLineItemsIdsByPodId(UUID podId, Set<UUID> subDraftIds) {
    if (CollectionUtils.isEmpty(subDraftIds)) {
      subDraftIds = getPodSubDraftsByPodId(podId).stream().map(PodSubDraft::getId)
          .collect(Collectors.toSet());
    }
    return getPodLineItemIdsBySubDraftIds(subDraftIds);
  }

  private void deleteAndUpdateSubDraftLineItemsWithLocation(List<PodLineItemWithLocationDto> podLineItemLocation) {
    Set<UUID> subDraftsLineItemIds = podLineItemLocation.stream().map(PodLineItemWithLocationDto::getPodLineItemId)
        .filter(Objects::nonNull).collect(Collectors.toSet());
    log.info("delete location info when submit all drafts, pod line item id: {}", subDraftsLineItemIds);
    podSubDraftLineItemsByLocationRepository.deleteByPodLineItemIdIn(subDraftsLineItemIds);
    List<PodSubDraftLineItemsByLocation> draftLineItemsByLocations = Lists.newArrayList();
    podLineItemLocation.forEach(location -> {
      PodSubDraftLineItemsByLocation draftLineItemsByLocation = PodSubDraftLineItemsByLocation
          .builder()
          .podLineItemId(location.getPodLineItemId())
          .locationCode(location.getLocationCode())
          .area(location.getArea())
          .quantityAccepted(location.getQuantityAccepted())
          .build();
      draftLineItemsByLocations.add(draftLineItemsByLocation);
    });
    log.info("save location info when save all drafts, size: {}", draftLineItemsByLocations.size());
    podSubDraftLineItemsByLocationRepository.save(draftLineItemsByLocations);
  }

  private List<PodLineItemWithLocationDto> getSubDraftLineItemsWithLocation(List<UUID> lineItemIds) {
    List<PodLineItemWithLocationDto> podLineItemWithLocationDtos = Lists.newArrayList();
    List<PodSubDraftLineItemsByLocation> subDraftLineItemsByLocationList = podSubDraftLineItemsByLocationRepository
        .findByPodLineItemIdIn(lineItemIds);
    subDraftLineItemsByLocationList.forEach(lineItemsWithLocation -> {
      PodLineItemWithLocationDto podLineItemWithLocationDto = new PodLineItemWithLocationDto();
      podLineItemWithLocationDto.setPodLineItemId(lineItemsWithLocation.getPodLineItemId());
      podLineItemWithLocationDto.setLocationCode(lineItemsWithLocation.getLocationCode());
      podLineItemWithLocationDto.setArea(lineItemsWithLocation.getArea());
      podLineItemWithLocationDto.setQuantityAccepted(lineItemsWithLocation.getQuantityAccepted());
      podLineItemWithLocationDtos.add(podLineItemWithLocationDto);
    });
    return podLineItemWithLocationDtos;
  }

  private void submitPodSubDraftsWithLocation(List<PodLineItemWithLocationDto> podLineItemLocationList) {
    deleteSubDraftAndSaveLineItemsWithLocation(podLineItemLocationList);
  }

  private void deleteSubDraftAndSaveLineItemsWithLocation(List<PodLineItemWithLocationDto> podLineItemLocationList) {
    Set<UUID> lineItemIds = podLineItemLocationList.stream().map(PodLineItemWithLocationDto::getPodLineItemId)
        .collect(Collectors.toSet());
    log.info("delete podSubDraftLineItemsByLocation when submit sub draft; line item ids: {}", lineItemIds);
    podSubDraftLineItemsByLocationRepository.deleteByPodLineItemIdIn(lineItemIds);

    List<PodLineItemsByLocation> podLineItemsByLocationList = Lists.newArrayList();
    podLineItemLocationList.forEach(lineItemWithLocation -> {
      PodLineItemsByLocation lineItemsByLocation = PodLineItemsByLocation
          .builder()
          .podLineItemId(lineItemWithLocation.getPodLineItemId())
          .locationCode(lineItemWithLocation.getLocationCode())
          .area(lineItemWithLocation.getArea())
          .quantityAccepted(lineItemWithLocation.getQuantityAccepted())
          .build();
      podLineItemsByLocationList.add(lineItemsByLocation);
    });
    log.info("save podLineItemsByLocation when submit sub draft; size: {}", podLineItemsByLocationList.size());
    podLineItemsByLocationRepository.save(podLineItemsByLocationList);
  }

  public ProofOfDeliveryWithLocationResponse getPodWithLocation(ProofOfDeliveryDto podDto) {
    ProofOfDeliveryWithLocationResponse podWithLocationDto = new ProofOfDeliveryWithLocationResponse();
    PodExtensionResponse podExtensionResponse = new PodExtensionResponse();
    podExtensionResponse.setPodDto(podDto);
    ProofsOfDeliveryExtension podExtension = getPodExtensionByPodId(podDto.getId());
    if (Objects.nonNull(podExtension)) {
      podExtensionResponse.setPreparedBy(podExtension.getPreparedBy());
      podExtensionResponse.setConferredBy(podExtension.getConferredBy());
    }
    podWithLocationDto.setPodExtension(podExtensionResponse);
    List<UUID> podLineItemIds = podExtensionResponse.getPodDto().getLineItems().stream()
        .map(Importer::getId).collect(Collectors.toList());
    List<PodLineItemWithLocationDto> lineItemsWithLocation = getSubDraftLineItemsWithLocation(podLineItemIds);
    podWithLocationDto.setPodLineItemLocation(CollectionUtils.isEmpty(lineItemsWithLocation)
        ? Lists.newArrayList() : lineItemsWithLocation);
    return podWithLocationDto;
  }

  private StockEventDto createStockEventDto(UUID podId) {
    ProofOfDelivery pod = proofOfDeliveryRepository.findOne(podId);
    org.openlmis.fulfillment.web.stockmanagement.StockEventDto fulfillmentStockEventDto = stockEventBuilder
        .fromProofOfDelivery(pod);
    StockEventDto stockEventDto = new StockEventDto();
    List<StockEventLineItemDto> lineItems = Lists.newArrayList();
    fulfillmentStockEventDto.getLineItems().forEach(lineItemDto -> {
      StockEventLineItemDto stockEventLineItemDto = new StockEventLineItemDto();
      BeanUtils.copyProperties(lineItemDto, stockEventLineItemDto);
      lineItems.add(stockEventLineItemDto);
    });
    BeanUtils.copyProperties(fulfillmentStockEventDto, stockEventDto);
    stockEventDto.setLineItems(lineItems);
    setStockCardLineItemId(lineItems, pod.getReceivingFacilityId());
    return stockEventDto;
  }

  private void setStockCardLineItemId(List<StockEventLineItemDto> lineItems, UUID facilityId) {
    Set<String> orderableLotIdPairs = lineItems.stream()
        .map(lineItem ->
            getOrderableLotIdPair(lineItem.getOrderableId(), lineItem.getLotId()))
        .collect(Collectors.toSet());
    List<StockCard> stockCards = siglusStockCardRepository.findByFacilityIdAndOrderableLotIdPairs(
        facilityId, orderableLotIdPairs);

    Map<UUID, StockEventLineItemDto> stockCardIdToStockEventLineItems = new HashMap<>();
    stockCards.forEach(stockCard -> lineItems.forEach(lineItem -> {
      if (stockCard.getOrderableId().equals(lineItem.getOrderableId())) {
        if (stockCard.getLotId() == null && lineItem.getLotId() == null) {
          stockCardIdToStockEventLineItems.put(stockCard.getId(), lineItem);
        } else if (stockCard.getLotId().equals(lineItem.getLotId())) {
          stockCardIdToStockEventLineItems.put(stockCard.getId(), lineItem);
        }
      }
    }));
    List<StockCardLineItem> stockCardLineItems = stockCardLineItemRepository
        .findLatestByStockCardIds(stockCardIdToStockEventLineItems.keySet());

    stockCardLineItems.forEach(stockCardLineItem -> {
      StockEventLineItemDto stockEventLineItemDto =
          stockCardIdToStockEventLineItems.get(stockCardLineItem.getStockCard().getId());
      if (null != stockEventLineItemDto) {
        stockEventLineItemDto.setId(stockCardLineItem.getId());
      }
    });
  }

  private void saveToStockCardLineItemByLocation(PodWithLocationRequest request, StockEventDto stockEventDto) {
    List<StockEventLineItemDto> stockEventLineItems = stockEventDto.getLineItems();
    Map<UUID, UUID> stockCardLineItemIdToPodLineItemId = new HashMap<>();
    List<Importer> podLineItems = request.getPodDto().getLineItems();
    stockEventLineItems.forEach(stockEventLineItem -> podLineItems.forEach(podLineItem -> {
      if (stockEventLineItem.getOrderableId().equals(podLineItem.getOrderableIdentity().getId())) {
        if (stockEventLineItem.getLotId() == null && podLineItem.getLotId() == null) {
          stockCardLineItemIdToPodLineItemId.put(stockEventLineItem.getId(), podLineItem.getId());
        } else if (stockEventLineItem.getLotId().equals(podLineItem.getLotId())) {
          stockCardLineItemIdToPodLineItemId.put(stockEventLineItem.getId(), podLineItem.getId());
        }
      }
    }));

    Map<UUID, PodLineItemWithLocationDto> podLineItemIdToLocation = new HashMap<>();
    request.getPodLineItemLocation().forEach(podLineItemLocation ->
        podLineItemIdToLocation.put(podLineItemLocation.getPodLineItemId(), podLineItemLocation));

    Map<UUID, PodLineItemWithLocationDto> stockCardLineItemIdToPodLocation = new HashMap<>();
    stockCardLineItemIdToPodLineItemId.forEach((stockCardLineItemId, podLineItemId) ->
        stockCardLineItemIdToPodLocation.put(stockCardLineItemId, podLineItemIdToLocation.get(podLineItemId)));

    List<StockCardLineItemExtension> stockCardLineItemExtensions = Lists.newArrayList();
    stockCardLineItemIdToPodLocation.forEach((stockCardLineItemId, podLocation) -> {
      StockCardLineItemExtension stockCardLineItemExtension = StockCardLineItemExtension
          .builder()
          .stockCardLineItemId(stockCardLineItemId)
          .locationCode(podLocation.getLocationCode())
          .area(podLocation.getArea())
          .build();
      stockCardLineItemExtensions.add(stockCardLineItemExtension);
    });
    log.info("save into stock card line item by location when submit pod; size: {}",
        stockCardLineItemExtensions.size());
    stockCardLineItemExtensionRepository.save(stockCardLineItemExtensions);
  }

  private void mergeSamePodLineItems(ProofOfDeliveryDto podDto) {
    List<Importer> lineItems = podDto.getLineItems();
    Map<String, Importer> uniqueKeyToPodDto = new HashMap<>();
    lineItems.forEach(lineItem -> {
      String key = getOrderableLotIdPair(lineItem.getOrderableIdentity().getId(), lineItem.getLotId());
      uniqueKeyToPodDto.put(key, uniqueKeyToPodDto.getOrDefault(key, lineItem));
    });
    List<ProofOfDeliveryLineItemDto> newLineItems = Lists.newArrayList();
    uniqueKeyToPodDto.forEach((key, value) -> {
      ProofOfDeliveryLineItemDto proofOfDeliveryLineItemDto = new ProofOfDeliveryLineItemDto();
      BeanUtils.copyProperties(value, proofOfDeliveryLineItemDto);
      newLineItems.add(proofOfDeliveryLineItemDto);
    });
    podDto.setLineItems(newLineItems);
  }

  private void mergeSameShipmentLineItems(ShipmentObjectReferenceDto shipmentDto) {
    List<ShipmentLineItemDto> shipmentLineItems = shipmentDto.getLineItems();
    Map<String, ShipmentLineItemDto> uniqueKeyToShipmentDto = new HashMap<>();
    shipmentLineItems.forEach(shipmentLineItem -> {
      String key = getOrderableLotIdPair(shipmentLineItem.getOrderable().getId(), shipmentLineItem.getLotId());
      if (uniqueKeyToShipmentDto.containsKey(key)) {
        ShipmentLineItemDto shipmentLineItemDto = uniqueKeyToShipmentDto.get(key);
        Long quantityShipped = shipmentLineItemDto.getQuantityShipped() + shipmentLineItem.getQuantityShipped();
        shipmentLineItemDto.setQuantityShipped(quantityShipped);
        uniqueKeyToShipmentDto.put(key, shipmentLineItemDto);
      } else {
        uniqueKeyToShipmentDto.put(key, shipmentLineItem);
      }
    });
    shipmentDto.setLineItems(new ArrayList<>(uniqueKeyToShipmentDto.values()));
  }

  private String getOrderableLotIdPair(UUID orderableId, UUID lotId) {
    if (null == lotId) {
      return orderableId.toString();
    }
    return orderableId.toString() + lotId;
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
