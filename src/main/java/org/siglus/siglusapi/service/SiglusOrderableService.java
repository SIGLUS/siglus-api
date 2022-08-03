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

import static org.siglus.siglusapi.constant.FieldConstants.CODE;
import static org.siglus.siglusapi.constant.FieldConstants.FULL_PRODUCT_NAME;
import static org.siglus.siglusapi.constant.FieldConstants.PRODUCT_CODE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_NOT_FOUND;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.web.Pagination;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.repository.ArchivedProductRepository;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.siglusapi.constant.PaginationConstants;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.dto.AssembledOrderableAndLotDto;
import org.siglus.siglusapi.dto.CanFulfillForMeEntryNewDto;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.SiglusOrderableRepository;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@RequiredArgsConstructor
public class SiglusOrderableService {

  private final SiglusOrderableReferenceDataService orderableReferenceDataService;
  private final SiglusOrderableRepository siglusOrderableRepository;
  private final ProgramAdditionalOrderableRepository programAdditionalOrderableRepository;
  private final ArchivedProductRepository archivedProductRepository;
  private final StockManagementDraftRepository stockManagementDraftRepository;
  private final SiglusStockCardSummariesService stockCardSummariesSiglusService;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final SiglusLotReferenceDataService siglusLotReferenceDataService;

  public Page<OrderableDto> searchOrderables(QueryOrderableSearchParams searchParams,
      Pageable pageable, UUID facilityId) {
    Page<OrderableDto> orderableDtoPage = orderableReferenceDataService
        .searchOrderables(searchParams, pageable);
    Set<String> archivedProducts = archivedProductRepository
        .findArchivedProductsByFacilityId(facilityId);
    orderableDtoPage.getContent().forEach(orderableDto -> orderableDto
        .setArchived(archivedProducts.contains(orderableDto.getId().toString())));
    return orderableDtoPage;
  }

  //TODO: delete after stockCardSummary and orderable is ok
  public Page<OrderableDto> searchDeduplicatedOrderables(UUID draftId,
      QueryOrderableSearchParams searchParams,
      Pageable pageable, UUID facilityId) {
    StockManagementDraft foundDraft = stockManagementDraftRepository.findOne(draftId);
    if (foundDraft == null) {
      throw new NotFoundException(ERROR_STOCK_MANAGEMENT_DRAFT_NOT_FOUND);
    }
    UUID initialDraftId = foundDraft.getInitialDraftId();

    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByInitialDraftId(initialDraftId);

    drafts.remove(foundDraft);

    Set<String> existOrderableIds = drafts.stream().flatMap(
        draft -> draft.getLineItems().stream()
            .map(lineItem -> lineItem.getOrderableId().toString()))
        .collect(Collectors.toSet());

    Set<String> orderableIds = searchParams.getIds().stream().map(UUID::toString)
        .collect(Collectors.toSet());

    orderableIds.removeAll(existOrderableIds);

    searchParams.clearIds();
    searchParams.setIds(orderableIds);

    Page<OrderableDto> orderableDtoPage = orderableReferenceDataService
        .searchOrderables(searchParams, pageable);
    Set<String> archivedProducts = archivedProductRepository
        .findArchivedProductsByFacilityId(facilityId);
    orderableDtoPage.getContent().forEach(orderableDto -> orderableDto
        .setArchived(archivedProducts.contains(orderableDto.getId().toString())));
    return orderableDtoPage;
  }

  public List<OrderableExpirationDateDto> getOrderableExpirationDate(Set<UUID> orderableIds,
      UUID facilityId) {
    return siglusOrderableRepository.findExpirationDate(orderableIds, facilityId);
  }

