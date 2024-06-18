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

import static org.siglus.siglusapi.constant.FieldConstants.DOT;
import static org.siglus.siglusapi.constant.FieldConstants.UNDERSCORE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_CANNOT_OPERATE_WHEN_SUB_DRAFT_SUBMITTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_ALL_SUB_DRAFTS_SUBMITTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_POD_OR_POD_LINE_ITEM_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_POD_SUB_DRAFT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_POD_ID_SUB_DRAFT_ID_NOT_MATCH;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SPLIT_NUM_TOO_LARGE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SUB_DRAFTS_ALREADY_EXISTED;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.openlmis.fulfillment.web.util.ObjectReferenceDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryLineItemDto;
import org.openlmis.fulfillment.web.util.StockEventBuilder;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.repository.StatusChangeRepository;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.PodLineItemsByLocation;
import org.siglus.siglusapi.domain.PodLineItemsExtension;
import org.siglus.siglusapi.domain.PodSubDraft;
import org.siglus.siglusapi.domain.PodSubDraftLineItem;
import org.siglus.siglusapi.domain.PodSubDraftLineItemLocation;
import org.siglus.siglusapi.domain.PodSubDraftLineItemsByLocation;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.PodLineItemWithLocationDto;
import org.siglus.siglusapi.dto.ProofOfDeliverySubDraftDto;
import org.siglus.siglusapi.dto.ProofOfDeliverySubDraftLineItemDto;
import org.siglus.siglusapi.dto.ProofOfDeliverySubDraftWithLocationDto;
import org.siglus.siglusapi.dto.enums.PodSubDraftStatusEnum;
import org.siglus.siglusapi.exception.AuthenticationException;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.localmachine.event.proofofdelivery.web.ProofOfDeliveryEmitter;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsByLocationRepository;
import org.siglus.siglusapi.repository.PodLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PodLineItemsRepository;
import org.siglus.siglusapi.repository.PodSubDraftLineItemRepository;
import org.siglus.siglusapi.repository.PodSubDraftLineItemsByLocationRepository;
import org.siglus.siglusapi.repository.PodSubDraftRepository;
import org.siglus.siglusapi.repository.SiglusOrdersRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.dto.OrderDto;
import org.siglus.siglusapi.repository.dto.PodLineItemDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusPodFulfillmentService;
import org.siglus.siglusapi.util.CustomListSortHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.request.CreatePodSubDraftRequest;
import org.siglus.siglusapi.web.request.OperateTypeEnum;
import org.siglus.siglusapi.web.request.SubmitPodSubDraftsRequest;
import org.siglus.siglusapi.web.request.UpdatePodSubDraftRequest;
import org.siglus.siglusapi.web.response.PodExtensionResponse;
import org.siglus.siglusapi.web.response.PodPrintInfoResponse;
import org.siglus.siglusapi.web.response.PodPrintInfoResponse.LineItemInfo;
import org.siglus.siglusapi.web.response.PodSubDraftsMergedResponse;
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
  private final SiglusOrdersRepository siglusOrdersRepository;
  private final SiglusRequisitionRepository siglusRequisitionRepository;
  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  private final StatusChangeRepository requisitionStatusChangeRepository;
  private final PodExtensionRepository podExtensionRepository;
  private final PodLineItemsByLocationRepository podLineItemsByLocationRepository;
  private final PodSubDraftLineItemsByLocationRepository podSubDraftLineItemsByLocationRepository;
  private final ProofOfDeliveryRepository proofOfDeliveryRepository;
  private final StockEventBuilder stockEventBuilder;
  private final ProofOfDeliveryEmitter proofOfDeliveryEmitter;
  private final SiglusStockEventsService stockEventsService;
  private final PodSubDraftLineItemRepository podSubDraftLineItemRepository;
  private final SiglusOrderableReferenceDataService orderableReferenceDataService;
  private final SiglusLotService siglusLotService;

  private static final String FILE_NAME_PREFIX = "OF.";
  private static final String FILE_NAME_PREFIX_EMERGENCY = FILE_NAME_PREFIX + "REM.";
  private static final List<String> REQUISITION_STATUS_POST_SUBMIT = Lists.newArrayList(
      RequisitionStatus.APPROVED.name(),
      RequisitionStatus.RELEASED.name(),
      RequisitionStatus.RELEASED_WITHOUT_ORDER.name());
  private static final Integer PROVINCE_LEVEL_NUMBER = 2;
  private static final Integer DISTRICT_LEVEL_NUMBER = 3;
  private static final Map<String, String> PROGRAM_CODE_TO_FILE_PREFIX_NORMAL = Maps.newHashMap();
  private static final Set<String> EXPAND_PARAM = Sets.asSet("shipment.order");
  private static final String SHORT_LINE = "-";

  private final DateFormat dateFormat = new SimpleDateFormat("yyMM");

  static {
    PROGRAM_CODE_TO_FILE_PREFIX_NORMAL.put("T", FILE_NAME_PREFIX + "MIA.");
    PROGRAM_CODE_TO_FILE_PREFIX_NORMAL.put("TR", FILE_NAME_PREFIX + "MIT.");
    PROGRAM_CODE_TO_FILE_PREFIX_NORMAL.put("TB", FILE_NAME_PREFIX + "MTB.");
    PROGRAM_CODE_TO_FILE_PREFIX_NORMAL.put("VC", FILE_NAME_PREFIX + "RNO.");
  }

  public PodExtensionResponse getPodExtensionResponse(UUID id, Set<String> expand) {
    PodExtensionResponse response = new PodExtensionResponse();
    ProofOfDeliveryDto podDto = getExpandedPodDtoById(id, expand);
    response.setPodDto(podDto);

    PodExtension podExtension = getPodExtensionByPodId(id);
    if (Objects.nonNull(podExtension)) {
      response.setPreparedBy(podExtension.getPreparedBy());
      response.setConferredBy(podExtension.getConferredBy());
    }
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
    return podController.getAllProofsOfDelivery(orderId, shipmentId, pageable);
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

  public ProofOfDeliverySubDraftDto getSubDraftDetail(UUID podId, UUID subDraftId, Set<String> expand) {
    return getPodSubDraftDto(podId, subDraftId, expand);
  }

  public ProofOfDeliverySubDraftWithLocationDto getPodSubDraftWithLocation(UUID podId, UUID subDraftId) {
    ProofOfDeliverySubDraftDto podSubDraftDto = getPodSubDraftDto(podId, subDraftId, EXPAND_PARAM);
    List<UUID> subDraftLineItemsIds = podSubDraftDto.getLineItems().stream()
        .map(ProofOfDeliverySubDraftLineItemDto::getId).collect(Collectors.toList());
    List<PodLineItemWithLocationDto> podLineItemWithLocationDtos =
        getSubDraftLineItemsWithLocation(subDraftLineItemsIds);
    podLineItemWithLocationDtos.addAll(podSubDraftDto.getLineItemLocations());
    // build result
    ProofOfDeliverySubDraftWithLocationDto podWithLocationDto = new ProofOfDeliverySubDraftWithLocationDto();
    podWithLocationDto.setPodDto(podSubDraftDto);
    podWithLocationDto.setPodLineItemLocation(podLineItemWithLocationDtos);
    return podWithLocationDto;
  }

  @Transactional
  public void updateSubDraft(UpdatePodSubDraftRequest request, UUID subDraftId) {
    updatePodSubDraft(subDraftId, request.getOperateType(), request.getPodDto(), null);
  }

  @Transactional
  public void updateSubDraftWithLocation(UpdatePodSubDraftRequest request, UUID subDraftId) {
    updatePodSubDraft(subDraftId, request.getOperateType(), request.getPodDto(), request.getPodLineItemLocation());
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
  public void resetSubDrafts(UUID podId) {
    resetPodSubDrafts(podId);
  }

  @Transactional
  public void resetSubDraftsWithLocation(UUID podId) {
    Set<UUID> podLineItemIds = findLineItemsIdsByPodId(podId, Collections.emptySet());
    log.info("delete all sub drafts with location, pod line items id: {}", podLineItemIds);
    podSubDraftLineItemsByLocationRepository.deleteByPodLineItemIdIn(podLineItemIds);
    resetPodSubDrafts(podId);
  }

  public PodSubDraftsMergedResponse mergeSubDrafts(UUID podId, Set<String> expand) {
    checkAuth();
    List<PodSubDraft> podSubDrafts = checkIfSubDraftsSubmitted(podId);
    List<UUID> subDraftIds = podSubDrafts.stream().map(PodSubDraft::getId).collect(Collectors.toList());
    ProofOfDeliverySubDraftDto draftDto = getPodSubDraftsDto(podId, subDraftIds, expand);
    PodSubDraftsMergedResponse response = new PodSubDraftsMergedResponse();
    response.setPodDto(draftDto);
    PodExtension podExtensionBy = getPodExtensionByPodId(podId);
    if (!ObjectUtils.isEmpty(podExtensionBy)) {
      response.setPreparedBy(podExtensionBy.getPreparedBy());
      response.setConferredBy(podExtensionBy.getConferredBy());
    }
    return response;
  }

  public PodSubDraftsMergedResponse getMergedSubDraftWithLocation(UUID podId) {
    PodSubDraftsMergedResponse response = mergeSubDrafts(podId, EXPAND_PARAM);
    List<UUID> subDraftLineItemsIds = response.getPodDto().getLineItems().stream()
        .map(ProofOfDeliverySubDraftLineItemDto::getId).collect(Collectors.toList());
    List<PodLineItemWithLocationDto> podLineItemWithLocationDtos =
        getSubDraftLineItemsWithLocation(subDraftLineItemsIds);
    podLineItemWithLocationDtos.addAll(response.getPodDto().getLineItemLocations());
    response.setPodLineItemLocation(podLineItemWithLocationDtos);
    return response;
  }

  public ProofOfDeliveryWithLocationResponse getPodExtensionResponseWithLocation(UUID podId) {
    ProofOfDeliveryDto podDto = getExpandedPodDtoById(podId, EXPAND_PARAM);
    return getPodWithLocation(podDto);
  }

  @Transactional
  public ProofOfDeliverySubDraftDto submitSubDrafts(UUID podId, SubmitPodSubDraftsRequest request,
      OAuth2Authentication authentication, boolean withLocation) {
    if (!podId.equals(request.getPodDto().getId())) {
      throw new IllegalArgumentException("podId and request mismatch");
    }
    checkAuth();
    List<PodSubDraft> subDrafts = checkIfSubDraftsSubmitted(podId);
    mergeLocationsToLot(request);
    deletePodSubDrafts(subDrafts);

    createAddedLots(request.getPodDto());
    ProofOfDelivery pod = proofOfDeliveryRepository.findOne(podId);
    createAddedPodLineItems(pod, request);

    savePodExtension(request);

    ProofOfDeliveryDto podDto = podController.updateProofOfDelivery(podId, request.getPodDto().to(),
        authentication, withLocation);
    if (withLocation) {
      savePodSubDraftLocations(request);
      StockEventDto stockEventDto = createStockEventDto(pod, request);
      stockEventsService.processStockEvent(stockEventDto, true);
    }
    if (podDto.getStatus() == ProofOfDeliveryStatus.CONFIRMED) {
      notificationService.postConfirmPod(podDto);
      proofOfDeliveryEmitter.emit(request.getPodDto().getId());
    }
    return request.getPodDto();
  }

  private void deletePodSubDrafts(List<PodSubDraft> subDrafts) {
    List<UUID> subDraftIds = subDrafts.stream().map(PodSubDraft::getId).collect(Collectors.toList());
    List<PodLineItemsExtension> extensions = podLineItemsExtensionRepository.findAllBySubDraftIds(subDraftIds);
    Set<UUID> lineItemIds = extensions.stream().map(PodLineItemsExtension::getPodLineItemId)
        .collect(Collectors.toSet());
    podSubDraftLineItemsByLocationRepository.deleteByPodLineItemIdIn(lineItemIds);
    podSubDraftLineItemRepository.deleteByPodSubDraftIdIn(subDraftIds);
    podLineItemsExtensionRepository.deleteAllBySubDraftIds(subDraftIds);
    podSubDraftRepository.deleteAllByIds(subDraftIds);
  }

  private void mergeLocationsToLot(SubmitPodSubDraftsRequest request) {
    if (ObjectUtils.isEmpty(request.getPodLineItemLocation())) {
      return;
    }
    Map<UUID, List<PodLineItemWithLocationDto>> locationMap = request.getPodLineItemLocation().stream()
        .collect(Collectors.groupingBy(PodLineItemWithLocationDto::getPodLineItemId));
    request.getPodDto().getLineItems().forEach(
        item -> {
          List<PodLineItemWithLocationDto> locationDtos = locationMap.get(item.getId());
          if (ObjectUtils.isEmpty(locationDtos)) {
            throw new IllegalArgumentException("missing locations of line item id : " + item.getId());
          }
          item.setLocations(locationDtos.stream()
              .map(PodLineItemWithLocationDto::toItemLocation).collect(Collectors.toList()));
        }
    );
  }

  private void createAddedLots(ProofOfDeliverySubDraftDto podDto) {
    if (ObjectUtils.isEmpty(podDto.getLineItems())) {
      return;
    }
    List<ProofOfDeliverySubDraftLineItemDto> addedLineItemDto = podDto.getLineItems().stream()
        .filter(ProofOfDeliverySubDraftLineItemDto::isAdded)
        .filter(itemDto -> !(itemDto.getLot() == null || itemDto.getLot().getId() == null))
        .collect(Collectors.toList());
    if (ObjectUtils.isEmpty(addedLineItemDto)) {
      return;
    }
    if (addedLineItemDto.stream().anyMatch(item -> item.getOrderable() == null || item.getOrderable().getId() == null
        || item.getLotCode() == null || item.getExpirationDate() == null)) {
      throw new ValidationMessageException("missing lot info");
    }
    Set<UUID> orderableIds = addedLineItemDto.stream().map(item -> item.getOrderable().getId())
        .collect(Collectors.toSet());
    Map<UUID, OrderableDto> orderableDtos = orderableReferenceDataService.findByIds(orderableIds).stream()
        .collect(Collectors.toMap(OrderableDto::getId, orderableDto -> orderableDto));
    UUID homeFacilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    addedLineItemDto.forEach(item -> {
      OrderableDto orderableDto = orderableDtos.getOrDefault(item.getOrderable().getId(), null);
      if (ObjectUtils.isEmpty(orderableDto)) {
        throw new NotFoundException("Orderable not find: " + item.getOrderable().getId());
      }
      LotDto lotDto = siglusLotService.createNewLotOrReturnExisted(homeFacilityId,
          orderableDto.getTradeItemIdentifier(), item.getLotCode(), item.getExpirationDate());
      item.setLot(new ObjectReferenceDto(lotDto.getId()));
    });
  }

  private void createAddedPodLineItems(ProofOfDelivery pod, SubmitPodSubDraftsRequest request) {
    List<ProofOfDeliveryLineItem> podLineItems = pod.getLineItems();
    Set<UUID> podLineItemIds = podLineItems.stream().map(ProofOfDeliveryLineItem::getId).collect(Collectors.toSet());
    // update pod line items
    List<ProofOfDeliverySubDraftLineItemDto> draftLineItems = request.getPodDto().getLineItems();
    List<ProofOfDeliverySubDraftLineItemDto> podLineItemDrafts = draftLineItems.stream()
        .filter(item -> podLineItemIds.contains(item.getId())).collect(Collectors.toList());
    List<ProofOfDeliveryLineItem> toBeUpdatedLineItems = updateDraftLineItems(
        podLineItemDrafts, podLineItems, PodSubDraftStatusEnum.SUBMITTED);
    podLineItemsRepository.save(toBeUpdatedLineItems);
    // create pod line items
    List<ProofOfDeliverySubDraftLineItemDto> addedDraftLineItemDtos = draftLineItems.stream()
        .filter(item -> !podLineItemIds.contains(item.getId()))
        .collect(Collectors.toList());
    List<ProofOfDeliveryLineItem> addedPodLineItems = podLineItemsRepository.save(addedDraftLineItemDtos.stream()
        .map(ProofOfDeliverySubDraftLineItemDto::toPodLineItem).collect(Collectors.toList()));
    Map<String, ProofOfDeliveryLineItem> lineItemMap = addedPodLineItems.stream()
        .collect(Collectors.toMap(item ->
            buildUniqueKey(item.getOrderable().getId(), item.getOrderable().getVersionNumber(), item.getLotId()),
            item -> item));
    addedDraftLineItemDtos.forEach(
        draftLineItem -> {
          String key = buildUniqueKey(draftLineItem.getOrderable().getId(),
              draftLineItem.getOrderable().getVersionNumber(),
              draftLineItem.getLot().getId());
          ProofOfDeliveryLineItem podLineItem = lineItemMap.get(key);
          draftLineItem.setId(podLineItem.getId());
        }
    );
  }

  private String buildUniqueKey(UUID orderableId, Long version, UUID lotId) {
    return orderableId.toString() + "_" + version + "_" + lotId.toString();
  }

  private void savePodSubDraftLocations(SubmitPodSubDraftsRequest request) {
    if (ObjectUtils.isEmpty(request.getPodDto().getLineItems())) {
      return;
    }
    List<PodLineItemsByLocation> podLocations = request.getPodDto().getLineItems().stream()
        .map(ProofOfDeliverySubDraftLineItemDto::toLineItemByLocation)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    podLineItemsByLocationRepository.save(podLocations);
  }

  private PodExtension getPodExtensionByPodId(UUID podId) {
    Example<PodExtension> example = Example.of(PodExtension.builder().podId(podId).build());
    return podExtensionRepository.findOne(example);
  }

  private void savePodExtension(SubmitPodSubDraftsRequest request) {
    PodExtension podExtension = getPodExtensionByPodId(request.getPodDto().getId());
    if (Objects.isNull(podExtension)) {
      podExtension = PodExtension.builder().podId(request.getPodDto().getId()).build();
    }
    podExtension.setPreparedBy(request.getPreparedBy());
    podExtension.setConferredBy(request.getConferredBy());
    log.info("save pod extension: {}", podExtension);
    podExtensionRepository.save(podExtension);
  }

  public PodPrintInfoResponse getPrintInfo(UUID orderId, UUID podId) {
    OrderDto orderDto = siglusOrdersRepository.findOrderDtoById(orderId);
    UUID realRequisitionId =
        Objects.isNull(orderDto.getRequisitionId()) ? orderDto.getExternalId() : orderDto.getRequisitionId();

    PodPrintInfoResponse response = PodPrintInfoResponse.builder()
        .fileName(getFileName(orderDto, realRequisitionId))
        .client(orderDto.getReceivingFacilityName())
        .supplier(orderDto.getSupplyingFacilityName())
        .receivedBy(orderDto.getPodReceivedBy())
        .deliveredBy(orderDto.getPodDeliveredBy())
        .receivedDate(orderDto.getPodReceivedDate())
        .issueVoucherDate(orderDto.getShippedDate())
        .build();

    setGeographicInfo(response, orderDto);
    setPodExtensionInfo(response, podId);
    setRequisitionInfo(response, realRequisitionId);

    List<PodLineItemDto> podLineItemDtos = podLineItemsRepository.lineItemDtos(podId, orderId, realRequisitionId);
    response.setLineItems(toLineItemInfo(podLineItemDtos));
    return response;
  }

  @Transactional
  public PodSubDraftLineItem createPodSubDraftLineItem(UUID podId, UUID subDraftId, UUID podLineItemId) {
    PodSubDraft subDraft = checkIfPodIdAndSubDraftIdMatch(podId, subDraftId);
    checkIfCanOperate(subDraft);
    PodLineItemsExtension lineItemExtension = podLineItemsExtensionRepository.findByPodLineItemId(podLineItemId);
    if (ObjectUtils.isEmpty(lineItemExtension)) {
      throw new IllegalArgumentException("podLineItemId not exist");
    }
    if (!lineItemExtension.getSubDraftId().equals(subDraftId)) {
      throw new IllegalArgumentException("subDraftId and podLineItemId mismatch");
    }
    ProofOfDeliveryLineItem lineItem = podLineItemsRepository.findOne(podLineItemId);
    PodSubDraftLineItem subDraftLineItem = PodSubDraftLineItem.builder()
        .orderable(lineItem.getOrderable())
        .podSubDraftId(lineItemExtension.getSubDraftId())
        .build();
    return podSubDraftLineItemRepository.save(subDraftLineItem);
  }

  @Transactional
  public void deletePodSubDraftLineItem(UUID podId, UUID subDraftId, UUID podSubDraftLineItemId) {
    PodSubDraft subDraft = checkIfPodIdAndSubDraftIdMatch(podId, subDraftId);
    checkIfCanOperate(subDraft);
    PodSubDraftLineItem draftLineItem = podSubDraftLineItemRepository.findOne(podSubDraftLineItemId);
    if (ObjectUtils.isEmpty(draftLineItem)) {
      throw new IllegalArgumentException("podLineItemId is wrong.");
    }
    if (!subDraftId.equals(draftLineItem.getPodSubDraftId())) {
      throw new IllegalArgumentException("subDraftId and podLineItemId mismatch.");
    }
    podSubDraftLineItemRepository.delete(draftLineItem);
  }

  private void setRequisitionInfo(PodPrintInfoResponse response, UUID realRequisitionId) {
    response.setRequisitionId(realRequisitionId);
    response.setRequisitionNum(siglusRequisitionExtensionService.formatRequisitionNumber(realRequisitionId));

    StatusChange requisitionStatusChange = requisitionStatusChangeRepository.findByRequisitionId(realRequisitionId)
        .stream()
        .filter(e -> RequisitionStatus.IN_APPROVAL == e.getStatus()).findFirst().orElse(null);
    response.setRequisitionDate(
        Objects.nonNull(requisitionStatusChange) ? requisitionStatusChange.getCreatedDate() : null);
  }

  private void setPodExtensionInfo(PodPrintInfoResponse response, UUID podId) {
    PodExtension podExtension = getPodExtensionByPodId(podId);
    if (Objects.nonNull(podExtension)) {
      response.setPreparedBy(podExtension.getPreparedBy());
      response.setConferredBy(podExtension.getConferredBy());
    }
  }

  private void setGeographicInfo(PodPrintInfoResponse response, OrderDto orderDto) {
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
  }

  private String getFileName(OrderDto orderDto, UUID realRequisitionId) {
    int requisitionCount = getRequisitionCount(orderDto, realRequisitionId);

    StringBuilder fileName = new StringBuilder()
        .append(Boolean.TRUE.equals(orderDto.getEmergency()) ? FILE_NAME_PREFIX_EMERGENCY
            : PROGRAM_CODE_TO_FILE_PREFIX_NORMAL.get(orderDto.getProgramCode()))
        .append(orderDto.getReceivingFacilityCode()).append(DOT)
        .append(dateFormat.format(orderDto.getPeriodEndDate())).append(DOT)
        .append(formatNumber(String.valueOf(requisitionCount)));

    List<String> splitStrings = Arrays.asList(orderDto.getOrderCode().split(SHORT_LINE));
    if (splitStrings.size() > 2) {
      fileName.append(UNDERSCORE)
          .append(formatNumber(splitStrings.get(splitStrings.size() - 1)));
    } else {
      fileName.append(UNDERSCORE)
          .append(formatNumber("1"));
    }

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

  private String formatNumber(String number) {
    if (number.length() >= 2) {
      return number;
    }
    return "0" + number;
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

  private List<ProofOfDeliveryLineItem> updateDraftLineItems(List<ProofOfDeliverySubDraftLineItemDto> draftLineItems,
      List<ProofOfDeliveryLineItem> lineItems, PodSubDraftStatusEnum subDraftStatus) {
    Map<UUID, ProofOfDeliverySubDraftLineItemDto> draftLineItemDtoMap = draftLineItems.stream()
        .collect(Collectors.toMap(ProofOfDeliverySubDraftLineItemDto::getId, e -> e));
    ProofOfDeliveryLineItem resetItem = new ProofOfDeliveryLineItem(null,
        null, null, null, null, null, null);
    lineItems.forEach(lineItem -> {
      if (PodSubDraftStatusEnum.NOT_YET_STARTED == subDraftStatus) {
        lineItem.updateFrom(resetItem);
      } else {
        ProofOfDeliverySubDraftLineItemDto draftLineItem = draftLineItemDtoMap.getOrDefault(lineItem.getId(), null);
        if (ObjectUtils.isEmpty(draftLineItem)) {
          lineItem.updateFrom(resetItem);
        } else {
          ProofOfDeliveryLineItem updatedItem = new ProofOfDeliveryLineItem(null,
              null, draftLineItem.getQuantityAccepted(), null,
              draftLineItem.getQuantityRejected(), draftLineItem.getRejectionReasonId(), draftLineItem.getNotes());
          lineItem.updateFrom(updatedItem);
        }
      }
    });
    return lineItems;
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

  private PodSubDraft checkIfPodIdAndSubDraftIdMatch(UUID podId, UUID subDraftId) {
    PodSubDraft podSubDraft = getPodSubDraft(subDraftId);
    if (!podSubDraft.getPodId().equals(podId)) {
      throw new BusinessDataException(new Message(ERROR_POD_ID_SUB_DRAFT_ID_NOT_MATCH), subDraftId);
    }
    return podSubDraft;
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

  private void updatePodSubDraft(UUID subDraftId, OperateTypeEnum operateType,
      ProofOfDeliverySubDraftDto draftDto, List<PodLineItemWithLocationDto> draftLocations) {
    PodSubDraft podSubDraft = getPodSubDraft(subDraftId);
    checkIfCanOperate(podSubDraft);
    Set<UUID> podLineItemIds = getPodLineItemIdsBySubDraftId(subDraftId);
    List<ProofOfDeliveryLineItem> podLineItems = podLineItemsRepository.findAll(podLineItemIds);
    // update pod line items
    List<ProofOfDeliverySubDraftLineItemDto> draftLineItems = draftDto.getLineItems();
    List<ProofOfDeliverySubDraftLineItemDto> podLineItemDrafts = draftLineItems.stream()
        .filter(item -> podLineItemIds.contains(item.getId())).collect(Collectors.toList());
    List<ProofOfDeliveryLineItem> toBeUpdatedLineItems = updateDraftLineItems(
        podLineItemDrafts, podLineItems, PodSubDraftStatusEnum.getPodSubDraftEnum(operateType));
    podLineItemsRepository.save(toBeUpdatedLineItems);
    List<PodLineItemWithLocationDto> podLineItemLocations = new ArrayList<>();
    if (!ObjectUtils.isEmpty(draftLocations)) {
      podLineItemLocations = draftLocations.stream()
          .filter(location -> podLineItemIds.contains(location.getPodLineItemId())).collect(Collectors.toList());
      draftLocations.removeAll(podLineItemLocations);
    }
    updatePodSubDraftLocation(podLineItemLocations);

    // update sub draft line items
    draftLineItems.removeAll(podLineItemDrafts);
    if (!ObjectUtils.isEmpty(draftLineItems)) {
      updateSubDraftLineItem(draftLineItems, draftLocations);
    }

    // update sub draft
    updateSubDraftStatusAndOperator(podSubDraft, PodSubDraftStatusEnum.getPodSubDraftEnum(operateType));
  }

  private void updateSubDraftLineItem(List<ProofOfDeliverySubDraftLineItemDto> lineItems,
                                      List<PodLineItemWithLocationDto> locations) {
    Map<UUID, List<PodLineItemWithLocationDto>> locationMap = locations.stream()
        .collect(Collectors.groupingBy(PodLineItemWithLocationDto::getPodLineItemId));
    List<PodSubDraftLineItem> draftLineItems = lineItems.stream().map(
        item -> {
          List<PodSubDraftLineItemLocation> itemLocations = locationMap.getOrDefault(item.getId(), new ArrayList<>())
              .stream().map(PodLineItemWithLocationDto::toItemLocation).collect(Collectors.toList());
          return item.toDraftLineItem(itemLocations);
        }
    ).collect(Collectors.toList());
    podSubDraftLineItemRepository.save(draftLineItems);
  }

  private void deletePodSubDraft(UUID podId, UUID subDraftId) {
    PodSubDraft podSubDraft = getPodSubDraft(subDraftId);
    checkIfCanOperate(podSubDraft);

    Set<UUID> lineItemIds = getPodLineItemIdsBySubDraftId(subDraftId);
    ProofOfDeliveryDto proofOfDeliveryDto = getPodDtoByPodId(podId);
    resetLineItems(lineItemIds, proofOfDeliveryDto);

    updateSubDraftStatusAndOperator(podSubDraft, PodSubDraftStatusEnum.NOT_YET_STARTED);
    podSubDraftLineItemRepository.deleteByPodSubDraftId(subDraftId);
  }

  private ProofOfDeliverySubDraftDto getPodSubDraftDto(UUID podId, UUID subDraftId, Set<String> expand) {
    checkIfPodIdAndSubDraftIdMatch(podId, subDraftId);
    ProofOfDeliveryDto dto = getExpandedPodDtoById(podId, expand);
    List<ProofOfDeliveryLineItemDto> currentSubDraftLineItems = getCurrentSubDraftPodLineItemDtos(subDraftId, dto);
    dto.setLineItems(currentSubDraftLineItems);
    ProofOfDeliverySubDraftDto draftDto = ProofOfDeliverySubDraftDto.from(dto, subDraftId);
    List<ProofOfDeliverySubDraftLineItemDto> addedDraftLineItemDtos =
        podSubDraftLineItemRepository.findAllByPodSubDraftId(subDraftId)
            .stream().map(ProofOfDeliverySubDraftLineItemDto::fromDraftLineItem)
            .peek(item -> item.setAdded(true))
            .collect(Collectors.toList());
    draftDto.getLineItems().addAll(addedDraftLineItemDtos);
    return draftDto;
  }

  private ProofOfDeliverySubDraftDto getPodSubDraftsDto(UUID podId, List<UUID> subDraftIds, Set<String> expand) {
    ProofOfDeliveryDto dto = getExpandedPodDtoById(podId, expand);
    List<PodLineItemsExtension> extensions = podLineItemsExtensionRepository.findAllBySubDraftIds(subDraftIds);
    ProofOfDeliverySubDraftDto draftDto = ProofOfDeliverySubDraftDto.from(dto, extensions);
    List<ProofOfDeliverySubDraftLineItemDto> addedDraftLineItemDtos =
        podSubDraftLineItemRepository.findAllByPodSubDraftIdIn(subDraftIds)
            .stream().map(ProofOfDeliverySubDraftLineItemDto::fromDraftLineItem)
            .peek(item -> item.setAdded(true))
            .collect(Collectors.toList());
    draftDto.getLineItems().addAll(addedDraftLineItemDtos);
    return draftDto;
  }

  private void resetPodSubDrafts(UUID podId) {
    checkAuth();
    Set<UUID> subDraftIds = getPodSubDraftsByPodId(podId).stream().map(PodSubDraft::getId).collect(Collectors.toSet());
    ProofOfDeliveryDto proofOfDeliveryDto = getPodDtoByPodId(podId);
    Set<UUID> lineItemIds = findLineItemsIdsByPodId(podId, subDraftIds);
    resetLineItems(lineItemIds, proofOfDeliveryDto);
    deleteSubDraftAndLineExtensionBySubDraftIds(subDraftIds);
    podSubDraftLineItemRepository.deleteByPodSubDraftIdIn(new ArrayList<>(subDraftIds));
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
    podSubDraftLineItemRepository.flush();
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

  private List<PodLineItemWithLocationDto> getLineItemsWithLocation(List<UUID> lineItemIds) {
    List<PodLineItemWithLocationDto> podLineItemWithLocationDtos = Lists.newArrayList();
    List<PodLineItemsByLocation> podLineItemsByLocationList = podLineItemsByLocationRepository
        .findByPodLineItemIdIn(lineItemIds);
    podLineItemsByLocationList.forEach(lineItemsWithLocation -> {
      PodLineItemWithLocationDto podLineItemWithLocationDto = new PodLineItemWithLocationDto();
      podLineItemWithLocationDto.setPodLineItemId(lineItemsWithLocation.getPodLineItemId());
      podLineItemWithLocationDto.setLocationCode(lineItemsWithLocation.getLocationCode());
      podLineItemWithLocationDto.setArea(lineItemsWithLocation.getArea());
      podLineItemWithLocationDto.setQuantityAccepted(lineItemsWithLocation.getQuantityAccepted());
      podLineItemWithLocationDtos.add(podLineItemWithLocationDto);
    });
    return podLineItemWithLocationDtos;
  }

  public ProofOfDeliveryWithLocationResponse getPodWithLocation(ProofOfDeliveryDto podDto) {
    ProofOfDeliveryWithLocationResponse podWithLocationDto = new ProofOfDeliveryWithLocationResponse();
    PodExtensionResponse podExtensionResponse = new PodExtensionResponse();
    podExtensionResponse.setPodDto(podDto);
    PodExtension podExtension = getPodExtensionByPodId(podDto.getId());
    if (Objects.nonNull(podExtension)) {
      podExtensionResponse.setPreparedBy(podExtension.getPreparedBy());
      podExtensionResponse.setConferredBy(podExtension.getConferredBy());
    }
    podWithLocationDto.setPodExtension(podExtensionResponse);
    List<UUID> podLineItemIds = podExtensionResponse.getPodDto().getLineItems().stream()
        .map(Importer::getId).collect(Collectors.toList());
    List<PodLineItemWithLocationDto> lineItemsWithLocation;
    if (ProofOfDeliveryStatus.CONFIRMED.equals(podDto.getStatus())) {
      lineItemsWithLocation = getLineItemsWithLocation(podLineItemIds);
    } else {
      lineItemsWithLocation = getSubDraftLineItemsWithLocation(podLineItemIds);
    }
    podWithLocationDto.setPodLineItemLocation(CollectionUtils.isEmpty(lineItemsWithLocation)
        ? Lists.newArrayList() : lineItemsWithLocation);
    return podWithLocationDto;
  }

  private StockEventDto createStockEventDto(ProofOfDelivery pod, SubmitPodSubDraftsRequest request) {
    org.openlmis.fulfillment.web.stockmanagement.StockEventDto fulfillmentStockEventDto = stockEventBuilder
        .fromProofOfDelivery(pod);

    Map<String, ProofOfDeliverySubDraftLineItemDto> draftLineItemMap = request.getPodDto().getLineItems().stream()
        .collect(Collectors.toMap(
            item -> getOrderableLotIdPair(item.getOrderable().getId(), item.getLot().getId()), item -> item
        ));
    List<StockEventLineItemDto> lineItems = fulfillmentStockEventDto.getLineItems().stream().map(lineItemEventDto -> {
      String newKey = getOrderableLotIdPair(lineItemEventDto.getOrderableId(), lineItemEventDto.getLotId());
      ProofOfDeliverySubDraftLineItemDto draftLineItemDto = draftLineItemMap.get(newKey);
      if (ObjectUtils.isEmpty(draftLineItemDto)) {
        return new ArrayList<StockEventLineItemDto>();
      }
      return draftLineItemDto.getLocations().stream().map(
          location -> {
            StockEventLineItemDto stockEventLineItemDto = new StockEventLineItemDto();
            BeanUtils.copyProperties(lineItemEventDto, stockEventLineItemDto);
            stockEventLineItemDto.setQuantity(location.getQuantityAccepted());
            stockEventLineItemDto.setLocationCode(location.getLocationCode());
            stockEventLineItemDto.setArea(location.getArea());
            return stockEventLineItemDto;
          }
      ).collect(Collectors.toList());
    })
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    StockEventDto stockEventDto = new StockEventDto();
    BeanUtils.copyProperties(fulfillmentStockEventDto, stockEventDto);
    stockEventDto.setLineItems(lineItems);
    return stockEventDto;
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
