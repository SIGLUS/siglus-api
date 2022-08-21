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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_CARDS_VIEW;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.PaginationConstants.DEFAULT_PAGE_NUMBER;
import static org.siglus.siglusapi.constant.PaginationConstants.NO_PAGINATION;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND;
import static org.siglus.siglusapi.i18n.PermissionMessageKeys.ERROR_NO_FOLLOWING_PERMISSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.service.LotSearchParams;
import org.openlmis.referencedata.web.LotController;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.referencedata.PermissionStringDto;
import org.openlmis.requisition.utils.Pagination;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.service.StockCardSummaries;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.openlmis.stockmanagement.util.Message;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummariesV2DtoBuilder;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.repository.ProgramOrderableRepository;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.siglus.siglusapi.domain.PhysicalInventorySubDraft;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.LotLocationSohDto;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.siglus.siglusapi.dto.StockCardDetailsDto;
import org.siglus.siglusapi.dto.StockCardDetailsWithLocationDto;
import org.siglus.siglusapi.dto.StockCardSummaryDto;
import org.siglus.siglusapi.dto.StockCardSummaryWithLocationDto;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.repository.PhysicalInventorySubDraftRepository;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusStockCardStockManagementService;
import org.siglus.siglusapi.util.FormatHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
@SuppressWarnings({"PMD"})
public class SiglusStockCardSummariesService {

  private static final String PROGRAM_ID = "programId";
  private static final String EXCLUDE_ARCHIVED = "excludeArchived";
  private static final String ARCHIVED_ONLY = "archivedOnly";
  private static final String NON_EMPTY_ONLY = "nonEmptyOnly";

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  SiglusStockCardStockManagementService siglusStockManagementService;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private SiglusArchiveProductService archiveProductService;

  @Autowired
  private StockCardSummariesService stockCardSummariesService;

  @Autowired
  private StockCardSummariesV2DtoBuilder stockCardSummariesV2DtoBuilder;

  @Autowired
  private ProgramOrderableRepository programOrderableRepository;

  @Autowired
  private PhysicalInventorySubDraftRepository physicalInventorySubDraftRepository;

  @Autowired
  private PhysicalInventoryLineItemsExtensionRepository lineItemsExtensionRepository;

  @Autowired
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Autowired
  private SiglusLotReferenceDataService siglusLotReferenceDataService;

  @Autowired
  private SiglusOrderableService siglusOrderableService;

  @Autowired
  private OrderableRepository orderableRepository;

  @Autowired
  private LotController lotController;

  @Autowired
  private CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;

  public List<org.openlmis.referencedata.dto.LotDto> getLotsDataByOrderableIds(List<UUID> orderableIds) {
    if (CollectionUtils.isEmpty(orderableIds)) {
      return Collections.emptyList();
    }
    Page<Orderable> orderablePage = orderableRepository.findAllLatestByIds(orderableIds,
        new PageRequest(DEFAULT_PAGE_NUMBER, NO_PAGINATION));
    Set<UUID> tradeItemIds = orderablePage.getContent()
        .stream()
        .map(Orderable::getTradeItemIdentifier)
        .map(UUID::fromString)
        .collect(Collectors.toSet());
    LotSearchParams requestParams = new LotSearchParams(null, new ArrayList<>(tradeItemIds), null, null);

    return lotController.getLots(requestParams, null).getContent();
  }

