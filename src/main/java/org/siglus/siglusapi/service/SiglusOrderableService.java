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

import static org.siglus.siglusapi.constant.CacheConstants.CACHE_KEY_GENERATOR;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_PROGRAM_ORDERABLES;
import static org.siglus.siglusapi.constant.FieldConstants.CODE;
import static org.siglus.siglusapi.constant.FieldConstants.FULL_PRODUCT_NAME;
import static org.siglus.siglusapi.constant.FieldConstants.MONTHLY;
import static org.siglus.siglusapi.constant.FieldConstants.MONTHLY_REPORT_ONLY;
import static org.siglus.siglusapi.constant.FieldConstants.MONTHLY_SUBMIT_PRODUCT_ZERO_INVENTORY_MONTH_RANGE;
import static org.siglus.siglusapi.constant.FieldConstants.PRODUCT_CODE;
import static org.siglus.siglusapi.constant.FieldConstants.QUARTERLY;
import static org.siglus.siglusapi.constant.FieldConstants.QUARTERLY_REPORT_ONLY;
import static org.siglus.siglusapi.constant.FieldConstants.QUARTERLY_SUBMIT_PRODUCT_ZERO_INVENTORY_MONTH_RANGE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_NOT_FOUND;

import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.web.Pagination;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.repository.ArchivedProductRepository;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.siglusapi.constant.PaginationConstants;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.dto.DisplayedLotDto;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.ProgramOrderablesRepository;
import org.siglus.siglusapi.repository.SiglusOrderableRepository;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.repository.dto.FacilityProgramPeriodScheduleDto;
import org.siglus.siglusapi.repository.dto.ProgramOrderableDto;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.cache.annotation.Cacheable;
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
  private final ProgramOrderablesRepository programOrderablesRepository;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final StockCardRepository stockCardRepository;
  private final FacilityNativeRepository facilityNativeRepository;
  private final CalculatedStockOnHandRepository calculatedStocksOnHandRepository;

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

    Set<UUID> existOrderableIds = drafts.stream().flatMap(
        draft -> draft.getLineItems().stream()
            .map(StockManagementDraftLineItem::getOrderableId))
        .collect(Collectors.toSet());

    Set<UUID> orderableIds = searchParams.getIds();

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

  @Cacheable(value = SIGLUS_PROGRAM_ORDERABLES, keyGenerator = CACHE_KEY_GENERATOR)
  public List<ProgramOrderableDto> getAllProgramOrderableDtos() {
    return programOrderablesRepository.findAllMaxVersionProgramOrderableDtos();
  }

  public List<DisplayedLotDto> searchDisplayedLots(List<UUID> orderableIds) {

    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();

    Map<UUID, FacilityProgramPeriodScheduleDto> programIdToSchedulesCode = Maps.uniqueIndex(
        facilityNativeRepository.findFacilityProgramPeriodScheduleByFacilityId(
            facilityId), FacilityProgramPeriodScheduleDto::getProgramId);

    List<DisplayedLotDto> displayedLotDtos = new LinkedList<>();
    orderableIds.forEach(orderableId -> {
      List<StockCard> stockCardList = stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId);
      List<UUID> lotIds = getDisplayedLotIds(programIdToSchedulesCode, stockCardList);
      displayedLotDtos.add(
          DisplayedLotDto
              .builder()
              .orderableId(orderableId)
              .lotIds(lotIds)
              .build()
      );
    });

    return displayedLotDtos;
  }

  private List<UUID> getDisplayedLotIds(Map<UUID, FacilityProgramPeriodScheduleDto> programIdToSchedulesCode,
      List<StockCard> stockCardList) {
    List<UUID> lotIds = new LinkedList<>();
    stockCardList.forEach(stockCard -> {
      String schedulesCode = programIdToSchedulesCode.get(stockCard.getProgramId()).getSchedulesCode();
      List<CalculatedStockOnHand> calculatedStockOnHands = calculatedStocksOnHandRepository
          .findByStockCardIdInAndOccurredDateLessThanEqual(
              Collections.singletonList(stockCard.getId()), LocalDate.now());

      Optional<CalculatedStockOnHand> recentlyCalculatedStockOnHand = calculatedStockOnHands.stream()
          .max(Comparator.comparing(CalculatedStockOnHand::getOccurredDate));
      if (!recentlyCalculatedStockOnHand.isPresent()) {
        return;
      }
      Integer stockonhand = recentlyCalculatedStockOnHand.get().getStockOnHand();
      if (stockonhand > 0) {
        lotIds.add(stockCard.getLotId());
      } else {
        if ((schedulesCode.equals(MONTHLY) || schedulesCode.equals(MONTHLY_REPORT_ONLY))
            && hasSohInMouthRange(calculatedStockOnHands, MONTHLY_SUBMIT_PRODUCT_ZERO_INVENTORY_MONTH_RANGE)) {
          lotIds.add(stockCard.getLotId());
        }
        if ((schedulesCode.equals(QUARTERLY) || schedulesCode.equals(QUARTERLY_REPORT_ONLY))
            && hasSohInMouthRange(calculatedStockOnHands, QUARTERLY_SUBMIT_PRODUCT_ZERO_INVENTORY_MONTH_RANGE)) {
          lotIds.add(stockCard.getLotId());
        }
      }

    });
    return lotIds;
  }

  private boolean hasSohInMouthRange(List<CalculatedStockOnHand> calculatedStockOnHandList, int monthRange) {
    return calculatedStockOnHandList
        .stream().filter(o -> o.getOccurredDate()
            .isAfter(LocalDate.now().minusMonths(monthRange)))
        .anyMatch(o -> o.getStockOnHand() != 0);
  }

}