  public Page<OrderableDto> additionalToAdd(UUID programId, QueryOrderableSearchParams searchParams,
      Pageable pageable) {
    Pageable noPagination = new PageRequest(PaginationConstants.DEFAULT_PAGE_NUMBER,
        PaginationConstants.NO_PAGINATION, pageable.getSort());
    List<OrderableDto> orderableDtos = orderableReferenceDataService
        .searchOrderables(searchParams, noPagination)
        .getContent();
    Set<UUID> additionalOrderableIds = programAdditionalOrderableRepository
        .findAllByProgramId(programId)
        .stream()
        .map(ProgramAdditionalOrderable::getAdditionalOrderableId)
        .collect(Collectors.toSet());
    orderableDtos = orderableDtos.stream()
        .filter(orderableDto -> {
          if (!CollectionUtils.isEmpty(orderableDto.getPrograms())) {
            return !programId.equals(orderableDto.getPrograms().stream().findFirst().get()
                .getProgramId());
          }
          return false;
        })
        .filter(orderableDto -> !additionalOrderableIds.contains(orderableDto.getId()))
        .collect(Collectors.toList());
    if (null == pageable.getSort()) {
      return Pagination.getPage(orderableDtos, pageable);
    }
    if (pageable.getSort().toString().contains(FULL_PRODUCT_NAME)) {
      orderableDtos = orderableDtos.stream()
          .sorted(Comparator.comparing(OrderableDto::getFullProductName))
          .collect(Collectors.toList());
    }
    if (pageable.getSort().toString().contains(PRODUCT_CODE)) {
      orderableDtos = orderableDtos.stream()
          .sorted(Comparator.comparing(OrderableDto::getProductCode))
          .collect(Collectors.toList());
    }
    return Pagination.getPage(orderableDtos, pageable);
  }

  public OrderableDto getOrderableByCode(String productCode) {
    Pageable noPagination = new PageRequest(PaginationConstants.DEFAULT_PAGE_NUMBER,
        PaginationConstants.NO_PAGINATION);
    MultiValueMap<String, Object> queryParams = new LinkedMultiValueMap<>();
    queryParams.set(CODE, productCode);
    QueryOrderableSearchParams searchParams = new QueryOrderableSearchParams(queryParams);
    return orderableReferenceDataService.searchOrderables(searchParams, noPagination).getContent()
        .stream()
        .filter(orderableDto -> productCode.equals(orderableDto.getProductCode()))
        .findFirst()
        .orElse(null);
  }

  public List<OrderableDto> getAllProducts() {
    QueryOrderableSearchParams params = new QueryOrderableSearchParams(new LinkedMultiValueMap<>());
    Pageable pageable = new PageRequest(PaginationConstants.DEFAULT_PAGE_NUMBER,
        PaginationConstants.NO_PAGINATION);
    return orderableReferenceDataService.searchOrderables(params, pageable).getContent();
  }

  public List<List<CanFulfillForMeEntryNewDto>> assembleStockCardAndOrderable(
      MultiValueMap<String, String> parameters,
      List<UUID> subDraftIds,
      UUID draftId,
      Pageable pageable) {
    Page<StockCardSummaryV2Dto> stockCardSummaryV2DtosPage = stockCardSummariesSiglusService
        .searchStockCardSummaryV2Dtos(parameters, subDraftIds, draftId, pageable);
    List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos = stockCardSummaryV2DtosPage.getContent();

    List<String> orderableIds = new ArrayList<>();

    stockCardSummaryV2Dtos
        .forEach(stockCardSummaryV2Dto -> orderableIds.add(stockCardSummaryV2Dto.getOrderable().getId().toString()));

    List<CanFulfillForMeEntryDto> canFulfillForMeEntryDtos = stockCardSummaryV2Dtos.stream()
        .map(StockCardSummaryV2Dto::getCanFulfillForMe)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    canFulfillForMeEntryDtos.stream()
        .filter(canFulfillForMeEntryDto -> Objects.nonNull(canFulfillForMeEntryDto.getOrderable()))
        .forEach(
            canFulfillForMeEntryDto -> orderableIds.add(canFulfillForMeEntryDto.getOrderable().getId().toString()));

    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    QueryOrderableSearchParams searchParams = new QueryOrderableSearchParams(new LinkedMultiValueMap());
    Set<String> strings = new HashSet(orderableIds);
    searchParams.setIds(strings);
    List<OrderableDto> orderableDtos = searchOrderables(searchParams, pageable, facilityId).getContent();

    List<UUID> lotIds = canFulfillForMeEntryDtos.stream()
        .filter(canFulfillForMeEntryDto -> Objects.nonNull(canFulfillForMeEntryDto.getLot()))
        .map(canFulfillForMeEntryDto -> canFulfillForMeEntryDto.getLot().getId()).collect(Collectors.toList());

    List<LotDto> lotDtos = siglusLotReferenceDataService.findByIds(lotIds);

    List<AssembledOrderableAndLotDto> assembledOrderableAndLotDtos = combineResponse(stockCardSummaryV2Dtos,
        orderableDtos, lotDtos);

    return getFulfillForMe(assembledOrderableAndLotDtos);
  }