  public Page<StockCardSummaryV2Dto> findSiglusStockCard(
      MultiValueMap<String, String> parameters, List<UUID> subDraftIds, Pageable pageable) {
    log.info("findSiglusStockCard subDraftIds=" + Optional.ofNullable(subDraftIds));
    UUID userId = authenticationHelper.getCurrentUser().getId();
    Set<String> archivedProducts = null;
    if (Boolean.parseBoolean(parameters.getFirst(EXCLUDE_ARCHIVED)) || Boolean
        .parseBoolean(parameters.getFirst(ARCHIVED_ONLY))) {
      archivedProducts = archiveProductService
          .searchArchivedProductsByFacilityId(UUID.fromString(parameters.getFirst(FACILITY_ID)));
    }
    StockCardSummariesV2SearchParams v2SearchParams = new StockCardSummariesV2SearchParams(
        parameters);
    List<UUID> orderableIds = v2SearchParams.getOrderableIds();
    UUID inputProgramId = getId(PROGRAM_ID, parameters);
    Set<UUID> programIds = getProgramIds(inputProgramId, userId, parameters.getFirst(RIGHT_NAME),
        parameters.getFirst(FACILITY_ID));
    List<StockCardSummaryV2Dto> summaryV2Dtos = new ArrayList<>();
    for (UUID programId : programIds) {
      v2SearchParams.setProgramId(programId);
      if (archiveEmptyForArchiveOnly(parameters, archivedProducts)) {
        return Pagination.getPage(Collections.emptyList(), pageable);
      } else if (needFilterOrSearchAllProgram(orderableIds, parameters, archivedProducts,
          programIds)) {
        // revert below code after V3 upgraded
        getSummaries(parameters, archivedProducts, v2SearchParams, orderableIds, summaryV2Dtos);
      } else {
        // revert below code after V3 upgraded
        if (programOrderableRepository.countByProgramId(v2SearchParams.getProgramId()) > 0) {
          StockCardSummaries summaries = stockCardSummariesService.findStockCards(v2SearchParams);
          summaryV2Dtos = stockCardSummariesV2DtoBuilder.build(
              summaries.getPageOfApprovedProducts(),
              summaries.getStockCardsForFulfillOrderables(),
              summaries.getOrderableFulfillMap(),
              v2SearchParams.isNonEmptyOnly());
        }
      }
    }
    if (CollectionUtils.isNotEmpty(subDraftIds)) {
      summaryV2Dtos = filterBySubDraftIds(summaryV2Dtos, subDraftIds);
    }
    return Pagination.getPage(summaryV2Dtos, pageable);
  }

  private List<StockCardSummaryV2Dto> filterBySubDraftIds(List<StockCardSummaryV2Dto> summaryV2Dtos,
      List<UUID> subDraftIds) {
    List<PhysicalInventoryLineItemsExtension> orderables = filterLineItemsExtensionBySubDraftIds(
        subDraftIds);

    Set<UUID> orderableIds = orderables.stream()
        .map(PhysicalInventoryLineItemsExtension::getOrderableId)
        .collect(Collectors.toSet());

    log.info("filterBySubDraftIds orderableIds=" + orderableIds);

    return summaryV2Dtos.stream()
        .filter(item -> !orderableIds.contains(item.getOrderable().getId()))
        .collect(Collectors.toList());
  }

  public List<PhysicalInventoryLineItemsExtension> filterLineItemsExtensionBySubDraftIds(
      List<UUID> subDraftIds) {
    List<PhysicalInventoryLineItemsExtension> lineItemsExtensions = new ArrayList<>();
    for (UUID subDraftId : subDraftIds) {
      lineItemsExtensions.addAll(filterExistLineItemsExtensionBySubDraftId(subDraftId));
    }
    return lineItemsExtensions;
  }

  public List<PhysicalInventoryLineItemsExtension> filterExistLineItemsExtensionBySubDraftId(
      UUID subDraftId) {
    if (subDraftId == null) {
      return Collections.emptyList();
    }
    PhysicalInventorySubDraft subDraft = physicalInventorySubDraftRepository.findOne(subDraftId);
    List<PhysicalInventorySubDraft> physicalInventoryList =
        physicalInventorySubDraftRepository
            .findByPhysicalInventoryId(subDraft.getPhysicalInventoryId());
    List<UUID> subDraftUuids = physicalInventoryList.stream().map(PhysicalInventorySubDraft::getId)
        .collect(Collectors.toList());
    List<PhysicalInventoryLineItemsExtension> physicalInventoryLineItemsExtensions =
        lineItemsExtensionRepository.findBySubDraftIdIn(subDraftUuids);
    return physicalInventoryLineItemsExtensions.stream()
        // exclude current searching subDraftId
        .filter(item -> !item.getSubDraftId().equals(subDraftId))
        .collect(Collectors.toList());
  }

