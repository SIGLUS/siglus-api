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

import static java.util.Comparator.comparing;
import static org.siglus.siglusapi.constant.CacheConstants.CACHE_KEY_GENERATOR;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_PROGRAM_ORDERABLES;
import static org.siglus.siglusapi.constant.FieldConstants.CODE;
import static org.siglus.siglusapi.constant.FieldConstants.FULL_PRODUCT_NAME;
import static org.siglus.siglusapi.constant.FieldConstants.PRODUCT_CODE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_NOT_FOUND;

import com.google.common.collect.Lists;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.dto.DispensableDto;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.util.Message;
import org.openlmis.stockmanagement.web.Pagination;
import org.siglus.common.constant.KitConstants;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.repository.ArchivedProductRepository;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.siglusapi.constant.PaginationConstants;
import org.siglus.siglusapi.domain.DispensableAttributes;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.siglus.siglusapi.dto.SimplifyOrderablesDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.DispensableAttributesRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.ProgramOrderablesRepository;
import org.siglus.siglusapi.repository.SiglusOrderableRepository;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
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
  private final CalculatedStockOnHandRepository calculatedStockOnHandRepository;
  private final OrderableRepository orderableRepository;
  private final DispensableAttributesRepository dispensableAttributesRepository;

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

  public List<SimplifyOrderablesDto> searchOrderablesDropDownList(UUID draftId) {
    List<OrderableDto> allProducts = getAllProducts();
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    Set<String> archivedProducts = archivedProductRepository
        .findArchivedProductsByFacilityId(facilityId);
    List<UUID> archivedProductIds = archivedProducts.stream().map(UUID::fromString).collect(Collectors.toList());
    allProducts = allProducts.stream()
        .filter(e -> !archivedProductIds.contains(e.getId()))
        .collect(Collectors.toList());
    if (draftId != null) {
      Set<UUID> existOrderableIds = getExistOrderablesIdByDraftId(draftId);
      allProducts = allProducts.stream()
          .filter(e -> !existOrderableIds.contains(e.getId()))
          .collect(Collectors.toList());
    }
    return allProducts.stream()
        .map(SimplifyOrderablesDto::from)
        .sorted(Comparator.comparing(
            e -> e.getFullProductName().trim(), Comparator.nullsLast(String::compareTo)))
        .collect(Collectors.toList());
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

  public Map<UUID, String> getAllProductIdToCode() {
    return getAllProducts().stream()
        .collect(Collectors.toMap(OrderableDto::getId, OrderableDto::getProductCode, (a, b) -> a));
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

  public List<SimplifyOrderablesDto> getAvailableOrderablesByFacility(Boolean isRequestAll, UUID draftId) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    List<StockCard> stockCards = stockCardRepository.findByFacilityIdIn(facilityId);
    List<UUID> allStockCardIds = stockCards.stream().map(StockCard::getId).collect(Collectors.toList());
    List<CalculatedStockOnHand> sohForOrderable = calculatedStockOnHandRepository.findLatestStockOnHands(
        allStockCardIds, ZonedDateTime.now());
    List<UUID> stockCardIds;
    if (isRequestAll) {
      stockCardIds = sohForOrderable.stream()
          .map(CalculatedStockOnHand::getStockCardId)
          .collect(Collectors.toList());
    } else {
      stockCardIds = sohForOrderable.stream()
          .filter(soh -> soh.getStockOnHand() > 0)
          .map(CalculatedStockOnHand::getStockCardId)
          .collect(Collectors.toList());
    }

    List<StockCard> satisfiedStockCards = Lists.newArrayList();
    for (StockCard stockCard : stockCards) {
      if (stockCardIds.contains(stockCard.getId())) {
        satisfiedStockCards.add(stockCard);
      }
    }

    if (draftId != null) {
      Set<UUID> existOrderableIds = getExistOrderablesIdByDraftId(draftId);
      satisfiedStockCards.removeIf(e -> existOrderableIds.contains(e.getOrderableId()));
    }

    Set<UUID> orderableIds = satisfiedStockCards.stream().map(StockCard::getOrderableId).collect(Collectors.toSet());
    List<Orderable> orderables = orderableRepository.findLatestByIds(orderableIds);
    List<ProgramOrderableDto> programOrderables = programOrderablesRepository.findAllMaxVersionProgramOrderableDtos();
    Map<UUID, UUID> orderableIdToProgramIdMap = programOrderables.stream()
        .filter(programOrderable -> orderableIds.contains(programOrderable.getOrderableId()))
        .collect(Collectors.toMap(ProgramOrderableDto::getOrderableId, ProgramOrderableDto::getProgramId));

    Map<UUID, DispensableAttributes> orderableIdToDispensable = new HashMap<>();
    Map<UUID, UUID> orderableIdToDispensableIdMap = orderables.stream()
        .collect(Collectors.toMap(Orderable::getId, orderable -> orderable.getDispensable().getId()));
    List<DispensableAttributes> dispensableAttributes = dispensableAttributesRepository
        .findAll(orderableIdToDispensableIdMap.values());
    orderableIdToDispensableIdMap.forEach((orderableId, dispensableId) -> orderableIdToDispensable
        .put(orderableId, dispensableAttributes.stream()
        .filter(dispensable -> dispensable.getDispensableId().equals(dispensableId))
        .findFirst().orElse(null)));

    List<SimplifyOrderablesDto> simplifyOrderablesDtoList = new ArrayList<>();
    orderables.forEach(orderable -> {
      SimplifyOrderablesDto simplifyOrderablesDto = new SimplifyOrderablesDto();
      simplifyOrderablesDto.setOrderableId(orderable.getId());
      simplifyOrderablesDto.setProductCode(orderable.getProductCode().toString());
      simplifyOrderablesDto.setFullProductName(orderable.getFullProductName());
      simplifyOrderablesDto.setProgramId(orderableIdToProgramIdMap.get(orderable.getId()));
      simplifyOrderablesDto.setIsKit(KitConstants.isKit(orderable.getProductCode().toString()));
      DispensableDto dispensable = new DispensableDto(orderableIdToDispensable.get(orderable.getId()).getValue(),
          null, null, orderableIdToDispensable.get(orderable.getId()).getValue());
      simplifyOrderablesDto.setDispensable(dispensable);
      simplifyOrderablesDtoList.add(simplifyOrderablesDto);
    });
    return simplifyOrderablesDtoList.stream().sorted(comparing(SimplifyOrderablesDto::getFullProductName)).collect(
        Collectors.toList());
  }

  private Set<UUID> getExistOrderablesIdByDraftId(UUID draftId) {
    StockManagementDraft foundDraft = stockManagementDraftRepository.findOne(draftId);
    if (foundDraft == null) {
      throw new ResourceNotFoundException(
          new Message(
              ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND,
              draftId));
    }
    List<StockManagementDraft> drafts = stockManagementDraftRepository
        .findByInitialDraftId(foundDraft.getInitialDraftId());

    drafts.remove(foundDraft);

    return drafts.stream()
        .flatMap(draft -> draft.getLineItems().stream().map(StockManagementDraftLineItem::getOrderableId))
        .collect(Collectors.toSet());
  }

}