  public List<AssembledOrderableAndLotDto> combineResponse(List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      List<OrderableDto> orderableDtos, List<LotDto> lotDtos) {
    List<AssembledOrderableAndLotDto> assembledOrderableAndLotDtos = new ArrayList<>();

    stockCardSummaryV2Dtos.forEach(stockCardSummaryV2Dto -> {
      AssembledOrderableAndLotDto assembledOrderableAndLotDto = new AssembledOrderableAndLotDto();

      OrderableDto orderableDto = getOrderableFromObjectReference(orderableDtos, stockCardSummaryV2Dto.getOrderable());

      assembledOrderableAndLotDto.setOrderable(orderableDto);

      assembledOrderableAndLotDto.setStockOnHand(stockCardSummaryV2Dto.getStockOnHand());

      Set<CanFulfillForMeEntryNewDto> canFulfillForMeEntryNewDtos = new HashSet<>();

      stockCardSummaryV2Dto.getCanFulfillForMe().forEach(canFulfillForMeEntryDto -> {
        CanFulfillForMeEntryNewDto fulfill = CanFulfillForMeEntryNewDto.builder()
            .orderable(getOrderableFromObjectReference(orderableDtos, canFulfillForMeEntryDto.getOrderable()))
            .lot(getLotFromObjectReference(lotDtos, canFulfillForMeEntryDto.getLot()))
            .occurredDate(canFulfillForMeEntryDto.getOccurredDate())
            .stockOnHand(canFulfillForMeEntryDto.getStockOnHand())
            .processedDate(canFulfillForMeEntryDto.getProcessedDate())
            .stockCard(canFulfillForMeEntryDto.getStockCard())
            .build();
        canFulfillForMeEntryNewDtos.add(fulfill);
      });
      assembledOrderableAndLotDto.setCanFulfillForMe(canFulfillForMeEntryNewDtos);
      assembledOrderableAndLotDtos.add(assembledOrderableAndLotDto);
    });
    return assembledOrderableAndLotDtos;
  }

  private OrderableDto getOrderableFromObjectReference(List<OrderableDto> orderableDtos,
      ObjectReferenceDto objectReferenceDto) {
    if (Objects.nonNull(objectReferenceDto)) {
      return orderableDtos.stream().filter(dto -> {
        return dto.getId().equals(objectReferenceDto.getId());
      }).findFirst().orElse(null);
    }
    return null;
  }

  private LotDto getLotFromObjectReference(List<LotDto> lotDtos, ObjectReferenceDto objectReferenceDto) {
    if (Objects.nonNull(objectReferenceDto)) {
      return lotDtos.stream().filter(dto -> {
        return dto.getId().equals(objectReferenceDto.getId());
      }).findFirst().orElse(null);
    }
    return null;
  }

  private List<List<CanFulfillForMeEntryNewDto>> getFulfillForMe(
      List<AssembledOrderableAndLotDto> assembledOrderableAndLotDtos) {
    Set<CanFulfillForMeEntryNewDto> canFulfillForMeEntryNewDtos = new HashSet<>();
    assembledOrderableAndLotDtos.forEach(assembledOrderableAndLotDto -> {
      canFulfillForMeEntryNewDtos.addAll(assembledOrderableAndLotDto.getCanFulfillForMe());
    });
    Map<UUID, List<CanFulfillForMeEntryNewDto>> orderableIdFulfillMap = canFulfillForMeEntryNewDtos.stream().collect(
        Collectors.groupingBy(canFulfillForMeEntryNewDto -> canFulfillForMeEntryNewDto.getOrderable().getId()));

    List<List<CanFulfillForMeEntryNewDto>> lists = new ArrayList<>();
    Set<Entry<UUID, List<CanFulfillForMeEntryNewDto>>> entries = orderableIdFulfillMap.entrySet();
    entries.forEach(entrie -> {
      ArrayList<CanFulfillForMeEntryNewDto> fulfill = new ArrayList<>(entrie.getValue());
      lists.add(fulfill);
    });
    return lists;
  }
}
