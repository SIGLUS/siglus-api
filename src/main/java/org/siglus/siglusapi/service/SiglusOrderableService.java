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
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.web.Pagination;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.repository.ArchivedProductRepository;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.siglusapi.constant.PaginationConstants;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.siglus.siglusapi.repository.SiglusOrderableRepository;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
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

  public Page<OrderableDto> searchDeduplicatedOrderables(UUID draftId,
      QueryOrderableSearchParams searchParams,
      Pageable pageable, UUID facilityId) {
    StockManagementDraft foundDraft = stockManagementDraftRepository.findOne(draftId);
    if (foundDraft == null) {
      throw new ResourceNotFoundException(
          new org.openlmis.stockmanagement.util.Message(ERROR_STOCK_MANAGEMENT_DRAFT_ID_NOT_FOUND,
              draftId));
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

}