  public Set<UUID> getProgramIds(UUID programId, UUID userId, String rightName,
      String facilityId) {
    Set<UUID> programIds = newHashSet();
    Set<PermissionStringDto> permissionStrings = permissionService
        .getPermissionStrings(userId).get();
    if (ALL_PRODUCTS_PROGRAM_ID.equals(programId)) {
      Set<UUID> programsByRight = permissionStrings
          .stream()
          .filter(permissionStringDto -> permissionStringDto.getRightName().equals(rightName)
              && UUID.fromString(facilityId).equals(permissionStringDto.getFacilityId())
          )
          .map(PermissionStringDto::getProgramId)
          .collect(Collectors.toSet());
      if (CollectionUtils.isEmpty(programsByRight)) {
        throw new PermissionMessageException(
            new Message(ERROR_NO_FOLLOWING_PERMISSION, rightName, facilityId));
      }
      return programsByRight;
    }

    programIds.add(programId);

    return programIds;
  }

  public List<StockCardSummaryV2Dto> findAllProgramStockSummaries() {
    MultiValueMap<String, String> valueMap = new LinkedMultiValueMap<>();
    valueMap.set(PROGRAM_ID, ALL_PRODUCTS_PROGRAM_ID.toString());
    valueMap.set(RIGHT_NAME, STOCK_CARDS_VIEW);
    valueMap.set(NON_EMPTY_ONLY, Boolean.TRUE.toString());
    valueMap.set(FACILITY_ID, authenticationHelper.getCurrentUser().getHomeFacilityId().toString());
    Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);
    return findSiglusStockCard(valueMap, null, pageable).getContent();
  }

  private void getSummaries(MultiValueMap<String, String> parameters, Set<String> archivedProducts,
      StockCardSummariesV2SearchParams v2SearchParams, List<UUID> orderableIds,
      List<StockCardSummaryV2Dto> summaryV2Dtos) {
    // revert below code after V3 upgraded
    List<StockCardSummaryV2Dto> summaries = newArrayList();
    if (programOrderableRepository.countByProgramId(v2SearchParams.getProgramId()) > 0) {
      StockCardSummaries stockCardSummaries = stockCardSummariesService
          .findStockCards(v2SearchParams);
      summaries = stockCardSummariesV2DtoBuilder.build(
          stockCardSummaries.getPageOfApprovedProducts(),
          stockCardSummaries.getStockCardsForFulfillOrderables(),
          stockCardSummaries.getOrderableFulfillMap(),
          v2SearchParams.isNonEmptyOnly());
    }
    summaries = filterByOrderableIds(orderableIds, summaries);
    if (Boolean.parseBoolean(parameters.getFirst(EXCLUDE_ARCHIVED))) {
      summaryV2Dtos.addAll(summaries.stream()
          .filter(isNotArchived(archivedProducts))
          .collect(Collectors.toList()));
    } else if (Boolean.parseBoolean(parameters.getFirst(ARCHIVED_ONLY))) {
      summaryV2Dtos.addAll(summaries.stream()
          .filter(isArchived(archivedProducts))
          .collect(Collectors.toList()));
    } else {
      summaryV2Dtos.addAll(summaries);
    }
  }

  private boolean archiveEmptyForArchiveOnly(MultiValueMap<String, String> parameters,
      Set<String> archivedProducts) {
    return CollectionUtils.isEmpty(archivedProducts)
        && Boolean.parseBoolean(parameters.getFirst(ARCHIVED_ONLY));
  }

  private boolean needFilterOrSearchAllProgram(List<UUID> orderableIds,
      MultiValueMap<String, String> parameters,
      Set<String> archivedProducts, Set<UUID> programIds) {
    return !CollectionUtils.isEmpty(orderableIds)
        || filterForArchive(parameters, archivedProducts)
        || programIds.size() > 1;
  }

  private boolean filterForArchive(MultiValueMap<String, String> parameters,
      Set<String> archiveProducts) {
    return !CollectionUtils.isEmpty(archiveProducts)
        && (Boolean.parseBoolean(parameters.getFirst(ARCHIVED_ONLY))
        || Boolean.parseBoolean(parameters.getFirst(EXCLUDE_ARCHIVED)));
  }

  private List<StockCardSummaryV2Dto> filterByOrderableIds(List<UUID> orderableIds,
      List<StockCardSummaryV2Dto> summaries) {
    if (CollectionUtils.isEmpty(orderableIds)) {
      return summaries;
    }
    return summaries.stream().filter(summaryV2Dto ->
            orderableIds.contains(summaryV2Dto.getOrderable().getId()))
        .collect(Collectors.toList());
  }

  public Page<StockCardSummaryV2Dto> searchStockCardSummaryV2Dtos(
      MultiValueMap<String, String> parameters, List<UUID> subDraftIds, UUID draftId,
      Pageable pageable) {
    try {
      if (draftId != null) {
        Page<StockCardSummaryV2Dto> stockCards = findSiglusStockCard(parameters, subDraftIds,
            pageable);
        StockManagementDraft foundDraft = stockManagementDraftRepository.findOne(draftId);
        if (foundDraft == null) {
          throw new ResourceNotFoundException(
              new org.openlmis.stockmanagement.util.Message(
                  ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND,
                  draftId));
        }
        List<StockManagementDraft> drafts = stockManagementDraftRepository
            .findByInitialDraftId(foundDraft.getInitialDraftId());

        drafts.remove(foundDraft);

        Set<UUID> existOrderableIds = drafts.stream().flatMap(
            draft -> draft.getLineItems().stream().map(StockManagementDraftLineItem::getOrderableId))
            .collect(Collectors.toSet());

        return Pagination.getPage(stockCards.getContent().stream()
            .filter(stockCard -> !existOrderableIds.contains(stockCard.getOrderable().getId()))
            .collect(Collectors.toList()), pageable);
      }
      // reason: support all program && archive
      return findSiglusStockCard(parameters, subDraftIds, pageable);

    } catch (PermissionMessageException e) {
      if (parameters.getFirst(RIGHT_NAME).equals(STOCK_INVENTORIES_EDIT)) {
        throw new PermissionMessageException(
            new org.openlmis.stockmanagement.util.Message(ERROR_PERMISSION_NOT_SUPPORTED));
      }

      throw e;
    }
  }

  private Predicate<StockCardSummaryV2Dto> isArchived(Set<String> archivedProducts) {
    return stockCard -> archivedProducts.contains(stockCard.getOrderable().getId().toString());
  }

  private Predicate<StockCardSummaryV2Dto> isNotArchived(Set<String> archivedProducts) {
    return stockCard -> !archivedProducts.contains(stockCard.getOrderable().getId().toString());
  }

  private UUID getId(String fieldName, MultiValueMap<String, String> parameters) {
    String id = parameters.getFirst(fieldName);
    return FormatHelper.formatId(id, fieldName);
  }

  public List<List<StockCardDetailsDto>> getStockCardDetailsDtoByGroup(MultiValueMap<String, String> parameters,
      List<UUID> subDraftIds, UUID draftId, Pageable pageable) {

    List<StockCardSummaryDto> stockCardSummaryDtos =  getStockCardSummaryDtos(parameters,
        subDraftIds, draftId, pageable);

    return getFulfillForMe(stockCardSummaryDtos);
  }

  public List<StockCardSummaryDto> getStockCardSummaryDtos(MultiValueMap<String, String> parameters,
      List<UUID> subDraftIds, UUID draftId, Pageable pageable) {
    List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos = getStockCardSummaryV2Dtos(parameters, subDraftIds, draftId,
        pageable);

    List<UUID> orderableIds = new ArrayList<>();
    List<CanFulfillForMeEntryDto> canFulfillForMeEntryDtos = getCanFulfillForMeEntryDtos(
        stockCardSummaryV2Dtos, orderableIds);

    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    List<OrderableDto> orderableDtos = getOrderableDtos(pageable, orderableIds, facilityId);
    List<LotDto> lotDtos = getLotDtos(canFulfillForMeEntryDtos);

    return combineResponse(stockCardSummaryV2Dtos, orderableDtos, lotDtos);
  }

  public List<StockCardSummaryWithLocationDto> getStockCardSummaryWithLocationDtos(
      MultiValueMap<String, String> parameters,
      List<UUID> subDraftIds, UUID draftId, Pageable pageable) {
    List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos = getStockCardSummaryV2Dtos(parameters, subDraftIds, draftId,
        pageable);

    List<UUID> orderableIds = new ArrayList<>();
    List<CanFulfillForMeEntryDto> canFulfillForMeEntryDtos = getCanFulfillForMeEntryDtos(
        stockCardSummaryV2Dtos, orderableIds);

    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    List<OrderableDto> orderableDtos = getOrderableDtos(pageable, orderableIds, facilityId);
    List<LotDto> lotDtos = getLotDtos(canFulfillForMeEntryDtos);
    List<UUID> lotIds = lotDtos.stream().map(LotDto::getId).collect(Collectors.toList());
    List<LotLocationSohDto> locationSoh = calculatedStockOnHandByLocationRepository.getLocationSoh(lotIds);
    return combineResponse(stockCardSummaryV2Dtos, orderableDtos, lotDtos, locationSoh);
  }

  private List<LotDto> getLotDtos(List<CanFulfillForMeEntryDto> canFulfillForMeEntryDtos) {
    List<UUID> lotIds = canFulfillForMeEntryDtos.stream()
        .filter(canFulfillForMeEntryDto -> Objects.nonNull(canFulfillForMeEntryDto.getLot()))
        .map(canFulfillForMeEntryDto -> canFulfillForMeEntryDto.getLot().getId()).collect(Collectors.toList());

    return siglusLotReferenceDataService.findByIds(lotIds);
  }

  private List<OrderableDto> getOrderableDtos(Pageable pageable, List<UUID> orderableIds, UUID facilityId) {
    QueryOrderableSearchParams searchParams = new QueryOrderableSearchParams(new LinkedMultiValueMap());
    HashSet orderableIdSet = new HashSet(orderableIds);
    searchParams.setIds(orderableIdSet);
    return siglusOrderableService.searchOrderables(searchParams, pageable, facilityId).getContent();
  }

  private List<CanFulfillForMeEntryDto> getCanFulfillForMeEntryDtos(List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      List<UUID> orderableIds) {
    stockCardSummaryV2Dtos
        .forEach(stockCardSummaryV2Dto -> orderableIds.add(stockCardSummaryV2Dto.getOrderable().getId()));

    List<CanFulfillForMeEntryDto> canFulfillForMeEntryDtos = stockCardSummaryV2Dtos.stream()
        .map(StockCardSummaryV2Dto::getCanFulfillForMe)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    canFulfillForMeEntryDtos.stream()
        .filter(canFulfillForMeEntryDto -> Objects.nonNull(canFulfillForMeEntryDto.getOrderable()))
        .forEach(canFulfillForMeEntryDto -> orderableIds.add(canFulfillForMeEntryDto.getOrderable().getId()));
    return canFulfillForMeEntryDtos;
  }

  private List<StockCardSummaryV2Dto> getStockCardSummaryV2Dtos(MultiValueMap<String, String> parameters,
      List<UUID> subDraftIds, UUID draftId, Pageable pageable) {
    return searchStockCardSummaryV2Dtos(parameters, subDraftIds, draftId, pageable).getContent();
  }

  private List<StockCardSummaryDto> combineResponse(List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      List<OrderableDto> orderableDtos, List<LotDto> lotDtos) {
    List<StockCardSummaryDto> stockCardSummaryDtos = new ArrayList<>();

    stockCardSummaryV2Dtos.forEach(stockCardSummaryV2Dto -> {
      StockCardSummaryDto stockCardSummaryDto = new StockCardSummaryDto();
      OrderableDto orderableDto = getOrderableFromObjectReference(orderableDtos, stockCardSummaryV2Dto.getOrderable());
      stockCardSummaryDto.setOrderable(orderableDto);
      stockCardSummaryDto.setStockOnHand(stockCardSummaryV2Dto.getStockOnHand());

      Set<StockCardDetailsDto> stockCardDetailsDtos = new HashSet<>();

      stockCardSummaryV2Dto.getCanFulfillForMe().forEach(canFulfillForMeEntryDto -> {
        StockCardDetailsDto fulfill = StockCardDetailsDto.builder()
            .orderable(getOrderableFromObjectReference(orderableDtos, canFulfillForMeEntryDto.getOrderable()))
            .lot(getLotFromObjectReference(lotDtos, canFulfillForMeEntryDto.getLot()))
            .occurredDate(canFulfillForMeEntryDto.getOccurredDate())
            .stockOnHand(canFulfillForMeEntryDto.getStockOnHand())
            .processedDate(canFulfillForMeEntryDto.getProcessedDate())
            .stockCard(canFulfillForMeEntryDto.getStockCard())
            .build();
        stockCardDetailsDtos.add(fulfill);
      });
      stockCardSummaryDto.setStockCardDetails(stockCardDetailsDtos);
      stockCardSummaryDtos.add(stockCardSummaryDto);
    });
    return stockCardSummaryDtos;
  }

  private List<StockCardSummaryWithLocationDto> combineResponse(List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      List<OrderableDto> orderableDtos, List<LotDto> lotDtos, List<LotLocationSohDto> lotLocationSohDtoList) {
    List<StockCardSummaryWithLocationDto> stockCardSummaryDtos = new ArrayList<>();

    stockCardSummaryV2Dtos.forEach(stockCardSummaryV2Dto -> {
      StockCardSummaryWithLocationDto stockCardSummaryDto = new StockCardSummaryWithLocationDto();
      OrderableDto orderableDto = getOrderableFromObjectReference(orderableDtos, stockCardSummaryV2Dto.getOrderable());
      stockCardSummaryDto.setOrderable(orderableDto);
      stockCardSummaryDto.setStockOnHand(stockCardSummaryV2Dto.getStockOnHand());

      Set<StockCardDetailsWithLocationDto> stockCardDetailsDtos = new HashSet<>();
      Map<UUID, List<LotLocationSohDto>> lotLocationMaps = lotLocationSohDtoList.stream()
          .collect(Collectors.groupingBy(LotLocationSohDto::getLotId));
      stockCardSummaryV2Dto.getCanFulfillForMe().forEach(canFulfillForMeEntryDto -> {
        StockCardDetailsWithLocationDto fulfill = StockCardDetailsWithLocationDto.builder()
            .lotLocationSohDtoList(lotLocationMaps.isEmpty() || canFulfillForMeEntryDto.getLot() == null ? null
                : lotLocationMaps.get(canFulfillForMeEntryDto.getLot().getId()))
            .orderable(getOrderableFromObjectReference(orderableDtos, canFulfillForMeEntryDto.getOrderable()))
            .lot(getLotFromObjectReference(lotDtos, canFulfillForMeEntryDto.getLot()))
            .occurredDate(canFulfillForMeEntryDto.getOccurredDate())
            .stockOnHand(canFulfillForMeEntryDto.getStockOnHand())
            .processedDate(canFulfillForMeEntryDto.getProcessedDate())
            .stockCard(canFulfillForMeEntryDto.getStockCard())
            .build();
        stockCardDetailsDtos.add(fulfill);
      });
      stockCardSummaryDto.setStockCardDetails(stockCardDetailsDtos);
      stockCardSummaryDtos.add(stockCardSummaryDto);
    });
    return stockCardSummaryDtos;
  }


  private OrderableDto getOrderableFromObjectReference(List<OrderableDto> orderableDtos,
      ObjectReferenceDto objectReferenceDto) {
    if (Objects.nonNull(objectReferenceDto)) {
      return orderableDtos.stream().filter(dto ->
          dto.getId().equals(objectReferenceDto.getId())).findFirst().orElse(null);
    }
    return null;
  }

  private LotDto getLotFromObjectReference(List<LotDto> lotDtos, ObjectReferenceDto objectReferenceDto) {
    if (Objects.nonNull(objectReferenceDto)) {
      return lotDtos.stream().filter(dto ->
          dto.getId().equals(objectReferenceDto.getId())).findFirst().orElse(null);
    }
    return null;
  }

  private List<List<StockCardDetailsDto>> getFulfillForMe(List<StockCardSummaryDto> stockCardSummaryDtos) {
    Set<StockCardDetailsDto> stockCardDetailsDtos = getStockCardDetailsDtos(stockCardSummaryDtos);
    Map<UUID, List<StockCardDetailsDto>> orderableIdFulfillMap = stockCardDetailsDtos.stream().collect(
        Collectors.groupingBy(stockCardDetailsDto -> stockCardDetailsDto.getOrderable().getId()));

    List<List<StockCardDetailsDto>> lists = new ArrayList<>();
    Set<Entry<UUID, List<StockCardDetailsDto>>> entries = orderableIdFulfillMap.entrySet();
    entries.forEach(entrie -> {
      ArrayList<StockCardDetailsDto> fulfill = new ArrayList<>(entrie.getValue());
      lists.add(fulfill);
    });
    return lists;
  }

  private Set<StockCardDetailsDto> getStockCardDetailsDtos(List<StockCardSummaryDto> stockCardSummaryDtos) {
    Set<StockCardDetailsDto> stockCardDetailsDtos = new HashSet<>();
    stockCardSummaryDtos.forEach(stockCardSummaryDto ->
        stockCardDetailsDtos.addAll(stockCardSummaryDto.getStockCardDetails()));
    return stockCardDetailsDtos;
  }
}
